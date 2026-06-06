package net.aegisnet.app.runtime

import android.content.Context
import android.content.SharedPreferences
import java.nio.charset.StandardCharsets
import java.util.Base64

class ProxyConfigStore(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    fun loadAll(): List<ImportedProxyConfig> {
        return preferences.getStringSet(KEY_CONFIGS, emptySet())
            .orEmpty()
            .mapNotNull(::decodeConfig)
            .sortedByDescending { it.importedAtMillis }
    }

    fun save(config: ImportedProxyConfig) {
        val updated = (loadAll().filterNot { it.id == config.id } + config)
            .map(::encodeConfig)
            .toSet()
        preferences.edit()
            .putStringSet(KEY_CONFIGS, updated)
            .putString(KEY_ACTIVE_CONFIG_ID, config.id)
            .apply()
    }

    fun loadActive(): ImportedProxyConfig? {
        val activeId = preferences.getString(KEY_ACTIVE_CONFIG_ID, null)
        val configs = loadAll()
        return configs.firstOrNull { it.id == activeId } ?: configs.firstOrNull()
    }

    fun setActive(configId: String) {
        preferences.edit()
            .putString(KEY_ACTIVE_CONFIG_ID, configId)
            .apply()
    }

    private fun encodeConfig(config: ImportedProxyConfig): String {
        return listOf(
            config.id,
            config.name,
            config.type.name,
            config.importedAtMillis.toString(),
            config.content,
        ).joinToString(separator = FIELD_SEPARATOR) { field ->
            Base64.getEncoder().encodeToString(field.toByteArray(StandardCharsets.UTF_8))
        }
    }

    private fun decodeConfig(encoded: String): ImportedProxyConfig? {
        val fields = encoded.split(FIELD_SEPARATOR)
        if (fields.size != 5) return null
        return runCatching {
            val decoded = fields.map { field ->
                String(Base64.getDecoder().decode(field), StandardCharsets.UTF_8)
            }
            ImportedProxyConfig(
                id = decoded[0],
                name = decoded[1],
                type = ProxyConfigType.valueOf(decoded[2]),
                importedAtMillis = decoded[3].toLong(),
                content = decoded[4],
            )
        }.getOrNull()
    }

    private companion object {
        const val PREFERENCES_NAME = "aegis_proxy_configs"
        const val KEY_CONFIGS = "configs"
        const val KEY_ACTIVE_CONFIG_ID = "active_config_id"
        const val FIELD_SEPARATOR = "\t"
    }
}
