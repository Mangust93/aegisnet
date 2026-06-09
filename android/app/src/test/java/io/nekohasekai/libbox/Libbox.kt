package io.nekohasekai.libbox

class Libbox private constructor() {
    companion object {
        @JvmStatic
        var lastSetupOptions: SetupOptions? = null
            private set

        @JvmStatic
        lateinit var lastClient: CommandClient
            private set

        @JvmStatic
        fun setup(options: SetupOptions) {
            lastSetupOptions = options
        }

        @JvmStatic
        fun newStandaloneCommandClient(): CommandClient {
            lastClient = CommandClient()
            return lastClient
        }

        @JvmStatic
        fun reset() {
            lastSetupOptions = null
            CommandClient.resetBehavior()
        }
    }
}
