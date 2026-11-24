package com.example.data.repository

import com.example.data.local.ProductAliasEntity
import com.example.data.local.ProductEntity
import java.text.Normalizer

/**
 * Fonte única de verdade para normalização de nomes, geração de códigos únicos AST-XXXX
 * e correspondência canônica (por código AST/EAN/alias ou por nome normalizado) para evitar
 * duplicação de produtos entre mercados diferentes.
 */
object ProductMatcher {
    private val combiningMarksRegex = "\\p{InCombiningDiacriticalMarks}+".toRegex()
    private val multiSpaceRegex = "\\s+".toRegex()

    fun normalizeProductName(name: String): String {
        val decomposed = Normalizer.normalize(name.trim(), Normalizer.Form.NFD)
        return combiningMarksRegex.replace(decomposed, "")
            .lowercase()
            .replace(multiSpaceRegex, " ")
            .trim()
    }

    fun formatUniqueCode(sequence: Int): String {
        return "AST-${sequence.toString().padStart(4, '0')}"
    }

    fun nextUniqueSequence(products: List<ProductEntity>): Int {
        val maxSeq = products.mapNotNull { prod ->
            prod.uniqueCode.removePrefix("AST-").toIntOrNull()
        }.maxOrNull() ?: 0
        return maxSeq + 1
    }

    fun findByCode(
        products: List<ProductEntity>,
        aliases: List<ProductAliasEntity>,
        rawCode: String
    ): ProductEntity? {
        val cleanCode = rawCode.trim()
        if (cleanCode.isBlank()) return null

        return products.firstOrNull {
            it.uniqueCode.equals(cleanCode, ignoreCase = true) ||
                it.code.equals(cleanCode, ignoreCase = true)
        } ?: aliases.firstOrNull {
            it.externalCode.equals(cleanCode, ignoreCase = true)
        }?.let { alias ->
            products.firstOrNull { it.id == alias.productId }
        }
    }

    fun findByNormalizedName(
        products: List<ProductEntity>,
        name: String
    ): ProductEntity? {
        val target = normalizeProductName(name)
        if (target.isBlank()) return null
        return products.firstOrNull { normalizeProductName(it.name) == target }
    }

    fun findCanonicalMatch(
        products: List<ProductEntity>,
        aliases: List<ProductAliasEntity>,
        code: String,
        name: String
    ): ProductEntity? {
        return findByCode(products, aliases, code)
            ?: findByNormalizedName(products, name)
    }

    fun matchesSearch(
        product: ProductEntity,
        aliasesForProduct: List<ProductAliasEntity>,
        rawQuery: String
    ): Boolean {
        val q = rawQuery.trim()
        if (q.isEmpty()) return true
        val normalizedQ = normalizeProductName(q)
        return normalizeProductName(product.name).contains(normalizedQ) ||
            product.uniqueCode.contains(q, ignoreCase = true) ||
            product.code.contains(q, ignoreCase = true) ||
            aliasesForProduct.any { it.externalCode.contains(q, ignoreCase = true) }
    }
}
