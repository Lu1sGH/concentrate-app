package com.example.oakridgeproject.data

import android.content.Context
import android.content.SharedPreferences
import com.example.oakridgeproject.model.AppBlockRule
import com.example.oakridgeproject.model.ThemeMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class AppPreferencesRepository private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _themeMode = MutableStateFlow(loadThemeMode())
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _dynamicColor = MutableStateFlow(loadDynamicColor())
    val dynamicColor: StateFlow<Boolean> = _dynamicColor.asStateFlow()

    private val _isBlockerEnabled = MutableStateFlow(loadBlockerEnabled())
    val isBlockerEnabled: StateFlow<Boolean> = _isBlockerEnabled.asStateFlow()

    private val _hasSeenTutorial = MutableStateFlow(loadHasSeenTutorial())
    val hasSeenTutorial: StateFlow<Boolean> = _hasSeenTutorial.asStateFlow()

    private val _blockRules = MutableStateFlow(loadBlockRules())
    val blockRules: StateFlow<Map<String, AppBlockRule>> = _blockRules.asStateFlow()

    /**
     * Contador almacenado como entero sin signo de 7 bits (rango físico 0..127, máscara 0x7F),
     * detenido en un tope máximo de 101 intentos.
     */
    private val _prankAttemptCount7Bit = MutableStateFlow(loadPrankAttemptCount7Bit())
    val prankAttemptCount7Bit: StateFlow<UByte> = _prankAttemptCount7Bit.asStateFlow()

    /**
     * Marca de tiempo (ms) en la que se comenzó a usar la app / se inició el contador.
     * El reinicio ocurre exactamente 24 horas después de este instante (no a las 12:00 am).
     */
    private val _cycleStartTimestampMs = MutableStateFlow(ensureCycleStartTimestamp())
    val cycleStartTimestampMs: StateFlow<Long> = _cycleStartTimestampMs.asStateFlow()

    init {
        clearAccidentalMassBlockOnce()
        checkAndReset24HourCycleIfNeeded()
    }

    /**
     * Limpia por única vez el bloqueo masivo activado por accidente desde las píldoras de Núcleo,
     * restaurando las apps sin perder la configuración de tema ni el estado del tutorial.
     */
    private fun clearAccidentalMassBlockOnce() {
        if (!prefs.getBoolean(KEY_CLEANUP_ACCIDENTAL_MASS_BLOCK_V1, false)) {
            prefs.edit()
                .remove(KEY_BLOCK_RULES)
                .putInt(KEY_PRANK_ATTEMPT_7BIT, 0)
                .putBoolean(KEY_CLEANUP_ACCIDENTAL_MASS_BLOCK_V1, true)
                .apply()
            _blockRules.value = emptyMap()
            _prankAttemptCount7Bit.value = 0u
        }
    }

    /**
     * Verifica si ya transcurrieron 24 horas desde que inició el ciclo de uso/contador.
     * Si se cumplieron las 24 horas, reinicia el contador de 7 bits a 0 y comienza un nuevo ciclo.
     */
    fun checkAndReset24HourCycleIfNeeded(): Boolean {
        val now = System.currentTimeMillis()
        val start = prefs.getLong(KEY_CYCLE_START_TIMESTAMP_MS, 0L)

        if (start <= 0L) {
            prefs.edit().putLong(KEY_CYCLE_START_TIMESTAMP_MS, now).apply()
            _cycleStartTimestampMs.value = now
            return false
        }

        if (now - start >= ROLLING_24_HOURS_MS) {
            prefs.edit()
                .putInt(KEY_PRANK_ATTEMPT_7BIT, 0)
                .putLong(KEY_CYCLE_START_TIMESTAMP_MS, now)
                .apply()
            _prankAttemptCount7Bit.value = 0u
            _cycleStartTimestampMs.value = now
            return true
        }
        return false
    }

    /**
     * Incrementa el contador sin signo de 7 bits (máscara 0x7F) y se detiene al llegar a 101.
     * Si el contador estaba en 0, registra el inicio del ciclo de 24 horas.
     */
    fun incrementPrankAttemptCount(): UByte {
        checkAndReset24HourCycleIfNeeded()

        val current7Bit: UByte = _prankAttemptCount7Bit.value and SEVEN_BIT_UNSIGNED_MASK
        if (current7Bit == 0u.toUByte()) {
            val now = System.currentTimeMillis()
            prefs.edit().putLong(KEY_CYCLE_START_TIMESTAMP_MS, now).apply()
            _cycleStartTimestampMs.value = now
        }

        val next7Bit: UByte = if (current7Bit >= MAX_PRANK_ATTEMPTS_7BIT) {
            MAX_PRANK_ATTEMPTS_7BIT
        } else {
            ((current7Bit + 1u).toUByte() and SEVEN_BIT_UNSIGNED_MASK)
        }

        prefs.edit().putInt(KEY_PRANK_ATTEMPT_7BIT, next7Bit.toInt() and 0x7F).apply()
        _prankAttemptCount7Bit.value = next7Bit
        return next7Bit
    }

    /**
     * Devuelve los milisegundos restantes para que se cumplan las 24 horas desde el inicio del ciclo.
     */
    fun getRemainingCycleMillis(): Long {
        checkAndReset24HourCycleIfNeeded()
        val elapsed = (System.currentTimeMillis() - _cycleStartTimestampMs.value).coerceAtLeast(0L)
        return (ROLLING_24_HOURS_MS - elapsed).coerceAtLeast(0L)
    }

    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        _themeMode.value = mode
    }

    fun setDynamicColor(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, enabled).apply()
        _dynamicColor.value = enabled
    }

    fun setBlockerEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_BLOCKER_ENABLED, enabled).apply()
        _isBlockerEnabled.value = enabled
    }

    fun setTutorialSeen(seen: Boolean) {
        prefs.edit().putBoolean(KEY_HAS_SEEN_TUTORIAL, seen).apply()
        _hasSeenTutorial.value = seen
    }

    /**
     * Activa o desactiva el bloqueo de una app.
     * Por defecto, el umbral para apps bloqueadas instantáneamente es 0 segundos.
     */
    fun setAppBlocked(
        packageName: String,
        isBlocked: Boolean,
        limitSeconds: Long = _blockRules.value[packageName]?.limitSeconds
            ?: AppBlockRule.DEFAULT_INSTANT_BLOCK_THRESHOLD_SECONDS
    ) {
        checkAndReset24HourCycleIfNeeded()
        val updated = _blockRules.value.toMutableMap()
        if (!isBlocked && limitSeconds == AppBlockRule.DEFAULT_INSTANT_BLOCK_THRESHOLD_SECONDS) {
            updated.remove(packageName)
        } else {
            updated[packageName] = AppBlockRule(
                packageName = packageName,
                isBlocked = isBlocked,
                limitSeconds = limitSeconds.coerceAtLeast(0L)
            )
        }
        saveBlockRules(updated)
        _blockRules.value = updated
    }

    fun updateAppThreshold(packageName: String, limitSeconds: Long) {
        checkAndReset24HourCycleIfNeeded()
        val current = _blockRules.value[packageName]
        val coercedRequested = limitSeconds.coerceAtLeast(0L)
        val finalLimitSeconds = if (current?.isBlocked == true) {
            minOf(current.limitSeconds, coercedRequested)
        } else {
            coercedRequested
        }
        val updated = _blockRules.value.toMutableMap()
        updated[packageName] = AppBlockRule(
            packageName = packageName,
            isBlocked = true,
            limitSeconds = finalLimitSeconds
        )
        saveBlockRules(updated)
        _blockRules.value = updated
    }

    fun applyBlockToPackages(
        packageNames: Collection<String>,
        isBlocked: Boolean,
        limitSeconds: Long = AppBlockRule.DEFAULT_INSTANT_BLOCK_THRESHOLD_SECONDS
    ) {
        checkAndReset24HourCycleIfNeeded()
        val updated = _blockRules.value.toMutableMap()
        for (pkg in packageNames) {
            if (!isBlocked) {
                updated.remove(pkg)
            } else {
                val existingLimit = updated[pkg]?.limitSeconds ?: limitSeconds
                updated[pkg] = AppBlockRule(
                    packageName = pkg,
                    isBlocked = true,
                    limitSeconds = existingLimit.coerceAtLeast(0L)
                )
            }
        }
        saveBlockRules(updated)
        _blockRules.value = updated
    }

    fun getRuleForPackage(packageName: String): AppBlockRule? {
        return _blockRules.value[packageName]
    }

    fun reloadFromDisk() {
        checkAndReset24HourCycleIfNeeded()
        _themeMode.value = loadThemeMode()
        _dynamicColor.value = loadDynamicColor()
        _isBlockerEnabled.value = loadBlockerEnabled()
        _hasSeenTutorial.value = loadHasSeenTutorial()
        _blockRules.value = loadBlockRules()
        _prankAttemptCount7Bit.value = loadPrankAttemptCount7Bit()
        _cycleStartTimestampMs.value = ensureCycleStartTimestamp()
    }

    private fun loadThemeMode(): ThemeMode {
        val saved = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
        return ThemeMode.entries.firstOrNull { it.name == saved } ?: ThemeMode.SYSTEM
    }

    private fun loadDynamicColor(): Boolean {
        return prefs.getBoolean(KEY_DYNAMIC_COLOR, true)
    }

    private fun loadBlockerEnabled(): Boolean {
        return prefs.getBoolean(KEY_BLOCKER_ENABLED, true)
    }

    private fun loadHasSeenTutorial(): Boolean {
        return prefs.getBoolean(KEY_HAS_SEEN_TUTORIAL, false)
    }

    private fun loadPrankAttemptCount7Bit(): UByte {
        val raw = prefs.getInt(KEY_PRANK_ATTEMPT_7BIT, 0) and 0x7F
        val as7Bit = raw.toUByte() and SEVEN_BIT_UNSIGNED_MASK
        return if (as7Bit > MAX_PRANK_ATTEMPTS_7BIT) MAX_PRANK_ATTEMPTS_7BIT else as7Bit
    }

    private fun ensureCycleStartTimestamp(): Long {
        val existing = prefs.getLong(KEY_CYCLE_START_TIMESTAMP_MS, 0L)
        if (existing > 0L) return existing
        val now = System.currentTimeMillis()
        prefs.edit().putLong(KEY_CYCLE_START_TIMESTAMP_MS, now).apply()
        return now
    }

    private fun loadBlockRules(): Map<String, AppBlockRule> {
        val rawJson = prefs.getString(KEY_BLOCK_RULES, null) ?: return emptyMap()
        return try {
            val array = JSONArray(rawJson)
            val result = mutableMapOf<String, AppBlockRule>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val pkg = obj.optString("packageName")
                if (pkg.isNotBlank()) {
                    val isBlocked = obj.optBoolean("isBlocked", false)
                    val limitSeconds = obj.optLong(
                        "limitSeconds",
                        AppBlockRule.DEFAULT_INSTANT_BLOCK_THRESHOLD_SECONDS
                    )
                    result[pkg] = AppBlockRule(
                        packageName = pkg,
                        isBlocked = isBlocked,
                        limitSeconds = limitSeconds
                    )
                }
            }
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }

    private fun saveBlockRules(rules: Map<String, AppBlockRule>) {
        val array = JSONArray()
        rules.values.forEach { rule ->
            val obj = JSONObject().apply {
                put("packageName", rule.packageName)
                put("isBlocked", rule.isBlocked)
                put("limitSeconds", rule.limitSeconds)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_BLOCK_RULES, array.toString()).apply()
    }

    companion object {
        private const val PREFS_NAME = "oakridge_blocker_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
        private const val KEY_BLOCKER_ENABLED = "key_blocker_enabled"
        private const val KEY_HAS_SEEN_TUTORIAL = "key_has_seen_tutorial"
        private const val KEY_BLOCK_RULES = "key_block_rules"
        private const val KEY_PRANK_ATTEMPT_7BIT = "key_prank_attempt_count"
        private const val KEY_CYCLE_START_TIMESTAMP_MS = "key_cycle_start_timestamp_ms"
        private const val KEY_CLEANUP_ACCIDENTAL_MASS_BLOCK_V1 =
            "key_cleanup_accidental_mass_block_v1"

        val SEVEN_BIT_UNSIGNED_MASK: UByte = 0x7Fu
        val MAX_PRANK_ATTEMPTS_7BIT: UByte = 101u

        const val ROLLING_24_HOURS_MS: Long = 24L * 60L * 60L * 1000L

        @Volatile
        private var INSTANCE: AppPreferencesRepository? = null

        fun getInstance(context: Context): AppPreferencesRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppPreferencesRepository(context).also { INSTANCE = it }
            }
        }
    }
}
