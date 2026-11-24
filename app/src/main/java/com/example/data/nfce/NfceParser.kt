package com.example.data.nfce

import android.util.Xml
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit

data class ParsedNfceItem(
    val code: String = "",
    val name: String,
    val unit: String = "un",
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val selected: Boolean = true,
    val categoryId: Long? = null
) {
    val totalPrice: Double
        get() = quantity * unitPrice
}

data class NfceParseResult(
    val storeName: String? = null,
    val accessKey: String? = null,
    val detectedUfSigla: String? = null,
    val items: List<ParsedNfceItem> = emptyList(),
    val requiresWebPortal: Boolean = false,
    val portalUrl: String? = null,
    val message: String? = null
)

object NfceParser {

    private val client = OkHttpClient.Builder()
        .connectTimeout(14, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun fetchAndParseUrl(url: String): NfceParseResult {
        val key = SefazPortals.extractAccessKey(url)
        val detectedPortal = SefazPortals.detectForInput(url)
        return try {
            val request = Request.Builder()
                .url(url)
                .header(
                    "User-Agent",
                    "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Mobile Safari/537.36"
                )
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "pt-BR,pt;q=0.9")
                .build()

            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) {
                    return NfceParseResult(
                        accessKey = key,
                        detectedUfSigla = detectedPortal.ufSigla,
                        requiresWebPortal = true,
                        portalUrl = url,
                        message = "A SEFAZ (${detectedPortal.ufSigla}) retornou código ${response.code}. Abrindo no navegador integrado com cookies ativos para consulta."
                    )
                }

                if (body.trimStart().startsWith("<") && body.contains("<infNFe", ignoreCase = true)) {
                    val xmlResult = parseNfceXml(body)
                    if (xmlResult.items.isNotEmpty()) {
                        return xmlResult.copy(detectedUfSigla = detectedPortal.ufSigla)
                    }
                }

                val hasCaptcha = body.contains("g-recaptcha", ignoreCase = true) ||
                    body.contains("hcaptcha", ignoreCase = true) ||
                    body.contains("captcha", ignoreCase = true) ||
                    body.contains("ConsultaPublicaNFe", ignoreCase = true)

                val htmlResult = parseSefazHtml(body, key)
                if (htmlResult.items.isNotEmpty()) {
                    return htmlResult.copy(
                        detectedUfSigla = detectedPortal.ufSigla,
                        message = "${htmlResult.items.size} item(ns) extraído(s) da SEFAZ (${detectedPortal.ufSigla})!"
                    )
                }

                NfceParseResult(
                    accessKey = key,
                    detectedUfSigla = detectedPortal.ufSigla,
                    requiresWebPortal = true,
                    portalUrl = url,
                    message = if (hasCaptcha) {
                        "A SEFAZ (${detectedPortal.ufSigla}) exige verificação/CAPTCHA. Resolva na tela abaixo; assim que a nota abrir, os itens serão capturados automaticamente (ou toque em Extrair Itens)."
                    } else {
                        "Nota aberta no portal (${detectedPortal.ufSigla}). Assim que os produtos aparecerem na página, toque em \"Extrair Itens da Página\" (ou envie a foto do cupom na primeira aba)."
                    }
                )
            }
        } catch (e: Exception) {
            NfceParseResult(
                accessKey = key,
                detectedUfSigla = detectedPortal.ufSigla,
                requiresWebPortal = true,
                portalUrl = url,
                message = "Não foi possível consultar direto (${e.localizedMessage ?: "rede"}). Abrindo o link no portal integrado (${detectedPortal.ufSigla})."
            )
        }
    }

    fun parseNfceXml(xmlContent: String): NfceParseResult {
        val items = mutableListOf<ParsedNfceItem>()
        var storeName: String? = null
        var accessKey: String? = null

        var inEmit = false
        var inDet = false
        var inProd = false
        var currentCode = ""
        var currentName = ""
        var currentUnit = "un"
        var currentQty = 1.0
        var currentUnitPrice = 0.0

        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xmlContent))

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name.orEmpty()
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        tagName.equals("infNFe", ignoreCase = true) -> {
                            val idAttr = parser.getAttributeValue(null, "Id").orEmpty()
                            SefazPortals.extractAccessKey(idAttr)?.let { accessKey = it }
                        }
                        tagName.equals("emit", ignoreCase = true) -> inEmit = true
                        tagName.equals("det", ignoreCase = true) -> {
                            inDet = true
                            currentCode = ""
                            currentName = ""
                            currentUnit = "un"
                            currentQty = 1.0
                            currentUnitPrice = 0.0
                        }
                        tagName.equals("prod", ignoreCase = true) && inDet -> inProd = true
                        inEmit && tagName.equals("xNome", ignoreCase = true) -> {
                            storeName = parser.nextText().trim()
                        }
                        inProd && (tagName.equals("cEAN", ignoreCase = true) || tagName.equals("cEANTrib", ignoreCase = true)) -> {
                            val ean = parser.nextText().trim()
                            if (ean.length >= 8 && !ean.equals("SEM GTIN", ignoreCase = true)) {
                                currentCode = ean
                            }
                        }
                        inProd && tagName.equals("cProd", ignoreCase = true) -> {
                            val cProd = parser.nextText().trim()
                            if (currentCode.isBlank()) currentCode = cProd
                        }
                        inProd && tagName.equals("xProd", ignoreCase = true) -> {
                            currentName = parser.nextText().trim()
                        }
                        inProd && tagName.equals("uCom", ignoreCase = true) -> {
                            currentUnit = normalizeUnit(parser.nextText().trim())
                        }
                        inProd && tagName.equals("qCom", ignoreCase = true) -> {
                            currentQty = parseDecimal(parser.nextText()) ?: 1.0
                        }
                        inProd && tagName.equals("vUnCom", ignoreCase = true) -> {
                            currentUnitPrice = parseDecimal(parser.nextText()) ?: 0.0
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    when {
                        tagName.equals("emit", ignoreCase = true) -> inEmit = false
                        tagName.equals("prod", ignoreCase = true) -> inProd = false
                        tagName.equals("det", ignoreCase = true) -> {
                            if (currentName.isNotBlank()) {
                                items.add(
                                    ParsedNfceItem(
                                        code = currentCode,
                                        name = currentName,
                                        unit = currentUnit,
                                        quantity = currentQty.coerceAtLeast(0.01),
                                        unitPrice = currentUnitPrice.coerceAtLeast(0.0)
                                    )
                                )
                            }
                            inDet = false
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        val uf = accessKey?.take(2)?.let { code -> SefazPortals.allPortals.firstOrNull { it.ufCode == code }?.ufSigla }
        return NfceParseResult(
            storeName = storeName,
            accessKey = accessKey,
            detectedUfSigla = uf,
            items = items,
            message = if (items.isNotEmpty()) {
                "${items.size} item(ns) extraído(s) do XML com sucesso."
            } else {
                "Nenhum item <det><prod> encontrado no XML selecionado."
            }
        )
    }

    fun parseSefazHtml(html: String, keyHint: String? = null): NfceParseResult {
        val items = mutableListOf<ParsedNfceItem>()

        val rowRegex = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val titleRegex = Regex("class=[\"'][^\"']*txtTit[^\"']*[\"'][^>]*>(.*?)</", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val codeRegex = Regex("\\(C[oó]digo:\\s*([^)<]+)", RegexOption.IGNORE_CASE)
        val qtyRegex = Regex("Qtde\\.?\\s*:?\\s*</[^>]+>\\s*([0-9.,]+)|Qtde\\.?\\s*:?\\s*([0-9.,]+)", RegexOption.IGNORE_CASE)
        val unitRegex = Regex("UN\\s*:?\\s*</[^>]+>\\s*([A-Za-z]+)|UN\\s*:?\\s*([A-Za-z]+)", RegexOption.IGNORE_CASE)
        val valUnitRegex = Regex("Vl\\.\\s*Unit\\.?\\s*:?\\s*</[^>]+>\\s*(?:R\\$)?\\s*([0-9.,]+)|Vl\\.\\s*Unit\\.?\\s*:?\\s*(?:R\\$)?\\s*([0-9.,]+)", RegexOption.IGNORE_CASE)

        rowRegex.findAll(html).forEach { match ->
            val rowHtml = match.groupValues[1]
            val titleMatch = titleRegex.find(rowHtml)
            if (titleMatch != null) {
                val cleanName = stripTags(titleMatch.groupValues[1]).trim()
                if (cleanName.isNotBlank()) {
                    val code = codeRegex.find(rowHtml)?.groupValues?.getOrNull(1)?.trim().orEmpty()
                    val qtyRaw = qtyRegex.find(rowHtml)?.let { it.groupValues[1].ifBlank { it.groupValues[2] } }
                    val unitRaw = unitRegex.find(rowHtml)?.let { it.groupValues[1].ifBlank { it.groupValues[2] } }
                    val priceRaw = valUnitRegex.find(rowHtml)?.let { it.groupValues[1].ifBlank { it.groupValues[2] } }

                    items.add(
                        ParsedNfceItem(
                            code = code,
                            name = cleanName,
                            unit = normalizeUnit(unitRaw ?: "un"),
                            quantity = parseDecimal(qtyRaw) ?: 1.0,
                            unitPrice = parseDecimal(priceRaw) ?: 0.0
                        )
                    )
                }
            }
        }

        if (items.isEmpty()) {
            val cellRegex = Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            rowRegex.findAll(html).forEach { rowMatch ->
                val cells = cellRegex.findAll(rowMatch.groupValues[1])
                    .map { stripTags(it.groupValues[1]).trim() }
                    .filter { it.isNotBlank() }
                    .toList()
                if (cells.size >= 4) {
                    val nameCandidate = cells.firstOrNull { c ->
                        c.length >= 4 && c.any { it.isLetter() } &&
                            !c.contains("descri", ignoreCase = true) &&
                            !c.contains("subtotal", ignoreCase = true) &&
                            !c.contains("valor total", ignoreCase = true) &&
                            !c.contains("quantidade", ignoreCase = true)
                    }
                    val nums = cells.mapNotNull { c ->
                        if (c.matches(Regex("(?:R\\$\\s*)?[0-9.]+,[0-9]{2,4}|[0-9]+\\.[0-9]{2,3}"))) {
                            parseDecimal(c)
                        } else null
                    }
                    if (nameCandidate != null && nums.size >= 2) {
                        val unitCandidate = cells.firstOrNull {
                            it.lowercase() in setOf("un", "und", "kg", "g", "l", "lt", "ml", "pct", "cx", "pc")
                        } ?: "un"
                        items.add(
                            ParsedNfceItem(
                                name = nameCandidate,
                                unit = normalizeUnit(unitCandidate),
                                quantity = nums.first().coerceAtLeast(0.01),
                                unitPrice = nums.getOrElse(1) { nums.last() }.coerceAtLeast(0.0)
                            )
                        )
                    }
                }
            }
        }

        val storeRegex = Regex("id=[\"']u20[\"'][^>]*>(.*?)</|class=[\"']txtTopo[\"'][^>]*>(.*?)</", setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val storeName = storeRegex.find(html)?.let {
            stripTags(it.groupValues[1].ifBlank { it.groupValues[2] }).trim().takeIf { s -> s.isNotBlank() }
        }

        return NfceParseResult(
            storeName = storeName,
            accessKey = keyHint ?: SefazPortals.extractAccessKey(html),
            items = items
        )
    }

    fun parseWebViewJson(rawResult: String): NfceParseResult {
        val cleaned = rawResult.trim()
        if (cleaned.isBlank() || cleaned == "null" || cleaned == "\"\"") {
            return NfceParseResult(message = "Nenhum item encontrado nesta página ainda.")
        }
        return try {
            val unwrapped = if (cleaned.startsWith("\"")) {
                JSONObject("{\"v\":$cleaned}").getString("v")
            } else {
                cleaned
            }
            val obj = JSONObject(unwrapped)
            val storeName = obj.optString("store").takeIf { it.isNotBlank() }
            val array: JSONArray = obj.optJSONArray("items") ?: JSONArray()
            val items = buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val name = item.optString("name").trim()
                    if (name.isNotBlank()) {
                        add(
                            ParsedNfceItem(
                                code = item.optString("code").trim(),
                                name = name,
                                unit = normalizeUnit(item.optString("unit", "un")),
                                quantity = parseDecimal(item.optString("qty"))?.coerceAtLeast(0.01) ?: 1.0,
                                unitPrice = parseDecimal(item.optString("unitPrice"))?.coerceAtLeast(0.0) ?: 0.0
                            )
                        )
                    }
                }
            }
            if (items.isNotEmpty()) {
                NfceParseResult(
                    storeName = storeName,
                    items = items,
                    message = "${items.size} item(ns) extraído(s) do portal SEFAZ!"
                )
            } else {
                val bodyText = obj.optString("rawText")
                if (bodyText.isNotBlank()) {
                    val ocrFallback = parseOcrReceiptText(bodyText)
                    if (ocrFallback.items.isNotEmpty()) {
                        return ocrFallback.copy(
                            storeName = storeName ?: ocrFallback.storeName,
                            message = "${ocrFallback.items.size} item(ns) extraído(s) da visualização da SEFAZ!"
                        )
                    }
                }
                NfceParseResult(
                    message = "Nenhum produto localizado na página atual. Resolva o CAPTCHA até aparecer a lista de produtos ou use a foto/print do cupom."
                )
            }
        } catch (e: Exception) {
            NfceParseResult(message = "Não foi possível ler os itens da página: ${e.localizedMessage}")
        }
    }

    fun parseOcrReceiptText(rawText: String): NfceParseResult {
        val decimalSpaceFixRegex = Regex("(\\d)\\s*,\\s*(\\d{2,4})")
        val lines = rawText.lines()
            .map { raw ->
                decimalSpaceFixRegex.replace(raw.trim().replace(Regex("\\s+"), " "), "$1,$2")
            }
            .filter { it.isNotBlank() }

        val accessKey = SefazPortals.extractAccessKey(rawText)
        val detectedPortal = accessKey?.let { SefazPortals.detectForInput(it) }

        val storeKeywords = listOf(
            "supermercado", "mercado", "atacad", "comercial", "padaria", "farmacia",
            "drogaria", "hiper", "ltda", "s/a", "angeloni", "bistek", "giassi",
            "fort", "carrefour", "assaí", "assai", "koch", "muffato", "condor", "imperatriz", "cooper"
        )
        val storeName = lines.take(10).firstOrNull { line ->
            val low = line.lowercase()
            line.length >= 4 && !low.contains("cnpj") && !low.contains("danfe") &&
                !low.contains("documento auxiliar") &&
                (storeKeywords.any { low.contains(it) } || line.count { it.isLetter() } >= 6)
        }

        val ignoreTokens = listOf(
            "cnpj", "insc.", "ie:", "im:", "danfe", "documento auxiliar", "consumidor",
            "subtotal", "valor total", "total a pagar", "valor pago", "troco", "desconto",
            "acrescimo", "cartao", "crédito", "credito", "débito", "debito", "pix", "dinheiro",
            "forma pagamento", "tributos", "lei 12.741", "consulte pela chave", "chave de acesso",
            "protocolo", "procon", "serie", "extrato", "qtd. total", "qtde. total", "vl. total",
            "codigo descricao", "código descrição", "item codigo"
        )

        fun isNoiseLine(line: String): Boolean {
            val low = line.lowercase()
            return ignoreTokens.any { low.contains(it) } ||
                low.startsWith("http") ||
                low.matches(Regex("^\\d{2}/\\d{2}/\\d{2,4}.*")) ||
                low.matches(Regex("^cep[:\\s].*")) ||
                low.matches(Regex("^rua\\s.*|^av\\.?\\s.*")) ||
                low.matches(Regex("^[-=_*]{3,}$"))
        }

        val unitPattern = "(un|und|unid|um|kg|g|gr|l|lt|ml|pct|cx|pc|pç|gf|fd|rl|bd|dz)"
        val sefazDetailsLineRegex = Regex(
            "Qtde\\.?\\s*:?\\s*([0-9]+[.,]?[0-9]*)\\s+(?:UN\\.?\\s*:?\\s*([A-Za-zÀ-ÿ]+)\\s+)?(?:Vl\\.?\\s*Unit\\.?\\s*:?\\s*(?:R\\$)?\\s*([0-9.,]+))?",
            RegexOption.IGNORE_CASE
        )
        val qtyTimesPriceRegex = Regex(
            "^([0-9]+[.,]?[0-9]*)\\s*$unitPattern?\\s*[xX*]\\s*(?:R\\$)?\\s*([0-9]+,[0-9]{2})(?:\\s*=.*|\\s+[0-9]+,[0-9]{2}.*)?$",
            RegexOption.IGNORE_CASE
        )
        val sefazCodeSuffixRegex = Regex("\\(C[oó]digo:\\s*([^)]+)\\)", RegexOption.IGNORE_CASE)
        val priceTokenRegex = Regex("(?:R\\$\\s*)?([0-9]{1,4}(?:\\.[0-9]{3})*,[0-9]{2})")
        val inlineItemWithOperatorRegex = Regex(
            "^(?:\\d{1,4}\\s+)?(?:(\\d{4,14})\\s+)?(.+?)\\s+([0-9]+[.,]?[0-9]*)\\s*$unitPattern\\s+[xX*]\\s*(?:R\\$)?\\s*([0-9]+,[0-9]{2})(?:\\s+(?:R\\$)?\\s*[0-9]+,[0-9]{2})?(?:\\s+[A-Z0-9%]{1,5})?$",
            RegexOption.IGNORE_CASE
        )
        val inlineItemTwoPricesRegex = Regex(
            "^(?:\\d{1,4}\\s+)?(?:(\\d{4,14})\\s+)?(.+?)\\s+([0-9]+[.,]?[0-9]*)\\s+$unitPattern\\s+(?:R\\$)?\\s*([0-9]+,[0-9]{2})\\s+(?:R\\$)?\\s*([0-9]+,[0-9]{2})(?:\\s+[A-Z0-9%]{1,5})?$",
            RegexOption.IGNORE_CASE
        )
        val descQtyTotalRegex = Regex(
            "^(?:\\d{1,4}\\s+)?(?:(\\d{4,14})\\s+)?(.+?)\\s+([0-9]+[.,]?[0-9]*)\\s+$unitPattern\\s+(?:R\\$)?\\s*([0-9]+,[0-9]{2})(?:\\s+[A-Z0-9%]{1,5})?$",
            RegexOption.IGNORE_CASE
        )
        val descPlusPriceRegex = Regex(
            "^(?:\\d{1,4}\\s+)?(?:(\\d{4,14})\\s+)?(.+?)\\s+(?:R\\$\\s*)?([0-9]+,[0-9]{2})(?:\\s+[A-Z0-9%]{1,5})?$"
        )

        val items = mutableListOf<ParsedNfceItem>()
        var pendingCode = ""
        var pendingName = ""

        fun cleanCandidateName(raw: String): Pair<String, String> {
            val suffixCode = sefazCodeSuffixRegex.find(raw)?.groupValues?.getOrNull(1)?.trim().orEmpty()
            val clean = sefazCodeSuffixRegex.replace(raw, "")
                .replace(Regex("^\\d{1,3}\\s*[-.)]\\s*"), "")
                .trim()
            return suffixCode to clean
        }

        fun clearPending() {
            pendingCode = ""
            pendingName = ""
        }

        val startsWithItemIndexRegex = Regex("^\\d{1,4}\\s+")

        for (line in lines) {
            if (isNoiseLine(line) || line == storeName) {
                clearPending()
                continue
            }

            // Formato print/site da SEFAZ: "Qtde.: 2 UN: UN Vl. Unit.: 9,99"
            val sefazLine = sefazDetailsLineRegex.find(line)
            if (sefazLine != null && pendingName.isNotBlank()) {
                val qty = parseDecimal(sefazLine.groupValues[1]) ?: 1.0
                val unit = normalizeUnit(sefazLine.groupValues[2].ifBlank { "un" })
                val unitPrice = parseDecimal(sefazLine.groupValues[3]) ?: 0.0
                items.add(
                    ParsedNfceItem(
                        code = pendingCode,
                        name = pendingName,
                        unit = unit,
                        quantity = qty.coerceAtLeast(0.01),
                        unitPrice = unitPrice.coerceAtLeast(0.0)
                    )
                )
                clearPending()
                continue
            }

            // Linha de detalhe de cupom térmico: "2,000 UN X 4,89   9,78"
            val qMatch = qtyTimesPriceRegex.find(line)
            if (qMatch != null && pendingName.isNotBlank()) {
                val qty = parseDecimal(qMatch.groupValues[1]) ?: 1.0
                val unit = normalizeUnit(qMatch.groupValues[2].ifBlank { "un" })
                val unitPrice = parseDecimal(qMatch.groupValues[3]) ?: 0.0
                items.add(
                    ParsedNfceItem(
                        code = pendingCode,
                        name = pendingName,
                        unit = unit,
                        quantity = qty.coerceAtLeast(0.01),
                        unitPrice = unitPrice.coerceAtLeast(0.0)
                    )
                )
                clearPending()
                continue
            }

            val startsWithItemIndex = startsWithItemIndexRegex.containsMatchIn(line)

            // Linha única completa: "001 789... LEITE INTEGRAL 2 UN x 4,89 9,78" ou "... 2 UN 4,89 9,78" ou "... 2 UN 9,78"
            val unitPriceMatch = inlineItemWithOperatorRegex.find(line) ?: inlineItemTwoPricesRegex.find(line)
            val totalOnlyMatch = if (unitPriceMatch == null) descQtyTotalRegex.find(line) else null
            val inlineMatch = unitPriceMatch ?: totalOnlyMatch
            if (inlineMatch != null) {
                val rawCode = inlineMatch.groupValues[1].trim()
                val (suffixCode, cleanName) = cleanCandidateName(inlineMatch.groupValues[2])
                val qty = parseDecimal(inlineMatch.groupValues[3]) ?: 1.0
                val unit = normalizeUnit(inlineMatch.groupValues[4])
                val rawPrice = parseDecimal(inlineMatch.groupValues[5]) ?: 0.0
                val unitPrice = if (unitPriceMatch != null) {
                    rawPrice
                } else if (qty > 0.0) {
                    (rawPrice / qty).coerceAtLeast(0.0)
                } else rawPrice

                val isContinuation = pendingName.isNotBlank() && !startsWithItemIndex && rawCode.isBlank() && suffixCode.isBlank()
                val finalName = if (isContinuation) "$pendingName $cleanName".trim() else cleanName
                val finalCode = if (isContinuation) pendingCode else rawCode.ifBlank { suffixCode }

                if (finalName.length >= 2 && finalName.any { it.isLetter() }) {
                    items.add(
                        ParsedNfceItem(
                            code = finalCode,
                            name = finalName,
                            unit = unit,
                            quantity = qty.coerceAtLeast(0.01),
                            unitPrice = unitPrice.coerceAtLeast(0.0)
                        )
                    )
                    clearPending()
                    continue
                }
            }

            // Linha única nome + preço: "001 789... CAFE MELITTA 500G   18,90 F1" ou continuação de descrição + preço
            val descPriceMatch = descPlusPriceRegex.find(line)
            if (descPriceMatch != null) {
                val rawCode = descPriceMatch.groupValues[1].trim()
                val (suffixCode, cleanName) = cleanCandidateName(descPriceMatch.groupValues[2])
                val price = parseDecimal(descPriceMatch.groupValues[3]) ?: 0.0

                val isContinuation = pendingName.isNotBlank() && !startsWithItemIndex && rawCode.isBlank() && suffixCode.isBlank()
                val finalName = if (isContinuation) "$pendingName $cleanName".trim() else cleanName
                val finalCode = if (isContinuation) pendingCode else rawCode.ifBlank { suffixCode }

                if (finalName.length >= 3 && finalName.any { it.isLetter() } && price > 0.0 && price < 50000.0) {
                    items.add(
                        ParsedNfceItem(
                            code = finalCode,
                            name = finalName,
                            unit = "un",
                            quantity = 1.0,
                            unitPrice = price
                        )
                    )
                    clearPending()
                    continue
                }
            }

            // Preço isolado na linha logo abaixo de uma descrição pendente
            if (pendingName.isNotBlank() && line.matches(Regex("^(?:R\\$\\s*)?[0-9.,]+$"))) {
                val lonePrice = parseDecimal(line)
                if (lonePrice != null && lonePrice > 0.0 && lonePrice < 50000.0) {
                    items.add(
                        ParsedNfceItem(
                            code = pendingCode,
                            name = pendingName,
                            unit = "un",
                            quantity = 1.0,
                            unitPrice = lonePrice
                        )
                    )
                    clearPending()
                    continue
                }
            }

            // Cabeçalho de produto (1ª linha do item, ou descrição continuada em 2 linhas)
            val headerCodeAndName = Regex("^(?:\\d{1,4}\\s+)?(?:(\\d{4,14})\\s+)?(.{3,})$").find(line)
            if (headerCodeAndName != null && priceTokenRegex.find(line) == null) {
                val rawCode = headerCodeAndName.groupValues[1].trim()
                val (suffixCode, cleanName) = cleanCandidateName(headerCodeAndName.groupValues[2])
                if (cleanName.count { it.isLetter() } >= 3) {
                    if (pendingName.isNotBlank() && !startsWithItemIndex && rawCode.isBlank() && suffixCode.isBlank()) {
                        pendingName = "$pendingName $cleanName".trim()
                    } else {
                        pendingCode = rawCode.ifBlank { suffixCode }
                        pendingName = cleanName
                    }
                    continue
                }
            }
            clearPending()
        }

        return NfceParseResult(
            storeName = storeName,
            accessKey = accessKey,
            detectedUfSigla = detectedPortal?.ufSigla,
            items = items,
            message = if (items.isNotEmpty()) {
                "${items.size} item(ns) lido(s) automaticamente do cupom!"
            } else {
                null
            }
        )
    }

    fun parseRawReceiptText(text: String): NfceParseResult {
        val items = mutableListOf<ParsedNfceItem>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotBlank() }

        for (line in lines) {
            if (line.startsWith("http", ignoreCase = true)) continue
            val parts = line.split(Regex("\\t|\\s{2,}|\\|")).map { it.trim() }.filter { it.isNotBlank() }
            if (parts.size >= 2) {
                val name = parts.first()
                if (name.length < 2 || name.all { it.isDigit() }) continue
                val numbers = parts.drop(1).mapNotNull { parseDecimal(it.replace(Regex("[^0-9.,]"), "")) }
                val unitFound = parts.drop(1).firstOrNull {
                    it.lowercase() in setOf("un", "kg", "g", "l", "ml", "pct", "cx", "pc", "lt", "und", "unid")
                }
                val qty = numbers.getOrNull(0) ?: 1.0
                val price = numbers.lastOrNull() ?: 0.0
                items.add(
                    ParsedNfceItem(
                        name = name,
                        unit = normalizeUnit(unitFound ?: "un"),
                        quantity = qty.coerceAtLeast(0.01),
                        unitPrice = if (numbers.size >= 2) price else 0.0
                    )
                )
            }
        }
        if (items.isNotEmpty()) {
            return NfceParseResult(
                items = items,
                message = "${items.size} linha(s) interpretada(s) do texto."
            )
        }
        return parseOcrReceiptText(text)
    }

    fun normalizeUnit(raw: String): String {
        return when (raw.trim().lowercase()) {
            "kg", "quilo", "quilograma" -> "kg"
            "g", "gr", "grama" -> "g"
            "l", "lt", "litro" -> "L"
            "ml" -> "ml"
            "pct", "pacote" -> "pct"
            "cx", "caixa" -> "cx"
            else -> "un"
        }
    }

    private fun parseDecimal(raw: String?): Double? {
        if (raw.isNullOrBlank()) return null
        val cleaned = raw.trim().replace("R$", "").replace(" ", "")
        return if (cleaned.contains(",") && cleaned.contains(".")) {
            cleaned.replace(".", "").replace(",", ".").toDoubleOrNull()
        } else {
            cleaned.replace(",", ".").toDoubleOrNull()
        }
    }

    private fun stripTags(html: String): String {
        return html.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ")
    }
}
