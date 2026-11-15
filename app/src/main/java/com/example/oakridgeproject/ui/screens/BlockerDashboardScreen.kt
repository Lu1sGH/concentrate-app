package com.example.oakridgeproject.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.oakridgeproject.model.AppBlockRule
import com.example.oakridgeproject.model.AppCategory
import com.example.oakridgeproject.model.InstalledAppInfo
import com.example.oakridgeproject.ui.components.FuturisticGlassButton
import com.example.oakridgeproject.ui.components.FuturisticGlowButton
import com.example.oakridgeproject.ui.components.FuturisticOrbToggle
import com.example.oakridgeproject.ui.components.FuturisticSurfaceCard
import com.example.oakridgeproject.util.AppUsageHelper

@Composable
fun BlockerDashboardScreen(
    apps: List<InstalledAppInfo>,
    blockRules: Map<String, AppBlockRule>,
    isBlockerEnabled: Boolean,
    hasUsagePermission: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onToggleBlockerMaster: (Boolean) -> Unit,
    onUpdateAppThreshold: (String, Long) -> Unit,
    onTriggerPrankUnblock: (String) -> Unit,
    onTestBlockApp: (InstalledAppInfo, AppBlockRule) -> Unit,
    onNavigateToAppsCatalog: () -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    var editingApp by remember { mutableStateOf<InstalledAppInfo?>(null) }
    var selectedCategoryFilter by remember { mutableStateOf<AppCategory?>(null) }

    val allBlockedAppsWithRules = remember(apps, blockRules) {
        apps.mapNotNull { app ->
            val rule = blockRules[app.packageName]
            if (rule?.isBlocked == true) app to rule else null
        }
    }

    val blockedAppsWithRules = remember(allBlockedAppsWithRules, selectedCategoryFilter) {
        if (selectedCategoryFilter == null) {
            allBlockedAppsWithRules
        } else {
            allBlockedAppsWithRules.filter { (app, _) -> app.category == selectedCategoryFilter }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 104.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. Núcleo Maestro Minimalista
        item(key = "master_status_card") {
            MasterBlockerCard(
                isBlockerEnabled = isBlockerEnabled,
                blockedAppsCount = allBlockedAppsWithRules.size,
                onToggleMaster = { wantEnabled ->
                    if (!wantEnabled) {
                        onTriggerPrankUnblock("Núcleo de Bloqueo")
                    } else {
                        onToggleBlockerMaster(true)
                    }
                }
            )
        }

        // 2. Permisos en una sola fila minimalista si falta alguno
        if (!hasUsagePermission || (!hasOverlayPermission && !hasAccessibilityPermission)) {
            item(key = "permissions_setup_card") {
                FuturisticSurfaceCard(
                    modifier = Modifier.fillMaxWidth(),
                    isHighlighted = true
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "⚡ Permisos",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (!hasUsagePermission) {
                            FuturisticGlassButton(
                                text = "Uso",
                                onClick = onRequestUsagePermission,
                                isSelected = true,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        if (!hasOverlayPermission) {
                            FuturisticGlassButton(
                                text = "Pantalla",
                                onClick = onRequestOverlayPermission,
                                isSelected = true,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                        if (!hasAccessibilityPermission) {
                            FuturisticGlassButton(
                                text = "Accesibilidad",
                                onClick = onRequestAccessibilityPermission,
                                isSelected = true,
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Filtro de visualización por categoría (ya NO bloquea todas las apps por accidente)
        item(key = "quick_category_filter") {
            CategorySummaryFilterRow(
                allBlockedApps = allBlockedAppsWithRules.map { it.first },
                selectedCategory = selectedCategoryFilter,
                onSelectCategory = { category ->
                    selectedCategoryFilter = if (selectedCategoryFilter == category) null else category
                }
            )
        }

        // 4. Encabezado minimalista
        item(key = "blocked_list_header") {
            Text(
                text = "RESTRICCIONES ACTIVAS  [${blockedAppsWithRules.size}]",
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.4.sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (blockedAppsWithRules.isEmpty()) {
            item(key = "empty_blocked_state") {
                FuturisticSurfaceCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "⬡",
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Ninguna aplicación en el núcleo",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        FuturisticGlowButton(
                            text = "Seleccionar Apps",
                            leadingSymbol = "◈",
                            onClick = onNavigateToAppsCatalog,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        } else {
            items(
                items = blockedAppsWithRules,
                key = { (app, _) -> app.packageName }
            ) { (app, rule) ->
                AppliedBlockAppCard(
                    app = app,
                    rule = rule,
                    onEditThreshold = { editingApp = app },
                    onRemoveBlock = { onTriggerPrankUnblock(app.appName) },
                    onTestBlock = { onTestBlockApp(app, rule) }
                )
            }
        }
    }

    editingApp?.let { targetApp ->
        val currentRule = blockRules[targetApp.packageName]
        val currentLimitSec = currentRule?.limitSeconds
            ?: AppBlockRule.DEFAULT_INSTANT_BLOCK_THRESHOLD_SECONDS

        ThresholdConfigDialog(
            app = targetApp,
            initialSeconds = currentLimitSec,
            isAlreadyBlocked = true,
            onDismiss = { editingApp = null },
            onConfirmSeconds = { newLimitSeconds ->
                editingApp = null
                if (newLimitSeconds > currentLimitSec) {
                    onTriggerPrankUnblock(targetApp.appName)
                } else {
                    onUpdateAppThreshold(targetApp.packageName, newLimitSeconds)
                }
            }
        )
    }
}

@Composable
private fun MasterBlockerCard(
    isBlockerEnabled: Boolean,
    blockedAppsCount: Int,
    onToggleMaster: (Boolean) -> Unit
) {
    FuturisticSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        isHighlighted = isBlockerEnabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isBlockerEnabled) "NÚCLEO ACTIVO" else "NÚCLEO EN PAUSA",
                    style = MaterialTheme.typography.labelLarge.copy(
                        letterSpacing = 1.4.sp
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$blockedAppsCount apps blindadas • Ciclo 24h",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            FuturisticOrbToggle(
                checked = isBlockerEnabled,
                onCheckedChange = onToggleMaster
            )
        }
    }
}

@Composable
private fun CategorySummaryFilterRow(
    allBlockedApps: List<InstalledAppInfo>,
    selectedCategory: AppCategory?,
    onSelectCategory: (AppCategory) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AppCategory.entries.forEach { category ->
            val blockedInCategory = allBlockedApps.count { it.category == category }
            val isSelected = selectedCategory == category
            val shortTitle = when (category) {
                AppCategory.SOCIAL -> "Social"
                AppCategory.RECREATIONAL -> "Ocio"
                AppCategory.OTHER -> "Otras"
            }

            FuturisticGlassButton(
                text = "$shortTitle ($blockedInCategory)",
                isSelected = isSelected,
                onClick = { onSelectCategory(category) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun AppliedBlockAppCard(
    app: InstalledAppInfo,
    rule: AppBlockRule,
    onEditThreshold: () -> Unit,
    onRemoveBlock: () -> Unit,
    onTestBlock: () -> Unit
) {
    val isReached = rule.shouldBlock(app.usedSecondsToday)
    val progress = remember(app.usedSecondsToday, rule.limitSeconds) {
        if (rule.limitSeconds <= 0L) {
            1f
        } else {
            (app.usedSecondsToday.toFloat() / rule.limitSeconds.toFloat()).coerceIn(0f, 1f)
        }
    }

    FuturisticSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        isHighlighted = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(
                    drawable = app.icon,
                    appName = app.appName,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.appName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = app.category.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FuturisticGlassButton(
                    text = if (rule.limitSeconds == 0L) {
                        "⚡ 0s"
                    } else {
                        "⏱ ${AppUsageHelper.formatDurationThreshold(rule.limitSeconds)}"
                    },
                    isSelected = true,
                    onClick = onEditThreshold,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isReached) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                }
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FuturisticGlassButton(
                    text = "⚡ Simular",
                    onClick = onTestBlock,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                )
                FuturisticGlassButton(
                    text = "✕ Liberar",
                    isAccentDanger = true,
                    onClick = onRemoveBlock,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 8.dp)
                )
            }
        }
    }
}
