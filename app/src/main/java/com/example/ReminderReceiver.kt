package com.example

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val prefs: SharedPreferences = context.getSharedPreferences("pdf_master_settings", Context.MODE_PRIVATE)
        val remindersEnabled = prefs.getBoolean("reminder_enabled", true)
        
        // Respect the user's setting in Settings Screen
        if (!remindersEnabled) return

        val messages = listOf(
            "Ready to organize more PDFs?",
            "Your PDF tools are waiting.",
            "Compress your documents in seconds.",
            "Merge, split, and scan with PDF MASTER!",
            "Need to sign or edit a PDF? Open PDF MASTER now."
        )
        val randomMessage = messages.random()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "pdf_master_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "PDF MASTER Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Friendly tips & activity reminders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // System default icon since we don't have a customized notification icon yet
            .setContentTitle("PDF MASTER")
            .setContentText(randomMessage)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(200, notification)
    }
}
