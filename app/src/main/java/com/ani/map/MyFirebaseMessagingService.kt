package com.ani.map

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Handle data payload
        remoteMessage.data.let { data ->
            val title = data["title"] ?: "Update Available"
            val message = data["message"] ?: "A new version of the app is available!"
            sendNotification(title, message)
        }
    }

    private fun sendNotification(title: String, message: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "update_channel"

        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Update Notifications",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        // Create an intent to open the app or the GitHub Releases page
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = android.net.Uri.parse("https://github.com/YeswanthKasi/Ecorvi_Tracking_App_New/releases")
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

        // Build notification
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        // Show notification
        notificationManager.notify(0, notification)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send the new token to your backend if needed
    }
}
