package com.example.util

import androidx.compose.ui.graphics.Color
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

val UNIT_OPTIONS = listOf("un", "pct", "cx", "kg", "g", "L", "ml")

val LOCATION_SUGGESTIONS = listOf(
    "Despensa",
    "Geladeira",
    "Freezer",
    "Armário Cozinha",
    "Lavanderia",
    "Banheiro",
    "Garagem / Ferramentas"
)

val PRESET_CATEGORY_COLORS = listOf(
    "#f59e0b",
    "#0ea5e9",
    "#10b981",
    "#8b5cf6",
    "#ec4899",
    "#ef4444",
    "#64748b"
)

private val ptBrLocale = Locale.forLanguageTag("pt-BR")

fun formatCurrency(value: Double): String {
    return NumberFormat.getCurrencyInstance(ptBrLocale).format(value)
}

fun formatNumber(value: Double): String {
    val nf = NumberFormat.getNumberInstance(ptBrLocale)
    nf.maximumFractionDigits = 2
    nf.minimumFractionDigits = 0
    return nf.format(value)
}

fun formatIsoDate(dateString: String?): String {
    if (dateString.isNullOrBlank()) return "—"
    return try {
        val parsed = LocalDate.parse(dateString.take(10))
        parsed.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
    } catch (_: Exception) {
        dateString
    }
}

fun formatTimestamp(date: Date?): String {
    if (date == null) return "Agora"
    return SimpleDateFormat("dd/MM/yy HH:mm", ptBrLocale).format(date)
}

fun daysUntilExpiration(dateString: String?): Long? {
    if (dateString.isNullOrBlank()) return null
    return try {
        val exp = LocalDate.parse(dateString.take(10))
        ChronoUnit.DAYS.between(LocalDate.now(), exp)
    } catch (_: Exception) {
        null
    }
}

enum class StockStatusKind { EMPTY, LOW, OK }

data class StockStatus(
    val kind: StockStatusKind,
    val label: String
)

fun getStockStatus(quantity: Double, minQuantity: Double): StockStatus {
    return when {
        quantity <= 0.0 -> StockStatus(StockStatusKind.EMPTY, "Sem estoque")
        quantity <= minQuantity -> StockStatus(StockStatusKind.LOW, "Estoque baixo")
        else -> StockStatus(StockStatusKind.OK, "Normal")
    }
}

fun parseHexColor(hex: String?, fallback: Color = Color(0xFF3B82F6)): Color {
    if (hex.isNullOrBlank()) return fallback
    return try {
        val clean = hex.trim().removePrefix("#")
        val value = clean.toLong(16)
        when (clean.length) {
            6 -> Color(0xFF000000L or value)
            8 -> Color(value)
            else -> fallback
        }
    } catch (_: Exception) {
        fallback
    }
}
