package org.zimmob.zimlx

import com.android.launcher3.Launcher
import com.android.launcher3.compat.UserManagerCompat

class ZimPreferencesChangeCallback(private val launcher: Launcher) {

    fun recreate() {
        launcher.recreate()
    }

    fun reloadApps() {
        UserManagerCompat.getInstance(launcher).userProfiles.forEach { launcher.model.onPackagesReload(it) }
    }

    fun reloadAll() {
        launcher.model.forceReload()
    }

    fun restart() {
        launcher.scheduleRestart()
    }

    fun refreshGrid() {
        launcher.refreshGrid()
    }
}
