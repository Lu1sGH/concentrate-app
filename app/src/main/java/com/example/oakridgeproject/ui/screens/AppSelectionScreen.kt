package com.example.oakridgeproject.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
fun AppSelectionScreen(
    apps: List<InstalledAppInfo>,
    blockRules: Map<String, AppBlockRule>,
    isLoading: Boolean,
    hasUsagePermission: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onToggleAppBlock: (String, Boolean) -> Unit,
    onUpdateAppThreshold: (String, Long) -> Unit,
    onApplyCategoryBlock: (AppCategory, Boolean) -> Unit,
    onTriggerPrankUnblock: (String) -> Unit,
    onRequestUsagePermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<AppCategory?>(null) }
    var appEditingThreshold by remember { mutableStateOf<InstalledAppInfo?>(null) }

    val filteredApps = remember(apps, searchQuery, selectedCategory) {
        apps.filter { app ->
            val matchesCategory = selectedCategory == null || app.category == selectedCategory
            val matchesQuery = searchQuery.isBlank() ||
                app.appName.contains(searchQuery, ignoreCase = true) ||
                app.packageName.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesQuery
        }
    }

    val groupedApps = remember(filteredApps) {
        AppCategory.entries.associateWith { category ->
            filteredApps.filter { it.category == category }
        }.filterValues { it.isNotEmpty() }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Barra de permisos minimalista solo cuando falta alguno
        AnimatedVisibility(
            visible = !hasUsagePermission || (!hasOverlayPermission && !hasAccessibilityPermission)
        ) {
            MinimalPermissionBanner(
                hasUsagePermission = hasUsagePermission,
                hasOverlayPermission = hasOverlayPermission,
                hasAccessibilityPermission = hasAccessibilityPermission,
                onRequestUsagePermission = onRequestUsagePermission,
                onRequestOverlayPermission = onRequestOverlayPermission,
                onRequestAccessibilityPermission = onRequestAccessibilityPermission
            )
        }

        // Buscador estilo cristal minimalista
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    text = "◈  Explorar aplicaciones...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.7f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.5f),
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Selector horizontal de categorías futurista
        CategoryFilterRow(
            selectedCategory = selectedCategory,
            apps = apps,
            onSelectCategory = { selectedCategory = it }
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        } else if (groupedApps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Sin resultados",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 104.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                groupedApps.forEach { (category, categoryApps) ->
                    val allCategoryBlocked = categoryApps.all {
                        blockRules[it.packageName]?.isBlocked == true
                    }

                    item(key = "header_${category.name}") {
                        CategorySectionHeader(
                            category = category,
                            count = categoryApps.size,
                            allBlocked = allCategoryBlocked,
                            onToggleAllCategory = {
                                if (allCategoryBlocked) {
                                    onTriggerPrankUnblock(category.title)
                                } else {
                                    onApplyCategoryBlock(category, true)
                                }
                            }
                        )
                    }

                    items(
                        items = categoryApps,
                        key = { it.packageName }
                    ) { app ->
                        val rule = blockRules[app.packageName]
                        val isCurrentlyBlocked = rule?.isBlocked == true

                        AppListItemCard(
                            app = app,
                            rule = rule,
                            onToggleBlock = { wantBlocked ->
                                if (!wantBlocked && isCurrentlyBlocked) {
                                    onTriggerPrankUnblock(app.appName)
                                } else {
                                    onToggleAppBlock(app.packageName, true)
                                }
                            },
                            onConfigureThreshold = {
                                appEditingThreshold = app
                            }
                        )
                    }
                }
            }
        }
    }

    appEditingThreshold?.let { targetApp ->
        val currentRule = blockRules[targetApp.packageName]
        val isAlreadyBlocked = currentRule?.isBlocked == true
        val currentLimitSec = currentRule?.limitSeconds
            ?: AppBlockRule.DEFAULT_INSTANT_BLOCK_THRESHOLD_SECONDS

        ThresholdConfigDialog(
            app = targetApp,
            initialSeconds = currentLimitSec,
            isAlreadyBlocked = isAlreadyBlocked,
            onDismiss = { appEditingThreshold = null },
            onConfirmSeconds = { newLimitSeconds ->
                appEditingThreshold = null
                if (isAlreadyBlocked && newLimitSeconds > currentLimitSec) {
                    onTriggerPrankUnblock(targetApp.appName)
                } else {
                    onUpdateAppThreshold(targetApp.packageName, newLimitSeconds)
                }
            }
        )
    }
}

