package net.aegisnet.app.runtime

import java.net.URI
import java.util.UUID

class ProxyConfigValidator(
    private val clockMillis: () -> Long = System::currentTimeMillis,
    private val idFactory: () -> String = { UUID.randomUUID().toString() },
) {
    fun validate(rawInput: String): ProxyConfigValidationResult {
        val content = rawInput.trim()
        if (content.isEmpty()) {
            return invalid("Config is empty")
        }

        return when {
            content.startsWith("{") -> validateSingBoxJson(content)
            content.startsWith("vless://", ignoreCase = true) -> validateVlessUri(content)
            content.startsWith("hysteria2://", ignoreCase = true) ||
                content.startsWith("hy2://", ignoreCase = true) -> validateHysteria2Uri(content)
            else -> invalid("Unsupported config format. Paste VLESS, Hysteria2, or raw sing-box JSON.")
        }
    }

    private fun validateVlessUri(content: String): ProxyConfigValidationResult {
        val uri = parseUri(content) ?: return invalid("VLESS URI is not parseable")
        if (!uri.scheme.equals("vless", ignoreCase = true)) return invalid("VLESS URI must use vless://")
        if (uri.userInfo.isNullOrBlank()) return invalid("VLESS URI is missing user id")
        if (uri.host.isNullOrBlank()) return invalid("VLESS URI is missing host")
        if (uri.port <= 0) return invalid("VLESS URI is missing port")

        return valid(
            name = uri.fragment?.takeIf { it.isNotBlank() } ?: "VLESS ${uri.host}:${uri.port}",
            type = ProxyConfigType.VlessUri,
            content = content,
        )
    }

    private fun validateHysteria2Uri(content: String): ProxyConfigValidationResult {
        val uri = parseUri(content) ?: return invalid("Hysteria2 URI is not parseable")
        val schemeAllowed = uri.scheme.equals("hysteria2", ignoreCase = true) ||
            uri.scheme.equals("hy2", ignoreCase = true)
        if (!schemeAllowed) return invalid("Hysteria2 URI must use hysteria2:// or hy2://")
        if (uri.host.isNullOrBlank()) return invalid("Hysteria2 URI is missing host")
        if (uri.port <= 0) return invalid("Hysteria2 URI is missing port")

        return valid(
            name = uri.fragment?.takeIf { it.isNotBlank() } ?: "Hysteria2 ${uri.host}:${uri.port}",
            type = ProxyConfigType.Hysteria2Uri,
            content = content,
        )
    }

    private fun validateSingBoxJson(content: String): ProxyConfigValidationResult {
        val syntaxError = JsonObjectSyntaxValidator.firstError(content)
        if (syntaxError != null) return invalid(syntaxError)

        return valid(
            name = "sing-box JSON ${clockMillis()}",
            type = ProxyConfigType.SingBoxJson,
            content = content,
        )
    }

    private fun parseUri(content: String): URI? {
        return runCatching { URI(content) }.getOrNull()
    }

    private fun valid(
        name: String,
        type: ProxyConfigType,
        content: String,
    ): ProxyConfigValidationResult {
        return ProxyConfigValidationResult(
            config = ImportedProxyConfig(
                id = idFactory(),
                name = name,
                type = type,
                content = content,
                importedAtMillis = clockMillis(),
            ),
            error = null,
        )
    }

    private fun invalid(error: String): ProxyConfigValidationResult {
        return ProxyConfigValidationResult(config = null, error = error)
    }
}

private object JsonObjectSyntaxValidator {
    fun firstError(input: String): String? {
        if (!input.trimStart().startsWith("{")) return "sing-box JSON must start with an object"
        if (!input.trimEnd().endsWith("}")) return "sing-box JSON must end with an object"

        val stack = ArrayDeque<Char>()
        var inString = false
        var escaped = false

        input.forEachIndexed { index, char ->
            if (escaped) {
                escaped = false
                return@forEachIndexed
            }

            if (inString) {
                when (char) {
                    '\\' -> escaped = true
                    '"' -> inString = false
                }
                return@forEachIndexed
            }

            when (char) {
                '"' -> inString = true
                '{', '[' -> stack.addLast(char)
                '}' -> {
                    if (stack.removeLastOrNull() != '{') return "JSON object closes incorrectly at $index"
                }
                ']' -> {
                    if (stack.removeLastOrNull() != '[') return "JSON array closes incorrectly at $index"
                }
            }
        }

        if (inString) return "JSON string is not closed"
        if (stack.isNotEmpty()) return "JSON object or array is not closed"
        return null
    }
}
