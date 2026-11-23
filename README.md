# Online Now

A minimal, dot-matrix Android launcher. Black screen, big dotted clock, real app icons redrawn as dots, and a fast app drawer with search.

Built for the **POCO C71** (Android 15), but it works on any Android 15+ phone.

<!-- Add screenshots here: Home | App drawer | Hidden apps | Settings -->

## Features

**Home**
- Big dot-matrix clock (follows your 12 / 24 hour setting). Tap it to open alarms.
- Status pills: connection (Wi-Fi / mobile / offline) and battery.
- Calendar card with the week around today. Tap it to open your calendar.
- Music card: shows what is playing, with play / pause. Tap the title to open the player.
- "Tap to play" card: ask the dots a yes / no question.
- Up to 8 apps in a 4-column grid, with notification dots.

**Icons**
- Every app icon is redrawn as a dot-matrix picture (works with any app, uses themed icons when the app has one).
- Prefer the real icons? Settings > Icons > Original.

**App drawer** (swipe up anywhere)
- Search as you type. Enter opens the best match.
- Pinned apps on top, everything else A-Z, with a fast A-Z rail on the right edge.
- Grid or list layout.
- **Hidden apps**: tap *Hidden* (or swipe left) to open the hidden page, *Back* (or swipe right) to return. Hidden apps stay out of the drawer and search.
- Swipe down at the top to close.

**App menu** (long-press any app)
Add to / remove from home, move earlier / later, pin in drawer, hide, app info, uninstall.

**Settings** (long-press the clock or an empty spot on the home screen)
Theme (system / light / dark), background (black / dim wallpaper / wallpaper), accent color, icon style, label size, which home cards to show, drawer layout, notification access, wallpaper, default launcher.

## Install

Every push builds a signed APK with GitHub Actions. No Android Studio needed.

1. Push this project to GitHub (the *Build and Release* workflow starts by itself).
2. When it finishes, open the **Releases** page and download `OnlineNow.apk` (or grab it from the workflow run under *Artifacts*).
3. Install it. New builds install over old ones because every build uses the same signing key.
4. Press Home and choose **Online Now** as your home app.

### Notification dots and the music card
Both need notification access: Settings > Access > *Allow*.
If the switch is greyed out: Settings > Apps > Online Now > three dots > **Allow restricted settings**, then try again.

## Gestures

| Where | Gesture | Does |
|---|---|---|
| Home | swipe up | open the app drawer |
| Home | long-press empty space / the clock | open settings |
| Drawer | swipe left / right | hidden apps / back |
| Drawer | swipe down at the top | close |
| Any app | long-press | app menu |

## Built with
- Kotlin and Jetpack Compose, no other UI libraries
- [Space Mono](https://github.com/googlefonts/spacemono) (SIL Open Font License, see `licenses/`)

## Author
Made by [Arlo](https://github.com/arlo-dev-io)
