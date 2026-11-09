# SimpleTV – Set-top-box style IPTV app for Android TV (Realme TV)

A lightweight IPTV player for **Android TV / Google TV / Fire TV** that behaves like a traditional
DishTV / Tata Play set-top box:

* App opens **directly into Live TV** – last watched channel starts playing. No home screen, no tiles.
* **Number keys** on the remote tune channels (type `1 2 3` → channel 123), **CH+ / CH-** or **UP / DOWN** zap.
* **OK** opens a channel list on the left; **LEFT / RIGHT** switch categories (Sports, News, …).
* **INFO** shows channel number, name and Now / Next programme (EPG).
* **BACK** opens a simple menu → **Movies (VOD)**, **Series**, **Settings**, **Exit**. Family members never need to go there.
* **Xtream Codes login** (server URL + username + password) – exactly the same details you enter in XCIPTV / IPTV Smarters.
* Channel / movie lists are cached on the TV for 12 h, so startup is instant.
* Optional **auto-start when the TV turns on**.
* 100 % free: no accounts, no ads, no paid libraries. You only need your existing IPTV subscription.

> **Note:** This app is only a player (like VLC). It contains no channels. Whether the content you
> stream is legal depends on your IPTV provider.

---

## 1. What you need (all free)

| Item | Why |
|---|---|
| A PC (Windows / Mac / Linux) with ~8 GB free disk | to build the APK |
| **Android Studio** (free) – https://developer.android.com/studio | Android SDK + build tools. **Recommended.** |
| *or* VS Code + Java 17 + Android command-line tools | if you really don't want Android Studio (see §3b) |
| Realme TV on the same Wi-Fi as the PC | to install the APK over the network |
| Your IPTV **server URL, username, password** | from your IPTV provider |

> **About VS Code:** VS Code can *edit* the code, but it cannot build an Android app on its own –
> the Android SDK must be installed either way. The easiest zero-cost route is Android Studio.
> If you prefer VS Code, follow §3b (command-line build) and just open the folder in VS Code for editing.

---

## 2. Project structure

```
SimpleTV/
├── README.md                     ← this file
├── settings.gradle.kts           ← Gradle project settings
├── build.gradle.kts              ← plugin versions (AGP 8.5.2, Kotlin 1.9.24)
├── gradle.properties
├── gradlew / gradlew.bat         ← Gradle wrapper (downloads Gradle 8.7 itself)
└── app/
    ├── build.gradle.kts          ← dependencies: Media3/ExoPlayer, OkHttp, Coil, RecyclerView
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/simpletv/
        │   ├── BootReceiver.kt               ← auto-start on boot
        │   ├── data/
        │   │   ├── Prefs.kt                  ← saved login + settings
        │   │   ├── Models.kt                 ← Channel, Card, Episode, …
        │   │   ├── XtreamApi.kt              ← Xtream Codes API client + JSON parsers
        │   │   └── Repo.kt                   ← loads lists, 12-hour disk cache
        │   └── ui/
        │       ├── SplashActivity.kt         ← routes to Login or Live TV
        │       ├── LoginActivity.kt          ← server / username / password
        │       ├── LiveActivity.kt           ← ★ the set-top-box screen (player, zapping, channel list, EPG)
        │       ├── MenuActivity.kt           ← BACK menu: Live / Movies / Series / Settings / Exit
        │       ├── BrowseActivity.kt         ← Movies & Series: categories + poster grid
        │       ├── SeriesDetailActivity.kt   ← episode list
        │       ├── PlayerActivity.kt         ← VOD player with controls + resume
        │       ├── SettingsActivity.kt
        │       └── Adapters.kt               ← RecyclerView adapters
        └── res/
            ├── layout/                       ← one XML per screen
            ├── drawable/                     ← focus highlights, icons
            ├── drawable-xhdpi/banner.png     ← TV launcher banner
            └── values/                       ← strings, colors, themes
```

---

## 3a. Build with Android Studio (recommended)

1. Install **Android Studio** and open it once so it finishes its first-run setup (it downloads the SDK).
2. **File → Open…** → select the `SimpleTV` folder (the one containing `settings.gradle.kts`).
3. Wait for **Gradle sync** to finish (first time: 3-10 min, it downloads Gradle 8.7 and libraries).
   * If it asks to install missing SDK components (Android 14 / API 34, Build-Tools), click **Install**.
