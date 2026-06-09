package net.aegisnet.app.runtime

import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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

        val tunFd = config.tunFd
        if (tunFd == null) {
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
            ProxyConfigType.SingBoxJson -> startRawSingBoxConfig(config, proxyConfig, tunFd)
        }
    }

    override suspend fun stop() {
        if (mutableState.value == RuntimeState.Stopped) {
            emit(DiagnosticLevel.Debug, "Experimental runtime stop ignored from Stopped")
            return
        }

        mutableState.value = RuntimeState.Stopping
        emit(DiagnosticLevel.Info, "experimental_runtime_stopping")
        val closedMethods = withContext(Dispatchers.IO) {
            bridge.stop()
        }
        if (closedMethods.isEmpty()) {
            emit(DiagnosticLevel.Debug, "sfa_libbox_command_client_stop_skipped")
        } else {
            emit(DiagnosticLevel.Info, "sfa_libbox_command_client_stopped methods=${closedMethods.joinToString(",")}")
        }
        mutableState.value = RuntimeState.Stopped
        emit(DiagnosticLevel.Info, "experimental_runtime_stopped")
    }

    private suspend fun startRawSingBoxConfig(
        config: RuntimeConfig,
        proxyConfig: ImportedProxyConfig,
        tunFd: Int,
    ) {
        val missingRequirements = bridge.missingRequirements()
        if (missingRequirements.isNotEmpty()) {
            fail(
                "SFA libbox runtime artifact missing: expected Java bindings at " +
                    "${SfaLibboxRuntimeBridge.LOCAL_JAVA_PATH} and native libraries at " +
                    "${SfaLibboxRuntimeBridge.LOCAL_JNI_LIBS_PATH}. Missing classes: " +
                    missingRequirements.joinToString(", ") +
                    ". Next setup step: extract the official SFA APK runtime into " +
                    SfaLibboxRuntimeBridge.LOCAL_ARTIFACT_ROOT +
                    " and rebuild.",
            )
            return
        }

        val basePath = config.runtimeBasePath
        val workingPath = config.runtimeWorkingPath
        val tempPath = config.runtimeTempPath
        if (basePath == null || workingPath == null || tempPath == null) {
            fail("SFA libbox runtime paths are unavailable")
            return
        }

        emit(
            DiagnosticLevel.Debug,
            "experimental_runtime_config_ready id=${proxyConfig.id} type=${proxyConfig.type.name}",
        )

        runCatching {
            withContext(Dispatchers.IO) {
                writeRuntimeConfig(workingPath, proxyConfig.content)
                File(basePath).mkdirs()
                File(tempPath).mkdirs()
                bridge.start(
                    basePath = basePath,
                    workingPath = workingPath,
                    tempPath = tempPath,
                    tunFd = tunFd,
                )
            }
        }.onFailure { error ->
            fail("SFA libbox runtime start failed: ${error.message ?: error.javaClass.simpleName}")
            return
        }

        mutableState.value = RuntimeState.Running
        emit(
            DiagnosticLevel.Info,
            "sfa_libbox_runtime_connected tunFd=$tunFd config=${proxyConfig.name}",
        )
    }

    private fun writeRuntimeConfig(
        workingPath: String,
        content: String,
    ) {
        val workingDirectory = File(workingPath)
        workingDirectory.mkdirs()
        File(workingDirectory, SING_BOX_CONFIG_FILE_NAME).writeText(content)
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

    private val bridge = SfaLibboxRuntimeBridge(classLoader)

    private companion object {
        const val SING_BOX_CONFIG_FILE_NAME = "config.json"
    }
}
