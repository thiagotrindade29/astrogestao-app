package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.ProductEntity
import com.example.ui.components.formatQty

object LowStockReminderPolicy {
    const val REMINDER_COOLDOWN_MS: Long = 12L * 60L * 60L * 1000L

    fun buildSignature(lowStockProducts: List<ProductEntity>): String {
        if (lowStockProducts.isEmpty()) return ""
        return lowStockProducts.map { it.id }.distinct().sorted().joinToString(",")
    }

    fun shouldSendReminder(
        lowStockProducts: List<ProductEntity>,
        notificationsEnabled: Boolean,
        lastNotifiedSignature: String,
        lastNotifiedAtMillis: Long,
        nowMillis: Long = System.currentTimeMillis()
    ): Boolean {
        if (!notificationsEnabled || lowStockProducts.isEmpty()) return false
        val currentIds = lowStockProducts.map { it.id.toString() }.toSet()
        val previousIds = lastNotifiedSignature
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        val hasNewlyLowStockItem = currentIds.any { it !in previousIds }
        val cooldownElapsed = lastNotifiedAtMillis <= 0L || (nowMillis - lastNotifiedAtMillis) >= REMINDER_COOLDOWN_MS
        return hasNewlyLowStockItem || cooldownElapsed
    }
}

object LowStockNotificationHelper {
    const val CHANNEL_ID = "astro_low_stock_reminders"
    const val NOTIFICATION_ID = 1001

    fun hasPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Lembretes de estoque baixo",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Lembretes simples quando um item ainda não foi reposto no estoque."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun showLowStockReminder(
        context: Context,
        lowStockProducts: List<ProductEntity>
    ): Boolean {
        if (lowStockProducts.isEmpty() || !hasPermission(context)) return false
        ensureChannel(context)

        val sorted = lowStockProducts.sortedBy { it.quantity }
        val title = if (sorted.size == 1) {
            "Lembrete de reposição"
        } else {
            "Lembrete de reposição (${sorted.size} itens)"
        }
        val body = when (sorted.size) {
            1 -> {
                val item = sorted.first()
                "${item.name} está com estoque baixo (${formatQty(item.quantity)} ${item.unit}). Lembre-se de repor quando puder."
            }
            2 -> {
                "${sorted[0].name} e ${sorted[1].name} ainda aguardam reposição no estoque."
            }
            else -> {
                "${sorted[0].name}, ${sorted[1].name} e mais ${sorted.size - 2} item(ns) ainda aguardam reposição."
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_inventory)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setOnlyAlertOnce(true)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
            true
        }.getOrDefault(false)
    }

    fun cancelLowStockReminder(context: Context) {
        runCatching {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
        }
    }
}
