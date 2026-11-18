# NetMozi for the Android TV / NVIDIA Shield

A streaming-store style app for the Shield, with a **Discover** screen modelled on apps like Flick:

- **Left menu**: Home, Search, Hindi, My List, Settings (expands with labels when focused).
- **Home**: a big banner carousel of trending titles with **Play** and **+** (My List), a **Movies / TV** switch,
  a **Genres** grid (with **Hindi Movies / Hindi Shows** first), **Top 10** with big numbers,
  and rows for Trending, Hindi, Popular today and Top rated.
- **Search**: search bar with a **microphone button** (voice search) and a "Trending now" poster grid.
- **Details**: backdrop, info, and **Play**, **Stremio**, **Kodi** and **My List** buttons, plus "More like this".
- **Shield home screen row** "NetMozi Discover": voice search and search tiles plus trending and Hindi posters.

Movie data, posters and genres come from [TMDB](https://www.themoviedb.org/). **Play** opens the title in
Stremio (by IMDb id), or in Kodi through the NetMozi add-on's search — choose in Settings.

## Install

1. Download the APK: GitHub → **Actions** → *Android TV app* → latest run → artifact **NetMoziTV-apk** (unzip it).
2. `adb connect <shield-ip>:5555` then `adb install -r app-debug.apk`.
3. Open NetMozi → **Settings**:
   - **TMDB API key**: free at themoviedb.org → Settings → API. (Or add it once as the GitHub
     repository secret `TMDB_API_KEY`, and every build has it built in.)
   - **Play with**: Stremio or Kodi (NetMozi).
   - **Kodi connection** (only for Kodi): in Kodi turn on Settings → Services → Control →
     *Allow remote control via HTTP*, and enter the same port, username and password here.

If the home screen row doesn't appear: press and hold on the Shield home screen → **Customize channels**
→ turn on *NetMozi Discover*.

## Build locally

Open `androidtv/` in Android Studio, or run `gradle assembleDebug` with the Android SDK installed.
