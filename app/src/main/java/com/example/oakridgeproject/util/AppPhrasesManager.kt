package com.example.oakridgeproject.util

import android.content.Context
import org.json.JSONObject

data class PrankDialogPhrase(
    val emoji: String,
    val title: String,
    val message: String
)

data class TutorialStepPhrase(
    val emoji: String,
    val badge: String,
    val title: String,
    val description: String
)

/**
 * Gestor centralizado de frases de toda la aplicación.
 *
 * Lee dinámicamente el archivo `app/src/main/assets/frases_app.json` sin depender de ningún
 * tamaño fijo, permitiendo al desarrollador agregar o quitar frases libremente y usar
 * wildcards como `{NomApp}`, `{Intento}`, `{Segundos}` o cualquier wildcard futuro.
 */
object AppPhrasesManager {

    private const val PHRASES_ASSET_FILE = "frases_app.json"
    private const val PREFS_PHRASE_INDEX = "oakridge_phrase_rotation_prefs"
    private const val KEY_LAST_PASSIVE_INDEX = "key_last_passive_index"
    private const val KEY_LAST_PASSIVE_TIMESTAMP = "key_last_passive_timestamp"
    private const val KEY_LAST_PASSIVE_PACKAGE = "key_last_passive_package"

    @Volatile
    private var cachedRootJson: JSONObject? = null

    private fun getRootJson(context: Context): JSONObject {
        cachedRootJson?.let { return it }
        return synchronized(this) {
            cachedRootJson ?: runCatching {
                val rawText = context.assets.open(PHRASES_ASSET_FILE)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
                JSONObject(rawText)
            }.getOrDefault(JSONObject()).also { cachedRootJson = it }
        }
    }

    /**
     * Reemplaza todos los wildcards presentes en la plantilla (ej. `{NomApp}`, `{Intento}`, `{Segundos}`)
     * sin importar mayúsculas/minúsculas.
     */
    fun applyWildcards(
        template: String,
        nomApp: String = "",
        intento: Int? = null,
        segundos: Int? = null,
        extraWildcards: Map<String, String> = emptyMap()
    ): String {
        var result = template
        if (nomApp.isNotEmpty()) {
            result = result.replace(Regex("\\{NomApp\\}", RegexOption.IGNORE_CASE), nomApp)
        }
        if (intento != null) {
            result = result.replace(Regex("\\{Intento\\}", RegexOption.IGNORE_CASE), intento.toString())
        }
        if (segundos != null) {
            result = result.replace(Regex("\\{Segundos\\}", RegexOption.IGNORE_CASE), segundos.toString())
        }
        for ((key, value) in extraWildcards) {
            val cleanKey = key.removePrefix("{").removeSuffix("}")
            result = result.replace(
                Regex("\\{${Regex.escape(cleanKey)}\\}", RegexOption.IGNORE_CASE),
                value
            )
        }
        return result
    }

