package com.example.oakridgeproject.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Ícono vectorial minimalista y animado de "Concentrate":
 * Anillos de enfoque segmentados, prisma romboidal y núcleo convergente.
 */
@Composable
fun ConcentrateBrandIcon(
    modifier: Modifier = Modifier,
    size: Dp = 30.dp,
    animated: Boolean = true
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    val infiniteTransition = rememberInfiniteTransition(label = "brand_icon_anim")
    val outerAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "brand_outer_angle"
    )
    val corePulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = if (animated) 1.15f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "brand_core_pulse"
    )

    Canvas(modifier = modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val radius = w * 0.42f

        val ringBrush = Brush.linearGradient(
            colors = listOf(primaryColor, tertiaryColor)
        )

        // Anillos de enfoque giratorios
        rotate(degrees = outerAngle, pivot = center) {
            drawArc(
                brush = ringBrush,
                startAngle = -55f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
            )
            drawArc(
                brush = ringBrush,
                startAngle = 125f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2f, radius * 2f),
                style = Stroke(width = w * 0.075f, cap = StrokeCap.Round)
            )
        }

        // Prisma romboidal central
        val diamondRadius = w * 0.28f
        val diamondPath = Path().apply {
            moveTo(center.x, center.y - diamondRadius)
            lineTo(center.x + diamondRadius, center.y)
            lineTo(center.x, center.y + diamondRadius)
            lineTo(center.x - diamondRadius, center.y)
            close()
        }
        drawPath(
            path = diamondPath,
            color = primaryColor.copy(alpha = 0.16f)
        )
        drawPath(
            path = diamondPath,
            brush = ringBrush,
            style = Stroke(width = w * 0.06f, join = StrokeJoin.Round)
        )

        // Núcleo luminoso de concentración
        val coreRadius = w * 0.11f * corePulse
        val corePath = Path().apply {
            moveTo(center.x, center.y - coreRadius)
            lineTo(center.x + coreRadius, center.y)
            lineTo(center.x, center.y + coreRadius)
            lineTo(center.x - coreRadius, center.y)
            close()
        }
        drawPath(
            path = corePath,
            color = primaryColor
        )
    }
}

/**
 * Pantalla de inicio / carga futurista y minimalista para "Concentrate".
 * Reemplaza el indicador circular estéril por un núcleo orbital convergente,
 * telemetría dinámica, haz de escaneo láser y tarjetas holográficas de pre-carga.
 */
