package net.aegisnet.app.runtime

import java.net.URLClassLoader
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun startFailureCleansCommandClientAndRejectedTunFd() = runBlocking {
        io.nekohasekai.libbox.Libbox.reset()
        io.nekohasekai.libbox.CommandClient.connectFailure = RuntimeException("connect failed")
        val runtime = ExperimentalSingBoxRuntime()
        var closedFd: Int? = null

        runtime.start(
            RuntimeConfig(
                sessionId = "connect-failure",
                tunFd = 77,
                tunFdCloser = TunFdCloser { fd -> closedFd = fd },
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

        assertTrue(runtime.state.value is RuntimeState.Failed)
        assertEquals(77, closedFd)
        assertTrue(io.nekohasekai.libbox.Libbox.lastClient.disconnectCalled)
        assertTrue(io.nekohasekai.libbox.Libbox.lastClient.serviceCloseCalled)
    }

    @Test
    fun stopNeverThrowsWhenCommandClientCleanupFails() = runBlocking {
        io.nekohasekai.libbox.Libbox.reset()
        val runtime = ExperimentalSingBoxRuntime()
        runtime.start(
            RuntimeConfig(
                sessionId = "stop-failure",
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
        io.nekohasekai.libbox.CommandClient.disconnectFailure = RuntimeException("disconnect failed")
        io.nekohasekai.libbox.CommandClient.serviceCloseFailure = RuntimeException("serviceClose failed")

        val result = runCatching { runtime.stop() }

        assertTrue(result.isSuccess)
        assertEquals(RuntimeState.Stopped, runtime.state.value)
        assertTrue(io.nekohasekai.libbox.Libbox.lastClient.disconnectCalled)
        assertTrue(io.nekohasekai.libbox.Libbox.lastClient.serviceCloseCalled)
    }

    @Test
    fun bridgeTracksCommandClientBeforeConnectWithFd() {
        io.nekohasekai.libbox.Libbox.reset()
        val bridge = SfaLibboxRuntimeBridge()
        var trackedDuringConnect = false
        io.nekohasekai.libbox.CommandClient.onConnectWithFd = {
            trackedDuringConnect = bridge.isClientTracked()
        }

        bridge.start(
            basePath = temporaryFolder.newFolder("base").absolutePath,
            workingPath = temporaryFolder.newFolder("working").absolutePath,
            tempPath = temporaryFolder.newFolder("temp").absolutePath,
            tunFd = 88,
        )

        assertTrue(trackedDuringConnect)
        bridge.stop()
    }

    @Test
    fun acceptedTunFdIsNotClosedByRuntimeAfterConnectSucceeds() = runBlocking {
        io.nekohasekai.libbox.Libbox.reset()
        val runtime = ExperimentalSingBoxRuntime()
        var closeCount = 0

        runtime.start(
            RuntimeConfig(
                sessionId = "accepted-fd",
                tunFd = 42,
                tunFdCloser = TunFdCloser { closeCount += 1 },
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

        assertEquals(RuntimeState.Running, runtime.state.value)
        runtime.stop()

        assertEquals(0, closeCount)
        assertFalse(runtime.state.value is RuntimeState.Failed)
    }
}
