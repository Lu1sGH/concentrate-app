package com.example.oakridgeproject.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oakridgeproject.data.AppPreferencesRepository
import com.example.oakridgeproject.ui.components.FuturisticGlowButton
import com.example.oakridgeproject.ui.components.FuturisticSurfaceCard
import com.example.oakridgeproject.ui.theme.OakRidgeProjectTheme
import com.example.oakridgeproject.util.AppPhrasesManager
import com.example.oakridgeproject.util.AppUsageHelper

class BlockedAppActivity : ComponentActivity() {

    private var blockedPackageState by mutableStateOf("")
    private var limitSecondsState by mutableLongStateOf(0L)
    private var openTriggerId by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        onBackPressedDispatcher.addCallback(
            this,
            object : OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    navigateToHomeScreen()
                }
            }
        )

        extractIntentData(intent)

        setContent {
            val repo = remember { AppPreferencesRepository.getInstance(applicationContext) }
            val themeMode by repo.themeMode.collectAsState()
            val dynamicColor by repo.dynamicColor.collectAsState()

            OakRidgeProjectTheme(
                themeMode = themeMode,
                dynamicColor = dynamicColor
            ) {
                BlockedAppScreen(
                    packageName = blockedPackageState,
                    limitSeconds = limitSecondsState,
                    openTriggerId = openTriggerId,
                    onExitToHome = ::navigateToHomeScreen
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractIntentData(intent)
    }

    private fun extractIntentData(sourceIntent: Intent?) {
        blockedPackageState = sourceIntent?.getStringExtra(EXTRA_PACKAGE_NAME).orEmpty()
        limitSecondsState = sourceIntent?.getLongExtra(EXTRA_LIMIT_SECONDS, 0L) ?: 0L
        openTriggerId++
    }

    private fun navigateToHomeScreen() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    companion object {
        const val EXTRA_PACKAGE_NAME = "extra_package_name"
        const val EXTRA_LIMIT_SECONDS = "extra_limit_seconds"
        const val EXTRA_USED_SECONDS = "extra_used_seconds"
    }
}

@Composable
private fun BlockedAppScreen(
    packageName: String,
    limitSeconds: Long,
    openTriggerId: Int,
    onExitToHome: () -> Unit
) {
    val context = LocalContext.current
    val appName = remember(packageName) {
        runCatching {
            val pm = context.packageManager
            val info = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(info).toString()
        }.getOrDefault(packageName.ifBlank { "esta app" })
    }

    val passiveAggressiveMessage = remember(packageName, appName, openTriggerId) {
        AppPhrasesManager.getNextPassiveAggressivePhraseForOpening(
            context = context,
            packageName = packageName,
            appName = appName
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "lock_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 30.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    .border(
                        1.5.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🙄",
                    style = MaterialTheme.typography.displaySmall
                )
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "ACCESO BLOQUEADO",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 2.sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = appName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            FuturisticSurfaceCard(
                modifier = Modifier.fillMaxWidth(),
                isHighlighted = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "“$passiveAggressiveMessage”",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "UMBRAL • ${AppUsageHelper.formatDurationThreshold(limitSeconds).uppercase()}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.2.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            FuturisticGlowButton(
                text = "Hacer algo productivo",
                leadingSymbol = "◈",
                onClick = onExitToHome,
                modifier = Modifier.fillMaxWidth(),
                height = 54.dp
            )
        }
    }
}
