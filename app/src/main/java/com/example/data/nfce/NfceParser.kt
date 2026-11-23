package com.example.data.nfce

import android.util.Xml
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.concurrent.TimeUnit

data class RegionalPortal(
    val ufCode: String,
    val ufSigla: String,
    val label: String,
    val url: String
)

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

    // Todos os 26 estados + DF (código IBGE nos 2 primeiros dígitos da chave de 44 dígitos) + Portal Nacional
    val regionalPortals = listOf(
        RegionalPortal("00", "BR", "Portal Nacional (NF-e / Qualquer UF)", "https://www.nfe.fazenda.gov.br/portal/consultaRecaptcha.aspx?tipoConsulta=resumo&tipoConteudo=7PhJ+gAVw2g="),
        RegionalPortal("12", "AC", "Acre (AC)", "https://www.sefaznet.ac.gov.br/nfce/consulta"),
        RegionalPortal("27", "AL", "Alagoas (AL)", "https://nfce.sefaz.al.gov.br/consultaNFCe.htm"),
        RegionalPortal("16", "AP", "Amapá (AP)", "https://www.sefaz.ap.gov.br/sate/seg/SEGf_AcessarFuncao.jsp?cdFuncao=FIS_1261"),
        RegionalPortal("13", "AM", "Amazonas (AM)", "https://sistemas.sefaz.am.gov.br/nfceweb/formConsulta.do"),
        RegionalPortal("29", "BA", "Bahia (BA)", "https://nfe.sefaz.ba.gov.br/servicos/nfce/Modulos/Geral/NFCEC_consulta_chave_acesso.aspx"),
        RegionalPortal("23", "CE", "Ceará (CE)", "https://nfce.sefaz.ce.gov.br/pages/ShowNFCe.html"),
        RegionalPortal("53", "DF", "Distrito Federal (DF)", "https://www.fazenda.df.gov.br/nfce/consulta"),
        RegionalPortal("32", "ES", "Espírito Santo (ES)", "https://app.sefaz.es.gov.br/ConsultaNFCe"),
        RegionalPortal("52", "GO", "Goiás (GO)", "https://nfeweb.sefaz.go.gov.br/nfeweb/sites/nfce/danfeNFCe"),
        RegionalPortal("21", "MA", "Maranhão (MA)", "https://www.nfce.sefaz.ma.gov.br/portal/consultarNFCe.jsp"),
        RegionalPortal("51", "MT", "Mato Grosso (MT)", "https://www.sefaz.mt.gov.br/nfce/consultanfce"),
        RegionalPortal("50", "MS", "Mato Grosso do Sul (MS)", "https://www.dfe.ms.gov.br/nfce/consulta"),
        RegionalPortal("31", "MG", "Minas Gerais (MG)", "https://portalsped.fazenda.mg.gov.br/portalnfce"),
        RegionalPortal("15", "PA", "Pará (PA)", "https://appnfc.sefa.pa.gov.br/portal/view/consultas/nfce/consultanfce.seam"),
        RegionalPortal("25", "PB", "Paraíba (PB)", "https://www.receita.pb.gov.br/nfce"),
        RegionalPortal("41", "PR", "Paraná (PR)", "https://www.fazenda.pr.gov.br/nfce/consulta"),
        RegionalPortal("26", "PE", "Pernambuco (PE)", "https://nfce.sefaz.pe.gov.br/nfce-web/consultarNFCe"),
        RegionalPortal("22", "PI", "Piauí (PI)", "https://www.sefaz.pi.gov.br/nfce"),
        RegionalPortal("33", "RJ", "Rio de Janeiro (RJ)", "https://www4.fazenda.rj.gov.br/consultaNFCe/QRCode"),
        RegionalPortal("24", "RN", "Rio Grande do Norte (RN)", "https://nfce.set.rn.gov.br/consultarNFCe.aspx"),
        RegionalPortal("43", "RS", "Rio Grande do Sul (RS)", "https://www.sefaz.rs.gov.br/NFCE/NFCE-COM.aspx"),
        RegionalPortal("11", "RO", "Rondônia (RO)", "https://www.nfce.sefin.ro.gov.br/"),
        RegionalPortal("14", "RR", "Roraima (RR)", "https://www.sefaz.rr.gov.br/nfce"),
        RegionalPortal("42", "SC", "Santa Catarina (SC — SAT)", "https://sat.sef.sc.gov.br/tax.NET/Sat.NFe.Web/Consultas/ConsultaPublicaNFe.aspx?amb=PROD"),
        RegionalPortal("35", "SP", "São Paulo (SP)", "https://www.nfce.fazenda.sp.gov.br/NFCeConsultaPublica/Paginas/ConsultaPublica.aspx"),
        RegionalPortal("28", "SE", "Sergipe (SE)", "https://www.nfce.sefaz.se.gov.br/portal/portalNoticias.jsp"),
        RegionalPortal("17", "TO", "Tocantins (TO)", "https://www.sefaz.to.gov.br/nfce/consulta.jsf")
    )

    private val client = OkHttpClient.Builder()
        .connectTimeout(14, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    fun extractAccessKey(input: String): String? {
        val digitsOnly = input.replace(Regex("[^0-9]"), "")
        val match = Regex("\\d{44}").find(digitsOnly)
        return match?.value
    }

    fun detectPortalForInput(input: String): RegionalPortal {
        val trimmed = input.trim()
        // 1. Always prefer extracting 44-digit Chave de Acesso (works nationwide for all 27 UFs!)
        val key = extractAccessKey(trimmed)
        if (key != null && key.length >= 2) {
            val uf = key.substring(0, 2)
            regionalPortals.firstOrNull { it.ufCode == uf }?.let { return it }
        }
        // 2. Otherwise match by SEFAZ domain host
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            val matchedByHost = regionalPortals.firstOrNull { portal ->
                val host = portal.url.substringAfter("://").substringBefore("/")
                trimmed.contains(host, ignoreCase = true)
            }
            if (matchedByHost != null) return matchedByHost
            val matchedByUfSuffix = regionalPortals.firstOrNull { portal ->
                portal.ufCode != "00" && trimmed.contains(".${portal.ufSigla.lowercase()}.gov.br", ignoreCase = true)
            }
            if (matchedByUfSuffix != null) return matchedByUfSuffix
        }
        return regionalPortals.first() // Portal Nacional (BR) when unknown
    }

    fun fetchAndParseUrl(url: String): NfceParseResult {
        val key = extractAccessKey(url)
        val detectedPortal = detectPortalForInput(url)
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
                            extractAccessKey(idAttr)?.let { accessKey = it }
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

        val uf = accessKey?.take(2)?.let { code -> regionalPortals.firstOrNull { it.ufCode == code }?.ufSigla }
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

        // Fallback generic <td> rows if state portal uses standard HTML tables (PR / SP / SC SAT)
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
            accessKey = keyHint ?: extractAccessKey(html),
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

    /**
     * Parser inteligente para texto extraído via OCR de cupons fiscais térmicos brasileiros
     * (suporta formato 1 linha e 2 linhas: "COD DESCRICAO" + "QTD UN X VL_UNIT = TOTAL").
     */
    fun parseOcrReceiptText(rawText: String): NfceParseResult {
        val lines = rawText.lines()
            .map { it.trim().replace(Regex("\\s+"), " ") }
            .filter { it.isNotBlank() }

        val accessKey = extractAccessKey(rawText)
        val detectedPortal = accessKey?.let { detectPortalForInput(it) }

        val storeKeywords = listOf("supermercado", "mercado", "atacad", "comercial", "padaria", "farmacia", "drogaria", "hiper", "ltda", "s/a", "angeloni", "bistek", "giassi", "fort", "carrefour", "assaí", "assai", "koch", "muffato", "condor")
        val storeName = lines.take(8).firstOrNull { line ->
            val low = line.lowercase()
            line.length >= 4 && !low.contains("cnpj") && !low.contains("danfe") &&
                (storeKeywords.any { low.contains(it) } || line.count { it.isLetter() } >= 6)
        }

        val ignoreTokens = listOf(
            "cnpj", "insc.", "ie:", "im:", "danfe", "documento auxiliar", "consumidor",
            "subtotal", "valor total", "total a pagar", "valor pago", "troco", "desconto",
            "acrescimo", "cartao", "crédito", "credito", "débito", "debito", "pix", "dinheiro",
            "forma pagamento", "tributos", "lei 12.741", "consulte pela chave", "chave de acesso",
            "protocolo", "procon", "serie", "extrato", "qtd. total", "itens"
        )

        fun isNoiseLine(line: String): Boolean {
            val low = line.lowercase()
            return ignoreTokens.any { low.contains(it) } ||
                low.startsWith("http") ||
                low.matches(Regex("^\\d{2}/\\d{2}/\\d{2,4}.*")) ||
                low.matches(Regex("^cep[:\\s].*")) ||
                low.matches(Regex("^rua\\s.*|^av\\.?\\s.*"))
        }

        val items = mutableListOf<ParsedNfceItem>()
        val qtyLineRegex = Regex(
            "^([0-9]+[.,]?[0-9]*)\\s*(un|und|kg|g|gr|l|lt|ml|pct|cx|pc)?\\s*[xX*]\\s*(?:R\\$)?\\s*([0-9]+,[0-9]{2})",
            RegexOption.IGNORE_CASE
        )
        val priceTokenRegex = Regex("(?:R\\$\\s*)?([0-9]{1,4}(?:\\.[0-9]{3})*,[0-9]{2})")
        val inlineItemRegex = Regex(
            "^(?:\\d{1,4}\\s+)?(?:(\\d{6,14})\\s+)?(.+?)\\s+([0-9]+[.,]?[0-9]*)\\s*(un|und|kg|g|gr|l|lt|ml|pct|cx|pc)\\s+(?:[xX*]\\s*)?(?:R\\$)?\\s*([0-9]+,[0-9]{2})",
            RegexOption.IGNORE_CASE
        )
        val descPlusPriceRegex = Regex(
            "^(?:\\d{1,4}\\s+)?(?:(\\d{6,14})\\s+)?(.+?)\\s+(?:R\\$\\s*)?([0-9]+,[0-9]{2})(?:\\s+[A-Z0-9])?$"
        )

        var pendingProductLine: Pair<String, String>? = null // code to name

        for (line in lines) {
            if (isNoiseLine(line)) {
                pendingProductLine = null
                continue
            }

            // Case 1: Line matches "2,000 UN x 5,49" following a product name line
            val qMatch = qtyLineRegex.find(line)
            if (qMatch != null && pendingProductLine != null) {
                val qty = parseDecimal(qMatch.groupValues[1]) ?: 1.0
                val unit = normalizeUnit(qMatch.groupValues[2].ifBlank { "un" })
                val unitPrice = parseDecimal(qMatch.groupValues[3]) ?: 0.0
                items.add(
                    ParsedNfceItem(
                        code = pendingProductLine.first,
                        name = pendingProductLine.second,
                        unit = unit,
                        quantity = qty.coerceAtLeast(0.01),
                        unitPrice = unitPrice.coerceAtLeast(0.0)
                    )
                )
                pendingProductLine = null
                continue
            }

            // Case 2: Single line with inline qty, unit & price
            val inlineMatch = inlineItemRegex.find(line)
            if (inlineMatch != null) {
                val code = inlineMatch.groupValues[1].trim()
                val name = inlineMatch.groupValues[2].trim()
                val qty = parseDecimal(inlineMatch.groupValues[3]) ?: 1.0
                val unit = normalizeUnit(inlineMatch.groupValues[4])
                val unitPrice = parseDecimal(inlineMatch.groupValues[5]) ?: 0.0
                if (name.length >= 2 && name.any { it.isLetter() }) {
                    items.add(
                        ParsedNfceItem(
                            code = code,
                            name = name,
                            unit = unit,
                            quantity = qty.coerceAtLeast(0.01),
                            unitPrice = unitPrice.coerceAtLeast(0.0)
                        )
                    )
                    pendingProductLine = null
                    continue
                }
            }

            // Case 3: Single line ending in price ("ARROZ TIO JOAO 5KG  24,90")
            val descPriceMatch = descPlusPriceRegex.find(line)
            if (descPriceMatch != null) {
                val code = descPriceMatch.groupValues[1].trim()
                val name = descPriceMatch.groupValues[2].trim()
                val price = parseDecimal(descPriceMatch.groupValues[3]) ?: 0.0
                if (name.length >= 3 && name.any { it.isLetter() } && price > 0.0 && price < 50000.0) {
                    items.add(
                        ParsedNfceItem(
                            code = code,
                            name = name,
                            unit = "un",
                            quantity = 1.0,
                            unitPrice = price
                        )
                    )
                    pendingProductLine = null
                    continue
                }
            }

            // Case 4: Could be Line 1 of a 2-line cupom item ("001 7891000100103 ARROZ 5KG")
            val headerCodeAndName = Regex("^(?:\\d{1,4}\\s+)?(?:(\\d{6,14})\\s+)?([A-Za-zÀ-ÿ].{2,})$").find(line)
            if (headerCodeAndName != null && priceTokenRegex.find(line) == null) {
                val code = headerCodeAndName.groupValues[1].trim()
                val name = headerCodeAndName.groupValues[2].trim()
                if (name.count { it.isLetter() } >= 3) {
                    pendingProductLine = code to name
                }
            } else {
                pendingProductLine = null
            }
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
                    it.lowercase() in setOf("un", "kg", "g", "l", "ml", "pct", "cx", "pc", "lt")
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

    fun buildFillKeyJs(accessKey: String): String {
        val safeKey = accessKey.replace(Regex("[^0-9]"), "")
        return """
            (function() {
                var inputs = Array.from(document.querySelectorAll('input[type="text"], input[type="search"], input[type="tel"], input:not([type])'));
                var target = inputs.find(function(el) {
                    var hint = ((el.id || '') + ' ' + (el.name || '') + ' ' + (el.placeholder || '')).toLowerCase();
                    return hint.indexOf('chave') >= 0 || hint.indexOf('chnfe') >= 0 || el.maxLength === 44;
                }) || inputs.find(function(el) { return el.offsetParent !== null; });
                if (target) {
                    target.focus();
                    target.value = '$safeKey';
                    target.dispatchEvent(new Event('input', { bubbles: true }));
                    target.dispatchEvent(new Event('change', { bubbles: true }));
                    return 'ok';
                }
                return 'not_found';
            })();
        """.trimIndent()
    }

    val WEBVIEW_EXTRACTION_JS = """
        (function() {
            var items = [];
            var store = '';
            var storeEl = document.querySelector('#u20, .txtTopo, [id*="Razao"], [id*="Emitente"], [id*="NomeEmitente"], h1, h2');
            if (storeEl) store = (storeEl.innerText || storeEl.textContent || '').trim();

            // Strategy 1: Standard SVRS/SEFAZ NFC-e #tabResult rows
            var svrsRows = document.querySelectorAll('#tabResult tr, tr[id^="Item"]');
            svrsRows.forEach(function(tr) {
                var tit = tr.querySelector('.txtTit');
                if (!tit) return;
                var name = (tit.innerText || '').trim();
                var text = tr.innerText || '';
                var codeMatch = text.match(/C[oó]digo:\s*([^\)\s]+)/i);
                var qtyMatch = text.match(/Qtde\.?:?\s*([0-9.,]+)/i);
                var unMatch = text.match(/UN\.?:?\s*([A-Za-z]+)/i);
                var priceMatch = text.match(/Vl\.?\s*Unit\.?:?\s*(?:R\$)?\s*([0-9.,]+)/i);
                if (name) {
                    items.push({
                        code: codeMatch ? codeMatch[1] : '',
                        name: name,
                        qty: qtyMatch ? qtyMatch[1] : '1',
                        unit: unMatch ? unMatch[1] : 'un',
                        unitPrice: priceMatch ? priceMatch[1] : '0'
                    });
                }
            });

            // Strategy 2: SAT SEFAZ SC / PR / SP / MG / DANFE HTML tables
            if (items.length === 0) {
                var tables = document.querySelectorAll('table');
                tables.forEach(function(tbl) {
                    var headerText = (tbl.rows[0] ? tbl.rows[0].innerText : '').toLowerCase();
                    if (headerText.indexOf('descri') >= 0 || headerText.indexOf('produto') >= 0 || headerText.indexOf('qtde') >= 0 || headerText.indexOf('item') >= 0 || headerText.indexOf('mercadoria') >= 0) {
                        for (var r = 1; r < tbl.rows.length; r++) {
                            var cells = tbl.rows[r].cells;
                            if (!cells || cells.length < 3) continue;
                            var texts = Array.from(cells).map(function(c) { return (c.innerText || '').trim(); });
                            var desc = texts.find(function(t) { return t.length > 3 && !/^[0-9.,\sR$]+$/.test(t); }) || '';
                            if (!desc || desc.toLowerCase().indexOf('total') === 0 || desc.toLowerCase().indexOf('subtotal') === 0) continue;
                            var nums = texts.filter(function(t) { return /^[0-9.,\sR$]+$/.test(t.replace('R$','').trim()) && /[0-9]/.test(t); });
                            var unit = texts.find(function(t) { return /^(un|und|kg|g|l|ml|pct|cx|pc|lt)$/i.test(t); }) || 'un';
                            var code = texts.find(function(t) { return /^[0-9]{4,14}$/.test(t); }) || '';
                            items.push({
                                code: code,
                                name: desc,
                                qty: nums.length >= 1 ? nums[0] : '1',
                                unit: unit,
                                unitPrice: nums.length >= 2 ? nums[1] : '0'
                            });
                        }
                    }
                });
            }

            // Strategy 3: Raw page text fallback for div-based portals
            var rawText = (document.body && document.body.innerText) ? document.body.innerText : '';
            return JSON.stringify({ store: store, items: items, rawText: items.length === 0 ? rawText : '' });
        })();
    """.trimIndent()
}
