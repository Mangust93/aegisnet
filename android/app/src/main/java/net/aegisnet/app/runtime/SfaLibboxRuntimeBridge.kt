package net.aegisnet.app.runtime

import java.lang.reflect.Method

internal class SfaLibboxRuntimeBridge(
    private val classLoader: ClassLoader = SfaLibboxRuntimeBridge::class.java.classLoader
        ?: ClassLoader.getSystemClassLoader(),
) {
    private var commandClient: Any? = null

    fun missingRequirements(): List<String> {
        return REQUIRED_CLASSES.filter { className ->
            runCatching { loadClass(className) }.isFailure
        }
    }

    fun start(
        basePath: String,
        workingPath: String,
        tempPath: String,
        tunFd: Int,
    ) {
        val setupOptionsClass = loadClass(SETUP_OPTIONS_CLASS)
        val setupOptions = setupOptionsClass.getDeclaredConstructor().newInstance()
        setupOptions.setStringOption("basePath", basePath)
        setupOptions.setStringOption("workingPath", workingPath)
        setupOptions.setStringOption("tempPath", tempPath)

        val libboxClass = loadClass(LIBBOX_CLASS)
        libboxClass.findStaticMethod("setup", setupOptionsClass).invoke(null, setupOptions)
        val client = libboxClass.findStaticMethod("newStandaloneCommandClient").invoke(null)
            ?: error("Libbox.newStandaloneCommandClient returned null")

        commandClient = client
        runCatching {
            client.javaClass.findInstanceMethod("connectWithFD", Int::class.javaPrimitiveType ?: Integer.TYPE)
                .invoke(client, tunFd)
        }.onFailure { error ->
            stop()
            throw error
        }
    }

    fun isClientTracked(): Boolean {
        return commandClient != null
    }

    fun stop(): StopResult {
        val client = commandClient ?: return StopResult()
        commandClient = null
        val called = mutableListOf<String>()
        val failures = mutableListOf<String>()
        STOP_METHODS.forEach { methodName ->
            val method = client.javaClass.methods.firstOrNull { method ->
                method.name == methodName && method.parameterTypes.isEmpty()
            }
            if (method == null) {
                failures += "$methodName:missing"
                return@forEach
            }
            runCatching {
                method.invoke(client)
            }.onSuccess {
                called += methodName
            }.onFailure { error ->
                failures += "$methodName:${error.message ?: error.javaClass.simpleName}"
            }
        }
        return StopResult(calledMethods = called, failures = failures)
    }

    private fun loadClass(className: String): Class<*> {
        return Class.forName(className, true, classLoader)
    }

    private fun Any.setStringOption(
        name: String,
        value: String,
    ) {
        val capitalizedName = name.replaceFirstChar { it.uppercaseChar() }
        val setter = javaClass.methods.firstOrNull { method ->
            method.name == "set$capitalizedName" &&
                method.parameterTypes.contentEquals(arrayOf(String::class.java))
        }
        if (setter != null) {
            setter.invoke(this, value)
            return
        }

        val field = javaClass.fields.firstOrNull { field ->
            field.name == name && field.type == String::class.java
        } ?: error("SetupOptions is missing writable string option: $name")
        field.set(this, value)
    }

    private fun Class<*>.findStaticMethod(
        name: String,
        vararg parameterTypes: Class<*>,
    ): Method {
        return methods.firstOrNull { method ->
            method.name == name && method.parameterTypes.contentEquals(parameterTypes)
        } ?: error("Missing static method: $this.$name(${parameterTypes.joinToString { it.simpleName }})")
    }

    private fun Class<*>.findInstanceMethod(
        name: String,
        vararg parameterTypes: Class<*>,
    ): Method {
        return methods.firstOrNull { method ->
            method.name == name && method.parameterTypes.contentEquals(parameterTypes)
        } ?: error("Missing instance method: $this.$name(${parameterTypes.joinToString { it.simpleName }})")
    }

    companion object {
        const val LOCAL_ARTIFACT_ROOT = "android/local-libs/sfa-libbox"
        const val LOCAL_JAVA_PATH = "$LOCAL_ARTIFACT_ROOT/java"
        const val LOCAL_JNI_LIBS_PATH = "$LOCAL_ARTIFACT_ROOT/jniLibs"
        const val LIBBOX_CLASS = "io.nekohasekai.libbox.Libbox"
        const val SETUP_OPTIONS_CLASS = "io.nekohasekai.libbox.SetupOptions"
        const val COMMAND_CLIENT_CLASS = "io.nekohasekai.libbox.CommandClient"

        private val REQUIRED_CLASSES = listOf(
            LIBBOX_CLASS,
            SETUP_OPTIONS_CLASS,
            COMMAND_CLIENT_CLASS,
            "go.Seq",
        )

        private val STOP_METHODS = listOf("disconnect", "serviceClose")
    }

    data class StopResult(
        val calledMethods: List<String> = emptyList(),
        val failures: List<String> = emptyList(),
    ) {
        fun isEmpty(): Boolean {
            return calledMethods.isEmpty() && failures.isEmpty()
        }
    }
}
