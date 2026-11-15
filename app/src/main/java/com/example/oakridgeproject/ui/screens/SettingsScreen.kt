package com.example.oakridgeproject.ui.screens

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oakridgeproject.model.ThemeMode
import com.example.oakridgeproject.ui.components.FuturisticGlassButton
import com.example.oakridgeproject.ui.components.FuturisticOrbToggle
import com.example.oakridgeproject.ui.components.FuturisticSurfaceCard

@Composable
fun SettingsScreen(
    selectedThemeMode: ThemeMode,
    dynamicColorEnabled: Boolean,
    hasUsagePermission: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onToggleDynamicColor: (Boolean) -> Unit,
    onOpenUsageSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Sección de Tema Minimalista
        FuturisticSurfaceCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ATMOSFERA VISUAL",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.4.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val shortLabels = mapOf(
                        ThemeMode.SYSTEM to "Auto",
                        ThemeMode.LIGHT to "Claro",
                        ThemeMode.DARK to "Oscuro"
                    )
                    ThemeMode.entries.forEach { mode ->
                        FuturisticGlassButton(
                            text = shortLabels[mode] ?: mode.title,
                            isSelected = selectedThemeMode == mode,
                            onClick = { onSelectThemeMode(mode) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        )
                    }
                }

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Espectro Dinámico",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Sincronizar colores con el dispositivo",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        FuturisticOrbToggle(
                            checked = dynamicColorEnabled,
                            onCheckedChange = onToggleDynamicColor
                        )
                    }
                }
            }
        }

        // Sección de Estado de Permisos del Sistema
        FuturisticSurfaceCard(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "ENLACES DEL SISTEMA",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.4.sp
                    ),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                PermissionStatusRow(
                    title = "Telemetría de Uso",
                    status = if (hasUsagePermission) "En línea" else "Requerido",
                    isGranted = hasUsagePermission,
                    onActionClick = onOpenUsageSettings
                )

                PermissionStatusRow(
                    title = "Escudo de Superposición",
                    status = if (hasOverlayPermission) "En línea" else "Requerido",
                    isGranted = hasOverlayPermission,
                    onActionClick = onOpenOverlaySettings
                )

                PermissionStatusRow(
                    title = "Núcleo de Accesibilidad",
                    status = if (hasAccessibilityPermission) "En línea" else "Requerido",
                    isGranted = hasAccessibilityPermission,
                    onActionClick = onOpenAccessibilitySettings
                )
            }
        }

        Spacer(modifier = Modifier.height(88.dp))
    }
}

@Composable
private fun PermissionStatusRow(
    title: String,
    status: String,
    isGranted: Boolean,
    onActionClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall,
                color = if (isGranted) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
        }
        FuturisticGlassButton(
            text = if (isGranted) "✓ Activo" else "Conectar",
            isSelected = isGranted,
            onClick = onActionClick,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}
