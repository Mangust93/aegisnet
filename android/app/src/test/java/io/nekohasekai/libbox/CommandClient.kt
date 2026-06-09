package io.nekohasekai.libbox

class CommandClient {
    var connectedFd = -1
        private set
    var disconnectCalled = false
        private set
    var serviceCloseCalled = false
        private set

    fun connectWithFD(fd: Int) {
        onConnectWithFd?.invoke()
        connectFailure?.let { failure -> throw failure }
        connectedFd = fd
    }

    fun disconnect() {
        val failure = disconnectFailure
        if (failure != null) {
            disconnectCalled = true
            throw failure
        }
        disconnectCalled = true
    }

    fun serviceClose() {
        val failure = serviceCloseFailure
        if (failure != null) {
            serviceCloseCalled = true
            throw failure
        }
        serviceCloseCalled = true
    }

    companion object {
        var connectFailure: RuntimeException? = null
        var disconnectFailure: RuntimeException? = null
        var serviceCloseFailure: RuntimeException? = null
        var onConnectWithFd: (() -> Unit)? = null

        fun resetBehavior() {
            connectFailure = null
            disconnectFailure = null
            serviceCloseFailure = null
            onConnectWithFd = null
        }
    }
}