@Composable
fun ConcentrateLoadingScreen(
    modifier: Modifier = Modifier,
    customStatusText: String? = null
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

    val telemetryMessages = remember {
        listOf(
            "SINCRONIZANDO NÚCLEO DE ENFOQUE",
            "ANALIZANDO ECOSISTEMA DE APPS",
            "CALIBRANDO BARRERAS DE TIEMPO",
            "ACTIVANDO ESCUDO CONCENTRATE"
        )
    }
    var statusIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(650L)
            statusIndex = (statusIndex + 1) % telemetryMessages.size
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "concentrate_loader")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rot"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rot"
    )

    val rippleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ripple_wave"
    )

    val convergencePulse by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "convergence_pulse"
    )

    val laserSweep by infiniteTransition.animateFloat(
        initialValue = -0.35f,
        targetValue = 1.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1450, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "laser_sweep"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Orbe Central de Convergencia "Concentrate"
            Box(
                modifier = Modifier.size(188.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val center = Offset(w / 2f, h / 2f)
                    val maxRadius = w * 0.45f

                    val cyberGradient = Brush.linearGradient(
                        colors = listOf(primaryColor, tertiaryColor, primaryColor)
                    )

                    // 1. Halo radial de fondo
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                primaryColor.copy(alpha = 0.22f),
                                tertiaryColor.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = maxRadius * 1.15f
                        ),
                        radius = maxRadius * 1.15f,
                        center = center
                    )

                    // 2. Onda expansiva de pulso (Sonar)
                    val waveRadius = maxRadius * (0.35f + rippleProgress * 0.72f)
                    drawCircle(
                        color = primaryColor.copy(alpha = (1f - rippleProgress) * 0.45f),
                        radius = waveRadius,
                        center = center,
                        style = Stroke(width = 1.5.dp.toPx())
                    )

                    // 3. Anillo orbital exterior segmentado
                    rotate(degrees = outerRotation, pivot = center) {
                        val rOut = maxRadius * 0.88f
                        for (angle in listOf(-60f, 60f, 180f)) {
                            drawArc(
                                brush = cyberGradient,
                                startAngle = angle,
                                sweepAngle = 72f,
                                useCenter = false,
                                topLeft = Offset(center.x - rOut, center.y - rOut),
                                size = Size(rOut * 2f, rOut * 2f),
                                style = Stroke(width = 3.2.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    }

                    // 4. Retícula orbital intermedia en contra-rotación
                    rotate(degrees = innerRotation, pivot = center) {
                        val rMid = maxRadius * 0.66f
                        drawArc(
                            color = tertiaryColor.copy(alpha = 0.65f),
                            startAngle = 15f,
                            sweepAngle = 135f,
                            useCenter = false,
                            topLeft = Offset(center.x - rMid, center.y - rMid),
                            size = Size(rMid * 2f, rMid * 2f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                        drawArc(
                            color = primaryColor.copy(alpha = 0.65f),
                            startAngle = 195f,
                            sweepAngle = 135f,
                            useCenter = false,
                            topLeft = Offset(center.x - rMid, center.y - rMid),
                            size = Size(rMid * 2f, rMid * 2f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // 5. Vectores cardinales que convergen hacia el centro
                    val tickOuter = maxRadius * (0.78f - convergencePulse * 0.08f)
                    val tickInner = maxRadius * (0.52f - convergencePulse * 0.08f)
                    val tickStroke = 2.4.dp.toPx()

                    // Arriba, Abajo, Izquierda, Derecha
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x, center.y - tickOuter),
                        end = Offset(center.x, center.y - tickInner),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x, center.y + tickInner),
                        end = Offset(center.x, center.y + tickOuter),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x - tickOuter, center.y),
                        end = Offset(center.x - tickInner, center.y),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                    drawLine(
                        color = primaryColor,
                        start = Offset(center.x + tickInner, center.y),
                        end = Offset(center.x + tickOuter, center.y),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )

                    // 6. Prisma romboidal de enfoque
                    val prismRadius = maxRadius * 0.42f
                    val prismPath = Path().apply {
                        moveTo(center.x, center.y - prismRadius)
                        lineTo(center.x + prismRadius, center.y)
                        lineTo(center.x, center.y + prismRadius)
                        lineTo(center.x - prismRadius, center.y)
                        close()
                    }
                    drawPath(
                        path = prismPath,
                        color = primaryColor.copy(alpha = 0.14f)
                    )
                    drawPath(
                        path = prismPath,
                        brush = cyberGradient,
                        style = Stroke(width = 2.8.dp.toPx(), join = StrokeJoin.Round)
                    )

                    // 7. Núcleo singular brillante
                    val coreRadius = maxRadius * (0.16f + convergencePulse * 0.05f)
                    val corePath = Path().apply {
                        moveTo(center.x, center.y - coreRadius)
                        lineTo(center.x + coreRadius, center.y)
                        lineTo(center.x, center.y + coreRadius)
                        lineTo(center.x - coreRadius, center.y)
                        close()
                    }
                    drawPath(
                        path = corePath,
                        brush = cyberGradient
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 3.2.dp.toPx(),
                        center = center
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Identidad tipográfica "CONCENTRATE"
            Text(
                text = "CONCENTRATE",
                style = MaterialTheme.typography.headlineSmall.copy(
                    letterSpacing = 6.sp
                ),
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Telemetría dinámica animada
            AnimatedContent(
                targetState = customStatusText ?: telemetryMessages[statusIndex],
                transitionSpec = {
                    fadeIn(tween(260)) togetherWith fadeOut(tween(200))
                },
                label = "telemetry_status"
            ) { currentMessage ->
                Text(
                    text = currentMessage,
                    style = MaterialTheme.typography.labelSmall.copy(
                        letterSpacing = 1.8.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = primaryColor.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Barra de escaneo láser futurista
            Canvas(
                modifier = Modifier
                    .width(200.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(surfaceColor.copy(alpha = 0.55f))
            ) {
                val barWidth = size.width
                val beamWidth = barWidth * 0.42f
                val startX = barWidth * laserSweep

                drawLine(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            Color.Transparent,
                            primaryColor,
                            tertiaryColor,
                            Color.Transparent
                        ),
                        startX = startX,
                        endX = startX + beamWidth
                    ),
                    start = Offset(startX, size.height / 2f),
                    end = Offset(startX + beamWidth, size.height / 2f),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round
                )
            }

            Spacer(modifier = Modifier.height(34.dp))

            // Tarjetas holográficas fantasma que anticipan la interfaz sin verse estériles
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                repeat(3) { index ->
                    val rowAlpha = (0.22f + (1f - index * 0.28f) * convergencePulse * 0.35f)
                        .coerceIn(0.12f, 0.55f)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(surfaceColor.copy(alpha = rowAlpha * 0.6f))
                            .border(
                                1.dp,
                                primaryColor.copy(alpha = rowAlpha * 0.45f),
                                RoundedCornerShape(18.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(primaryColor.copy(alpha = rowAlpha * 0.55f))
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(if (index == 1) 0.52f else 0.68f)
                                    .height(10.dp)
                                    .clip(CircleShape)
                                    .background(
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = rowAlpha * 0.45f)
                                    )
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.32f)
                                    .height(7.dp)
                                    .clip(CircleShape)
                                    .background(primaryColor.copy(alpha = rowAlpha * 0.4f))
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(20.dp)
                                .clip(CircleShape)
                                .background(primaryColor.copy(alpha = rowAlpha * 0.35f))
                        )
                    }
                }
            }
        }
    }
}

/**
 * Botón principal futurista con gradiente luminoso, borde brillante sutil
 * y animación elástica al presionar.
 */
@Composable
fun FuturisticGlowButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isDanger: Boolean = false,
    leadingSymbol: String? = null,
    height: Dp = 52.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "glow_btn_scale"
    )

    val gradientColors = if (isDanger) {
        listOf(
            MaterialTheme.colorScheme.error,
            MaterialTheme.colorScheme.errorContainer
        )
    } else {
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.tertiary
        )
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = 0.45f),
            gradientColors.first().copy(alpha = 0.2f)
        )
    )

    val contentColor = if (isDanger) {
        MaterialTheme.colorScheme.onError
    } else {
        MaterialTheme.colorScheme.onPrimary
    }

    Box(
        modifier = modifier
            .scale(scale)
            .height(height)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(gradientColors))
            .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(18.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (leadingSymbol != null) {
                Text(
                    text = leadingSymbol,
                    style = MaterialTheme.typography.labelLarge,
                    color = contentColor
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 0.6.sp
                ),
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Botón secundario translúcido estilo cristal (Glassmorphism) con borde luminoso
 * y respuesta táctil animada.
 */
@Composable
fun FuturisticGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isAccentDanger: Boolean = false,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "glass_btn_scale"
    )

    val accentColor = when {
        isAccentDanger -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline
    }

    val containerColor by animateColorAsState(
        targetValue = when {
            isAccentDanger -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.28f)
            isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
            else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.55f)
        },
        animationSpec = tween(250),
        label = "glass_bg"
    )

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accentColor.copy(alpha = if (isSelected || isAccentDanger) 0.8f else 0.35f),
            accentColor.copy(alpha = 0.1f)
        )
    )

    val textColor = when {
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
        isAccentDanger -> MaterialTheme.colorScheme.error
        isSelected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(14.dp))
            .background(containerColor)
            .border(BorderStroke(1.dp, borderBrush), RoundedCornerShape(14.dp))
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(contentPadding),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.4.sp
                ),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Interruptor cápsula futurista que reemplaza al Switch tradicional por un orbe deslizante con luz.
 */
