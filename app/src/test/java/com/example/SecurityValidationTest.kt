package com.example

import com.example.data.nfce.NfceParser
import com.example.data.nfce.SefazPortals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecurityValidationTest {

    @Test
    fun `sefaz url sanitizer allows official gov_br https and upgrades http gov_br`() {
        val scUrl = "https://sat.sef.sc.gov.br/tax.NET/Sat.DFe.NFCe.Web/Consultas/NFCe_Detalhes.aspx?rq=abc"
        assertEquals(scUrl, SefazPortals.sanitizeSefazUrl(scUrl))

        val legacyHttpGov = "http://www.fazenda.pr.gov.br/nfce/consulta?p=123"
        assertEquals(
            "https://www.fazenda.pr.gov.br/nfce/consulta?p=123",
            SefazPortals.sanitizeSefazUrl(legacyHttpGov)
        )

        val scQrV2WithPipes =
            "https://sat.sef.sc.gov.br/nfce/consulta?p=42261144907858000100651040000489121436426346|2|1|1|ABCDEF0123456789"
        assertEquals(
            "https://sat.sef.sc.gov.br/nfce/consulta?p=42261144907858000100651040000489121436426346%7C2%7C1%7C1%7CABCDEF0123456789",
            SefazPortals.sanitizeSefazUrl(scQrV2WithPipes)
        )
    }

    @Test
    fun `sefaz url sanitizer blocks ssrf, private ips, arbitrary hosts and unsafe schemes`() {
        assertNull(SefazPortals.sanitizeSefazUrl("http://127.0.0.1:8080/admin"))
        assertNull(SefazPortals.sanitizeSefazUrl("https://localhost/secret"))
        assertNull(SefazPortals.sanitizeSefazUrl("http://169.254.169.254/latest/meta-data/"))
        assertNull(SefazPortals.sanitizeSefazUrl("https://192.168.1.1/"))
        assertNull(SefazPortals.sanitizeSefazUrl("https://evil.example.com/phish"))
        assertNull(SefazPortals.sanitizeSefazUrl("file:///etc/passwd"))
        assertNull(SefazPortals.sanitizeSefazUrl("javascript:alert(1)"))
        assertNull(SefazPortals.sanitizeSefazUrl("intent://scan/#Intent;scheme=zxing;end"))
        assertNull(SefazPortals.sanitizeSefazUrl("https://user:pass@sat.sef.sc.gov.br/"))
    }

    @Test
    fun `webview navigation allows only https external non-private hosts`() {
        assertTrue(SefazPortals.isAllowedWebViewNavigationUrl("https://sat.sef.sc.gov.br/consulta"))
        assertTrue(
            SefazPortals.isAllowedWebViewNavigationUrl(
                "https://sat.sef.sc.gov.br/nfce/consulta?p=42261144907858000100651040000489121436426346|2|1|1|HASH"
            )
        )
        assertFalse(SefazPortals.isAllowedWebViewNavigationUrl("http://sat.sef.sc.gov.br/consulta"))
        assertFalse(SefazPortals.isAllowedWebViewNavigationUrl("file:///data/data/com.example/files/a"))
        assertFalse(SefazPortals.isAllowedWebViewNavigationUrl("content://com.example/secret"))
        assertFalse(SefazPortals.isAllowedWebViewNavigationUrl("javascript:alert(document.cookie)"))
        assertFalse(SefazPortals.isAllowedWebViewNavigationUrl("https://127.0.0.1/internal"))
    }

    @Test
    fun `nfce fetch and xml parser reject untrusted urls and doctype entities`() {
        val blockedResult = NfceParser.fetchAndParseUrl("http://169.254.169.254/latest/meta-data/")
        assertFalse(blockedResult.requiresWebPortal)
        assertNull(blockedResult.portalUrl)
        assertTrue(blockedResult.items.isEmpty())

        val xxeXml = """
            <?xml version="1.0"?>
            <!DOCTYPE foo [ <!ENTITY xxe SYSTEM "file:///etc/passwd"> ]>
            <nfeProc><NFe><infNFe><det><prod><xProd>&xxe;</xProd></prod></det></infNFe></NFe></nfeProc>
        """.trimIndent()
        val xmlResult = NfceParser.parseNfceXml(xxeXml)
        assertTrue(xmlResult.items.isEmpty())
        assertNotNull(xmlResult.message)
    }
}
