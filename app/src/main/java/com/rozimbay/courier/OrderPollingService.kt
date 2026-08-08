package com.rozimbay.courier

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

class OrderPollingService : Service() {

    private val handler = Handler(Looper.getMainLooper())
    private val pollIntervalMs = 15_000L
    private var knownOrderIds = mutableSetOf<String>()
    private var firstRun = true

    companion object {
        const val CHANNEL_ID = "new_order_channel"
        const val FOREGROUND_NOTIF_ID = 1
        const val ORDER_NOTIF_ID = 2
        const val ORDERS_URL = "${MainActivity.BASE_URL}/courier/api/get_orders.php"
    }

    private val pollRunnable = object : Runnable {
        override fun run() {
            Thread { checkForNewOrders() }.start()
            handler.postDelayed(this, pollIntervalMs)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannels()
        startForeground(FOREGROUND_NOTIF_ID, buildForegroundNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        handler.removeCallbacks(pollRunnable)
        handler.post(pollRunnable)
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(pollRunnable)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            val serviceChannel = NotificationChannel(
                "service_channel", "Xizmat holati", NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(serviceChannel)

            val orderChannel = NotificationChannel(
                CHANNEL_ID, "Yangi buyurtmalar", NotificationManager.IMPORTANCE_HIGH
            )
            orderChannel.enableVibration(true)
            orderChannel.vibrationPattern = longArrayOf(0, 500, 250, 500)
            val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            orderChannel.setSound(soundUri, audioAttributes)
            manager.createNotificationChannel(orderChannel)
        }
    }

    private fun buildForegroundNotification(): android.app.Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, "service_channel")
            .setContentTitle("Kuryer ilovasi ishlamoqda")
            .setContentText("Yangi buyurtmalar kuzatilmoqda")
            .setSmallIcon(android.R.drawable.ic_menu_send)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun checkForNewOrders() {
        val cookie = getSharedPreferences(MainActivity.PREFS, MODE_PRIVATE)
            .getString("session_cookie", null) ?: return

        try {
            val url = URL(ORDERS_URL)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("Cookie", cookie)
            connection.connectTimeout = 10_000
            connection.readTimeout = 10_000

            if (connection.responseCode != 200) {
                connection.disconnect()
                return
            }

            val body = connection.inputStream.bufferedReader().use { it.readText() }
            connection.disconnect()

            val json = org.json.JSONObject(body)
            if (!json.optBoolean("success", false)) return

            val orders: JSONArray = json.optJSONArray("orders") ?: JSONArray()
            val currentIds = mutableSetOf<String>()
            val newOrders = mutableListOf<org.json.JSONObject>()

            for (i in 0 until orders.length()) {
                val order = orders.getJSONObject(i)
                val id = order.optString("id")
                currentIds.add(id)
                if (!firstRun && !knownOrderIds.contains(id)) {
                    newOrders.add(order)
                }
            }

            if (newOrders.isNotEmpty()) {
                notifyNewOrders(newOrders)
            }

            knownOrderIds = currentIds
            firstRun = false

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun notifyNewOrders(newOrders: List<org.json.JSONObject>) {
        val manager = getSystemService(NotificationManager::class.java)

        val text = if (newOrders.size == 1) {
            val order = newOrders[0]
            "Manzil: ${order.optString("address")} — ${order.optString("amount")} so'm"
        } else {
            "${newOrders.size} ta yangi buyurtma mavjud"
        }

        val openIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 1, openIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Yangi buyurtma!")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(ORDER_NOTIF_ID, notification)
    }
}