@Composable
fun FuturisticOrbToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "orb_scale"
    )

    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 24.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "orb_offset"
    )

    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
        },
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label = "orb_track"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
        } else {
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        },
        animationSpec = tween(250),
        label = "orb_border"
    )

    val orbColor by animateColorAsState(
        targetValue = if (checked) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
        },
        animationSpec = tween(250),
        label = "orb_color"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .width(54.dp)
            .height(30.dp)
            .clip(CircleShape)
            .background(trackColor)
            .border(1.dp, borderColor, CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) }
            )
            .padding(horizontal = 4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(22.dp)
                .clip(CircleShape)
                .background(orbColor),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary)
                )
            }
        }
    }
}

/**
 * Tarjeta minimalista con acabado de cristal y borde sutil degradado.
 */
@Composable
fun FuturisticSurfaceCard(
    modifier: Modifier = Modifier,
    isHighlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val accent = if (isHighlighted) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accent.copy(alpha = if (isHighlighted) 0.65f else 0.28f),
            accent.copy(alpha = if (isHighlighted) 0.15f else 0.06f)
        )
    )

    val bgColor = if (isHighlighted) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.75f)
    }

    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        shape = RoundedCornerShape(22.dp),
        color = bgColor,
        border = BorderStroke(1.dp, borderBrush)
    ) {
        content()
    }
}

/**
 * Dock inferior flotante, minimalista y futurista para cambiar de sección.
 */
@Composable
fun FuturisticFloatingDock(
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "◈" to "Apps",
        "⬡" to "Núcleo",
        "◎" to "Ajustes"
    )

    val borderBrush = Brush.horizontalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 28.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, borderBrush),
            tonalElevation = 6.dp
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, (symbol, label) ->
                    val isSelected = selectedTab == index
                    val interactionSource = remember { MutableInteractionSource() }
                    val isPressed by interactionSource.collectIsPressedAsState()
                    val scale by animateFloatAsState(
                        targetValue = if (isPressed) 0.93f else 1f,
                        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                        label = "dock_item_scale"
                    )

                    val pillBg by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                        } else {
                            Color.Transparent
                        },
                        animationSpec = tween(250),
                        label = "dock_pill_bg"
                    )

                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        animationSpec = tween(250),
                        label = "dock_text_color"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .scale(scale)
                            .clip(RoundedCornerShape(22.dp))
                            .background(pillBg)
                            .then(
                                if (isSelected) {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                        RoundedCornerShape(22.dp)
                                    )
                                } else {
                                    Modifier
                                }
                            )
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = { onSelectTab(index) }
                            )
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = symbol,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    letterSpacing = 0.5.sp
                                ),
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }
    }
}
