package com.example.oakridgeproject.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import com.example.oakridgeproject.data.AppPreferencesRepository
import com.example.oakridgeproject.model.AppCategory
import com.example.oakridgeproject.model.InstalledAppInfo
import com.example.oakridgeproject.service.AppBlockerAccessibilityService
import java.util.Locale

object AppUsageHelper {

    private val SOCIAL_PACKAGES_KEYWORDS = setOf(
        "instagram", "facebook", "tiktok", "musically", "zhiliaoapp",
        "twitter", "com.twitter", "whatsapp", "telegram", "snapchat",
        "reddit", "discord", "linkedin", "pinterest", "threads",
        "bereal", "tumblr", "wechat", "line.android", "viber",
        "kik", "signal", "mastodon", "bluesky", "bsky", "tinder", "bumble",
        "messaging", "mms", "contacts", "dialer", "gm", "gmail", "email",
        "meet", "tachyon", "skype", "zoom", "teams", "slack"
    )

    private val RECREATIONAL_PACKAGES_KEYWORDS = setOf(
        "youtube", "netflix", "twitch", "spotify", "disney", "hbo",
        "primevideo", "amazon.avod", "crunchyroll", "soundcloud",
        "deezer", "tidal", "pandora", "roblox", "minecraft", "supercell",
        "brawlstars", "clash", "genshin", "epicgames", "steam",
        "playstation", "xbox", "nintendo", "vlc", "mxtech", "plex",
        "letterboxd", "imdb", "webtoon", "manga", "comic", "game",
        "photos", "gallery", "camera", "videos", "music", "podcast",
        "radio", "player", "entertainment"
    )

    /**
     * Paquetes críticos de infraestructura que nunca deben bloquearse para no impedir
     * el uso básico de la pantalla de inicio o ajustes del sistema.
     */
    private val ESSENTIAL_INFRASTRUCTURE_PACKAGES = setOf(
        "android",
        "com.android.systemui",
        "com.android.settings",
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.google.android.permissioncontroller",
        "com.android.permissioncontroller"
    )

    /**
     * Obtiene todas las aplicaciones instaladas en el sistema (tanto preinstaladas con interfaz
     * como todas las que el usuario instale) y las clasifica en Redes Sociales, Apps Recreativas y Otras Apps.
     */
    fun getInstalledApps(context: Context): List<InstalledAppInfo> {
        val pm = context.packageManager
        val ownPackage = context.packageName
        val defaultLauncherPkg = getDefaultLauncherPackage(pm)
        val usageMap = getCycleUsageSecondsMap(context)

        val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val launcherApps = pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
        val launchablePackages = launcherApps
            .mapNotNull { it.activityInfo?.applicationInfo?.packageName }
            .toSet()

        val allInstalledApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val seenPackages = mutableSetOf<String>()
        val result = mutableListOf<InstalledAppInfo>()

        for (appInfo in allInstalledApps) {
            val pkg = appInfo.packageName

            if (pkg == ownPackage ||
                pkg == defaultLauncherPkg ||
                pkg in ESSENTIAL_INFRASTRUCTURE_PACKAGES ||
                !seenPackages.add(pkg)
            ) {
                continue
            }

            val isUserInstalled = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) == 0
            val isLaunchable = pkg in launchablePackages || pm.getLaunchIntentForPackage(pkg) != null

            if (!isLaunchable && !isUserInstalled) {
                continue
            }

            val appName = runCatching {
                pm.getApplicationLabel(appInfo).toString().trim()
            }.getOrDefault(pkg)

            if (!isLaunchable && (appName == pkg || appName.startsWith("com."))) {
                continue
            }

            val icon = runCatching {
                pm.getApplicationIcon(appInfo)
            }.getOrNull()

            val category = categorizeApp(appInfo, pkg, appName)
            val usedSeconds = usageMap[pkg] ?: 0L
            val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0

            result.add(
                InstalledAppInfo(
                    packageName = pkg,
                    appName = appName,
                    icon = icon,
                    category = category,
                    usedSecondsToday = usedSeconds,
                    isSystemApp = isSystem
                )
            )
        }

