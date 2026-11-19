# SimpleTV

A lightweight IPTV player for **Android TV**, made for my own Smart TV (Realme TV) to get the
customised features that other paid providers don't offer.

It works like a DishTV / Tata Play set-top box: the app opens straight into Live TV, number keys
tune channels, CH+/CH- zaps, and family members never need to touch a menu.

## Features
* Opens directly on the last watched channel
* Number keys, CH+/CH- and an OK channel list with categories
* INFO shows channel name and Now / Next programme (EPG)
* Movies & Series (VOD) with resume
* Xtream Codes login (server URL + username + password)
* 12-hour list cache for instant startup
* Optional auto-start when the TV turns on
* Free: no accounts, no ads

> This app is only a player. It contains no channels; content comes from your own IPTV subscription.

## Build & install
1. Open the project in **Android Studio** → **Build → Build APK(s)**
   (APK: `app/build/outputs/apk/debug/app-debug.apk`)
2. On the TV, enable **Developer options** and **Network debugging**.
3. Install over Wi-Fi:
   ```
   adb connect <TV-IP>:5555
   adb install -r app-debug.apk
   ```
4. Open SimpleTV and log in with your IPTV details.

For auto-start on Android 10+:
```
adb shell appops set com.simpletv SYSTEM_ALERT_WINDOW allow
```

## Tech
Kotlin · Android 6.0+ · Media3 (ExoPlayer) · OkHttp · Coil · Xtream Codes API