4. **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. When done, click **locate** in the notification. The file is:
   `app/build/outputs/apk/debug/app-debug.apk`

That's the file you install on the TV.

> **If you see an error during sync or build**, copy the red text and send it to me – it is almost
> always a version mismatch that is a one-line fix.

## 3b. Build from the command line (VS Code users)

1. Install **JDK 17** (e.g. https://adoptium.net).
2. Download **Android command-line tools** from https://developer.android.com/studio#command-line-tools-only
   and unzip to e.g. `C:\Android\cmdline-tools\latest` (Windows) or `~/Android/cmdline-tools/latest`.
3. Set environment variables:
   ```
   ANDROID_HOME = C:\Android          (or ~/Android)
   PATH += %ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools
   ```
4. Install SDK packages:
   ```
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   sdkmanager --licenses      (answer y to all)
   ```
5. In the `SimpleTV` folder create `local.properties`:
   ```
   sdk.dir=C\:\\Android          (Windows)   or   sdk.dir=/home/you/Android
   ```
6. Build:
   ```
   ./gradlew assembleDebug        (Windows: gradlew.bat assembleDebug)
   ```
   APK: `app/build/outputs/apk/debug/app-debug.apk`

Open the folder in VS Code with the **Kotlin** extension for editing.

---

## 4. Install on the Realme TV

### Step 1 – enable Developer options on the TV
1. **Settings → Device Preferences → About**.
2. Scroll to **Android TV OS build** (or **Build**) and press **OK 7 times** → "You are now a developer".
3. Go back → **Settings → Device Preferences → Developer options** → turn on **USB debugging**
   and (if present) **Network debugging / ADB over network**.
4. Also allow installs from unknown sources: **Settings → Device Preferences → Security & restrictions
   → Unknown sources** → enable for the app you'll use (see Option B) – not needed for ADB.

### Step 2 – find the TV's IP address
**Settings → Network & Internet → (your Wi-Fi) → IP address**, e.g. `192.168.1.45`.

### Option A – install over Wi-Fi with ADB (cleanest)
`adb` comes with Android Studio (`platform-tools` folder) or from step 3b.

```
adb connect 192.168.1.45:5555
```
The TV shows **"Allow USB debugging?"** → tick *Always allow* → **OK**. Then:

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

"Success" → done. **SimpleTV** now appears in the TV's app list.

> `adb connect` says *connection refused*? Make sure Network debugging is ON, or reboot the TV
> once after enabling developer options.

### Option B – pendrive / file manager
1. Copy `app-debug.apk` to a pendrive and plug it into the TV, **or** install
   **"Send files to TV"** on both your phone and the TV (free on Play Store) and send the APK.
2. On the TV open a file manager (e.g. **FX File Explorer** / **X-plore**, free), find the APK, open it,
   allow "unknown sources" for that file manager when asked, install.

### Updating later
Build a new APK and run `adb install -r …` again – your login and settings are kept.

---

## 5. First start – login

1. Open **SimpleTV** from the TV's app list.
2. Enter:
   * **Server URL** – e.g. `http://myiptv.example:8080` (what your provider calls *host / portal / DNS*).
     You may also paste a full M3U link like `http://host:8080/get.php?username=…&password=…` – the app
     extracts host, username and password from it automatically.
   * **Username** and **Password** – same as in XCIPTV.
3. Press **Connect & Save**. On success the app jumps straight into Live TV.

The login is saved on the TV. From now on the app always starts in Live TV.

---

## 6. Remote control guide (Live TV screen)

| Remote key | Action |
|---|---|
| **0 – 9** | Type a channel number. Tunes after 2 s (or press **OK** immediately). |
| **CH+ / CH-** or **▲ / ▼** | Next / previous channel (inside the currently selected category) |
| **OK** (or ◀ / ▶) | Open channel list |
| in the list: **▲ ▼** | move, **OK** play, **◀ ▶** change category, **BACK** close |
| **INFO** / **GUIDE** | Show channel number, name, Now & Next programme |
| **BACK** or **MENU** | Open the menu: Live TV · Movies · Series · Settings · Exit |

On **Movies / Series** screens: ▲▼◀▶ to move, **OK** to open/play, **BACK** to go back.
In the movie player **OK** shows play / pause / seek controls, **BACK** hides them or exits.
Movies and episodes **resume** where you stopped.

---

## 7. Auto-start when the TV turns on (optional)

The app has **Settings → "Open Live TV automatically when the TV turns on"** (on by default).

On **Android 9** this just works. On **Android 10 and newer** Android blocks apps from opening a
screen in the background unless the app has the *Display over other apps* permission. Grant it once
over ADB:

```
adb shell appops set com.simpletv SYSTEM_ALERT_WINDOW allow
```

If that is still blocked on your firmware, use the TV's own option instead (name varies by model):
**Settings → Device Preferences → Power / Boot source / "Start on last input"**, or simply put
SimpleTV in the first position of the home screen's app row so it is one click away.

---

## 8. Settings screen

| Setting | What it does |
|---|---|
| Open Live TV automatically when the TV turns on | see §7 |
| Use HLS (.m3u8) live streams instead of .ts | Toggle if live channels show a black screen or stutter. The app also tries the other format automatically when one fails. |
| Refresh channel / movie / series lists | Clears the 12-hour cache and re-downloads from the provider (use when your provider adds channels). |
| Show account info | Expiry date, status, connections in use. |
| Change login / Logout | Goes back to the login screen. |

---

## 9. Troubleshooting

| Problem | Fix |
|---|---|
| *Login failed: HTTP 404 / Server did not return JSON* | URL must be the **base host and port** only (`http://host:8080`). Don't add `/c/` or `/player_api.php`. Try `http://` instead of `https://`. |
| *Login failed: Wrong username/password* | Check with provider; passwords are case-sensitive. |
| Login OK but *No channels found* | Your account has no live category, or the provider's API is down. Try **Refresh lists** later. |
| Black screen on a channel but others work | Normal for IPTV – that stream is down on the provider side. Try the HLS toggle. |
| All channels black / "Stream not available" | Provider may allow only 1 connection and another device is using it – check **Show account info**. |
| Channel numbers don't match DishTV numbers | The numbers come from your provider's playlist (`num` field). They can't be changed from the app yet. |
| `adb connect` refuses | Enable Network/USB debugging again, reboot TV, make sure PC and TV are on the same Wi-Fi. |
| *App not installed* when using a file manager | Enable **Unknown sources** for that file manager (§4 step 1.4). |
| Gradle sync error in Android Studio | Copy the error text and send it to me. |

---

## 10. Customising

All values are in one place each:

* **Accent colour** (orange): `app/src/main/res/values/colors.xml` → `accent`.
* **How long the info bar / channel list stays on screen**: `LiveActivity.kt` → `INFO_TIMEOUT_MS`,
  `PANEL_TIMEOUT_MS`, `DIGIT_TIMEOUT_MS` (bottom of the file).
* **Number of poster columns** in Movies/Series: `BrowseActivity.kt` → `COLUMNS`.
* **Cache duration**: `Repo.kt` → `CACHE_TTL_MS`.
* **Text shown on screen**: `res/values/strings.xml` (you can translate everything to Hindi here).
* **App name / icon**: `strings.xml` → `app_name`, `res/drawable/ic_launcher.xml`,
  `res/drawable-xhdpi/banner.png` (320×180).

### Ideas for later
* Favourites list (long-press OK on a channel).
* Full EPG grid.
* Parental lock on the menu (PIN before Movies / Series).
* Hide / reorder categories so only family-relevant ones zap with CH+/CH-.

---

## 11. Tech summary

* **Language:** Kotlin · **Min Android:** 6.0 (API 23) · **Target:** API 34
* **Player:** AndroidX Media3 (ExoPlayer) 1.4.1 – MPEG-TS, HLS, MP4, MKV
* **Network:** OkHttp 4 · **Images:** Coil 2 · **JSON:** org.json (built into Android)
* **Protocol:** Xtream Codes `player_api.php` (`get_live_streams`, `get_live_categories`,
  `get_vod_streams`, `get_series`, `get_series_info`, `get_short_epg`)
* Stream URL formats: `/live/USER/PASS/ID.ts|m3u8`, `/movie/USER/PASS/ID.ext`, `/series/USER/PASS/ID.ext`
