# Fugueti Launcher changelog

## 16.0.6 (2026-11-28)

* New installs no longer ask for notification access the first time At a Glance's settings
  open, and At a Glance no longer shows "Tap to set up": Now playing, the only item that needs
  that access, starts off. Turn it on in At a Glance > Now playing. Existing setups keep it as
  it was.
* New installs start with a 4 x 5 home screen and 4 dock icons on phones (Phone, Messages, the
  browser and Camera). Existing setups keep their grid.
* Android's own backup (Google account) now holds the whole setup, like a Fugueti backup file:
  the home screen, Fugueti's settings, custom icons, drawer folders, fonts and saved wallpapers.
  Before, it held only part of the settings and the home screen came back empty on a reinstall
  or a new phone.
* When Android restores that backup on install, Fugueti asks on its first start whether to
  restore it or start fresh.
* Fugueti's settings have their own icon, a gear in the Fugueti colors. Before, opening the
  settings flashed Lawnchair's settings icon on the splash screen.

## 16.0.5 (2026-11-27)

* The Google Play version (`app.fugueti.launcher.play`) explains where to get the Fugueti Feed
  app when the feed is on and it is missing, instead of linking to the download: Fugueti Feed is
  not on Google Play.
* Android's accessibility settings describe Fugueti's service with Fugueti's name; the text still
  said Lawnchair.
* The Google Play version no longer asks Android for photo and video access, which it never used
  (file search and the wallpaper preview are off there), and has no crash log upload: Google Play
  reports crashes itself. Google Play does not accept either without a policy exception.
* Restoring a Lawnchair backup no longer crashes the launcher, and the restored home screen
  stays: the backup's grid (columns, rows, dock icons) comes along with its layout, and the
  launcher no longer deleted the database it had just opened nor replaced the restored layout
  with the default one.
* Restoring a Lawnchair backup keeps At a Glance, the app drawer search bar and the icon sizes as
  they were in Lawnchair: a Lawnchair backup leaves out settings at Lawnchair's defaults, and
  Fugueti's defaults differ (At a Glance was always turned off).
* At a Glance is on by default on new installs, as in Lawnchair. Installs and backups from
  earlier versions that never turned it on keep it off: turning it on frees the first row of the
  first page, so the items there would be removed. Its first cards say "Welcome to Fugueti!" and
  "Set up Fugueti" instead of naming Lawnchair.
* The launcher no longer closes now and then when the Google feed (through Fugueti Feed)
  reconnects: its connection messages were handled on another thread than the launcher's.
* Importing a Nova backup works again: it failed with an error as soon as the backup set the app
  drawer search bar, and its home screen could replace the database the running launcher had
  open. A Nova backup without a home screen search bar turns the dock search bar off.
* New installs start with a 5 x 5 home screen and 5 dock icons on phones. Installs that kept
  the old 7 x 12 default (7 dock icons) keep it.
