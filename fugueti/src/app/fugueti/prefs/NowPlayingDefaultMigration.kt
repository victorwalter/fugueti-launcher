/*
 * Fugueti Launcher, an Android launcher based on Lawnchair.
 * Copyright (C) 2026 Fugueti Launcher contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package app.fugueti.prefs

import android.content.Context
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

/**
 * At a Glance's "Now playing" is off by default since 16.0.6; until then it was on (like
 * Lawnchair). It needs notification access, so on a new install it put "Tap to set up" on the home
 * screen and opening At a Glance asked for that access right away.
 *
 * A default is not stored, so this runs once per settings store, like [AtAGlanceDefaultMigration]:
 * an install that existed before (the app was updated, not freshly installed) and never set Now
 * playing keeps it on. [MIGRATED] marks the store as done, so it is in every backup made from then
 * on; restoring an older backup applies the same rule (BackupStore.applyBackupDefaults).
 */
class NowPlayingDefaultMigration(private val context: Context) : DataMigration<Preferences> {

    override suspend fun shouldMigrate(currentData: Preferences): Boolean = currentData[MIGRATED] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val prefs = currentData.toMutablePreferences()
        if (ENABLED !in prefs && wasUpdated()) prefs[ENABLED] = true
        prefs[MIGRATED] = true
        return prefs.toPreferences()
    }

    override suspend fun cleanUp() = Unit

    private fun wasUpdated(): Boolean = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        info.lastUpdateTime != info.firstInstallTime
    }.getOrDefault(false)

    companion object {
        val ENABLED = booleanPreferencesKey("enable_smartspace_now_playing")
        val MIGRATED = booleanPreferencesKey("fugueti_now_playing_default_migrated")
    }
}
