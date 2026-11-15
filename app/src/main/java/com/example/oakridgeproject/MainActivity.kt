package com.example.oakridgeproject

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.oakridgeproject.data.AppPreferencesRepository
import com.example.oakridgeproject.model.InstalledAppInfo
import com.example.oakridgeproject.service.AppBlockerAccessibilityService
import com.example.oakridgeproject.service.AppBlockerMonitorService
import com.example.oakridgeproject.ui.BlockedAppActivity
import com.example.oakridgeproject.ui.components.AnimatedOnboardingTutorial
import com.example.oakridgeproject.ui.components.ConcentrateBrandIcon
import com.example.oakridgeproject.ui.components.ConcentrateLoadingScreen
import com.example.oakridgeproject.ui.components.EndlessPrankUnblockDialog
import com.example.oakridgeproject.ui.components.FuturisticFloatingDock
import com.example.oakridgeproject.ui.components.INSTANT_LOCK_AFTER_ATTEMPT_7BIT
import com.example.oakridgeproject.ui.components.InitialPermissionsSetupDialog
import com.example.oakridgeproject.ui.components.SELF_DESTRUCT_ATTEMPTS_7BIT
import com.example.oakridgeproject.ui.screens.AppSelectionScreen
import com.example.oakridgeproject.ui.screens.BlockerDashboardScreen
import com.example.oakridgeproject.ui.screens.SettingsScreen
import com.example.oakridgeproject.ui.theme.OakRidgeProjectTheme
import com.example.oakridgeproject.util.AppUsageHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val repo = remember { AppPreferencesRepository.getInstance(applicationContext) }
            val themeMode by repo.themeMode.collectAsState()
            val dynamicColor by repo.dynamicColor.collectAsState()

            OakRidgeProjectTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                OakRidgeBlockerApp(repository = repo)
            }
        }
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AppBlockerAccessibilityService.isSelfDestructLockdownActive) {
            val returnIntent = Intent(this, MainActivity::class.java).apply {
                addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                )
            }
            runCatching { startActivity(returnIntent) }
        }
    }
}

