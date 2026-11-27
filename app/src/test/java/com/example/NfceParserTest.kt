package com.example

import com.example.data.nfce.NfceParser
import com.example.data.nfce.SefazPortals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class NfceParserTest {

    @Test
    fun `detectForInput identifies Brazilian states by 44-digit key prefix and portal URL`() {
        val scKey = "42260312345678000199650010000123451000123456"
        val prKey = "41260312345678000199650010000123451000123456"
        val spKey = "35260312345678000199650010000123451000123456"

        assertEquals("SC", SefazPortals.detectForInput(scKey).ufSigla)
        assertEquals("PR", SefazPortals.detectForInput(prKey).ufSigla)
        assertEquals("SP", SefazPortals.detectForInput(spKey).ufSigla)
        assertEquals(
            "SC",
            SefazPortals.detectForInput("https://sat.sef.sc.gov.br/tax.NET/Sat.NFe.Web/Consultas/ConsultaPublicaNFe.aspx?amb=PROD").ufSigla
        )
        assertEquals(
            "SC",
            SefazPortals.detectForInput("https://sat.sef.sc.gov.br/tax.NET/Sat.DFe.NFCe.Web/Consultas/NFCe_Detalhes.aspx?rq=t8BOzGQx").ufSigla
        )
        assertTrue(SefazPortals.allPortals.size >= 27)
    }

    @Test
    fun `parseNfceXml extracts store key and product items`() {
        val xml = """
            <nfeProc>
              <NFe>
                <infNFe Id="NFe42260312345678000199650010000123451000123456">
                  <emit><xNome>SUPERMERCADO ESTRELA LTDA</xNome></emit>
                  <det nItem="1">
                    <prod>
                      <cProd>001</cProd>
                      <cEAN>7891000100103</cEAN>
                      <xProd>LEITE INTEGRAL 1L</xProd>
                      <uCom>UN</uCom>
                      <qCom>6.0000</qCom>
                      <vUnCom>4.89</vUnCom>
                    </prod>
                  </det>
                  <det nItem="2">
                    <prod>
                      <cProd>002</cProd>
                      <cEAN>SEM GTIN</cEAN>
                      <xProd>BANANA PRATA</xProd>
                      <uCom>KG</uCom>
                      <qCom>1.250</qCom>
                      <vUnCom>6.40</vUnCom>
                    </prod>
                  </det>
                </infNFe>
              </NFe>
            </nfeProc>
        """.trimIndent()

        val result = NfceParser.parseNfceXml(xml)
        assertEquals("SUPERMERCADO ESTRELA LTDA", result.storeName)
        assertEquals("42260312345678000199650010000123451000123456", result.accessKey)
        assertEquals("SC", result.detectedUfSigla)
        assertEquals(2, result.items.size)
        assertEquals("7891000100103", result.items[0].code)
        assertEquals("LEITE INTEGRAL 1L", result.items[0].name)
        assertEquals(6.0, result.items[0].quantity, 0.001)
        assertEquals(4.89, result.items[0].unitPrice, 0.001)
        assertEquals("002", result.items[1].code)
        assertEquals("kg", result.items[1].unit)
    }

    @Test
    fun `parseRawReceiptText extracts items from pasted Brazilian receipt text`() {
        val text = """
            SUPERMERCADO CENTRAL
            ARROZ TIPO 1 5KG  2 un  R$ 27,90
            FEIJAO CARIOCA 1KG  3 un  R$ 7,50
        """.trimIndent()

        val result = NfceParser.parseRawReceiptText(text)
        assertEquals(2, result.items.size)
        assertEquals("ARROZ TIPO 1 5KG", result.items[0].name)
        assertEquals(2.0, result.items[0].quantity, 0.001)
        assertEquals(27.90, result.items[0].unitPrice, 0.001)
    }

    @Test
    fun `parseOcrReceiptText extracts items from SEFAZ print format`() {
        val sefazPrint = """
            BISTEK SUPERMERCADOS LTDA
            LEITE INTEGRAL TIROL 1L (Código: 7896256600123)
            Qtde.: 6 UN: UN Vl. Unit.: 4, 89
            ACUCAR REFINADO UNIAO 1KG (Código: 7891910000197)
            Qtde.: 2 UN: UN Vl. Unit.: 5,50
        """.trimIndent()

        val result = NfceParser.parseOcrReceiptText(sefazPrint)
        assertEquals(2, result.items.size)
        assertEquals("LEITE INTEGRAL TIROL 1L", result.items[0].name)
        assertEquals("7896256600123", result.items[0].code)
        assertEquals(6.0, result.items[0].quantity, 0.001)
        assertEquals(4.89, result.items[0].unitPrice, 0.001)
        assertEquals("ACUCAR REFINADO UNIAO 1KG", result.items[1].name)
        assertEquals(2.0, result.items[1].quantity, 0.001)
        assertEquals(5.50, result.items[1].unitPrice, 0.001)
    }

    @Test
    fun `parseOcrReceiptText extracts multiline thermal receipt with spaced decimals and tax flags`() {
        val ocrText = """
            ANGELONI SUPERMERCADOS LTDA
            CNPJ: 83.646.984/0001-01
            001 7891000100103 LEITE INTEGRAL 1L
            6,000 UN x 4, 89   29,34
            002 7891910000197 CAFE SOLUVEL
            TRADICIONAL 200G   18,90 F1
            003 FEIJAO PRETO 1KG   2 UN   15,00 T18%
        """.trimIndent()

        val result = NfceParser.parseOcrReceiptText(ocrText)
        assertEquals(3, result.items.size)

        assertEquals("LEITE INTEGRAL 1L", result.items[0].name)
        assertEquals(6.0, result.items[0].quantity, 0.001)
        assertEquals(4.89, result.items[0].unitPrice, 0.001)

        assertEquals("CAFE SOLUVEL TRADICIONAL 200G", result.items[1].name)
        assertEquals(1.0, result.items[1].quantity, 0.001)
        assertEquals(18.90, result.items[1].unitPrice, 0.001)

        assertEquals("FEIJAO PRETO 1KG", result.items[2].name)
        assertEquals(2.0, result.items[2].quantity, 0.001)
        assertEquals(7.50, result.items[2].unitPrice, 0.001)
    }

    @Test
    fun `resolvePortalUrl separates SC NFC-e model 65 from NF-e model 55`() {
        val scPortal = SefazPortals.allPortals.first { it.ufCode == "42" }
        val scNfce65Key = "42261144907858000100651040000489121436426346"
        val scNfe55Key = "42261144907858000100551040000489121436426346"

        assertEquals(
            "https://sat.sef.sc.gov.br/tax.NET/Sat.DFe.NFCe.Web/Consultas/ConsultaPublicaNFCe.aspx",
            SefazPortals.resolvePortalUrl(scPortal, scNfce65Key)
        )
        assertEquals(
            "https://sat.sef.sc.gov.br/tax.NET/Sat.NFe.Web/Consultas/ConsultaPublicaNFe.aspx?amb=PROD",
            SefazPortals.resolvePortalUrl(scPortal, scNfe55Key)
        )
    }

    @Test
    fun `parseOcrReceiptText extracts all 6 items from real SC receipt with wrapped lines`() {
        val realReceiptOcr = """
            MERCADO, ACOUGUE E PADARIA RENASCER LTDA - EPP
            PANIFICADORA RENASCER
            CNPJ: 44.907.858/0001-00 IE: 261497383
            RUA NOSSA SENHORA DE FATIMA, 298, VILA CUBATAO, JOINVILLE,
            SANTA CATARINA
            CEP: 89226-810
            Documento Auxiliar da Nota Fiscal de Consumidor Eletronica
            CODIGO DESCRICAO QTDE UN VL.UNIT VL.TOTAL
            7891149200504 CERVEJA SKOL 350ML   1,000 UN   4,00   4,00
            2000022006217 PAO DE QUEIJO   0,222 kg   27,99   6,21
            2000140007004 SANDUICHE NATURAL BAGUETTI
            1,000 UN   7,00   7,00
            2000103006990 PASTEL FRITO DE CALABRESA C \\ QUEIJO
            1,000 UN   6,99   6,99
            2000130004594 COXINHA 60GR   0,170 kg   26,99   4,59
            7894900701517 REFRIGERANTE COCA COLA 2L ZERO ACUCAR
            1,000 UN   11,99   11,99
            Qtde. total de itens 6
            Valor total R$ 40,78
            Valor a Pagar R$ 40,78
            FORMA DE PAGAMENTO VALOR PAGO R$
            PIX TEF 40.78
            Consulte pela Chave de Acesso em
            https://sat.sef.sc.gov.br/nfce/consulta
            4226 1144 9078 5800 0100 6510 4000 0489 1214 3642 6346
        """.trimIndent()

        val result = NfceParser.parseOcrReceiptText(realReceiptOcr)
        assertEquals(6, result.items.size)
        assertEquals("SC", result.detectedUfSigla)
        assertEquals("42261144907858000100651040000489121436426346", result.accessKey)
        assertEquals("CERVEJA SKOL 350ML", result.items[0].name)
        assertEquals("PAO DE QUEIJO", result.items[1].name)
        assertEquals("kg", result.items[1].unit)
        assertEquals("SANDUICHE NATURAL BAGUETTI", result.items[2].name)
        assertEquals(7.00, result.items[2].unitPrice, 0.001)
        assertEquals("PASTEL FRITO DE CALABRESA C / QUEIJO", result.items[3].name)
        assertEquals(6.99, result.items[3].unitPrice, 0.001)
        assertEquals("COXINHA 60GR", result.items[4].name)
        assertEquals(26.99, result.items[4].unitPrice, 0.001)
        assertEquals("REFRIGERANTE COCA COLA 2L ZERO ACUCAR", result.items[5].name)
        assertEquals(11.99, result.items[5].unitPrice, 0.001)
    }

    @Test
    fun `parseWebViewJson ignores rawText during autoScan and filters generic document titles`() {
        val portalSearchJson = """
            {"store":"Consulta Pública NFC-e","items":[],"rawText":"4226 1144 9078 5800 0100 6510 4000 0489 1214 3642 6346\n1. Digite a chave\nValor 10,00"}
        """.trimIndent()
        val autoScanEmpty = NfceParser.parseWebViewJson(portalSearchJson, allowRawTextFallback = false)
        assertTrue(autoScanEmpty.items.isEmpty())

        val extractedJson = """
            {
              "store": "DOCUMENTO AUXILIAR DA NOTA FISCAL DE CONSUMIDOR ELETRÔNICA",
              "items": [
                {"code":"2000022006217","name":"PAO DE QUEIJO","qty":"0,222","unit":"kg","unitPrice":"27,99"}
              ],
              "rawText": ""
            }
        """.trimIndent()
        val parsed = NfceParser.parseWebViewJson(extractedJson, allowRawTextFallback = false)
        assertEquals(null, parsed.storeName)
        assertEquals(1, parsed.items.size)
        assertEquals("PAO DE QUEIJO", parsed.items[0].name)
        assertEquals("kg", parsed.items[0].unit)
        assertEquals(0.222, parsed.items[0].quantity, 0.0001)
    }

    @Test
    fun `isAllowedWebViewNavigationUrl permits SEFAZ ASPNET queries and blocks unsafe schemes`() {
        assertTrue(
            SefazPortals.isAllowedWebViewNavigationUrl(
                "https://sat.sef.sc.gov.br/tax.NET/Sat.DFe.NFCe.Web/Consultas/ConsultaPublicaNFCe.aspx?msg=Nota valida|1"
            )
        )
        assertTrue(!SefazPortals.isAllowedWebViewNavigationUrl("javascript:alert(1)"))
        assertTrue(!SefazPortals.isAllowedWebViewNavigationUrl("file:///etc/hosts"))
        assertTrue(!SefazPortals.isAllowedWebViewNavigationUrl("https://127.0.0.1/admin"))
        assertEquals(
            "https://sat.sef.sc.gov.br/nfce/consulta?p=123",
            SefazPortals.upgradeGovHttpRedirect("http://sat.sef.sc.gov.br/nfce/consulta?p=123")
        )
    }
}
