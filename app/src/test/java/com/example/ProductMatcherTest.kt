package com.example

import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import com.example.data.repository.ProductMatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ProductMatcherTest {

    @Test
    fun `normalizeProductName removes accents extra spaces and casing`() {
        assertEquals("pao de acucar 500g", ProductMatcher.normalizeProductName("  Pão   de Açúcar 500G "))
        assertEquals("cafe melitta extra forte", ProductMatcher.normalizeProductName("CAFÉ MELITTA EXTRA FORTE"))
    }

    @Test
    fun `nextUniqueSequence increments past highest AST code`() {
        val products = listOf(
            ProductEntity(id = 1, uniqueCode = "AST-0001", name = "Arroz"),
            ProductEntity(id = 2, uniqueCode = "AST-0014", name = "Feijão"),
            ProductEntity(id = 3, uniqueCode = "", name = "Sem codigo")
        )
        assertEquals(15, ProductMatcher.nextUniqueSequence(products))
        assertEquals("AST-0015", ProductMatcher.formatUniqueCode(15))
    }

    @Test
    fun `findCanonicalMatch matches by primary code alias or normalized name without duplicating`() {
        val arroz = ProductEntity(id = 10, uniqueCode = "AST-0001", code = "7891234560011", name = "Arroz Branco 5kg")
        val aliases = listOf(
            ProductAliasEntity(id = 1, productId = 10, externalCode = "MERCADO_B_9981", marketName = "Mercado B")
        )

        val byEan = ProductMatcher.findCanonicalMatch(listOf(arroz), aliases, "7891234560011", "Outro nome")
        assertEquals(10L, byEan?.id)

        val byAlias = ProductMatcher.findCanonicalMatch(listOf(arroz), aliases, "MERCADO_B_9981", "Arroz B")
        assertEquals(10L, byAlias?.id)

        val byAst = ProductMatcher.findCanonicalMatch(listOf(arroz), aliases, "ast-0001", "")
        assertEquals(10L, byAst?.id)

        val byName = ProductMatcher.findCanonicalMatch(listOf(arroz), aliases, "NOVO_COD_MERCADO_C", "  ARROZ   BRANCO 5KG ")
        assertNotNull(byName)
        assertEquals(10L, byName?.id)

        val noMatch = ProductMatcher.findCanonicalMatch(listOf(arroz), aliases, "9999999", "Feijão Carioca 1kg")
        assertNull(noMatch)
    }

    @Test
    fun `matchesSearch finds product by alias or unique code`() {
        val item = ProductEntity(id = 2, uniqueCode = "AST-0007", code = "111222", name = "Café Torrado")
        val aliases = listOf(ProductAliasEntity(id = 1, productId = 2, externalCode = "EAN-EXTRA-77"))

        assertTrue(ProductMatcher.matchesSearch(item, aliases, "cafe"))
        assertTrue(ProductMatcher.matchesSearch(item, aliases, "AST-0007"))
        assertTrue(ProductMatcher.matchesSearch(item, aliases, "EXTRA-77"))
    }
}
