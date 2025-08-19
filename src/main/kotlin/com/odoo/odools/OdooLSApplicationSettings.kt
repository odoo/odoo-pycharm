package com.odoo.odools

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.components.StoragePathMacros
import java.nio.file.Paths

@State(
    name = "OdooLSApplicationSettings",
    storages = [Storage(StoragePathMacros.NON_ROAMABLE_FILE)] // Application-level
)
@Service(Service.Level.APP)
class OdooLSApplicationSettings : PersistentStateComponent<OdooLSApplicationSettings.State> {

    data class State(
        var dataPath: String? = defaultPath()
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    companion object {
        fun getInstance(): OdooLSApplicationSettings {
            return com.intellij.openapi.application.ApplicationManager
                .getApplication()
                .getService(OdooLSApplicationSettings::class.java)
        }

        private fun defaultPath(): String {
            val osName = System.getProperty("os.name").lowercase()
            return if (osName.contains("win")) {
                // AppData\Roaming\OdooLS
                Paths.get(System.getenv("APPDATA"), "OdooLS").toString()
            } else if (osName.contains("mac") || osName.contains("darwin")) {
                val userHome = System.getProperty("user.home")
                Paths.get(userHome, "Library", "Application Support", "OdooLS").toString()
            } else {
                // ~/.odools
                val userHome = System.getProperty("user.home")
                Paths.get(userHome, ".odools").toString()
            }
        }
    }
}