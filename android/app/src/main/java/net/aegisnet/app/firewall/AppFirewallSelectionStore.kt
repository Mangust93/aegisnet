package net.aegisnet.app.firewall

import android.content.Context

class AppFirewallSelectionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): Set<String> {
        return preferences.getStringSet(KEY_SELECTED_PACKAGES, emptySet()).orEmpty().toSortedSet()
    }

    fun save(packageNames: Set<String>) {
        preferences.edit()
            .putStringSet(KEY_SELECTED_PACKAGES, packageNames.toSortedSet())
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "app_firewall"
        private const val KEY_SELECTED_PACKAGES = "selected_packages"
    }
}

