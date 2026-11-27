package com.example.data.nfce

data class RegionalPortal(
    val ufCode: String,
    val ufSigla: String,
    val label: String,
    val url: String
)

/**
 * Catálogo nacional de portais SEFAZ NFC-e/NF-e (todos os 26 estados + DF + Portal Nacional)
 * e scripts JS para preenchimento de chave / extração automática dentro do WebView.
 */
object SefazPortals {

    val allPortals = listOf(
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
        RegionalPortal("42", "SC", "Santa Catarina (SC)", "https://sat.sef.sc.gov.br/tax.NET/Sat.DFe.NFCe.Web/Consultas/ConsultaPublicaNFCe.aspx"),
        RegionalPortal("35", "SP", "São Paulo (SP)", "https://www.nfce.fazenda.sp.gov.br/NFCeConsultaPublica/Paginas/ConsultaPublica.aspx"),
        RegionalPortal("28", "SE", "Sergipe (SE)", "https://www.nfce.sefaz.se.gov.br/portal/portalNoticias.jsp"),
        RegionalPortal("17", "TO", "Tocantins (TO)", "https://www.sefaz.to.gov.br/nfce/consulta.jsf")
    )

    private const val SC_NFE_MODEL_55_URL =
        "https://sat.sef.sc.gov.br/tax.NET/Sat.NFe.Web/Consultas/ConsultaPublicaNFe.aspx?amb=PROD"

    private val knownPortalHosts: Set<String> by lazy {
        allPortals.mapNotNull { portal ->
            runCatching { java.net.URI(portal.url).host?.lowercase() }.getOrNull()
        }.toSet()
    }

    private fun isPrivateOrLoopbackHost(host: String): Boolean {
        val h = host.trim().lowercase().trim('[', ']')
        if (h.isBlank() ||
            h == "localhost" ||
            h == "0.0.0.0" ||
            h == "::1" ||
            h.endsWith(".local") ||
            h.endsWith(".internal") ||
            h.startsWith("fe80:") ||
            h.startsWith("fc00:") ||
            h.startsWith("fd")
        ) {
            return true
        }
        val ipv4Match = Regex("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$").matchEntire(h)
        if (ipv4Match != null) {
            val a = ipv4Match.groupValues[1].toIntOrNull() ?: return true
            val b = ipv4Match.groupValues[2].toIntOrNull() ?: return true
            if (a == 10 || a == 127 || a == 0) return true
            if (a == 169 && b == 254) return true
            if (a == 192 && b == 168) return true
            if (a == 172 && b in 16..31) return true
            return true // Bloqueia IPs literais em consultas SEFAZ
        }
        return false
    }

