package com.example.oakridgeproject.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.oakridgeproject.service.AppBlockerAccessibilityService
import com.example.oakridgeproject.util.AppPhrasesManager
import com.example.oakridgeproject.util.AppUsageHelper
import com.example.oakridgeproject.util.TutorialStepPhrase
import kotlinx.coroutines.delay

val SELF_DESTRUCT_ATTEMPTS_7BIT: Set<UByte> = setOf(10u, 17u, 23u, 69u)
const val INSTANT_LOCK_AFTER_ATTEMPT_7BIT: UByte = 100u

/**
 * Tutorial de bienvenida súper sencillo, corto, minimalista y con toques futuristas.
 */
@Composable
fun AnimatedOnboardingTutorial(
    onFinishTutorial: () -> Unit
) {
    val context = LocalContext.current
    val steps = remember {
        AppPhrasesManager.getTutorialSteps(context).ifEmpty {
            listOf(
                TutorialStepPhrase(
                    emoji = "📱",
                    badge = "Paso 1",
                    title = "Elige tus distracciones",
                    description = "Explora tus apps clasificadas automáticamente."
                )
            )
        }
    }

    var currentStep by remember { mutableIntStateOf(0) }

    val infiniteTransition = rememberInfiniteTransition(label = "tutorial_float")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "tutorial_icon_scale"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 28.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                FuturisticGlassButton(
                    text = "Omitir",
                    onClick = onFinishTutorial,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                )
            }

            AnimatedContent(
                targetState = currentStep.coerceIn(steps.indices),
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn(tween(320)))
                            .togetherWith(slideOutHorizontally { width -> -width } + fadeOut(tween(250)))
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn(tween(320)))
                            .togetherWith(slideOutHorizontally { width -> width } + fadeOut(tween(250)))
                    }
                },
                label = "tutorial_step_transition"
            ) { stepIndex ->
                val step = steps[stepIndex]
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(112.dp)
                            .scale(iconScale)
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
                            text = step.emoji,
                            style = MaterialTheme.typography.displayMedium
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    Text(
                        text = step.badge.uppercase(),
                        style = MaterialTheme.typography.labelMedium.copy(
                            letterSpacing = 1.6.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FuturisticSurfaceCard(
                        modifier = Modifier.fillMaxWidth(),
                        isHighlighted = true
                    ) {
                        Text(
                            text = step.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(22.dp)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    steps.indices.forEach { index ->
                        val isSelected = index == currentStep
                        val dotWidth by animateDpAsState(
                            targetValue = if (isSelected) 30.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "dot_width"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                            animationSpec = tween(300),
                            label = "dot_color"
                        )
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 0) {
                        FuturisticGlassButton(
                            text = "Atrás",
                            onClick = { currentStep-- },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 15.dp)
                        )
                    }

                    FuturisticGlowButton(
                        text = if (currentStep < steps.lastIndex) "Continuar" else "Iniciar Sistema",
                        leadingSymbol = "◈",
                        onClick = {
                            if (currentStep < steps.lastIndex) {
                                currentStep++
                            } else {
                                onFinishTutorial()
                            }
                        },
                        modifier = Modifier.weight(1.3f),
                        height = 50.dp
                    )
                }
            }
        }
    }
}

/**
 * Pop-up de broma infinita con estética futurista y carga dinámica desde `assets/frases_app.json`.
 */
