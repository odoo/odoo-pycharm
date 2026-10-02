package com.odoo.odools

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

// Profile names with a special meaning, shared with the server
const val DEFAULT_PROFILE_NAME = "default"
const val DISABLED_PROFILE_NAME = "Disabled"

class OdooProjectSettings {
    var selectedProfile = DEFAULT_PROFILE_NAME
}

@Service(Service.Level.PROJECT)
@State(
    name = "OdooLSProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class OdooProjectSettingsService() : PersistentStateComponent<OdooProjectSettings> {
    var settings: OdooProjectSettings = OdooProjectSettings()

    // Names of the profiles with an odoo_path (set or inferred), as last reported by the server. Not persisted.
    @Volatile
    var profilesWithOdooPath: Set<String> = emptySet()

    fun isOdooLsActive(): Boolean {
        val profile = settings.selectedProfile
        return profile != DISABLED_PROFILE_NAME && profile in profilesWithOdooPath
    }

    override fun getState() = settings

    override fun loadState(state: OdooProjectSettings) {
        settings = state
    }
}