    /**
     * Obtiene la siguiente frase pasivo-agresiva para la pantalla de bloqueo cada vez que el usuario
     * intenta abrir la aplicación bloqueada (con debounce para que un mismo intento de apertura
     * mantenga su frase fija sin rotar sola).
     */
    fun getNextPassiveAggressivePhraseForOpening(
        context: Context,
        packageName: String,
        appName: String
    ): String {
        val rawList = getRawPassiveAggressiveList(context)
        if (rawList.isEmpty()) {
            return "Entrar a $appName está bloqueado por tu bien."
        }

        val prefs = context.getSharedPreferences(PREFS_PHRASE_INDEX, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        val lastTimestamp = prefs.getLong(KEY_LAST_PASSIVE_TIMESTAMP, 0L)
        val lastPkg = prefs.getString(KEY_LAST_PASSIVE_PACKAGE, null)
        val lastIndex = prefs.getInt(KEY_LAST_PASSIVE_INDEX, -1)

        // Si es un nuevo intento de abrir la app (o han pasado > 1.2s desde la intercepción), avanzamos a la siguiente frase
        val nextIndex = if (lastIndex in rawList.indices && lastPkg == packageName && (now - lastTimestamp) < 1200L) {
            lastIndex
        } else {
            val candidate = (lastIndex + 1) % rawList.size
            prefs.edit()
                .putInt(KEY_LAST_PASSIVE_INDEX, candidate)
                .putLong(KEY_LAST_PASSIVE_TIMESTAMP, now)
                .putString(KEY_LAST_PASSIVE_PACKAGE, packageName)
                .apply()
            candidate
        }

        return applyWildcards(
            template = rawList[nextIndex],
            nomApp = appName
        )
    }

    private fun getRawPassiveAggressiveList(context: Context): List<String> {
        val root = getRootJson(context)
        val array = root.optJSONArray("pantalla_bloqueo_pasivo_agresivas") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optString(i)
                if (item.isNotBlank()) add(item)
            }
        }
    }

    /**
     * Obtiene la reacción (emoji, título y mensaje con wildcards aplicados) para el pop-up
     * de intento de desactivar el bloqueo, calculando automáticamente el módulo sobre la cantidad
     * de frases definidas en `frases_app.json`.
     */
    fun getPrankUnblockReaction(
        context: Context,
        attemptCount: Int,
        targetName: String
    ): PrankDialogPhrase {
        val root = getRootJson(context)

        // Verificar evento especial del intento 67 en el archivo JSON
        if (attemptCount == 67) {
            val special67 = root.optJSONObject("popup_eventos_especiales")
                ?.optJSONObject("intento_67")
            if (special67 != null) {
                return PrankDialogPhrase(
                    emoji = special67.optString("emoji", "🗣️"),
                    title = applyWildcards(
                        special67.optString("titulo", "Intento #{Intento}"),
                        nomApp = targetName,
                        intento = attemptCount
                    ),
                    message = applyWildcards(
                        special67.optString("mensaje", "Sicseben 🗣️"),
                        nomApp = targetName,
                        intento = attemptCount
                    )
                )
            }
        }

        val array = root.optJSONArray("popup_intento_desactivar")
        if (array != null && array.length() > 0) {
            val index = (attemptCount - 1).coerceAtLeast(0) % array.length()
            val obj = array.optJSONObject(index)
            if (obj != null) {
                return PrankDialogPhrase(
                    emoji = obj.optString("emoji", "🔒"),
                    title = applyWildcards(
                        obj.optString("titulo", "¡Operación Imposible!"),
                        nomApp = targetName,
                        intento = attemptCount
                    ),
                    message = applyWildcards(
                        obj.optString("mensaje", "No puedes desactivar el bloqueo de {NomApp}."),
                        nomApp = targetName,
                        intento = attemptCount
                    )
                )
            }
        }

        return PrankDialogPhrase(
            emoji = "🔒",
            title = "¡Operación Imposible! (x$attemptCount)",
            message = "No puedes desactivar el bloqueo de $targetName."
        )
    }

    /**
     * Obtiene el texto de alerta de autodestrucción desde el archivo JSON aplicando wildcards.
     */
    fun getSelfDestructPhrase(
        context: Context,
        attemptCount: Int,
        countdownSeconds: Int,
        targetName: String
    ): PrankDialogPhrase {
        val root = getRootJson(context)
        val selfDestructObj = root.optJSONObject("popup_eventos_especiales")
            ?.optJSONObject("autodestruccion")

        val emoji = selfDestructObj?.optString("emoji", "💣") ?: "💣"
        val rawTitle = selfDestructObj?.optString(
            "titulo",
            "🚨 ¡ALERTA CRÍTICA! (Intento #{Intento})"
        ) ?: "🚨 ¡ALERTA CRÍTICA! (Intento #{Intento})"
        val rawMessage = selfDestructObj?.optString(
            "mensaje",
            "Autodestrucción del dispositivo en: {Segundos}"
        ) ?: "Autodestrucción del dispositivo en: {Segundos}"

        return PrankDialogPhrase(
            emoji = emoji,
            title = applyWildcards(rawTitle, nomApp = targetName, intento = attemptCount, segundos = countdownSeconds),
            message = applyWildcards(rawMessage, nomApp = targetName, intento = attemptCount, segundos = countdownSeconds)
        )
    }

    /**
     * Obtiene la etiqueta del botón de "Proseguir con la desactivación" según el intento actual.
     */
    fun getProceedButtonLabel(
        context: Context,
        attemptCount: Int,
        targetName: String = ""
    ): String {
        val root = getRootJson(context)
        val array = root.optJSONArray("popup_botones_proseguir")
        if (array != null && array.length() > 0) {
            val index = (attemptCount - 1).coerceAtLeast(0) % array.length()
            val raw = array.optString(index, "Proseguir con la desactivación")
            return applyWildcards(raw, nomApp = targetName, intento = attemptCount)
        }
        return "Proseguir con la desactivación"
    }

    /**
     * Obtiene los pasos del tutorial inicial desde `frases_app.json`.
     */
    fun getTutorialSteps(context: Context): List<TutorialStepPhrase> {
        val root = getRootJson(context)
        val array = root.optJSONArray("tutorial_bienvenida") ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                add(
                    TutorialStepPhrase(
                        emoji = obj.optString("emoji", "✨"),
                        badge = obj.optString("insignia", "Paso ${i + 1}"),
                        title = obj.optString("titulo", ""),
                        description = obj.optString("descripcion", "")
                    )
                )
            }
        }
    }

    /**
     * Permite a futuros módulos obtener cualquier lista de frases bajo `modulos_futuros`
     * aplicando wildcards como `{NomApp}` automáticamente sin configurar tamaños de lista.
     */
    fun getPhrasesForFutureModule(
        context: Context,
        moduleKey: String,
        nomApp: String = "",
        extraWildcards: Map<String, String> = emptyMap()
    ): List<String> {
        val root = getRootJson(context)
        val futureModules = root.optJSONObject("modulos_futuros") ?: return emptyList()
        val array = futureModules.optJSONArray(moduleKey) ?: return emptyList()
        return buildList {
            for (i in 0 until array.length()) {
                val raw = array.optString(i)
                if (raw.isNotBlank()) {
                    add(
                        applyWildcards(
                            template = raw,
                            nomApp = nomApp,
                            extraWildcards = extraWildcards
                        )
                    )
                }
            }
        }
    }
}
