package io.nekohasekai.libbox

class CommandClient {
    var connectedFd = -1
        private set
    var disconnectCalled = false
        private set
    var serviceCloseCalled = false
        private set

    fun connectWithFD(fd: Int) {
        connectedFd = fd
    }

    fun disconnect() {
        disconnectCalled = true
    }

    fun serviceClose() {
        serviceCloseCalled = true
    }
}
