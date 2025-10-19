package com.aidlink.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.aidlink.MainActivity
import com.aidlink.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d("FCM", "New FCM Token: $token")
        sendTokenToFirestore(token)
    }

    private fun sendTokenToFirestore(token: String) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId == null) {
            Log.w("FCM", "Cannot save token, user is not logged in.")
            return
        }
        val tokenData = mapOf("fcmToken" to token)
        FirebaseFirestore.getInstance().collection("users").document(userId)
            .update(tokenData)
            .addOnSuccessListener { Log.i("FCM", "FCM token successfully saved for user: $userId") }
            .addOnFailureListener { e -> Log.e("FCM", "Error saving FCM token", e) }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title
        val body = remoteMessage.notification?.body

        Log.d("FCM", "Message Received. Data payload: ${remoteMessage.data}")
        sendNotification(title, body, remoteMessage.data)
    }

    private fun sendNotification(title: String?, messageBody: String?, data: Map<String, String>) {
        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            for ((key, value) in data) {
                putExtra(key, value)
            }
        }

        val pendingIntent = PendingIntent.getActivity(this, System.currentTimeMillis().toInt(), intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)

        val channelId = "fcm_default_channel"
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "Default Channel", NotificationManager.IMPORTANCE_DEFAULT)
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}