package net.aegisnet.app.runtime

import java.net.URLClassLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ExperimentalSingBoxRuntimeTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun rawSingBoxJsonFailsClearlyWhenSfaLibboxArtifactIsMissing() = runBlocking {
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
                runtimeBasePath = temporaryFolder.newFolder("base").absolutePath,
                runtimeWorkingPath = temporaryFolder.newFolder("working").absolutePath,
                runtimeTempPath = temporaryFolder.newFolder("temp").absolutePath,
            ),
        )

        val failedState = runtime.state.value as RuntimeState.Failed
        assertTrue(failedState.message.contains("SFA libbox runtime artifact missing"))
        assertTrue(failedState.message.contains("android/local-libs/sfa-libbox/java"))
        assertTrue(failedState.message.contains("android/local-libs/sfa-libbox/jniLibs"))
    }

    @Test
    fun rawSingBoxJsonConnectsSfaStandaloneCommandClientWithTunFd() = runBlocking {
        io.nekohasekai.libbox.Libbox.reset()
        val workingPath = temporaryFolder.newFolder("working").absolutePath
        val runtime = ExperimentalSingBoxRuntime()

        runtime.start(
            RuntimeConfig(
                sessionId = "sfa-libbox",
                tunFd = 42,
                proxyConfig = ImportedProxyConfig(
                    id = "config-id",
                    name = "raw json",
                    type = ProxyConfigType.SingBoxJson,
                    content = """{"outbounds":[{"type":"direct"}]}""",
                    importedAtMillis = 123L,
                ),
                runtimeBasePath = temporaryFolder.newFolder("base").absolutePath,
                runtimeWorkingPath = workingPath,
                runtimeTempPath = temporaryFolder.newFolder("temp").absolutePath,
            ),
        )

        assertEquals(RuntimeState.Running, runtime.state.value)
        assertEquals(42, io.nekohasekai.libbox.Libbox.lastClient.connectedFd)
        assertEquals(
            """{"outbounds":[{"type":"direct"}]}""",
            java.io.File(workingPath, "config.json").readText(),
        )

        runtime.stop()

        assertEquals(RuntimeState.Stopped, runtime.state.value)
        assertTrue(io.nekohasekai.libbox.Libbox.lastClient.disconnectCalled)
        assertTrue(io.nekohasekai.libbox.Libbox.lastClient.serviceCloseCalled)
    }
}
