package com.example.oakridgeproject.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.oakridgeproject.MainActivity
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
 * Servicio de accesibilidad que detecta al instante cuándo se abre una aplicación,
 * verifica si el tiempo de uso acumulado supera el umbral configurado (limitSeconds),
 * impide salir de la app durante la cuenta regresiva de autodestrucción y permite
 * bloquear la pantalla del dispositivo (GLOBAL_ACTION_LOCK_SCREEN).
 */
class AppBlockerAccessibilityService : AccessibilityService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var activeMonitorJob: Job? = null
    private var currentForegroundPackage: String? = null
    private var sessionStartTimestampMs: Long = 0L
    private var baselineUsageSeconds: Long = 0L

    private val IGNORED_PACKAGES = setOf(
        "com.android.systemui",
        "com.android.settings",
        "android"
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        instance = this
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return

        // Si el contador de autodestrucción está activo, impedir que el usuario salga de la app
        if (isSelfDestructLockdownActive && pkg != packageName) {
            val returnIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            runCatching { startActivity(returnIntent) }
            return
        }

        val defaultLauncher = AppUsageHelper.getDefaultLauncherPackage(packageManager)
        if (pkg == packageName || pkg == defaultLauncher || pkg in IGNORED_PACKAGES) return

        if (pkg != currentForegroundPackage) {
            currentForegroundPackage = pkg
            sessionStartTimestampMs = System.currentTimeMillis()
            baselineUsageSeconds = AppUsageHelper.getAppUsageSecondsToday(this, pkg)
            evaluateAndStartMonitoring(pkg)
        } else {
            checkAndTriggerBlockIfNeeded(pkg)
        }
    }

    private fun evaluateAndStartMonitoring(pkg: String) {
        activeMonitorJob?.cancel()
        val repo = AppPreferencesRepository.getInstance(this)
        repo.reloadFromDisk()
        if (!repo.isBlockerEnabled.value) return

        val rule = repo.getRuleForPackage(pkg) ?: return
        if (!rule.isBlocked) return

        if (checkAndTriggerBlockIfNeeded(pkg)) {
            return
        }

        activeMonitorJob = serviceScope.launch {
            while (isActive && currentForegroundPackage == pkg) {
                delay(1000L)
                if (checkAndTriggerBlockIfNeeded(pkg)) {
                    break
                }
            }
        }
    }

    private fun checkAndTriggerBlockIfNeeded(pkg: String): Boolean {
        val repo = AppPreferencesRepository.getInstance(this)
        if (!repo.isBlockerEnabled.value) return false

        val rule = repo.getRuleForPackage(pkg) ?: return false
        if (!rule.isBlocked) return false

        val elapsedSessionSeconds =
            ((System.currentTimeMillis() - sessionStartTimestampMs).coerceAtLeast(0L)) / 1000L
        val statsUsageSeconds = AppUsageHelper.getAppUsageSecondsToday(this, pkg)
        val totalUsedSeconds = maxOf(statsUsageSeconds, baselineUsageSeconds + elapsedSessionSeconds)

        if (rule.shouldBlock(totalUsedSeconds)) {
            launchBlockScreen(pkg, rule.limitSeconds, totalUsedSeconds)
            return true
        }
        return false
    }

    private fun launchBlockScreen(
        blockedPackage: String,
        limitSeconds: Long,
        usedSeconds: Long
    ) {
        val intent = Intent(this, BlockedAppActivity::class.java).apply {
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
            putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, blockedPackage)
            putExtra(BlockedAppActivity.EXTRA_LIMIT_SECONDS, limitSeconds)
            putExtra(BlockedAppActivity.EXTRA_USED_SECONDS, usedSeconds)
        }
        startActivity(intent)
    }

    override fun onInterrupt() {
        activeMonitorJob?.cancel()
    }

    override fun onDestroy() {
        if (instance === this) {
            instance = null
        }
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        @Volatile
        private var instance: AppBlockerAccessibilityService? = null

        @Volatile
        var isSelfDestructLockdownActive: Boolean = false

        /**
         * Bloquea la pantalla del dispositivo inmediatamente usando AccessibilityService.
         * Si el servicio de accesibilidad no está activo, envía al usuario a la pantalla de bloqueo/inicio.
         */
        fun lockDeviceScreen(context: Context) {
            isSelfDestructLockdownActive = false
            val activeService = instance
            if (activeService != null) {
                val locked = activeService.performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
                if (locked) return
            }

            // Respaldo en caso de que el usuario aún no haya activado Accesibilidad
            val fallbackIntent = Intent(context, BlockedAppActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP
                )
                putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, "Dispositivo bloqueado")
                putExtra(BlockedAppActivity.EXTRA_LIMIT_SECONDS, 0L)
                putExtra(BlockedAppActivity.EXTRA_USED_SECONDS, 0L)
            }
            runCatching { context.startActivity(fallbackIntent) }
        }
    }
}
