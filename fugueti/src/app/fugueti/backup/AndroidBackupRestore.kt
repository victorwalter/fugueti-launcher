/*
 * Fugueti Launcher, an Android launcher based on Lawnchair.
 * Copyright (C) 2026 Fugueti Launcher contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package app.fugueti.backup

import android.app.Activity
import android.app.AlertDialog
import android.appwidget.AppWidgetHost
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import app.lawnchair.util.restartLauncher
import com.android.launcher3.InvariantDeviceProfile
import com.android.launcher3.LauncherFiles
import com.android.launcher3.R
import com.android.launcher3.model.DeviceGridState
import com.android.launcher3.provider.RestoreDbTask
import com.android.launcher3.widget.LauncherWidgetHolder
import java.io.File
import java.lang.ref.WeakReference
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

/**
 * Android's own backup (Google account) restores the home screen when the app is installed, before
 * it ever runs. Instead of applying it right away, [stage] keeps it aside and the first start asks
 * whether to use it ([maybeAsk]), like Nova does.
 *
 * - Restore: [accept] restarts the launcher and the next process, before anything opens the
 *   databases or the preferences ([applyIfAccepted], from `LawnchairApp.attachBaseContext`), puts
 *   the files back and marks the restore pending exactly as `LauncherBackupAgent` did, so
 *   `RestoreDbTask` runs as for a plain Android restore.
 * - No thanks: [decline] drops the files and the widget ids Android created for that backup.
 */
object AndroidBackupRestore {
    private const val TAG = "FuguetiAndroidRestore"
    private const val DIR = "android_restore"
    private const val PREFS_XML = "shared_prefs/${LauncherFiles.SHARED_PREFERENCES_KEY}.xml"
    private const val STAGED = "staged"
    private const val ACCEPTED = "accepted"

    /** Files and folders under the data dir that Android's backup brings (see backupscheme.xml). */
    private val RESTORED_PATHS = listOf(
        PREFS_XML,
        "files/datastore/preferences.preferences_pb",
        "files/customFonts",
        "files/wallpapers",
    )

    private var dialog: AlertDialog? = null
    private var dialogActivity: WeakReference<Activity>? = null

    private fun dir(context: Context) = File(context.noBackupFilesDir, DIR)

    private fun databaseDir(context: Context) = File(context.dataDir, "databases")

    private fun isHomeScreenDb(name: String) = name.startsWith("launcher")

    /** Home screen databases and the app database (`preferences`: icon overrides, drawer folders). */
    private fun isRestoredDb(name: String) =
        isHomeScreenDb(name) || name == "preferences" || name.startsWith("preferences-")

    /** What Android restored (or what this run created) that the question decides about. */
    private fun restoredItems(context: Context): List<String> =
        databaseDir(context).listFiles { f -> f.isFile && isRestoredDb(f.name) }.orEmpty()
            .map { "databases/${it.name}" } +
            RESTORED_PATHS.filter { File(context.dataDir, it).exists() }