        return result.sortedWith(
            compareBy<InstalledAppInfo> { it.category.ordinal }
                .thenBy { it.appName.lowercase(Locale.getDefault()) }
        )
    }

    /**
     * Clasifica una aplicación en Redes Sociales, Apps Recreativas u Otras Apps.
     */
    fun categorizeApp(
        appInfo: ApplicationInfo,
        packageName: String,
        appName: String
    ): AppCategory {
        val lowerPkg = packageName.lowercase(Locale.ROOT)
        val lowerName = appName.lowercase(Locale.ROOT)

        when (appInfo.category) {
            ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
            ApplicationInfo.CATEGORY_GAME,
            ApplicationInfo.CATEGORY_VIDEO,
            ApplicationInfo.CATEGORY_AUDIO,
            ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.RECREATIONAL
        }

        if (SOCIAL_PACKAGES_KEYWORDS.any { lowerPkg.contains(it) || lowerName.contains(it) }) {
            return AppCategory.SOCIAL
        }
        if (RECREATIONAL_PACKAGES_KEYWORDS.any { lowerPkg.contains(it) || lowerName.contains(it) }) {
            return AppCategory.RECREATIONAL
        }

        return AppCategory.OTHER
    }

    fun getDefaultLauncherPackage(pm: PackageManager): String? {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
        }
        val resolveInfo = pm.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        return resolveInfo?.activityInfo?.packageName
    }

    /**
     * Obtiene el paquete de la aplicación que está actualmente en primer plano mediante UsageEvents.
     */
    fun getCurrentForegroundPackage(context: Context): String? {
        if (!hasUsageStatsPermission(context)) return null
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return null

        val now = System.currentTimeMillis()
        val start = now - 1000L * 60L * 15L
        val events = usageStatsManager.queryEvents(start, now)
        val event = UsageEvents.Event()
        var lastResumedPackage: String? = null
        var lastTimestamp = 0L

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED &&
                event.timeStamp >= lastTimestamp
            ) {
                lastResumedPackage = event.packageName
                lastTimestamp = event.timeStamp
            }
        }
        return lastResumedPackage
    }

    /**
     * Calcula los segundos de uso en primer plano desde que inició el ciclo actual de 24 horas
     * (en lugar de reiniciar a las 12:00 am).
     */
    fun getCycleUsageSecondsMap(context: Context): Map<String, Long> {
        if (!hasUsageStatsPermission(context)) return emptyMap()
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return emptyMap()

        val repo = AppPreferencesRepository.getInstance(context)
        repo.checkAndReset24HourCycleIfNeeded()
        val cycleStartMs = repo.cycleStartTimestampMs.value
        val now = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(cycleStartMs, now)
        val event = UsageEvents.Event()
        val totalForegroundMsMap = mutableMapOf<String, Long>()
        val lastResumeMap = mutableMapOf<String, Long>()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                UsageEvents.Event.ACTIVITY_RESUMED -> {
                    lastResumeMap[pkg] = event.timeStamp
                }
                UsageEvents.Event.ACTIVITY_PAUSED,
                UsageEvents.Event.ACTIVITY_STOPPED -> {
                    val start = lastResumeMap.remove(pkg)
                    if (start != null && event.timeStamp >= start) {
                        totalForegroundMsMap[pkg] =
                            (totalForegroundMsMap[pkg] ?: 0L) + (event.timeStamp - start)
                    }
                }
            }
        }

        for ((pkg, start) in lastResumeMap) {
            if (now > start) {
                totalForegroundMsMap[pkg] = (totalForegroundMsMap[pkg] ?: 0L) + (now - start)
            }
        }

        return totalForegroundMsMap.mapValues { (_, ms) -> ms / 1000L }
    }

    /**
     * Obtiene los segundos de uso de una app específica dentro de la ventana actual de 24 horas
     * desde que se inició el uso de la app / contador (no a las 12:00 am).
     */
    fun getAppUsageSecondsToday(context: Context, packageName: String): Long {
        if (!hasUsageStatsPermission(context)) return 0L
        val usageStatsManager =
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
                ?: return 0L

        val repo = AppPreferencesRepository.getInstance(context)
        repo.checkAndReset24HourCycleIfNeeded()
        val cycleStartMs = repo.cycleStartTimestampMs.value
        val now = System.currentTimeMillis()

        val events = usageStatsManager.queryEvents(cycleStartMs, now)
        var totalForegroundMs = 0L
        var lastResumeTime: Long? = null
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.packageName == packageName) {
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        lastResumeTime = event.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        val start = lastResumeTime
                        if (start != null && event.timeStamp >= start) {
                            totalForegroundMs += (event.timeStamp - start)
                            lastResumeTime = null
                        }
                    }
                }
            }
        }

        lastResumeTime?.let { start ->
            if (now > start) {
                totalForegroundMs += (now - start)
            }
        }

        return totalForegroundMs / 1000L
    }

    @Suppress("DEPRECATION")
    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager
            ?: return false
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasOverlayPermission(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }

    fun isAccessibilityServiceEnabled(context: Context): Boolean {
        val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager
            ?: return false
        val enabledServices =
            am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val expectedClass = AppBlockerAccessibilityService::class.java.name
        return enabledServices.any { info ->
            val serviceInfo = info.resolveInfo?.serviceInfo
            serviceInfo?.packageName == context.packageName &&
                (serviceInfo.name == expectedClass || serviceInfo.name.endsWith("AppBlockerAccessibilityService"))
        }
    }

    fun formatDurationThreshold(seconds: Long): String {
        if (seconds <= 0L) return "0 s (Instantáneo)"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remSeconds = seconds % 60
        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (remSeconds > 0 && hours == 0L) append("${remSeconds}s")
        }.trim()
    }

    fun formatUsageTime(seconds: Long): String {
        if (seconds <= 0L) return "0 s"
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val remSeconds = seconds % 60
        return buildString {
            if (hours > 0) append("${hours}h ")
            if (minutes > 0) append("${minutes}m ")
            if (hours == 0L && (minutes == 0L || remSeconds > 0)) append("${remSeconds}s")
        }.trim()
    }

    fun formatRemainingCycleTime(remainingMillis: Long): String {
        val totalMinutes = (remainingMillis.coerceAtLeast(0L)) / (1000L * 60L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L
        return "${hours}h ${minutes}m"
    }
}
