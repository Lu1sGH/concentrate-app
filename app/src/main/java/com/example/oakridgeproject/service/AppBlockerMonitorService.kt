package com.example.oakridgeproject.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.oakridgeproject.MainActivity
import com.example.oakridgeproject.R
import com.example.oakridgeproject.data.AppPreferencesRepository
import com.example.oakridgeproject.ui.BlockedAppActivity
import com.example.oakridgeproject.util.AppUsageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Servicio en primer plano encargado de supervisar continuamente el uso de aplicaciones
 * y aplicar el bloqueo cuando una app restringida supera su umbral X de tiempo (o al instante si es 0 s).
 */
class AppBlockerMonitorService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null

    private var trackedPackage: String? = null
    private var sessionStartMs: Long = 0L
    private var baselineUsageSeconds: Long = 0L

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_MONITORING) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        val repo = AppPreferencesRepository.getInstance(this)
        val activeCount = repo.blockRules.value.values.count { it.isBlocked }
        startForeground(NOTIFICATION_ID, buildNotification(activeCount))

        startMonitoringLoop()
        return START_STICKY
    }

    private fun startMonitoringLoop() {
        if (monitorJob?.isActive == true) return

        monitorJob = serviceScope.launch {
            val repo = AppPreferencesRepository.getInstance(this@AppBlockerMonitorService)
            val defaultLauncher = AppUsageHelper.getDefaultLauncherPackage(packageManager)

            while (isActive) {
                repo.reloadFromDisk()
                if (repo.isBlockerEnabled.value) {
                    val fgPkg = AppUsageHelper.getCurrentForegroundPackage(this@AppBlockerMonitorService)

                    if (fgPkg != null &&
                        fgPkg != packageName &&
                        fgPkg != defaultLauncher &&
                        fgPkg != "com.android.systemui" &&
                        fgPkg != "com.android.settings"
                    ) {
                        if (fgPkg != trackedPackage) {
                            trackedPackage = fgPkg
                            sessionStartMs = System.currentTimeMillis()
                            baselineUsageSeconds =
                                AppUsageHelper.getAppUsageSecondsToday(this@AppBlockerMonitorService, fgPkg)
                        }

                        val rule = repo.getRuleForPackage(fgPkg)
                        if (rule != null && rule.isBlocked) {
                            val elapsedSec =
                                ((System.currentTimeMillis() - sessionStartMs).coerceAtLeast(0L)) / 1000L
                            val statsSec =
                                AppUsageHelper.getAppUsageSecondsToday(this@AppBlockerMonitorService, fgPkg)
                            val totalUsedSec = maxOf(statsSec, baselineUsageSeconds + elapsedSec)

                            if (rule.shouldBlock(totalUsedSec)) {
                                launchBlockedScreen(
                                    blockedPackage = fgPkg,
                                    limitSeconds = rule.limitSeconds,
                                    usedSeconds = totalUsedSec
                                )
                            }
                        }
                    } else {
                        trackedPackage = null
                    }
                }
                delay(800L)
            }
        }
    }

    private fun launchBlockedScreen(
        blockedPackage: String,
        limitSeconds: Long,
        usedSeconds: Long
    ) {
        val blockIntent = Intent(this, BlockedAppActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, blockedPackage)
            putExtra(BlockedAppActivity.EXTRA_LIMIT_SECONDS, limitSeconds)
            putExtra(BlockedAppActivity.EXTRA_USED_SECONDS, usedSeconds)
        }
        runCatching {
            startActivity(blockIntent)
        }
    }

    private fun createNotificationChannel() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Motor de Bloqueo de Aplicaciones",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Mantiene activo el bloqueo de aplicaciones según su tiempo límite de uso"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(blockedAppsCount: Int): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Bloqueador de aplicaciones activo")
            .setContentText("Supervisando $blockedAppsCount aplicaciones restringidas")
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        monitorJob?.cancel()
        serviceScope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val CHANNEL_ID = "oakridge_blocker_channel"
        private const val NOTIFICATION_ID = 1001
        const val ACTION_STOP_MONITORING = "com.example.oakridgeproject.ACTION_STOP_MONITORING"

        fun syncServiceState(context: Context) {
            val repo = AppPreferencesRepository.getInstance(context)
            val hasActiveRules = repo.blockRules.value.values.any { it.isBlocked }
            val shouldRun = repo.isBlockerEnabled.value && hasActiveRules

            if (shouldRun) {
                val intent = Intent(context, AppBlockerMonitorService::class.java)
                runCatching {
                    context.startForegroundService(intent)
                }
            } else {
                val stopIntent = Intent(context, AppBlockerMonitorService::class.java).apply {
                    action = ACTION_STOP_MONITORING
                }
                runCatching {
                    context.stopService(stopIntent)
                }
            }
        }
    }
}
