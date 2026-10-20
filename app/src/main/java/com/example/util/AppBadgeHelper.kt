package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

/**
 * AppBadgeHelper:
 * Manages Home Screen App Launcher Icon Notification Badges / Numbers across:
 * 1. Android 8.0 - Android 15+ (API 26+) native system badge using NotificationChannel + NotificationCompat.setNumber(count).
 * 2. Samsung OneUI / TouchWiz via BADGE_COUNT_UPDATE broadcast.
 * 3. Sony Xperia Home via UPDATE_BADGE broadcast.
 * 4. Huawei EMUI / HarmonyOS via Settings ContentProvider change_badge.
 * 5. Xiaomi MIUI / HyperOS reflection + notification badge count.
 * 6. Apex / Nova / HTC / Asus launchers.
 */
object AppBadgeHelper {

    private const val TAG = "AppBadgeHelper"
    private const val NOTIFICATION_ID = 9901
    private const val CHANNEL_ID = "zoha_app_launcher_badge_channel"
    private const val CHANNEL_NAME = "Zoha Trading Activity & Badges"

    /**
     * Initializes notification channel with badge support.
     */
    fun initNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                if (notificationManager != null) {
                    val existingChannel = notificationManager.getNotificationChannel(CHANNEL_ID)
                    if (existingChannel == null) {
                        val channel = NotificationChannel(
                            CHANNEL_ID,
                            CHANNEL_NAME,
                            NotificationManager.IMPORTANCE_LOW // Low importance so it doesn't make sound/pop-up, but displays the badge dot and number!
                        ).apply {
                            description = "Application activity notification badge for Zoha Trading"
                            setShowBadge(true)
                            enableLights(false)
                            enableVibration(false)
                        }
                        notificationManager.createNotificationChannel(channel)
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to initialize notification channel", e)
            }
        }
    }

    /**
     * Updates the badge count displayed on the mobile home screen app launcher icon.
     */
    fun updateBadgeCount(context: Context, unreadCount: Int) {
        initNotificationChannel(context)

        // 1. Android Standard Notification Badging (Android 8.0+ / API 26+)
        updateSystemNotificationBadge(context, unreadCount)

        // 2. OEM Broadcast-based badging for diverse launchers (Samsung, Sony, Huawei, Xiaomi, etc.)
        updateOemLauncherBadges(context, unreadCount)
    }

    private fun updateSystemNotificationBadge(context: Context, count: Int) {
        try {
            val notificationManager = NotificationManagerCompat.from(context)

            if (count > 0) {
                // If notification permission is denied on Android 13+, areNotificationsEnabled() will be false
                if (!notificationManager.areNotificationsEnabled()) {
                    return
                }

                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    action = Intent.ACTION_MAIN
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                }

                val pendingIntent = PendingIntent.getActivity(
                    context,
                    0,
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_app_logo)
                    .setContentTitle("Zoha Trading")
                    .setContentText(if (count == 1) "1 new activity notification" else "$count new activity notifications")
                    .setNumber(count) // This sets the badge number on the home screen icon on Android 8.0+
                    .setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL)
                    .setOngoing(true)
                    .setAutoCancel(false)
                    .setOnlyAlertOnce(true)
                    .setPriority(NotificationCompat.PRIORITY_LOW)
                    .setContentIntent(pendingIntent)

                // Xiaomi MIUI Badge Count extra
                try {
                    val field = builder.javaClass.getDeclaredField("mNotification")
                    field.isAccessible = true
                    val notif = field.get(builder)
                    val miuiExtra = notif.javaClass.getDeclaredField("extraNotification")
                    miuiExtra.isAccessible = true
                    val extraObj = miuiExtra.get(notif)
                    val setMethod = extraObj.javaClass.getDeclaredMethod("setMessageCount", Int::class.javaPrimitiveType)
                    setMethod.invoke(extraObj, count)
                } catch (_: Throwable) {}

                notificationManager.notify(NOTIFICATION_ID, builder.build())
            } else {
                // Clear notification and badge
                notificationManager.cancel(NOTIFICATION_ID)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error updating system notification badge", e)
        }
    }

    private fun updateOemLauncherBadges(context: Context, count: Int) {
        val packageName = context.packageName
        val activityName = "com.example.MainActivity"

        // 1. Samsung OneUI / TouchWiz & Sony Broadcast
        try {
            val intent = Intent("android.intent.action.BADGE_COUNT_UPDATE").apply {
                putExtra("badge_count", count)
                putExtra("badge_count_package_name", packageName)
                putExtra("badge_count_class_name", activityName)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}

        // 2. Sony Ericsson / Xperia
        try {
            val intent = Intent("com.sonyericsson.home.action.UPDATE_BADGE").apply {
                putExtra("com.sonyericsson.home.intent.extra.badge.PACKAGE_NAME", packageName)
                putExtra("com.sonyericsson.home.intent.extra.badge.ACTIVITY_NAME", activityName)
                putExtra("com.sonyericsson.home.intent.extra.badge.SHOW_MESSAGE", count > 0)
                putExtra("com.sonyericsson.home.intent.extra.badge.MESSAGE", count.toString())
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}

        // 3. Huawei / Honor EMUI (Only execute on verified Huawei/Honor devices with existing launcher provider)
        try {
            val isHuaweiDevice = Build.MANUFACTURER.contains("huawei", ignoreCase = true) ||
                    Build.BRAND.contains("huawei", ignoreCase = true) ||
                    Build.MANUFACTURER.contains("honor", ignoreCase = true)
            if (isHuaweiDevice) {
                val providerInfo = context.packageManager.resolveContentProvider("com.huawei.android.launcher.settings", 0)
                if (providerInfo != null) {
                    val bundle = Bundle().apply {
                        putString("package", packageName)
                        putString("class", activityName)
                        putInt("badgenumber", count)
                    }
                    context.contentResolver.call(
                        Uri.parse("content://com.huawei.android.launcher.settings/badge/"),
                        "change_badge",
                        null,
                        bundle
                    )
                }
            }
        } catch (_: Throwable) {}

        // 4. HTC
        try {
            val intent = Intent("com.htc.launcher.action.UPDATE_SHORTCUT").apply {
                flags = Intent.FLAG_INCLUDE_STOPPED_PACKAGES
                putExtra("packagename", packageName)
                putExtra("count", count)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}

        // 5. Apex Launcher
        try {
            val intent = Intent("com.anddoes.launcher.EXT_NOTIFICATION").apply {
                putExtra("package", packageName)
                putExtra("class", activityName)
                putExtra("count", count)
            }
            context.sendBroadcast(intent)
        } catch (_: Exception) {}
    }
}
