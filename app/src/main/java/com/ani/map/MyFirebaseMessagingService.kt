package com.ani.map

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val CHANNEL_ID = "update_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle the data payload
        remoteMessage.data.let { data ->
            val title = data["title"] ?: "Update Available"
            val message = data["message"] ?: "A new version of the app is available!"
            sendNotification(title, message)
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (notificationManager.getNotificationChannel(CHANNEL_ID) == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "App Update Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for app updates"
                }
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Intent to open the GitHub Releases page
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse("https://github.com/YeswanthKasi/Ecorvi_Tracking_App_New/releases")
        }

        // Create a PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Build the notification
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // Replace with your app's notification icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Display the notification
        notificationManager.notify(NOTIFICATION_ID, notification)

        Log.d("MyFirebaseMessagingService", "Notification displayed: $title - $message")
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send the new token to your backend if needed
        Log.d("MyFirebaseMessagingService", "New token generated: $token")
    }
}
