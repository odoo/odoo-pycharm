package com.odoo.odools

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.StoragePathMacros
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

class OdooProjectSettings {
    var selectedProfile = "default"
}

@Service(Service.Level.PROJECT)
@State(
    name = "OdooLSProjectSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class OdooProjectSettingsService() : PersistentStateComponent<OdooProjectSettings> {
    var settings: OdooProjectSettings = OdooProjectSettings()

    override fun getState() = settings

    override fun loadState(state: OdooProjectSettings) {
        settings = state
    }
}