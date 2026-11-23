package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.MovementType
import com.example.ui.theme.AstroTheme
import com.example.ui.theme.JetBrainsMonoFamily
import com.example.ui.viewmodel.MonthlySpendingPoint
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

private val ptBrLocale = Locale("pt", "BR")

fun formatBrl(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(ptBrLocale)
    return formatter.format(amount)
}

fun formatQty(qty: Double): String {
    val symbols = DecimalFormatSymbols(ptBrLocale)
    val df = DecimalFormat("#,##0.##", symbols)
    return df.format(qty)
}

fun formatShortDateBr(millis: Long): String {
    val fmt = SimpleDateFormat("dd/MM/yyyy", ptBrLocale)
    return fmt.format(Date(millis))
}

fun formatDateTimeBr(millis: Long): String {
    val fmt = SimpleDateFormat("dd/MM HH:mm", ptBrLocale)
    return fmt.format(Date(millis))
}

fun daysUntil(expiryMillis: Long): Int {
    val diff = expiryMillis - System.currentTimeMillis()
    return (diff / 86_400_000L).toInt()
}

@Composable
fun AstroBrandLogo(
    modifier: Modifier = Modifier,
    sizeDp: Int = 38
) {
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .clip(RoundedCornerShape((sizeDp * 0.30f).dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF8B5CF6), Color(0xFF2563EB))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(sizeDp.dp)) {
            val w = size.width
            val h = size.height
            // Orbital ring (64x64 reference: cx=32, cy=32, rx=26, ry=10.5, rotate=-32)
            rotate(degrees = -32f, pivot = Offset(w * 0.5f, h * 0.5f)) {
                drawOval(
                    color = Color(0xFFC4B5FD).copy(alpha = 0.9f),
                    topLeft = Offset(w * (6f / 64f), h * (21.5f / 64f)),
                    size = Size(w * (52f / 64f), h * (21f / 64f)),
                    style = Stroke(width = w * (2f / 64f))
                )
            }
            // Stylized A letterform: m22 43 10-25 10 25 M26 34 h12
            val path = Path().apply {
                moveTo(w * (22f / 64f), h * (43f / 64f))
                lineTo(w * (32f / 64f), h * (18f / 64f))
                lineTo(w * (42f / 64f), h * (43f / 64f))
                moveTo(w * (26f / 64f), h * (34f / 64f))
                lineTo(w * (38f / 64f), h * (34f / 64f))
            }
            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(Color.White, Color(0xFFDBEAFE))
                ),
                style = Stroke(
                    width = w * (4f / 64f),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            // Orbiting star dot (51, 18, r=3)
            drawCircle(
                color = Color.White,
                radius = w * (3f / 64f),
                center = Offset(w * (51f / 64f), h * (18f / 64f))
            )
        }
    }
}

@Composable
fun SolidKpiCard(
    label: String,
    value: String,
    subtitle: String,
    containerColor: Color,
    modifier: Modifier = Modifier,
    testTag: String = "kpi_solid"
) {
    Card(
        modifier = modifier.testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.82f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = JetBrainsMonoFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.80f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AlertRedKpiCard(
    label: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    testTag: String = "kpi_alert_red"
) {
    val extended = AstroTheme.extended
    Card(
        modifier = modifier.testTag(testTag),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, extended.borderSubtle),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                modifier = Modifier
                    .width(5.dp)
                    .fillMaxHeight()
                    .background(extended.negative)
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = extended.negative,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = JetBrainsMonoFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun LowStockBadge(modifier: Modifier = Modifier) {
    val extended = AstroTheme.extended
    Surface(
        modifier = modifier,
        color = extended.warningContainer,
        shape = RoundedCornerShape(999.dp)
    ) {
        Text(
            text = stringResource(R.string.badge_low_stock),
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = extended.warning,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun MovementTypeBadge(type: MovementType, modifier: Modifier = Modifier) {
    val extended = AstroTheme.extended
    val (label, textColor, bgColor) = when (type) {
        MovementType.ENTRADA -> Triple("ENTRADA", extended.positive, extended.positiveContainer)
        MovementType.SAIDA -> Triple("SAÍDA", extended.negative, extended.negativeContainer)
        MovementType.AJUSTE -> Triple("AJUSTE", extended.info, extended.infoContainer)
    }
    Surface(
        modifier = modifier,
        color = bgColor,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun CategoryPill(
    name: String,
    colorHex: Long,
    modifier: Modifier = Modifier
) {
    val dotColor = Color(colorHex)
    Surface(
        modifier = modifier,
        color = dotColor.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = name,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun MonthlySpendingCanvasChart(
    points: List<MonthlySpendingPoint>,
    modifier: Modifier = Modifier
) {
    val purchasesColor = MaterialTheme.colorScheme.primary
    val consumptionColor = AstroTheme.extended.positive
    val gridColor = AstroTheme.extended.borderSubtle

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LegendDot(color = purchasesColor, label = "Compras (Entradas)")
            LegendDot(color = consumptionColor, label = "Consumo (Saídas)")
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            if (points.isEmpty()) return@Canvas

            val maxVal = max(
                20.0,
                points.maxOf { max(it.purchases, it.consumption) }
            ).toFloat()

            val topPad = 12f
            val bottomPad = 8f
            val chartHeight = (size.height - topPad - bottomPad).coerceAtLeast(10f)

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            for (step in 0..2) {
                val y = topPad + chartHeight * (step / 2f)
                drawLine(
                    color = gridColor.copy(alpha = 0.7f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 1.5f,
                    pathEffect = if (step < 2) dashEffect else null
                )
            }

            val groupWidth = size.width / points.size.toFloat()
            val barWidth = (groupWidth * 0.24f).coerceIn(10f, 34f)
            val gap = (barWidth * 0.35f).coerceAtLeast(4f)

            points.forEachIndexed { index, pt ->
                val centerX = groupWidth * index + groupWidth / 2f
                val buyHeight = ((pt.purchases.toFloat() / maxVal) * chartHeight).coerceAtLeast(
                    if (pt.purchases > 0.0) 6f else 2f
                )
                val consHeight = ((pt.consumption.toFloat() / maxVal) * chartHeight).coerceAtLeast(
                    if (pt.consumption > 0.0) 6f else 2f
                )

                val buyLeft = centerX - barWidth - gap / 2f
                val consLeft = centerX + gap / 2f
                val baseBottom = topPad + chartHeight

                drawRoundRect(
                    color = purchasesColor,
                    topLeft = Offset(buyLeft, baseBottom - buyHeight),
                    size = Size(barWidth, buyHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                drawRoundRect(
                    color = consumptionColor,
                    topLeft = Offset(consLeft, baseBottom - consHeight),
                    size = Size(barWidth, consHeight),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            points.forEach { pt ->
                Text(
                    text = pt.monthLabel.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    body: String,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    testTag: String = "empty_state_card"
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag(testTag),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, AstroTheme.extended.borderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (primaryActionLabel != null && onPrimaryAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onPrimaryAction,
                    modifier = Modifier.testTag("${testTag}_primary_button")
                ) {
                    Text(text = primaryActionLabel)
                }
            }
            if (secondaryActionLabel != null && onSecondaryAction != null) {
                TextButton(
                    onClick = onSecondaryAction,
                    modifier = Modifier.testTag("${testTag}_secondary_button")
                ) {
                    Text(text = secondaryActionLabel)
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    confirmLabel: String = stringResource(R.string.action_delete),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = Icons.Rounded.DeleteOutline,
                contentDescription = title,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text = message, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = Color.White
                ),
                modifier = Modifier.testTag("confirm_delete_button")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_delete_button")
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
