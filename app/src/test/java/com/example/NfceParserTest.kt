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
}