* **App drawer > Pull to search**, as in Nova: with the drawer search bar off, a swipe down in
  the lower third of the app drawer opens the search bar and the keyboard, so one thumb opens
  the drawer and searches. Back hides the search bar again. A swipe down higher up still closes
  the drawer. Importing a Nova backup brings this setting too.
  ([#2](https://github.com/fugueti/launcher/issues/2))
* **App drawer > Search bar > Position** puts the drawer search bar at the bottom, as in Nova:
  it rests above the navigation bar and rides just above the keyboard while you type. Importing
  a Nova backup brings this setting too.
* **Dock > Search bar > Position** moves the home screen search bar, as in Nova: in the dock below
  the icons (as before), in the dock above the icons, or at the top of the screen on every page,
  where the pages start below it and it hides outside the home screen (app drawer, recents,
  dragging an icon). Importing a Nova backup brings this setting too.
* **Search bar > Size** (with the Google Search bar) sets the size of Google's bar in the dock
  or at the top of the screen. The default, 100%, matches Fugueti's own bar; Google's used to
  fill its whole slot and looked bigger.
* The drawer search bar at the top sits where Nova puts it, just below the status bar, or just
  below the camera when the status bar is hidden; it used to leave an empty band above it.

## 16.0.4 (2026-11-26)

* Fugueti moved to the Fugueti organization on GitHub: the app, its update check and links now
  use github.com/fugueti/launcher, and the Fugueti Feed links use github.com/fugueti/feed. The
  old addresses keep redirecting, so older versions still find updates.
* With the status bar hidden, the apps in the drawer scroll up to the top of the screen, as in
  Nova, instead of stopping below an empty band. The drawer still opens with that space above
  the first row, and the scroll bar stays below it.
* The app drawer shows only the wallpaper behind it, as in Nova: the home screen icons and
  widgets no longer show through and mix with the apps. **App drawer > Show home screen behind**
  brings them back.
* **App drawer > Background opacity** at 0% is fully transparent, and the setting says what
  its ends mean (0% is transparent, 100% is solid). A gray scrim behind the drawer ignored the
  setting, so the wallpaper always looked dimmed.
* The wallpaper stays sharp behind the app drawer, as in Nova; it used to be blurred and
  zoomed. **App drawer > Blur wallpaper** brings the blur back.
* Themed icons no longer turn themselves off when an app is uninstalled or hidden. An old
  Lawnchair migration turned them off at every launcher start unless a themed icon pack was
  installed, although apps' own monochrome icons need none.
* With the status bar hidden, a swipe from the top edge shows it only for a moment; before, it
  could stay on the home screen.
* Widgets no longer disappear from the home screen when Android stops recognizing their id
  (seen after a restore): if the widget's app is still installed, Fugueti gets a new id for it.
  A widget with settings may ask to be set up again.
* Apps in the private space show a single uninstall option, Android's **Uninstall app**. A second
  **Uninstall** failed with "app not installed".
* Importing a Nova backup also brings icon sizes and labels (home screen and app drawer), the app
  drawer's columns, background transparency, search bar, suggested apps and keyboard on open, the
  status bar setting and screen rotation.
* The private space no longer shows up as a work profile (a row of apps, then **Personal** and
  **Work** tabs) in the app drawer after switching to Fugueti from another launcher. Fugueti
  reads the profiles again when it becomes the default home app.

## 16.0.3 (2026-11-22)

* **Google feed:** the feed could stop opening until Fugueti was restarted.
  * The Google app keeps one feed window for all Fugueti apps, since they all reach it through
    Fugueti Feed. After another Fugueti app (such as the nightly) was used, the feed opened in
    that app's hidden window and ignored swipes. Fugueti now attaches its window to the feed
    again whenever it comes to the foreground.
  * Fugueti also takes the feed connection back if it lost it while the connection itself
    stayed up, and logs feed connection changes (tag `FuguetiFeed`).

## 16.0.2 (2026-11-22)

* **Updates:** the release app now updates itself like the nightly. Every day at 10:00 it looks
  for a newer GitHub release and posts a notification on its own **App updates** channel; the
  notification opens *Settings → About*, which lists the changes, downloads the APK, checks its
  SHA-256 and hands it to the system installer. The check also runs when About opens and can be
  turned off in *Settings → General → Updates*. The launcher asks once for the notification
  permission.
* **Hide** in the long-press menu of an app drawer icon hides the app from the drawer at once,
  with **Undo** in the snackbar. Hidden apps are still managed in *Settings → App drawer →
  Hidden apps*.
* App drawer: no empty band above the apps when the drawer search bar is hidden.
* **Support** in *Settings → About* opens a sheet to report a bug or suggest a feature on GitHub
  (issue forms titled `[BUG]` / `[FEATURE]`, with app version, device and Android version filled
  in) or to send an e-mail.
* Release channels: stable versions (Launcher3 from Android 16) come from `main` as tagged
  releases; the **nightly** (`app.fugueti.launcher.nightly`) now follows `17-dev`, Fugueti on
  Launcher3 from Android 17.

## 16.0.1 (2026-11-21)

First release of **Fugueti**, built on Lawnchair 16 (`16-dev`, Launcher3 from Android 16,
compiled against API 37).

### Identity

* App name **Fugueti**, package `app.fugueti.launcher` (nightly and play variants with their
  own ids), so it installs next to the official Lawnchair. Own versioning (16.0.1: the Android base, then minor and patch); Lawnchair's
  version is still shown in its part of the About screen.
* Icon: outline rocket with orange exhaust on a light sky (release) or a night sky (nightly),
  with a monochrome layer for themed icons (winner of the icon poll). Sources and the other icon
  options in `art/icon/`.

### Gestures

* 13 configurable home screen gestures with their own recognition engine
  (`fugueti/src/app/fugueti/gestures/`): swipe up/down/left/right, double tap,
  double tap + swipe up/down, two finger swipe up/down, pinch in/out and rotation.
* Haptics, sensitivity, a "Test gestures" pad and TalkBack actions for configured gestures.
* New actions: open app shortcut, open activity, launch intent.
* Two finger swipes reuse Lawnchair's settings; the paged dock keeps its horizontal swipes;
  native animations stay for the default actions.

### Search bar

* Microphone with the Google provider opens the default assistant (Gemini) or Google voice
  search instead of the voice command the Google app now ignores (also broken upstream).
* "Google" dock search bar mode hosts the regular Google search widget instead of the Pixel-only
  one that rendered empty, and its microphone and Lens/QR buttons work (also broken upstream).

### Backup

* Fugueti backups include the gesture settings (they live in the backed up preferences).
* **Restore fixes:** settings stored in Lawnchair's shared preferences (icon pack, grid, drawer,
  hidden apps…) were undone right after being restored, and DataStore settings and the icon
  override / drawer folder database could be lost too. Restores now apply each part through the
  running app instead of overwriting its open files; backups fold database journals in first.
* Backups also carry custom fonts and saved wallpapers (backup format 2; older backups still
  restore).
* Backup files are named `YYYY-MM-DD_hh-mm.lawnchairbackup`.
* The backup picker accepts any file, so Lawnchair and Fugueti backups that went through other
  apps (and lost their zip type) can be selected; invalid files are still rejected.
* Restoring a **Nova Launcher** backup also imports its home screen gestures (swipes, double tap,
  double tap + swipe, two finger swipes, pinch and rotation): Nova actions such as app drawer,
  screen lock and notifications, app launches and other intents. Nova actions without a Fugueti
  equivalent are skipped.

### Feed

* Google feed (Discover) through **Fugueti Feed**
  ([victorwalter/fugueti-feed](https://github.com/victorwalter/fugueti-feed)), a small companion
  app signed with the same key. The "Feed provider" setting is gone (the Google app refuses
  direct connections from release builds): with the feed on and the bridge missing, the home
  screen settings link to its download.
  The settings screen also shows a red warning with the download link while the feed is on and
  the bridge is missing (translated into every language Lawnchair has).

### About and settings

* About screen: Fugueti section (version, update check, support e-mail, GitHub, crypto donation
  with QR code, team) above Lawnchair's unchanged About.
* Settings show "Fugueti <version>" and the menu says "Restart Fugueti" and theme options say "Managed by Fugueti"; GitHub links, release checks and the nightly updater use
  this repository.

### Other

* Release workflow: tag `v*` runs the gesture tests, builds the signed APK and publishes a
  GitHub release.
* English and Portuguese strings; unit tests for the gesture engine.
* Defaults: 7 x 12 home screen grid and 7 dock icons on phones (the grid sliders now go up to 12
  without "increase max grid size"), icons at 80% on the home screen and in the app drawer, app
  drawer search bar hidden, At a Glance and Lawnchair's online announcements off.
* The app drawer has no drag handle pill at the top.
