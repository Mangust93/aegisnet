package net.aegisnet.app.runtime

import java.net.URLClassLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class ExperimentalSingBoxRuntimeTest {
    @Test
    fun rawSingBoxJsonFailsClearlyWhenLibboxArtifactIsMissing() = runBlocking {
        val runtime = ExperimentalSingBoxRuntime(classLoader = URLClassLoader(emptyArray(), null))

        runtime.start(
            RuntimeConfig(
                sessionId = "missing-libbox",
                tunFd = 42,
                proxyConfig = ImportedProxyConfig(
                    id = "config-id",
                    name = "raw json",
                    type = ProxyConfigType.SingBoxJson,
                    content = """{"outbounds":[{"type":"direct"}]}""",
                    importedAtMillis = 123L,
                ),
            ),
        )

        val failedState = runtime.state.value as RuntimeState.Failed
        assertTrue(failedState.message.contains("libbox artifact missing"))
        assertTrue(failedState.message.contains("android/local-libs/libbox.aar"))
        assertTrue(failedState.message.contains("Next setup step"))
    }
}
