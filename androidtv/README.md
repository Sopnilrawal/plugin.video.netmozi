# NetMozi for the Android TV / NVIDIA Shield home screen

A small Android TV app that brings NetMozi to the Shield's own home screen:

- **"NetMozi Felfedezés" row** on the home screen: *Keresés hanggal* (voice search) and
  *Keresés* (search) tiles, then posters of the newest movies and series. Refreshed every 6 hours.
- **Discover screen** (open the NetMozi app): shortcut tiles and rows of movie/series posters
  for each sort order of the site.
- **Search with voice**: the search screen has a microphone button (uses the Shield's voice input).
  Titles may also appear in the Shield's global search.
- Choosing a poster opens that title in the **NetMozi Kodi add-on**, where you pick a source and play.

## Install

1. Download the APK: GitHub → **Actions** → *Android TV app* → latest run → artifact **NetMoziTV-apk** (unzip it).
2. Install on the Shield: `adb connect <shield-ip>:5555` then `adb install -r app-debug.apk`.
3. Kodi must have the NetMozi add-on installed.
4. In Kodi: **Settings → Services → Control** → turn on **Allow remote control via HTTP**
   (port 8080) and set a username/password.
5. In the NetMozi app: **Kodi beállítás** tile → enter the same port, username and password.

The home screen row appears a few seconds after the first start. If it doesn't, press and hold
on the home screen → **Customize channels** → turn on *NetMozi Felfedezés*.

## Build locally

Open `androidtv/` in Android Studio, or run `gradle assembleDebug` with the Android SDK installed.