    /**
     * Called by `LauncherBackupAgent.onRestoreFinished` (restricted backup process). Moves what
     * Android restored aside; returns false (stock behaviour) if Android restored no home screen
     * database or moving failed.
     */
    @JvmStatic
    fun stage(context: Context): Boolean {
        val dbs = databaseDir(context).listFiles().orEmpty()
            .filter { isHomeScreenDb(it.name) && it.name.endsWith(".db") }
        if (dbs.isEmpty()) return false
        val dir = dir(context)
        val items = restoredItems(context)
        return try {
            dir.deleteRecursively()
            items.forEach { move(File(context.dataDir, it), File(dir, it)) }
            File(dir, STAGED).writeText(System.currentTimeMillis().toString())
            Log.d(TAG, "staged Android restore: $items")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "staging failed, applying the restore as is", t)
            // Put back whatever was moved, so the stock restore still works.
            items.forEach { rel ->
                File(dir, rel).takeIf { it.exists() }?.let { runCatching { move(it, File(context.dataDir, rel)) } }
            }
            dir.deleteRecursively()
            false
        }
    }

    /**
     * Called by `LauncherBackupAgent.onFullBackup` (restricted process, the app is not running):
     * folds each database's -wal file into it, so the backup has one consistent file.
     */
    @JvmStatic
    fun prepareForBackup(context: Context) {
        databaseDir(context).listFiles { f -> f.isFile && (isHomeScreenDb(f.name) && f.name.endsWith(".db") || f.name == "preferences") }
            .orEmpty().forEach { db ->
                runCatching {
                    SQLiteDatabase.openDatabase(db.path, null, SQLiteDatabase.OPEN_READWRITE).use {
                        it.rawQuery("PRAGMA wal_checkpoint(TRUNCATE)", null).use { c -> c.moveToFirst() }
                    }
                }.onFailure { Log.w(TAG, "checkpoint of ${db.name} failed", it) }
            }
    }

    /** Moves a file or folder, replacing [to]. */
    private fun move(from: File, to: File) {
        to.deleteRecursively()
        to.parentFile?.mkdirs()
        if (!from.renameTo(to)) {
            from.copyRecursively(to, overwrite = true)
            from.deleteRecursively()
        }
    }

    private fun hasStaged(context: Context): Boolean {
        val dir = dir(context)
        return File(dir, STAGED).exists() && !File(dir, ACCEPTED).exists()
    }

    /** Shows the "backup found" question if Android restored a home screen that is waiting. */
    fun maybeAsk(activity: Activity) {
        if (dialog?.isShowing == true && dialogActivity?.get() === activity) return
        runCatching { dialog?.dismiss() }
        dialog = null
        if (!hasStaged(activity)) return
        dialogActivity = WeakReference(activity)
        dialog = AlertDialog.Builder(activity)
            .setTitle(R.string.fugueti_android_backup_title)
            .setMessage(R.string.fugueti_android_backup_message)
            .setCancelable(false)
            .setNegativeButton(R.string.fugueti_android_backup_decline) { _, _ -> decline(activity.applicationContext) }
            .setPositiveButton(R.string.fugueti_android_backup_restore) { _, _ -> accept(activity.applicationContext) }
            .show()
    }

    /** Is the question showing (other first-start prompts wait for it). */
    fun isAsking(): Boolean = dialog?.isShowing == true

    private fun accept(context: Context) {
        File(dir(context), ACCEPTED).writeText(System.currentTimeMillis().toString())
        Log.d(TAG, "Android restore accepted, restarting")
        restartLauncher(context)
    }

    private fun decline(context: Context) {
        Log.d(TAG, "Android restore declined")
        // The widget ids Android allocated for the backup's widgets (AppWidgetsRestoredReceiver).
        val prefs = context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
        val ids = prefs.getString(RestoreDbTask.APPWIDGET_IDS, null).orEmpty()
            .split(',').mapNotNull { it.trim().toIntOrNull() }
        if (ids.isNotEmpty()) {
            val host = AppWidgetHost(context, LauncherWidgetHolder.APPWIDGET_HOST_ID)
            ids.forEach { id -> runCatching { host.deleteAppWidgetId(id) } }
        }
        prefs.edit()
            .remove(RestoreDbTask.APPWIDGET_IDS)
            .remove(RestoreDbTask.APPWIDGET_OLD_IDS)
            .commit()
        dir(context).deleteRecursively()
    }

    /**
     * Runs in `LawnchairApp.attachBaseContext` of the main process, before any provider, database
     * or preference of this process is opened. Puts an accepted Android restore back in place.
     */
    @JvmStatic
    fun applyIfAccepted(context: Context) {
        val dir = dir(context)
        if (!File(dir, ACCEPTED).exists()) return
        try {
            val prefs = context.getSharedPreferences(LauncherFiles.SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE)
            // Written by AppWidgetsRestoredReceiver after staging, into the live preferences.
            val widgetIds = prefs.getString(RestoreDbTask.APPWIDGET_IDS, null)
            val oldWidgetIds = prefs.getString(RestoreDbTask.APPWIDGET_OLD_IDS, null)

            // The first run's databases, settings and files go; the restored ones come back.
            restoredItems(context).filter { it != PREFS_XML }
                .forEach { File(context.dataDir, it).deleteRecursively() }
            File(dir, "databases").listFiles()?.forEach { move(it, File(databaseDir(context), it.name)) }
            RESTORED_PATHS.filter { it != PREFS_XML }.forEach { rel ->
                File(dir, rel).takeIf { it.exists() }?.let { move(it, File(context.dataDir, rel)) }
            }

            // Preferences: the restored ones, as Android's restore leaves them, plus what
            // RestoreDbTask.setPending writes.
            val stagedPrefs = File(dir, PREFS_XML)
            val editor = prefs.edit().clear()
            if (stagedPrefs.exists()) BackupStore.putPrefsFromXml(editor, stagedPrefs)
            if (widgetIds != null && oldWidgetIds != null) {
                editor.putString(RestoreDbTask.APPWIDGET_IDS, widgetIds)
                editor.putString(RestoreDbTask.APPWIDGET_OLD_IDS, oldWidgetIds)
            }
            editor.putInt(RestoreDbTask.RESTORED_DEVICE_TYPE, restoredDeviceType(stagedPrefs))
            editor.commit()
            context.getSharedPreferences(LauncherFiles.DEVICE_PREFERENCES_KEY, Context.MODE_PRIVATE).edit()
                .putBoolean(RestoreDbTask.FIRST_LOAD_AFTER_RESTORE_KEY, true)
                .commit()
            Log.d(TAG, "Android restore applied")
        } catch (t: Throwable) {
            Log.e(TAG, "applying the Android restore failed", t)
        } finally {
            dir.deleteRecursively()
        }
    }

    /** `DeviceGridState`'s device type in the restored preferences (what setPending stores). */
    private fun restoredDeviceType(xml: File): Int {
        if (!xml.exists()) return InvariantDeviceProfile.TYPE_PHONE
        return runCatching {
            val nodes = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml)
                .documentElement.getElementsByTagName("int")
            (0 until nodes.length).map { nodes.item(it) as Element }
                .firstOrNull { it.getAttribute("name") == DeviceGridState.KEY_DEVICE_TYPE }
                ?.getAttribute("value")?.toInt()
        }.getOrNull() ?: InvariantDeviceProfile.TYPE_PHONE
    }
}