@Composable
fun OakRidgeBlockerApp(repository: AppPreferencesRepository) {
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    val themeMode by repository.themeMode.collectAsState()
    val dynamicColor by repository.dynamicColor.collectAsState()
    val isBlockerEnabled by repository.isBlockerEnabled.collectAsState()
    val hasSeenTutorial by repository.hasSeenTutorial.collectAsState()
    val blockRules by repository.blockRules.collectAsState()
    val prankAttemptCount7Bit by repository.prankAttemptCount7Bit.collectAsState()

    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var isLoadingApps by remember { mutableStateOf(true) }
    var hasCompletedInitialBoot by rememberSaveable { mutableStateOf(false) }

    var hasUsagePermission by remember {
        mutableStateOf(AppUsageHelper.hasUsageStatsPermission(context))
    }
    var hasOverlayPermission by remember {
        mutableStateOf(AppUsageHelper.hasOverlayPermission(context))
    }
    var hasAccessibilityPermission by remember {
        mutableStateOf(AppUsageHelper.isAccessibilityServiceEnabled(context))
    }
    var refreshTrigger by remember { mutableIntStateOf(0) }

    var permissionsDialogDismissedInSession by rememberSaveable { mutableStateOf(false) }

    var prankTargetName by rememberSaveable { mutableStateOf<String?>(null) }
    var prankVisible by rememberSaveable { mutableStateOf(false) }

    val handleNewPrankAttempt: (String) -> Unit = { targetName ->
        val nextAttempt7Bit = repository.incrementPrankAttemptCount()
        if (nextAttempt7Bit > INSTANT_LOCK_AFTER_ATTEMPT_7BIT) {
            prankVisible = false
            AppBlockerAccessibilityService.lockDeviceScreen(context)
        } else {
            prankTargetName = targetName
            prankVisible = true
        }
    }

    val isSelfDestructActive = prankVisible && prankAttemptCount7Bit in SELF_DESTRUCT_ATTEMPTS_7BIT
    LaunchedEffect(isSelfDestructActive) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        if (isSelfDestructActive) {
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    LaunchedEffect(isBlockerEnabled, blockRules) {
        AppBlockerMonitorService.syncServiceState(context)
    }

    DisposableEffect(context) {
        val packageReceiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                refreshTrigger++
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(
            context,
            packageReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        onDispose {
            runCatching { context.unregisterReceiver(packageReceiver) }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                repository.checkAndReset24HourCycleIfNeeded()
                hasUsagePermission = AppUsageHelper.hasUsageStatsPermission(context)
                hasOverlayPermission = AppUsageHelper.hasOverlayPermission(context)
                hasAccessibilityPermission = AppUsageHelper.isAccessibilityServiceEnabled(context)
                refreshTrigger++
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(refreshTrigger) {
        repository.checkAndReset24HourCycleIfNeeded()
        val isFirstBoot = !hasCompletedInitialBoot && installedApps.isEmpty()
        if (installedApps.isEmpty()) {
            isLoadingApps = true
        }
        installedApps = withContext(Dispatchers.IO) {
            val appsDeferred = async { AppUsageHelper.getInstalledApps(context) }
            if (isFirstBoot) {
                delay(1350L)
            }
            appsDeferred.await()
        }
        isLoadingApps = false
        hasCompletedInitialBoot = true
    }

    if (!hasSeenTutorial) {
        AnimatedOnboardingTutorial(
            onFinishTutorial = {
                repository.setTutorialSeen(true)
            }
        )
        return
    }

    Crossfade(
        targetState = isLoadingApps && installedApps.isEmpty(),
        animationSpec = tween(durationMillis = 480),
        label = "startup_loading_crossfade"
    ) { showStartupScreen ->
        if (showStartupScreen) {
            ConcentrateLoadingScreen()
        } else {
            val openUsageSettings = {
                context.startActivity(
                    Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }

            val openOverlaySettings = {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                ).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                runCatching {
                    context.startActivity(intent)
                }.onFailure {
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    )
                }
                Unit
            }

            val openAccessibilitySettings = {
                context.startActivity(
                    Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                )
            }

            val needsPermissionsSetup =
                !hasUsagePermission || !hasOverlayPermission || !hasAccessibilityPermission
            if (needsPermissionsSetup && !permissionsDialogDismissedInSession) {
                InitialPermissionsSetupDialog(
                    hasUsagePermission = hasUsagePermission,
                    hasOverlayPermission = hasOverlayPermission,
                    hasAccessibilityPermission = hasAccessibilityPermission,
                    onRequestUsagePermission = openUsageSettings,
                    onRequestOverlayPermission = openOverlaySettings,
                    onRequestAccessibilityPermission = openAccessibilitySettings,
                    onDismiss = { permissionsDialogDismissedInSession = true }
                )
            }

            if (prankVisible && prankTargetName != null) {
                val effectiveCount: UByte =
                    if (prankAttemptCount7Bit == 0u.toUByte()) 1u else prankAttemptCount7Bit
                EndlessPrankUnblockDialog(
                    targetName = prankTargetName.orEmpty(),
                    attemptCount7Bit = effectiveCount,
                    remainingCycleMillis = repository.getRemainingCycleMillis(),
                    onProceedPrankClick = {
                        val nextAttempt7Bit = repository.incrementPrankAttemptCount()
                        if (nextAttempt7Bit > INSTANT_LOCK_AFTER_ATTEMPT_7BIT) {
                            prankVisible = false
                            AppBlockerAccessibilityService.lockDeviceScreen(context)
                        } else {
                            coroutineScope.launch {
                                prankVisible = false
                                delay(140L)
                                prankVisible = true
                            }
                        }
                    },
                    onAcceptFateDismiss = {
                        prankVisible = false
                        prankTargetName = null
                    },
                    onLockDeviceNow = {
                        prankVisible = false
                        AppBlockerAccessibilityService.lockDeviceScreen(context)
                    }
                )
            }

            val activeBlockedCount = remember(blockRules) {
                blockRules.values.count { it.isBlocked }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                containerColor = MaterialTheme.colorScheme.background,
                topBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 22.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ConcentrateBrandIcon(size = 30.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "CONCENTRATE",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    letterSpacing = 2.6.sp
                                ),
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = when (selectedTab) {
                                    0 -> "CATÁLOGO DE APPS"
                                    1 -> "NÚCLEO DE ENFOQUE"
                                    else -> "SISTEMA"
                                },
                                style = MaterialTheme.typography.labelSmall.copy(
                                    letterSpacing = 1.4.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Text(
                            text = "$activeBlockedCount ACTIVAS",
                            style = MaterialTheme.typography.labelSmall.copy(
                                letterSpacing = 1.2.sp
                            ),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = innerPadding.calculateTopPadding())
                ) {
                    when (selectedTab) {
                        0 -> {
                            AppSelectionScreen(
                                apps = installedApps,
                                blockRules = blockRules,
                                isLoading = isLoadingApps,
                                hasUsagePermission = hasUsagePermission,
                                hasOverlayPermission = hasOverlayPermission,
                                hasAccessibilityPermission = hasAccessibilityPermission,
                                onUpdateAppThreshold = { packageName, limitSeconds ->
                                    repository.updateAppThreshold(
                                        packageName = packageName,
                                        limitSeconds = limitSeconds
                                    )
                                },
                                onTriggerPrankUnblock = handleNewPrankAttempt,
                                onRequestUsagePermission = openUsageSettings,
                                onRequestOverlayPermission = openOverlaySettings,
                                onRequestAccessibilityPermission = openAccessibilitySettings
                            )
                        }

                        1 -> {
                            BlockerDashboardScreen(
                                apps = installedApps,
                                blockRules = blockRules,
                                isBlockerEnabled = isBlockerEnabled,
                                hasUsagePermission = hasUsagePermission,
                                hasOverlayPermission = hasOverlayPermission,
                                hasAccessibilityPermission = hasAccessibilityPermission,
                                onToggleBlockerMaster = { enabled ->
                                    repository.setBlockerEnabled(enabled)
                                },
                                onUpdateAppThreshold = { packageName, limitSeconds ->
                                    repository.updateAppThreshold(
                                        packageName = packageName,
                                        limitSeconds = limitSeconds
                                    )
                                },
                                onTriggerPrankUnblock = handleNewPrankAttempt,
                                onTestBlockApp = { app, rule ->
                                    val intent = Intent(context, BlockedAppActivity::class.java).apply {
                                        putExtra(BlockedAppActivity.EXTRA_PACKAGE_NAME, app.packageName)
                                        putExtra(BlockedAppActivity.EXTRA_LIMIT_SECONDS, rule.limitSeconds)
                                        putExtra(
                                            BlockedAppActivity.EXTRA_USED_SECONDS,
                                            app.usedSecondsToday
                                        )
                                    }
                                    context.startActivity(intent)
                                },
                                onNavigateToAppsCatalog = { selectedTab = 0 },
                                onRequestUsagePermission = openUsageSettings,
                                onRequestOverlayPermission = openOverlaySettings,
                                onRequestAccessibilityPermission = openAccessibilitySettings
                            )
                        }

                        else -> {
                            SettingsScreen(
                                selectedThemeMode = themeMode,
                                dynamicColorEnabled = dynamicColor,
                                hasUsagePermission = hasUsagePermission,
                                hasOverlayPermission = hasOverlayPermission,
                                hasAccessibilityPermission = hasAccessibilityPermission,
                                onSelectThemeMode = { mode ->
                                    repository.setThemeMode(mode)
                                },
                                onToggleDynamicColor = { enabled ->
                                    repository.setDynamicColor(enabled)
                                },
                                onOpenUsageSettings = openUsageSettings,
                                onOpenOverlaySettings = openOverlaySettings,
                                onOpenAccessibilitySettings = openAccessibilitySettings
                            )
                        }
                    }

                    // Dock flotante inferior con estética futurista
                    FuturisticFloatingDock(
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                    )
                }
            }
        }
    }
}
