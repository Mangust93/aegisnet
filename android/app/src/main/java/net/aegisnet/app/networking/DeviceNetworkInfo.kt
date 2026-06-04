package net.aegisnet.app.networking

data class DeviceNetworkInfo(
    val networkType: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
) {
    val packageVersionLabel: String
        get() = "$packageName $versionName ($versionCode)"

    companion object {
        val Unknown = DeviceNetworkInfo(
            networkType = "Unknown",
            packageName = "Unknown",
            versionName = "Unknown",
            versionCode = 0L,
        )
    }
}
