package com.example.data.nfe

import com.example.data.model.Category
import com.example.data.model.ParsedReceiptData
import com.example.data.model.ParsedReceiptItem
import com.example.data.model.Product
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

object ReceiptParser {

    fun parseNfeXml(
        xmlContent: String,
        existingProducts: List<Product>,
        categories: List<Category>
    ): ParsedReceiptData {
        val factory = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }
        val parser = factory.newPullParser()
        parser.setInput(StringReader(xmlContent))

        var storeName = "Supermercado"
        var invoiceNumber = ""
        val rawItems = mutableListOf<ParsedReceiptItem>()

        var insideEmit = false
        var insideIde = false
        var insideProd = false
        var currentTag = ""

        var xProd = ""
        var cProd = ""
        var cEan = ""
        var uCom = "un"
        var qCom = 1.0
        var vUnCom = 0.0
        var vProd = 0.0

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    val name = parser.name ?: ""
                    currentTag = name
                    when (name) {
                        "emit" -> insideEmit = true
                        "ide" -> insideIde = true
                        "prod" -> {
                            insideProd = true
                            xProd = ""
                            cProd = ""
                            cEan = ""
                            uCom = "un"
                            qCom = 1.0
                            vUnCom = 0.0
                            vProd = 0.0
                        }
                    }
                }
                XmlPullParser.TEXT -> {
                    val text = parser.text?.trim().orEmpty()
                    if (text.isNotEmpty()) {
                        when {
                            insideEmit && currentTag == "xNome" && storeName == "Supermercado" -> storeName = text
                            insideEmit && currentTag == "xFant" -> storeName = text
                            insideIde && currentTag == "nNF" -> invoiceNumber = text
                            insideProd -> when (currentTag) {
                                "xProd" -> xProd = text
                                "cProd" -> cProd = text
                                "cEAN" -> if (text != "SEM GTIN") cEan = text
                                "uCom" -> uCom = normalizeUnit(text)
                                "qCom" -> qCom = text.replace(',', '.').toDoubleOrNull() ?: 1.0
                                "vUnCom" -> vUnCom = text.replace(',', '.').toDoubleOrNull() ?: 0.0
                                "vProd" -> vProd = text.replace(',', '.').toDoubleOrNull() ?: 0.0
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when (parser.name) {
                        "emit" -> insideEmit = false
                        "ide" -> insideIde = false
                        "prod" -> {
                            insideProd = false
                            if (xProd.isNotBlank()) {
                                val unitPrice = if (vUnCom > 0.0) vUnCom else if (qCom > 0.0) vProd / qCom else 0.0
                                val barcode = cEan.ifBlank { cProd }
                                rawItems += buildParsedItem(
                                    name = xProd,
                                    barcode = barcode,
                                    quantity = if (qCom > 0.0) qCom else 1.0,
                                    unit = uCom,
                                    unitPrice = unitPrice,
                                    existingProducts = existingProducts,
                                    categories = categories
                                )
                            }
                        }
                    }
                    currentTag = ""
                }
            }
            eventType = parser.next()
        }

        val notes = if (invoiceNumber.isNotBlank()) {
            "Nota Fiscal #$invoiceNumber (Arquivo XML)"
        } else {
            "Cupom importado via Arquivo XML"
        }

        return ParsedReceiptData(
            storeName = storeName.take(120),
            notes = notes.take(300),
            sourceLabel = "Arquivo XML",
            items = rawItems
        )
    }

    fun parseReceiptText(
        text: String,
        existingProducts: List<Product>,
        categories: List<Category>,
        sourceLabel: String = "Foto / OCR"
    ): ParsedReceiptData {
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val storeName = lines.take(5).firstOrNull { line ->
            line.length >= 3 && !Regex("CNPJ|CUPOM|EXTRATO|NOTA FISCAL|DANFE", RegexOption.IGNORE_CASE)
                .containsMatchIn(line)
        } ?: "Mercado / Estabelecimento"

        val skipPattern = Regex(
            "EXTRATO|CNPJ|TRIBUTOS|VALOR APROX|FORMA DE PAGAMENTO|DINHEIRO|CART[AÃ]O|TROCO|CHAVE DE ACESSO|CONSULTA PELA CHAVE|PROTOCOLO|TOTAL",
            RegexOption.IGNORE_CASE
        )
        val fullLineRegex = Regex(
            """^(?:(\d+)\s+)?(?:(\d{8,14})\s+)?(.+?)\s+([0-9.,]+)\s*(KG|UN|PC|CX|LT|G|ML)?\s*[xX*]\s*([0-9.,]+)(?:\s*[=\s]+([0-9.,]+))?$""",
            RegexOption.IGNORE_CASE
        )
        val calcOnlyRegex = Regex(
            """^([0-9.,]+)\s*(KG|UN|PC|CX|LT|G|ML)?\s*[xX*]\s*([0-9.,]+)(?:\s*[=\s]+([0-9.,]+))?$""",
            RegexOption.IGNORE_CASE
        )
        val simplePriceRegex = Regex(
            """^(?:(\d{8,14})\s+)?([A-Za-zÀ-ÿ0-9\s\-./()]{3,60}?)\s+R?\$?\s*([0-9]+,[0-9]{2})$"""
        )

        val parsedItems = mutableListOf<ParsedReceiptItem>()
        var pendingName: String? = null
        var pendingBarcode: String? = null

        for (line in lines) {
            if (skipPattern.containsMatchIn(line)) continue

            val fullMatch = fullLineRegex.matchEntire(line)
            if (fullMatch != null) {
                val barcode = fullMatch.groupValues[2]
                val name = fullMatch.groupValues[3].replace(Regex("""^\d+\s+"""), "").trim()
                val qty = parsePtBrDecimal(fullMatch.groupValues[4]).takeIf { it > 0.0 } ?: 1.0
                val unit = normalizeUnit(fullMatch.groupValues[5].ifBlank { "un" })
                val unitPrice = parsePtBrDecimal(fullMatch.groupValues[6])
                if (name.isNotBlank()) {
                    parsedItems += buildParsedItem(
                        name = name,
                        barcode = barcode,
                        quantity = qty,
                        unit = unit,
                        unitPrice = unitPrice,
                        existingProducts = existingProducts,
                        categories = categories
                    )
                }
                pendingName = null
                pendingBarcode = null
                continue
            }

            val calcMatch = calcOnlyRegex.matchEntire(line)
            if (calcMatch != null && !pendingName.isNullOrBlank()) {
                val qty = parsePtBrDecimal(calcMatch.groupValues[1]).takeIf { it > 0.0 } ?: 1.0
                val unit = normalizeUnit(calcMatch.groupValues[2].ifBlank { "un" })
                val unitPrice = parsePtBrDecimal(calcMatch.groupValues[3])
                parsedItems += buildParsedItem(
                    name = pendingName,
                    barcode = pendingBarcode.orEmpty(),
                    quantity = qty,
                    unit = unit,
                    unitPrice = unitPrice,
                    existingProducts = existingProducts,
                    categories = categories
                )
                pendingName = null
                pendingBarcode = null
                continue
            }

            val simpleMatch = simplePriceRegex.matchEntire(line)
            if (simpleMatch != null) {
                val barcode = simpleMatch.groupValues[1]
                val name = simpleMatch.groupValues[2].trim()
                val price = parsePtBrDecimal(simpleMatch.groupValues[3])
                if (name.length >= 2 && price > 0.0) {
                    parsedItems += buildParsedItem(
                        name = name,
                        barcode = barcode,
                        quantity = 1.0,
                        unit = "un",
                        unitPrice = price,
                        existingProducts = existingProducts,
                        categories = categories
                    )
                    pendingName = null
                    pendingBarcode = null
                    continue
                }
            }

            val barcodeLine = Regex("""^(\d{8,14})\s+(.{3,})$""").matchEntire(line)
            if (barcodeLine != null) {
                pendingBarcode = barcodeLine.groupValues[1]
                pendingName = barcodeLine.groupValues[2].trim()
            } else if (line.length >= 3 && line.any { it.isLetter() }) {
                pendingName = line
                pendingBarcode = null
            }
        }

        return ParsedReceiptData(
            storeName = storeName.take(120),
            notes = "Cupom importado via $sourceLabel",
            sourceLabel = sourceLabel,
            items = parsedItems
        )
    }

    suspend fun parseSefazUrl(
        url: String,
        existingProducts: List<Product>,
        categories: List<Category>
    ): Result<ParsedReceiptData> = withContext(Dispatchers.IO) {
        try {
            val trimmed = url.trim()
            require(trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
                "Informe uma URL válida começando com http:// ou https://"
            }
            val conn = (URL(trimmed).openConnection() as HttpURLConnection).apply {
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("User-Agent", "Mozilla/5.0 (Android) AstroGestao/1.0")
            }
            val code = conn.responseCode
            if (code !in 200..299) {
                return@withContext Result.failure(
                    IllegalStateException("Portal da SEFAZ retornou código $code. Tente importar via Foto ou Arquivo XML.")
                )
            }
            val html = conn.inputStream.bufferedReader().use { it.readText() }
            val parsed = parseSefazHtml(html, existingProducts, categories)
            if (parsed.items.isEmpty()) {
                Result.failure(
                    IllegalStateException("Não foi possível extrair itens deste link da SEFAZ. Tente usar Foto/OCR ou Arquivo XML.")
                )
            } else {
                Result.success(parsed)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun parseSefazHtml(
        html: String,
        existingProducts: List<Product>,
        categories: List<Category>
    ): ParsedReceiptData {
        val storeMatch = Regex(
            """<(?:div|span|h[1-6])[^>]*(?:class=["'][^"']*(?:txtTopo|nomeFantasia|txtTit|nome)[^"']*["']|id=["']u20["'])[^>]*>(.*?)</(?:div|span|h[1-6])>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)
        val storeName = storeMatch?.groupValues?.get(1)?.let(::stripHtml)?.ifBlank { null }
            ?: "Supermercado"

        val rowMatches = Regex(
            """<tr[^>]*id=["']Item[^"']*["'][^>]*>(.*?)</tr>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).findAll(html)

        val items = mutableListOf<ParsedReceiptItem>()
        for (row in rowMatches) {
            val rowHtml = row.groupValues[1]
            val name = extractClassText(rowHtml, "txtTit") ?: continue
            val code = extractClassText(rowHtml, "RCod").orEmpty()
                .replace(Regex("""[^\d]"""), "")
            val qtyStr = extractClassText(rowHtml, "Rqtd")
            val unitStr = extractClassText(rowHtml, "RUN") ?: "un"
            val unitPriceStr = extractClassText(rowHtml, "RvlUnit")
            val qty = qtyStr?.let(::parsePtBrDecimal)?.takeIf { it > 0.0 } ?: 1.0
            val unitPrice = unitPriceStr?.let(::parsePtBrDecimal) ?: 0.0

            items += buildParsedItem(
                name = name,
                barcode = if (code.length >= 8) code else "",
                quantity = qty,
                unit = normalizeUnit(unitStr),
                unitPrice = unitPrice,
                existingProducts = existingProducts,
                categories = categories
            )
        }

        return ParsedReceiptData(
            storeName = storeName.take(120),
            notes = "Cupom importado via Link SEFAZ",
            sourceLabel = "Link SEFAZ",
            items = items
        )
    }

    private fun extractClassText(html: String, className: String): String? {
        val regex = Regex(
            """class=["'][^"']*\b$className\b[^"']*["'][^>]*>(.*?)</(?:span|div|td)>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return regex.find(html)?.groupValues?.get(1)?.let(::stripHtml)?.ifBlank { null }
    }

    private fun stripHtml(raw: String): String {
        return raw.replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    private fun buildParsedItem(
        name: String,
        barcode: String,
        quantity: Double,
        unit: String,
        unitPrice: Double,
        existingProducts: List<Product>,
        categories: List<Category>
    ): ParsedReceiptItem {
        val cleanName = name.trim().take(140)
        val cleanBarcode = barcode.trim().take(64)
        val matched = existingProducts.firstOrNull { prod ->
            (cleanBarcode.isNotEmpty() && (prod.barcode == cleanBarcode || prod.sku == cleanBarcode)) ||
                prod.name.equals(cleanName, ignoreCase = true)
        }
        val guessedCategory = matched?.let { prod ->
            categories.firstOrNull { it.id == prod.categoryId }
        } ?: guessCategory(cleanName, categories)

        return ParsedReceiptItem(
            localId = UUID.randomUUID().toString(),
            productId = matched?.id,
            name = matched?.name ?: cleanName,
            barcode = cleanBarcode.ifBlank { matched?.barcode.orEmpty() },
            categoryId = guessedCategory?.id ?: categories.firstOrNull()?.id,
            categoryName = guessedCategory?.name ?: matched?.categoryName ?: "Alimentos Secos",
            categoryColor = guessedCategory?.color ?: matched?.categoryColor ?: "#3b82f6",
            quantityInput = formatEditNumber(quantity),
            unit = matched?.unit ?: unit,
            unitPriceInput = String.format(java.util.Locale.US, "%.2f", unitPrice),
            previousUnitPrice = matched?.unitPrice ?: unitPrice,
            existingStock = matched?.quantity ?: 0.0,
            alreadyInStock = matched != null
        )
    }

    private fun guessCategory(name: String, categories: List<Category>): Category? {
        val lower = name.lowercase()
        val keyword = when {
            listOf("leite", "queijo", "iogurte", "manteiga", "ovo", "requeijão", "carne", "frango")
                .any { it in lower } -> "Geladeira"
            listOf("detergente", "sabão", "desinfetante", "água sanitária", "esponja", "amaciante")
                .any { it in lower } -> "Limpeza"
            listOf("sabonete", "pasta", "shampoo", "papel higiênico", "escova", "desodorante")
                .any { it in lower } -> "Higiene"
            listOf("café", "suco", "refrigerante", "água", "cerveja", "chá")
                .any { it in lower } -> "Bebidas"
            else -> "Alimentos"
        }
        return categories.firstOrNull { it.name.contains(keyword, ignoreCase = true) }
            ?: categories.firstOrNull()
    }

    private fun normalizeUnit(raw: String): String {
        return when (raw.trim().lowercase()) {
            "kg" -> "kg"
            "g", "gr" -> "g"
            "l", "lt" -> "L"
            "ml" -> "ml"
            "cx" -> "cx"
            "pc", "pct" -> "pct"
            else -> "un"
        }
    }

    private fun parsePtBrDecimal(raw: String): Double {
        val clean = raw.trim().replace(Regex("[^0-9.,]"), "")
        if (clean.isEmpty()) return 0.0
        val normalized = if (clean.contains(',') && clean.contains('.')) {
            if (clean.lastIndexOf(',') > clean.lastIndexOf('.')) {
                clean.replace(".", "").replace(',', '.')
            } else {
                clean.replace(",", "")
            }
        } else if (clean.contains(',')) {
            clean.replace(',', '.')
        } else {
            clean
        }
        return normalized.toDoubleOrNull() ?: 0.0
    }

    private fun formatEditNumber(value: Double): String {
        return if (value % 1.0 == 0.0) {
            value.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", value)
        }
    }
}