@Composable
fun EndlessPrankUnblockDialog(
    targetName: String,
    attemptCount7Bit: UByte,
    remainingCycleMillis: Long,
    onProceedPrankClick: () -> Unit,
    onAcceptFateDismiss: () -> Unit,
    onLockDeviceNow: () -> Unit
) {
    val context = LocalContext.current
    val isSelfDestructAttempt = attemptCount7Bit in SELF_DESTRUCT_ATTEMPTS_7BIT
    val countInt = attemptCount7Bit.toInt()
    var countdownSeconds by remember(countInt) { mutableIntStateOf(5) }

    BackHandler(enabled = true) {
        if (!isSelfDestructAttempt) {
            onProceedPrankClick()
        }
    }

    DisposableEffect(isSelfDestructAttempt) {
        if (isSelfDestructAttempt) {
            AppBlockerAccessibilityService.isSelfDestructLockdownActive = true
        }
        onDispose {
            if (isSelfDestructAttempt) {
                AppBlockerAccessibilityService.isSelfDestructLockdownActive = false
            }
        }
    }

    LaunchedEffect(countInt, isSelfDestructAttempt) {
        if (isSelfDestructAttempt) {
            countdownSeconds = 5
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds--
            }
            AppBlockerAccessibilityService.isSelfDestructLockdownActive = false
            onLockDeviceNow()
        }
    }

    val reaction = remember(countInt, targetName) {
        AppPhrasesManager.getPrankUnblockReaction(
            context = context,
            attemptCount = countInt,
            targetName = targetName
        )
    }

    val selfDestructPhrase = remember(countInt, countdownSeconds, targetName) {
        AppPhrasesManager.getSelfDestructPhrase(
            context = context,
            attemptCount = countInt,
            countdownSeconds = countdownSeconds,
            targetName = targetName
        )
    }

    val currentProceedLabel = remember(countInt, targetName) {
        AppPhrasesManager.getProceedButtonLabel(
            context = context,
            attemptCount = countInt,
            targetName = targetName
        )
    }

    AlertDialog(
        onDismissRequest = {
            if (!isSelfDestructAttempt) {
                onProceedPrankClick()
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        .border(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isSelfDestructAttempt) selfDestructPhrase.emoji else reaction.emoji,
                        style = MaterialTheme.typography.headlineMedium
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = if (isSelfDestructAttempt) {
                        selfDestructPhrase.title
                    } else {
                        reaction.title
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isSelfDestructAttempt) {
                    Text(
                        text = selfDestructPhrase.message,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Text(
                        text = reaction.message,
                        style = if (countInt == 67) {
                            MaterialTheme.typography.headlineMedium
                        } else {
                            MaterialTheme.typography.bodyMedium
                        },
                        fontWeight = if (countInt == 67) FontWeight.ExtraBold else FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Text(
                        text = "INTENTO $countInt/101 • REINICIO EN ${
                            AppUsageHelper.formatRemainingCycleTime(remainingCycleMillis).uppercase()
                        }",
                        style = MaterialTheme.typography.labelSmall.copy(
                            letterSpacing = 1.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            if (!isSelfDestructAttempt) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    FuturisticGlowButton(
                        text = currentProceedLabel,
                        isDanger = true,
                        onClick = onProceedPrankClick,
                        modifier = Modifier.fillMaxWidth(),
                        height = 48.dp
                    )

                    FuturisticGlassButton(
                        text = "Mantener bloqueo activo",
                        onClick = onAcceptFateDismiss,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    )
                }
            }
        }
    )
}

/**
 * Diálogo inicial minimalista y futurista que solicita todos los permisos necesarios al abrir la app.
 */
@Composable
fun InitialPermissionsSetupDialog(
    hasUsagePermission: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestUsagePermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        ),
        title = {
            Column {
                Text(
                    text = "ENLACES REQUERIDOS",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.6.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Activa los módulos del sistema para iniciar el bloqueo",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PermissionStepItem(
                    stepNumber = "01",
                    title = "Telemetría de Uso",
                    isGranted = hasUsagePermission,
                    onClick = onRequestUsagePermission
                )

                PermissionStepItem(
                    stepNumber = "02",
                    title = "Escudo de Superposición",
                    isGranted = hasOverlayPermission,
                    onClick = onRequestOverlayPermission
                )

                PermissionStepItem(
                    stepNumber = "03",
                    title = "Núcleo de Accesibilidad",
                    isGranted = hasAccessibilityPermission,
                    onClick = onRequestAccessibilityPermission
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                    PermissionStepItem(
                        stepNumber = "04",
                        title = "Canal de Notificaciones",
                        isGranted = false,
                        onClick = {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
            }
        },
        confirmButton = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FuturisticGlassButton(
                    text = "Después",
                    onClick = onDismiss,
                    modifier = Modifier.weight(0.8f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                )

                val allReady = hasUsagePermission && hasOverlayPermission && hasAccessibilityPermission
                FuturisticGlowButton(
                    text = when {
                        !hasUsagePermission -> "1. Uso"
                        !hasOverlayPermission -> "2. Pantalla"
                        !hasAccessibilityPermission -> "3. Núcleo"
                        allReady -> "Listo"
                        else -> "Conectar"
                    },
                    leadingSymbol = "⚡",
                    onClick = {
                        when {
                            !hasUsagePermission -> onRequestUsagePermission()
                            !hasOverlayPermission -> onRequestOverlayPermission()
                            !hasAccessibilityPermission -> onRequestAccessibilityPermission()
                            else -> onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1.2f),
                    height = 44.dp
                )
            }
        }
    )
}

@Composable
private fun PermissionStepItem(
    stepNumber: String,
    title: String,
    isGranted: Boolean,
    onClick: () -> Unit
) {
    FuturisticSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        isHighlighted = isGranted,
        onClick = if (!isGranted) onClick else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (isGranted) "✓" else stepNumber,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (!isGranted) {
                FuturisticGlassButton(
                    text = "Activar",
                    isSelected = true,
                    onClick = onClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}
