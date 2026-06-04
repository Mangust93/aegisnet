package net.aegisnet.app.firewall

import android.content.Context
import android.content.Intent

class InstalledAppsRepository(
    private val context: Context,
) {
    fun loadLaunchableApps(): List<InstalledAppInfo> {
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)

        return packageManager.queryIntentActivities(launcherIntent, 0)
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                val packageName = activityInfo.packageName
                val appName = resolveInfo.loadLabel(packageManager)?.toString()
                    ?.trim()
                    ?.takeIf { it.isNotEmpty() }
                    ?: packageName
                InstalledAppInfo(
                    appName = appName,
                    packageName = packageName,
                )
            }
            .distinctBy { it.packageName }
            .filterNot { it.packageName == context.packageName }
            .sortedWith(
                compareBy<InstalledAppInfo> { it.appName.lowercase() }
                    .thenBy { it.packageName },
            )
    }
}
