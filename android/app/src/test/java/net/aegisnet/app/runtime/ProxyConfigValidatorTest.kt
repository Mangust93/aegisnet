package net.aegisnet.app.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxyConfigValidatorTest {
    private val validator = ProxyConfigValidator(
        clockMillis = { 123L },
        idFactory = { "config-id" },
    )

    @Test
    fun validatesVlessUri() {
        val result = validator.validate("vless://user@example.com:443?security=reality#home")

        assertTrue(result.isValid)
        assertEquals(ProxyConfigType.VlessUri, result.config?.type)
        assertEquals("home", result.config?.name)
    }

    @Test
    fun validatesHysteria2Uri() {
        val result = validator.validate("hysteria2://secret@example.com:443?sni=example.com")

        assertTrue(result.isValid)
        assertEquals(ProxyConfigType.Hysteria2Uri, result.config?.type)
    }

    @Test
    fun validatesRawSingBoxJson() {
        val result = validator.validate("""{"log":{"level":"info"},"outbounds":[{"type":"direct"}]}""")

        assertTrue(result.isValid)
        assertEquals(ProxyConfigType.SingBoxJson, result.config?.type)
    }

    @Test
    fun rejectsUnsupportedInput() {
        val result = validator.validate("not a config")

        assertFalse(result.isValid)
        assertEquals(
            "Unsupported config format. Paste VLESS, Hysteria2, or raw sing-box JSON.",
            result.error,
        )
    }

    @Test
    fun rejectsMalformedJson() {
        val result = validator.validate("""{"outbounds":[""")

        assertFalse(result.isValid)
        assertEquals("sing-box JSON must end with an object", result.error)
    }
}
