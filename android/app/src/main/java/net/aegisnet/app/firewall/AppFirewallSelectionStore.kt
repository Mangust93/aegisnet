package net.aegisnet.app.firewall

import android.content.Context

class AppFirewallSelectionStore(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun initializeProfiles(): List<FirewallProfile> {
        val createdProfiles = FirewallProfile.entries.filterNot { profile ->
            preferences.contains(profileKey(profile))
        }
        if (createdProfiles.isEmpty()) return emptyList()

        val legacySelection = preferences.getStringSet(KEY_SELECTED_PACKAGES, emptySet()).orEmpty()
        val editor = preferences.edit()
        createdProfiles.forEach { profile ->
            val initialPackages = if (profile == FirewallProfile.Custom) legacySelection else emptySet()
            editor.putStringSet(profileKey(profile), initialPackages.toSortedSet())
        }
        if (!preferences.contains(KEY_ACTIVE_PROFILE)) {
            editor.putString(KEY_ACTIVE_PROFILE, FirewallProfile.Custom.name)
        }
        editor.apply()
        return createdProfiles
    }

    fun loadActiveProfile(): FirewallProfile {
        val rawProfile = preferences.getString(KEY_ACTIVE_PROFILE, FirewallProfile.Custom.name)
        return FirewallProfile.entries.firstOrNull { it.name == rawProfile } ?: FirewallProfile.Custom
    }

    fun saveActiveProfile(profile: FirewallProfile) {
        preferences.edit()
            .putString(KEY_ACTIVE_PROFILE, profile.name)
            .apply()
    }

    fun load(): Set<String> {
        return load(loadActiveProfile())
    }

    fun load(profile: FirewallProfile): Set<String> {
        return preferences.getStringSet(profileKey(profile), emptySet()).orEmpty().toSortedSet()
    }

    fun save(packageNames: Set<String>) {
        save(loadActiveProfile(), packageNames)
    }

    fun save(profile: FirewallProfile, packageNames: Set<String>) {
        preferences.edit()
            .putStringSet(profileKey(profile), packageNames.toSortedSet())
            .apply()
    }

    companion object {
        private const val PREFERENCES_NAME = "app_firewall"
        private const val KEY_SELECTED_PACKAGES = "selected_packages"
        private const val KEY_ACTIVE_PROFILE = "active_profile"

        private fun profileKey(profile: FirewallProfile): String {
            return "profile_${profile.name.lowercase()}_packages"
        }
    }
}
