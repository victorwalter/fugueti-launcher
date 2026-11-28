# Backup and restore

How Fugueti backs up and restores the launcher, what can go wrong and how to test it. Read this
before touching `LawnchairBackup`, `BackupStore`, `NovaBackupConverter`, `ModelDbController`,
`RestoreDbTask` or the grid preferences.

## What a backup holds

A `.lawnchairbackup` is a zip (`LawnchairBackup.create`):

| Entry | Content |
| --- | --- |
| `info.pb` | `BackupInfo`: version, date, what it contains and the **grid state** (`grid_size` "columns,rows", `hotseat_count`, device type) |
| `launcher.db` | the home screen database of the current grid (`launcher_{rows}_{columns}_{dock}.db`), copied after a WAL checkpoint |
| `com.android.launcher3.prefs.xml` | the launcher's SharedPreferences (Lawnchair's `PreferenceManager`, grid prefs `pref_workspaceColumns`, `pref_workspaceRows`, `pref_hotseatColumns`, ...) |
| `preferences.preferences_pb` | the DataStore of `PreferenceManager2` (every Fugueti setting: search bar, gestures, drawer, ...) |
| `preferences` | the app database (`AppDatabase`: saved wallpapers, custom icons, ...) |
| `files/...` | fonts and saved wallpapers |
| `screenshot.png`, `wallpaper.png` | preview shown on the restore screen (only a picture; the database is what is restored) |

Settings are backed up **whole**: every new preference, in either store, is in the backup without
listing it anywhere.

Backups made by Lawnchair itself have the same format. A backup of a setup that never changed the
grid has **no grid preferences** in its prefs XML, only the grid in `info.pb`.

## Restoring a Fugueti / Lawnchair backup

`LawnchairBackup.restore` (settings screen `RestoreBackupScreen`), then `restartLauncher` kills the
process and starts a new one.

In the running process it must **not** touch the home screen databases: the launcher has them
open. It only:

1. extracts the zip; `launcher.db` is copied to `restored.db`, next to `restored.db.owner` holding
   this process's id (this process must not take it: it is killed by the restart, and if it
   opened the database in its last milliseconds it lost `RestoreDbTask`'s work). If anything
   fails, `restored.db*` is deleted so it is not applied at a later start;
2. replaces the SharedPreferences and the DataStore (`BackupStore.applySharedPrefs`,
   `applyDataStore`) and imports the app database;
3. writes the backup's grid: `DeviceGridState(info.gridState).writeToPrefs`, the grid prefs from
   `info.gridState` (`applyBackupGrid`, needed for backups without grid prefs) and `DB_FILE` =
   `launcher_{rows}_{columns}_{dock}.db` of that grid.

In the new process, `ModelDbController.createDbIfNotExists` (16) / `currentDbFile` (17), before
anything reads the database:

1. if `restored.db` exists and `LawnchairApp.hasRestoredDbToTake()` (no owner file, or another
   process's), the database to open is the current grid's (`idp.dbFile`, the backup's) and the
   grid state and `DB_FILE` are written again for it, synchronously;
2. `LawnchairApp.renameRestoredDb` moves `restored.db` over it (deleting the old file's journal
   files) and the "empty database created" flag of that file is cleared;
3. `RestoreDbTask.performRestore` sanitizes it (profiles, restored flags, widget ids).

The loader then sees grid state == grid (`Migration is not needed`) and loads the restored layout.

Expect, in the old process during its last moments, a `Migration is needed` /
`resetLauncherDb` on the **previous** grid's database: the grid preferences changed under the
running model. It is harmless (that database is dropped once the new grid's one is in place), but
it means the previous layout is gone if the restart never happens.

## Importing a Nova backup

`NovaBackupConverter.convertAndRestore` (`RestoreNovaBackupScreen`) runs on `Dispatchers.IO`:

1. `NovaGestureImporter` and `NovaSettingsImporter` apply Nova's gestures and settings
   (`nova.xml`: icon sizes, labels, drawer, search bars, status bar, rotation, ...). Nova's home
   search bar `NONE` turns the dock search bar off; any other placement turns it back on if it
   was off;
2. Nova's `nova.db` is converted into a launcher database, and Nova's grid is written (with
   `commit`) to the grid prefs, `DeviceGridState` and `DB_FILE` (`putSync`) and the IDP;
3. the database is staged exactly like a backup restore: `restored.db` with `restored.db.owner`
   (this process); imported deep shortcuts are pinned; the launcher restarts and the next process
   takes `restored.db` as above.

Taking `restored.db` in the importing process (`reopenDb`) was tried: the model of that process
was still on the old grid and wrote the old grid state back in its last moments, so the next start
"migrated" the old layout over Nova's.

## Android's own backup (reinstall, new phone)

Android's backup (`LauncherBackupAgent`; `res/xml/backupscheme.xml` up to Android 11,
`fugueti/res/xml/fugueti_data_extraction_rules.xml` from 12, keep both the same) holds what a
Fugueti backup file holds: the whole `databases` folder except caches and temporary files (the
home screen database is named after the grid, `launcher_{rows}_{columns}_{dock}.db`, so no fixed
list works; until 16.0.5 the list had only Launcher3's names and the home screen was never
backed up), `com.android.launcher3.prefs.xml`, the DataStore (`files/datastore/preferences.preferences_pb`),
`files/customFonts` and `files/wallpapers`. `onFullBackup` checkpoints the databases first
(`AndroidBackupRestore.prepareForBackup`).

Android restores them when the app is installed, before it ever runs, so the app cannot ask
first. Instead (`AndroidBackupRestore`):

1. `onRestoreFinished` (restricted backup process: no `LawnchairApp`, so nothing that needs the
   IDP or the Dagger graph) moves the restored databases (`launcher*`, `preferences*`), the prefs
   XML, the DataStore and the two folders to `no_backup/android_restore/` and does **not** call
   `RestoreDbTask.setPending`. If no home screen database was restored (backups from 16.0.5 and
   before), or moving fails, the stock path runs.
2. The first start is a new install (default layout and settings) and
   `LawnchairLauncher.onResume` asks "Restore backup?".
3. **No thanks**: the folder is deleted, and so are the widget ids Android allocated for the
   backup's widgets (`AppWidgetsRestoredReceiver` stored them in `appwidget_ids`).
4. **Restore**: writes `accepted` and restarts. The next process, in
   `LawnchairApp.attachBaseContext` (before any provider, database, DataStore or preference is
   opened), deletes the first run's files, moves the restored ones back, replaces the prefs with
   the restored ones (keeping the live `appwidget_ids` / `appwidget_old_ids`) and writes what
   `setPending` would have: `restored_task_pending` (device type of the backup) and
   `first_load_after_restore`. From there it is the stock Android restore (`RestoreDbTask`).

Test: `adb shell bmgr backupnow <pkg>`, `adb uninstall` and `adb install` (Android restores at
install), open the launcher; check both answers, that the backup's widgets come back after
Restore and that Fugueti's settings (e.g. a gesture) come back too.

## Pitfalls (all hit on 2026-11-27)

- **Never replace or delete a database file the running launcher has open.** Its next write fails
  with `SQLITE_READONLY_DBMOVED` and the loader crashes. Stage as `restored.db`; move it in place
  only from the next process (the owner file keeps the restoring one from taking it), never from a
  second `ModelDbController`.
- **`DB_FILE` is the loader's "source grid".** `GridSizeMigrationDBController.needsToMigrate`
  compares it with `idp.dbFile`. If they differ and the open database is already the target, the
  "migration" is treated as failed and `resetLauncherDb` wipes the layout and loads the default
  one. A stale `DB_FILE` (old grid) also made `LawnchairApp.cleanUpDatabases` delete the database
  that was open; it now keeps the open one.
- **Write synchronously before a restart.** `restartLauncher` ends with `exitProcess(0)`;
  anything written with `SharedPreferences.apply()` / `LauncherPrefs.put` just before can be
  lost. The grid state and `DB_FILE` are written with `commit` / `putSync`; a lost write left the
  previous grid in `DB_FILE` and the next start "migrated" the previous layout over the restored
  one.
- **The "empty database created" flag** (`EMPTY_DATABASE_CREATED@<db>`) makes the loader load the
  default layout over whatever is in the file. A restored file must clear it.
- **The backup's grid must come along.** Without it Fugueti's own default grid stays, the layout
  does not fit and is dropped.
- **Preferences set off the main thread.** The importers run on `Dispatchers.IO`, and a
  preference's `onSet` may reload the launcher. `ReloadHelper.reloadGrid/recreate` post to the
  main thread; `Activity.recreate()` elsewhere throws "Must be called from main thread", which made
  every Nova import fail as soon as it set the drawer search bar.
- **Backups from Lawnchair.** A setting left at its default is not stored, so a Lawnchair
  backup has no entry for it and the restore applied Fugueti's default. Where Fugueti's default
  differs or differed (At a Glance on, drawer search bar shown, icon sizes 100%),
  `BackupStore.applyBackupDefaults` sets Lawnchair's for the settings the backup leaves out. A
  Lawnchair backup is told apart by `BackupInfo.lawnchair_version`: 10 digits for Lawnchair, 8
  for Fugueti. When changing another default that Lawnchair also has, add it there.
- **At a Glance default (16.0.5).** It was off until 16.0.4 and is on since, like Lawnchair.
  Turning it on reserves the first row of the first page and the loader removes what is there,
  so an existing setup must not get it by the new default: `AtAGlanceDefaultMigration` (a
  DataStore migration) stores "off" once for an updated install that never set it and marks the
  store (`fugueti_at_a_glance_default_migrated`). A Fugueti backup without that mark is from
  before: `applyBackupDefaults` keeps At a Glance off when the backup has no value for it.
- **Now playing default (16.0.6).** On until 16.0.5, off since (it needs notification access,
  asked for as soon as At a Glance's settings opened). `NowPlayingDefaultMigration` stores "on"
  for an updated install that never set it (mark `fugueti_now_playing_default_migrated`);
  `applyBackupDefaults` sets it on for Lawnchair backups and Fugueti backups without the mark
  that leave it out.
- **Defaults are not stored.** An `IntPref` default that changes (the phone grid went from 7 x 12
  to 5 x 5 in 16.0.5 and to 4 x 5 with 4 dock icons in 16.0.6) changes the grid of every install
  that never touched it; the `PreferenceManager` migration writes the old values for existing
  installs (7 x 12 x 7 for prefs version < 3, 5 x 5 x 5 for version 3). Write such migrations
  before the preference listener is registered (see the comment there).
- After editing preference classes, clean the Kotlin build dirs before building (stale
  `$stable` fields crash at start).

## Testing

Test every change to this area on both Android 16 (`app.fugueti.launcher`) and 17 (nightly), on
the Pixel, with the log captured from before the restore (`adb logcat -b all -G 16M` first: the
default buffer only keeps a few minutes).

Backups to cover (keep copies in the phone's `/sdcard/tmp`):

| Backup | Why |
| --- | --- |
| Fugueti backup with a custom grid (e.g. 7 x 12, 7 dock icons, widgets, a folder) | grid prefs present, widgets get new ids |
| Fugueti backup of the default grid (no grid prefs) | grid only in `info.pb` |
| Backup made by Lawnchair (e.g. 4 x 5, 4 dock icons) | another app's backup, no Fugueti settings |
| Nova backup with the drawer search bar set (TOP/NONE) | settings that reload the launcher during the import |
| Nova backup with `searchbar_placement` NONE / DOCK_BELOW_ICONS | home search bar mapping |

For each restore check:

- no `FATAL EXCEPTION`, `SQLITE_READONLY_DBMOVED`, `Must be called from main thread`,
  `RestoreNovaBackup: failed` or `RestoreBackupScreen: failed` in the log;
- `RestoreDbTask: performRestore: starting restore from db` in the new process, then
  `Migration is not needed`, and no `resetLauncherDb` / `loading default workspace`;
- the home screen matches the backup's **database** (dock, pages, widgets, folders); widgets with
  settings (e.g. the analog clock) may ask to be set up again;
- the grid in Home screen > Grid is the backup's.
