package com.example.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.local.MakiDatabase
import com.example.domain.calendar.EthiopianCalendarConverter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit

object CelebrationReminderManager {
    const val CHANNEL_ID = "maki_celebration_reminders"
    const val CHANNEL_NAME = "Customer Celebrations & Reminders"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies MAKI BRANDS shop owner 7 days before customer celebrations"
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    fun checkAndNotifyCelebrations(context: Context) {
        createNotificationChannel(context)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MakiDatabase.getDatabase(context)
                val celebrations = db.celebrationDao().getActiveCelebrations().first()
                val today = LocalDate.now()

                celebrations.forEach { cel ->
                    val nextDate = EthiopianCalendarConverter.getNextCelebrationDate(
                        fromDate = today,
                        ethMonth = cel.ethMonth,
                        ethDay = cel.ethDay
                    )
                    val daysUntil = ChronoUnit.DAYS.between(today, nextDate)

                    // If within the reminder window (e.g. 7 days or on day of event)
                    if (daysUntil in 0..cel.reminderDaysBefore) {
                        val customer = db.customerDao().getCustomerById(cel.customerId)
                        val customerName = customer?.fullName ?: "Customer"
                        val customerPhone = customer?.phoneNumber ?: ""

                        // Look up their last purchased product snapshot
                        val sales = db.saleDao().getSalesForCustomer(cel.customerId).first()
                        val lastProduct = sales.firstOrNull()?.productNameSnapshot ?: "Special Gift"

                        val title = "MAKI BRANDS: Upcoming Celebration!"
                        val body = if (daysUntil == 0L) {
                            "$customerName is celebrating ${cel.celebrationName} (${cel.personCelebrated}) TODAY! Contact them now."
                        } else {
                            "$customerName's ${cel.celebrationName} is in $daysUntil days. Last gift: $lastProduct. Contact & recommend a gift!"
                        }

                        sendNotification(
                            context = context,
                            notificationId = cel.id.toInt(),
                            title = title,
                            body = body,
                            customerPhone = customerPhone
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun sendNotification(
        context: Context,
        notificationId: Int,
        title: String,
        body: String,
        customerPhone: String
    ) {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingOpen = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingOpen)

        // Action button to call customer directly via dialer
        if (customerPhone.isNotBlank()) {
            val callIntent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$customerPhone")
            }
            val pendingCall = PendingIntent.getActivity(
                context,
                notificationId + 10000,
                callIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(android.R.drawable.ic_menu_call, "Call Customer", pendingCall)
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}

class CelebrationReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        CelebrationReminderManager.checkAndNotifyCelebrations(context)
    }
}
