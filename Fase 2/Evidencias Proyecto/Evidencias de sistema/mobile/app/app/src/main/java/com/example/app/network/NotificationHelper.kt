package com.example.app.network

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

object NotificationHelper {

    private const val CHANNEL_ID = "bomberos_alerts_channel"
    private const val CHANNEL_NAME = "Alertas de Mantención Bomberos"
    private const val CHANNEL_DESC = "Notificaciones de mantenciones por vencer y vencidas de vehículos de emergencia."

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun areNotificationsEnabled(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun showNotification(context: Context, id: Int, title: String, message: String) {
        try {
            if (!areNotificationsEnabled(context)) {
                return
            }

            createNotificationChannel(context)

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(id, builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun showPdfDownloadNotification(context: Context, pdfUri: Uri, fileName: String) {
        try {
            if (!areNotificationsEnabled(context)) return

            createNotificationChannel(context)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(pdfUri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                clipData = ClipData.newUri(context.contentResolver, "PDF", pdfUri)
            }

            val pendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("Informe PDF Descargado")
                .setContentText("Toca para abrir: $fileName")
                .setStyle(NotificationCompat.BigTextStyle().bigText("El informe técnico se ha descargado correctamente. Toca aquí o usa el botón para abrirlo."))
                .setContentIntent(pendingIntent)
                .addAction(android.R.drawable.ic_menu_view, "Abrir PDF", pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setAutoCancel(true)

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify((System.currentTimeMillis() % 10000).toInt(), builder.build())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