@Composable
private fun MinimalPermissionBanner(
    hasUsagePermission: Boolean,
    hasOverlayPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    onRequestUsagePermission: () -> Unit,
    onRequestOverlayPermission: () -> Unit,
    onRequestAccessibilityPermission: () -> Unit
) {
    FuturisticSurfaceCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
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
                    text = "Núcleo",
                    onClick = onRequestAccessibilityPermission,
                    isSelected = true,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterRow(
    selectedCategory: AppCategory?,
    apps: List<InstalledAppInfo>,
    onSelectCategory: (AppCategory?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FuturisticGlassButton(
            text = "Todas  ${apps.size}",
            isSelected = selectedCategory == null,
            onClick = { onSelectCategory(null) },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
        )

        AppCategory.entries.forEach { category ->
            val count = apps.count { it.category == category }
            FuturisticGlassButton(
                text = "${category.title}  $count",
                isSelected = selectedCategory == category,
                onClick = {
                    onSelectCategory(if (selectedCategory == category) null else category)
                },
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun CategorySectionHeader(
    category: AppCategory,
    count: Int,
    allBlocked: Boolean,
    onToggleAllCategory: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = category.title.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    letterSpacing = 1.4.sp
                ),
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "[$count]",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FuturisticGlassButton(
            text = if (allBlocked) "🔒 Grupo activo" else "+ Bloquear grupo",
            isSelected = allBlocked,
            onClick = onToggleAllCategory,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun AppListItemCard(
    app: InstalledAppInfo,
    rule: AppBlockRule?,
    onToggleBlock: (Boolean) -> Unit,
    onConfigureThreshold: () -> Unit
) {
    val isBlocked = rule?.isBlocked == true
    val limitSeconds = rule?.limitSeconds ?: AppBlockRule.DEFAULT_INSTANT_BLOCK_THRESHOLD_SECONDS

    FuturisticSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        isHighlighted = isBlocked,
        onClick = onConfigureThreshold
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconImage(
                drawable = app.icon,
                appName = app.appName,
                modifier = Modifier.size(42.dp)
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val badgeText = if (isBlocked) {
                        if (limitSeconds == 0L) "⚡ 0s Instantáneo" else "⏱ ${AppUsageHelper.formatDurationThreshold(limitSeconds)}"
                    } else {
                        "⏱ Definir límite"
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isBlocked) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                } else {
                                    Color.Transparent
                                }
                            )
                            .clickable { onConfigureThreshold() }
                            .padding(
                                horizontal = if (isBlocked) 8.dp else 0.dp,
                                vertical = 2.dp
                            )
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isBlocked) FontWeight.Bold else FontWeight.Normal,
                            color = if (isBlocked) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            FuturisticOrbToggle(
                checked = isBlocked,
                onCheckedChange = onToggleBlock
            )
        }
    }
}

@Composable
fun AppIconImage(
    drawable: Drawable?,
    appName: String,
    modifier: Modifier = Modifier
) {
    val imageBitmap: ImageBitmap? = remember(drawable) {
        drawable?.toImageBitmapOrNull()
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = appName,
            modifier = modifier.clip(RoundedCornerShape(12.dp))
        )
    } else {
        Box(
            modifier = modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = appName.take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

private fun Drawable.toImageBitmapOrNull(): ImageBitmap? {
    return runCatching {
        if (this is BitmapDrawable && bitmap != null) {
            return bitmap.asImageBitmap()
        }
        val width = if (intrinsicWidth > 0) intrinsicWidth else 96
        val height = if (intrinsicHeight > 0) intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        bitmap.asImageBitmap()
    }.getOrNull()
}

@Composable
fun ThresholdConfigDialog(
    app: InstalledAppInfo,
    initialSeconds: Long,
    isAlreadyBlocked: Boolean = false,
    onDismiss: () -> Unit,
    onConfirmSeconds: (Long) -> Unit
) {
    val initialMinutes = (initialSeconds / 60L).toString()
    val initialRemSeconds = (initialSeconds % 60L).toString()

    var minutesInput by remember { mutableStateOf(initialMinutes) }
    var secondsInput by remember { mutableStateOf(initialRemSeconds) }

    val presets = listOf(
        0L to "0s",
        10L to "10s",
        60L to "1m",
        300L to "5m",
        900L to "15m",
        1800L to "30m",
        3600L to "1h"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = {
            Column {
                Text(
                    text = app.appName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isAlreadyBlocked) {
                        "Límite actual: ${AppUsageHelper.formatDurationThreshold(initialSeconds)} • Solo reducir"
                    } else {
                        "Configura el umbral X de uso"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    presets.forEach { (presetSec, label) ->
                        val currentTotal =
                            (minutesInput.toLongOrNull() ?: 0L) * 60L + (secondsInput.toLongOrNull() ?: 0L)
                        FuturisticGlassButton(
                            text = label,
                            isSelected = currentTotal == presetSec,
                            onClick = {
                                minutesInput = (presetSec / 60L).toString()
                                secondsInput = (presetSec % 60L).toString()
                            },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = minutesInput,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() } && value.length <= 4) {
                                minutesInput = value
                            }
                        },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    )

                    OutlinedTextField(
                        value = secondsInput,
                        onValueChange = { value ->
                            if (value.all { it.isDigit() } && value.length <= 2) {
                                secondsInput = value
                            }
                        },
                        label = { Text("Seg") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
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
                    text = "Cancelar",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(vertical = 12.dp)
                )
                FuturisticGlowButton(
                    text = if (isAlreadyBlocked) "Reducir" else "Bloquear",
                    leadingSymbol = "⚡",
                    onClick = {
                        val mins = minutesInput.toLongOrNull() ?: 0L
                        val secs = secondsInput.toLongOrNull() ?: 0L
                        onConfirmSeconds((mins * 60L + secs).coerceAtLeast(0L))
                    },
                    modifier = Modifier.weight(1f),
                    height = 44.dp
                )
            }
        }
    )
}