    /**
     * Normaliza caracteres `|` na query string (padrão oficial do QR Code NFC-e v2: `?p=CHAVE|2|1|1|HASH`)
     * para `%7C` para compatibilidade estrita com RFC 2396 (`java.net.URI`), rejeitando espaços/controles.
     */
    private fun normalizeQueryAndParseUri(rawUrl: String): Pair<String, java.net.URI>? {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank() || trimmed.length > 4096) return null
        if (trimmed.any { it.isWhitespace() || it.isISOControl() }) return null
        val queryIdx = trimmed.indexOf('?')
        val normalized = if (queryIdx >= 0) {
            trimmed.substring(0, queryIdx) + trimmed.substring(queryIdx).replace("|", "%7C")
        } else {
            trimmed
        }
        val uri = runCatching { java.net.URI(normalized) }.getOrNull() ?: return null
        return normalized to uri
    }

    /**
     * Valida e normaliza URLs para portais oficiais da SEFAZ (.gov.br) sobre HTTPS,
     * bloqueando esquemas perigosos (file://, javascript:, intent://) e endereços internos.
     */
    fun sanitizeSefazUrl(rawUrl: String): String? {
        val (normalized, uri) = normalizeQueryAndParseUri(rawUrl) ?: return null
        val scheme = uri.scheme?.lowercase() ?: return null
        if (scheme != "https" && scheme != "http") return null
        if (!uri.userInfo.isNullOrBlank()) return null
        if (uri.port != -1 && uri.port != 443 && uri.port != 80) return null
        val host = uri.host?.lowercase() ?: return null
        if (isPrivateOrLoopbackHost(host)) return null
        val isGovDomain = host.endsWith(".gov.br") || host in knownPortalHosts
        if (!isGovDomain) return null
        return if (scheme == "http") {
            normalized.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
        } else {
            normalized
        }
    }

    fun isAllowedWebViewNavigationUrl(rawUrl: String): Boolean {
        val trimmed = rawUrl.trim()
        if (trimmed.isBlank() || trimmed.length > 8192 || trimmed.any { it.isISOControl() }) return false
        val queryIdx = trimmed.indexOf('?')
        val safeForUri = if (queryIdx >= 0) {
            val base = trimmed.substring(0, queryIdx)
            val query = trimmed.substring(queryIdx)
                .replace(" ", "%20")
                .replace("|", "%7C")
                .replace("{", "%7B")
                .replace("}", "%7D")
                .replace("[", "%5B")
                .replace("]", "%5D")
            base + query
        } else {
            trimmed
        }
        val uri = runCatching { java.net.URI(safeForUri) }.getOrNull() ?: return false
        if (uri.scheme?.lowercase() != "https") return false
        if (!uri.userInfo.isNullOrBlank()) return false
        val host = uri.host?.lowercase() ?: return false
        return !isPrivateOrLoopbackHost(host)
    }

    fun upgradeGovHttpRedirect(rawUrl: String): String? {
        val trimmed = rawUrl.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true)) return null
        val upgraded = trimmed.replaceFirst(Regex("^http://", RegexOption.IGNORE_CASE), "https://")
        return sanitizeSefazUrl(upgraded)
    }

    private fun isValidUfPrefix(key: String): Boolean {
        if (key.length != 44) return false
        val uf = key.substring(0, 2)
        return allPortals.any { it.ufCode != "00" && it.ufCode == uf }
    }

    fun extractAccessKey(input: String): String? {
        // 1. Sequência contígua de 44 dígitos (URLs, XMLs, chave colada sem espaços)
        val contiguousCandidates = Regex("(?<!\\d)(\\d{44})(?!\\d)")
            .findAll(input)
            .map { it.groupValues[1] }
            .toList()
        contiguousCandidates.firstOrNull(::isValidUfPrefix)?.let { return it }
        contiguousCandidates.firstOrNull()?.let { return it }

        // 2. Linha contendo a chave formatada em blocos (ex: 4226 1144 9078 ... 6346)
        input.lines().forEach { line ->
            Regex("(?<!\\d)(?:\\d[ \\t.\\-]?){43}\\d(?!\\d)").findAll(line).forEach { match ->
                val candidate = match.value.replace(Regex("[^0-9]"), "")
                if (candidate.length == 44 && isValidUfPrefix(candidate)) return candidate
            }
            val lineDigits = line.replace(Regex("[^0-9]"), "")
            if (lineDigits.length == 44 && isValidUfPrefix(lineDigits)) {
                return lineDigits
            }
        }
        return null
    }

    fun detectForInput(input: String): RegionalPortal {
        val trimmed = input.trim()
        val key = extractAccessKey(trimmed)
        if (key != null && key.length >= 2) {
            val uf = key.substring(0, 2)
            allPortals.firstOrNull { it.ufCode == uf }?.let { return it }
        }
        val safeUrl = sanitizeSefazUrl(trimmed)
        if (safeUrl != null) {
            allPortals.firstOrNull { portal ->
                val host = portal.url.substringAfter("://").substringBefore("/")
                safeUrl.contains(host, ignoreCase = true)
            }?.let { return it }

            allPortals.firstOrNull { portal ->
                portal.ufCode != "00" && safeUrl.contains(".${portal.ufSigla.lowercase()}.gov.br", ignoreCase = true)
            }?.let { return it }
        }
        return allPortals.first()
    }

    /**
     * Escolhe a URL correta considerando se a chave é NFC-e (modelo 65) ou NF-e (modelo 55),
     * evitando erro "modelo de DF-e diferente do solicitado" em portais estaduais separados.
     */
    fun resolvePortalUrl(portal: RegionalPortal, accessKey: String? = null, rawInput: String = ""): String {
        sanitizeSefazUrl(rawInput)?.let { return it }
        if (accessKey != null && accessKey.length == 44) {
            val model = accessKey.substring(20, 22)
            if (model == "55") {
                return if (portal.ufCode == "42") SC_NFE_MODEL_55_URL else allPortals.first().url
            }
        }
        return portal.url
    }

    fun buildFillKeyJs(accessKey: String): String {
        val safeKey = accessKey.replace(Regex("[^0-9]"), "").take(44)
        return """
            (function() {
                var inputs = Array.from(document.querySelectorAll('input[type="text"], input[type="search"], input[type="tel"], input[type="number"], input:not([type])'));
                var target = inputs.find(function(el) {
                    var hint = ((el.id || '') + ' ' + (el.name || '') + ' ' + (el.placeholder || '')).toLowerCase();
                    return hint.indexOf('chave') >= 0 || hint.indexOf('chnfe') >= 0 || hint.indexOf('acesso') >= 0 || el.maxLength === 44 || el.maxLength === 54;
                }) || inputs.find(function(el) { return el.offsetParent !== null; });
                if (target) {
                    target.focus();
                    if (target.maxLength > 0 && target.maxLength < 54) {
                        target.removeAttribute('maxlength');
                    }
                    var setter = Object.getOwnPropertyDescriptor(window.HTMLInputElement.prototype, 'value');
                    if (setter && setter.set) {
                        setter.set.call(target, '$safeKey');
                    } else {
                        target.value = '$safeKey';
                    }
                    target.dispatchEvent(new Event('input', { bubbles: true }));
                    target.dispatchEvent(new Event('keyup', { bubbles: true }));
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
            var storeCandidates = Array.from(document.querySelectorAll('#u20, .txtTopo, [id*="Razao"], [id*="Emitente"], [id*="NomeEmitente"], h1, h2'));
            for (var s = 0; s < storeCandidates.length; s++) {
                var txt = (storeCandidates[s].innerText || storeCandidates[s].textContent || '').replace(/\s+/g, ' ').trim();
                var low = txt.toLowerCase();
                if (txt.length >= 3 &&
                    low.indexOf('documento auxiliar') < 0 &&
                    low.indexOf('danfe') < 0 &&
                    low.indexOf('consulta p') < 0 &&
                    low.indexOf('nfc-e') !== 0 &&
                    low.indexOf('nf-e') !== 0) {
                    store = txt;
                    break;
                }
            }

            var svrsRows = document.querySelectorAll('#tabResult tr, tr[id^="Item"]');
            svrsRows.forEach(function(tr) {
                var tit = tr.querySelector('.txtTit');
                if (!tit) return;
                var name = (tit.innerText || '').trim();
                var text = tr.innerText || '';
                var codeMatch = text.match(/C[oó]digo:\s*([^\)\s]+)/i);
                var qtyMatch = text.match(/Qtde\.?:?\s*([0-9.,]+)/i);
                var unMatch = text.match(/\b(?:UN|Unid|Unidade)\.?:?\s*([A-Za-zÀ-ÿ]{1,6})/i);
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

            if (items.length === 0) {
                var tables = document.querySelectorAll('table');
                tables.forEach(function(tbl) {
                    if (!tbl.rows || tbl.rows.length < 2) return;
                    var headerCells = Array.from(tbl.rows[0].cells || []).map(function(c) {
                        return (c.innerText || '').toLowerCase().trim();
                    });
                    var headerJoined = headerCells.join(' ');
                    if (headerJoined.indexOf('descri') >= 0 || headerJoined.indexOf('produto') >= 0 || headerJoined.indexOf('qtde') >= 0 || headerJoined.indexOf('qtd') >= 0 || headerJoined.indexOf('mercadoria') >= 0) {
                        var descIdx = headerCells.findIndex(function(h) { return h.indexOf('descri') >= 0 || h.indexOf('produto') >= 0 || h.indexOf('mercadoria') >= 0; });
                        var codeIdx = headerCells.findIndex(function(h) { return h.indexOf('cód') >= 0 || h.indexOf('cod') >= 0; });
                        var qtyIdx = headerCells.findIndex(function(h) { return h.indexOf('qtd') >= 0 || h.indexOf('quant') >= 0; });
                        var unitIdx = headerCells.findIndex(function(h) { return h === 'un' || h === 'un.' || h === 'unid' || h === 'unid.' || h.indexOf('unid') >= 0 || h.indexOf('med') >= 0; });
                        var priceIdx = headerCells.findIndex(function(h) { return h.indexOf('unit') >= 0 || h.indexOf('vl. un') >= 0 || h.indexOf('valor un') >= 0; });
                        var totalIdx = headerCells.findIndex(function(h) { return h.indexOf('total') >= 0; });

                        for (var r = 1; r < tbl.rows.length; r++) {
                            var cells = tbl.rows[r].cells;
                            if (!cells || cells.length < 3) continue;
                            var texts = Array.from(cells).map(function(c) { return (c.innerText || '').replace(/\s+/g, ' ').trim(); });
                            var desc = (descIdx >= 0 && texts[descIdx]) ? texts[descIdx] : (texts.find(function(t) {
                                var letters = (t.match(/[A-Za-zÀ-ÿ]/g) || []).length;
                                return letters >= 3 && !/^(un|und|kg|g|l|ml|pct|cx|pc|lt)$/i.test(t);
                            }) || '');
                            if (!desc || desc.toLowerCase().indexOf('total') === 0 || desc.toLowerCase().indexOf('subtotal') === 0) continue;

                            var code = (codeIdx >= 0 && texts[codeIdx]) ? texts[codeIdx] : (texts.find(function(t) { return /^[0-9]{4,14}$/.test(t); }) || '');
                            var qty = (qtyIdx >= 0 && texts[qtyIdx]) ? texts[qtyIdx] : '1';
                            var unit = (unitIdx >= 0 && texts[unitIdx]) ? texts[unitIdx] : (texts.find(function(t) { return /^(un|und|unid|kg|g|gr|l|ml|pct|cx|pc|lt)$/i.test(t); }) || 'un');
                            var inlineUnitMatch = (qty + ' ' + texts.join(' ')).match(/[0-9.,]+\s*(kg|unid|und|un|ml|lt|pct|cx|gr)\b/i);
                            if (unit.toLowerCase() === 'un' && inlineUnitMatch) {
                                unit = inlineUnitMatch[1];
                            }
                            var unitPrice = (priceIdx >= 0 && texts[priceIdx]) ? texts[priceIdx] : ((totalIdx >= 0 && texts[totalIdx]) ? texts[totalIdx] : '0');

                            if (priceIdx < 0) {
                                var nums = texts.filter(function(t) { return /^[0-9.,\sR$]+$/.test(t.replace('R$','').trim()) && /[0-9]/.test(t); });
                                if (nums.length >= 1 && qtyIdx < 0) qty = nums[0];
                                if (nums.length >= 2) unitPrice = nums[1];
                                else if (nums.length === 1) unitPrice = nums[0];
                            }

                            items.push({
                                code: code,
                                name: desc,
                                qty: qty,
                                unit: unit,
                                unitPrice: unitPrice
                            });
                        }
                    }
                });
            }

            var rawText = (document.body && document.body.innerText) ? document.body.innerText : '';
            return JSON.stringify({ store: store, items: items, rawText: items.length === 0 ? rawText : '' });
        })();
    """.trimIndent()
}
