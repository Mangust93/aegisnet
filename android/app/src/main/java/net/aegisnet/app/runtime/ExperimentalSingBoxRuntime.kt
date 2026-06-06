package net.aegisnet.app.runtime

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import net.aegisnet.app.diagnostics.DiagnosticEvent
import net.aegisnet.app.diagnostics.DiagnosticLevel
import net.aegisnet.app.diagnostics.DiagnosticSource

class ExperimentalSingBoxRuntime(
    private val classLoader: ClassLoader = ExperimentalSingBoxRuntime::class.java.classLoader
        ?: ClassLoader.getSystemClassLoader(),
) : NetworkRuntime {
    private val mutableState = MutableStateFlow<RuntimeState>(RuntimeState.Stopped)
    private val mutableDiagnostics = MutableSharedFlow<DiagnosticEvent>(
        replay = 32,
        extraBufferCapacity = 32,
    )

    override val state = mutableState.asStateFlow()
    override val diagnostics = mutableDiagnostics.asSharedFlow()

    override suspend fun start(config: RuntimeConfig) {
        if (mutableState.value == RuntimeState.Running || mutableState.value == RuntimeState.Starting) {
            emit(DiagnosticLevel.Debug, "Experimental runtime start ignored from ${mutableState.value.name}")
            return
        }

        mutableState.value = RuntimeState.Starting
        emit(DiagnosticLevel.Info, "experimental_runtime_starting session=${config.sessionId}")

        val proxyConfig = config.proxyConfig
        if (proxyConfig == null) {
            fail("No active proxy config selected")
            return
        }

        if (config.tunFd == null) {
            fail("VPN TUN file descriptor is unavailable")
            return
        }

        when (proxyConfig.type) {
            ProxyConfigType.VlessUri,
            ProxyConfigType.Hysteria2Uri,
            -> {
                fail("${proxyConfig.type.label} import is stored, but URI-to-sing-box conversion is not implemented yet")
                return
            }
            ProxyConfigType.SingBoxJson -> startRawSingBoxConfig(proxyConfig)
        }
    }

    override suspend fun stop() {
        if (mutableState.value == RuntimeState.Stopped) {
            emit(DiagnosticLevel.Debug, "Experimental runtime stop ignored from Stopped")
            return
        }

        mutableState.value = RuntimeState.Stopping
        emit(DiagnosticLevel.Info, "experimental_runtime_stopping")
        mutableState.value = RuntimeState.Stopped
        emit(DiagnosticLevel.Info, "experimental_runtime_stopped")
    }

    private suspend fun startRawSingBoxConfig(proxyConfig: ImportedProxyConfig) {
        val bindingClass = findLibboxClass()
        if (bindingClass == null) {
            fail(
                "sing-box/libbox runtime classes are not packaged. " +
                    "Add a vetted Android libbox AAR or native binding before raw sing-box JSON can start.",
            )
            return
        }

        emit(
            DiagnosticLevel.Debug,
            "experimental_runtime_config_ready id=${proxyConfig.id} type=${proxyConfig.type.name}",
        )
        fail(
            "Found ${bindingClass.name}, but this branch does not yet map the packaged binding API to start/stop calls.",
        )
    }

    private fun findLibboxClass(): Class<*>? {
        return LIBBOX_CLASS_CANDIDATES.firstNotNullOfOrNull { className ->
            runCatching { Class.forName(className, false, classLoader) }.getOrNull()
        }
    }

    private suspend fun fail(message: String) {
        mutableState.value = RuntimeState.Failed(message)
        emit(DiagnosticLevel.Error, "experimental_runtime_failed reason=$message")
    }

    private suspend fun emit(
        level: DiagnosticLevel,
        message: String,
    ) {
        mutableDiagnostics.emit(
            DiagnosticEvent(
                level = level,
                source = DiagnosticSource.Runtime,
                message = message,
            ),
        )
    }

    private companion object {
        val LIBBOX_CLASS_CANDIDATES = listOf(
            "io.nekohasekai.libbox.Libbox",
            "io.nekohasekai.libbox.BoxService",
            "libbox.Libbox",
        )
    }
}
