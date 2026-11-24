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
        RegionalPortal("42", "SC", "Santa Catarina (SC — SAT)", "https://sat.sef.sc.gov.br/tax.NET/Sat.NFe.Web/Consultas/ConsultaPublicaNFe.aspx?amb=PROD"),
        RegionalPortal("35", "SP", "São Paulo (SP)", "https://www.nfce.fazenda.sp.gov.br/NFCeConsultaPublica/Paginas/ConsultaPublica.aspx"),
        RegionalPortal("28", "SE", "Sergipe (SE)", "https://www.nfce.sefaz.se.gov.br/portal/portalNoticias.jsp"),
        RegionalPortal("17", "TO", "Tocantins (TO)", "https://www.sefaz.to.gov.br/nfce/consulta.jsf")
    )

    fun extractAccessKey(input: String): String? {
        val digitsOnly = input.replace(Regex("[^0-9]"), "")
        return Regex("\\d{44}").find(digitsOnly)?.value
    }

    fun detectForInput(input: String): RegionalPortal {
        val trimmed = input.trim()
        val key = extractAccessKey(trimmed)
        if (key != null && key.length >= 2) {
            val uf = key.substring(0, 2)
            allPortals.firstOrNull { it.ufCode == uf }?.let { return it }
        }
        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            allPortals.firstOrNull { portal ->
                val host = portal.url.substringAfter("://").substringBefore("/")
                trimmed.contains(host, ignoreCase = true)
            }?.let { return it }

            allPortals.firstOrNull { portal ->
                portal.ufCode != "00" && trimmed.contains(".${portal.ufSigla.lowercase()}.gov.br", ignoreCase = true)
            }?.let { return it }
        }
        return allPortals.first()
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
                        var unitIdx = headerCells.findIndex(function(h) { return h === 'un' || h === 'un.' || h.indexOf('unid') >= 0; });
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
