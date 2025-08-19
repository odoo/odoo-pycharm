package com.odoo.odools

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ProcessOutput
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.SystemInfo
import java.nio.charset.StandardCharsets
import java.nio.file.FileSystems
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

val ODOO_LSP_INSTALLED = Key.create<Boolean>("OdooLS.Installed")

class OdooLSInstallationProjectActivity : ProjectActivity, DumbAware {
    override suspend fun execute(project: Project) {
        println("Plugin started for project ${project.name}")
        // Check installation
        val pathToInstallation = OdooLSApplicationSettings.getInstance().state.dataPath
        if (pathToInstallation == null || pathToInstallation.isEmpty()) {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("OdooLS Notifications")
                .createNotification(
                    "OdooLS Installation not complete",
                    "Please configure the OdooLS installation path in Settings → Tools → OdooLS, then restart PyCharm",
                    NotificationType.WARNING
                ).addAction(NotificationAction.create("Open settings") { _, _ ->
                    com.intellij.openapi.options.ShowSettingsUtil.getInstance()
                        .showSettingsDialog(project, "OdooLS")
                })
                .notify(project)
            return
        }
        val isInstalled = project.getUserData(ODOO_LSP_INSTALLED) //TODO it would have been better to check if server is actually running, but API doesn't provide the information
        if (isInstalled == null || !isInstalled) {
            installFromResources(pathToInstallation) {
                project.putUserData(ODOO_LSP_INSTALLED, true)
            }
            return
        }
    }

    fun copyDirectoryFromResourcesToInstallLocation(directoryPath: String, targetPath: String) {
        val targetLocation = Paths.get(targetPath, "typeshed")
        val resourceUrl = javaClass.classLoader.getResource(directoryPath)
            ?: throw IllegalArgumentException("Resource not found: $directoryPath")

        if (resourceUrl.protocol == "jar") {
            val fileSystem = FileSystems.newFileSystem(resourceUrl.toURI(), emptyMap<String, Any>())
            val jarPath = fileSystem.getPath(directoryPath)
            Files.walk(jarPath).forEach { source ->
                val dest = targetLocation.resolve(jarPath.relativize(source).toString())
                if (Files.isDirectory(source)) {
                    if (!dest.exists()) dest.createDirectories()
                } else {
                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        } else {
            // Running in IDE (not packaged into jar yet)
            val sourcePath = Paths.get(resourceUrl.toURI())
            Files.walk(sourcePath).forEach { source ->
                val dest = targetLocation.resolve(sourcePath.relativize(source).toString())
                if (Files.isDirectory(source)) {
                    if (!dest.exists()) dest.createDirectories()
                } else {
                    Files.copy(source, dest, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    fun ressourceToInstallPath(resourcePath: String, targetPath: Path) {
        javaClass.classLoader.getResourceAsStream(resourcePath).use { input ->
            requireNotNull(input) { "Resource not found: $resourcePath" }
            try {
                val parentDir = targetPath.parent
                if (parentDir != null) {
                    Files.createDirectories(parentDir)
                }
                Files.copy(input, targetPath, StandardCopyOption.REPLACE_EXISTING)
            } catch (e: Exception) {
                if (!targetPath.exists()) {
                    //display message notification
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("OdooLS Notifications")
                        .createNotification(
                            "Error installing Odoo LS",
                            "Error (${e.javaClass}) installing Odoo LS: ${e.message}",
                            NotificationType.ERROR
                        )
                        .notify(null)
                }
            }
            true
        }
    }

    fun getExecutableVersion(executablePath: Path, callback: (String?) -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val cmd = GeneralCommandLine(executablePath.toString(), "--version")
                cmd.charset = StandardCharsets.UTF_8

                val processHandler = CapturingProcessHandler(cmd)
                val output: ProcessOutput = processHandler.runProcess(5000) // timeout in ms

                if (output.exitCode == 0) {
                    val regex = Regex("""\b\d+\.\d+\.\d+\b""")
                    ApplicationManager.getApplication().invokeLater {
                        callback(regex.find(output.stdout.trim())?.value)
                    }
                } else {
                    System.err.println("Error: ${output.stderr}")
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            ApplicationManager.getApplication().invokeLater {
                callback(null)
            }
        }
    }

    fun compareVersions(v1: String, v2: String): Int {
        val parts1 = v1.split(".").map { it.toIntOrNull() ?: 0 }
        val parts2 = v2.split(".").map { it.toIntOrNull() ?: 0 }

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val p1 = parts1.getOrElse(i) { 0 }
            val p2 = parts2.getOrElse(i) { 0 }
            if (p1 != p2) {
                return p1 - p2 // >0 if v1 > v2, <0 if v1 < v2
            }
        }
        return 0
    }

    fun installedVersionOlder(executablePath: Path, callback: (Boolean) -> Unit) {
        getExecutableVersion(executablePath) { currentVersion ->
            if (!Files.exists(executablePath)) {
                callback(true)
                return@getExecutableVersion
            }
            if (currentVersion == null) {
                callback(true)
                return@getExecutableVersion
            }

            val pluginId = "com.odoo.odools"
            val plugin = PluginManagerCore.getPlugin(com.intellij.openapi.extensions.PluginId.getId(pluginId))
            val version = plugin?.version
            if (version == null) {
                callback(false)
                return@getExecutableVersion
            }
            callback(compareVersions(currentVersion, version) < 0)
        }
    }

    fun isTypeshedOutDated(typeshedPath: Path): Boolean {
        //1: get current hash from resources
        val inputStream = object {}.javaClass.classLoader
            .getResourceAsStream("typeshed/odools_commit_hash.txt")
            ?: throw IllegalStateException("Resource not found: typeshed/odools_commit_hash.txt")
        val hash = inputStream.bufferedReader().use { it.readText().trim() }
        //2: get hash from installed typeshed
        val hashFile = typeshedPath.resolve("odools_commit_hash.txt")
        if (!hashFile.exists()) return true
        val file = hashFile.toFile()
        val installedHash = file.readText().trim()
        //3: compare hashes
        return installedHash != hash
    }

    fun installFromResources(targetLocation: String, callback: () -> Unit) {
        val exeName = if (SystemInfo.isWindows) "odoo_ls_server.exe" else "odoo_ls_server"


        installedVersionOlder(Paths.get(targetLocation, exeName)) { isOlder ->
            if (isOlder) {
                ressourceToInstallPath("odools-binaries/${targetOs}/$exeName", Paths.get(targetLocation, exeName))

                if (SystemInfo.isWindows &&
                    (!Files.exists(Paths.get(targetLocation, "odoo_ls_server.pdb")))) {
                    ressourceToInstallPath("odools-binaries/${targetOs}/odoo_ls_server.pdb", Paths.get(targetLocation, "odoo_ls_server.pdb"))
                }
                if (!SystemInfo.isWindows) {
                    Paths.get(targetLocation, exeName).toFile().setExecutable(true, false)
                }
            }
            if (!Files.exists(Paths.get(targetLocation, "typeshed")) && isTypeshedOutDated(Paths.get(targetLocation, "typeshed"))) {
                val file = Paths.get(targetLocation, "typeshed").toFile()
                if (file.exists()) {
                    file.deleteRecursively()
                }
                copyDirectoryFromResourcesToInstallLocation("typeshed", targetLocation)
            }
            println("Installation complete")
            callback()
        }
    }
}