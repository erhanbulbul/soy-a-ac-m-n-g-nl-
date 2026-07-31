package com.example.data.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Yeni FCM Token Alındı: $token")
        sendTokenToFirestore(token)
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Yeni Mesaj Alındı. Sender: ${remoteMessage.from}")

        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "Yeni Mesaj"
        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "Yeni bir bildiriminiz var."

        showNotification(title, body)
    }

    private fun showNotification(title: String, messageBody: String) {
        val channelId = "soyaagaci_chat_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Sohbet Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Gelen sohbet ve arkadaşlık bildirimleri"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }

    companion object {
        private const val TAG = "FCM_Service"

        fun sendTokenToFirestore(token: String) {
            val currentUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUid)
                .set(mapOf("fcmToken" to token, "lastTokenUpdate" to System.currentTimeMillis()), SetOptions.merge())
                .addOnSuccessListener {
                    Log.d(TAG, "FCM Token Firestore'a başarıyla kaydedildi. UID: $currentUid")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "FCM Token Firestore'a kaydedilemedi: ${e.message}", e)
                }
        }
    }
}
