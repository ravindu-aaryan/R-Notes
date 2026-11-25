# Screen Notes

A personal Android app (Galaxy S24, Android 14+) that saves text as notes without using the clipboard or screenshots.

## Features

- **Floating button** (turned on from the app's Settings). Tap it, then **Save screen as note** to save the visible text of the app you're using.
- **Save selected text.** Select text in most apps and choose **Save to Screen Notes** in the selection menu.
- **Edge Panel.** Add the app to your Edge panel from Settings > Display > Edge panels.
- **Notes.** Search, pin, edit and delete. Notes are stored on the phone only.
- **Export.** Share all notes as a JSON file.

## Build

1. Open the `ScreenNotes` folder in Android Studio (Ladybug or newer). Let it create the Gradle wrapper and sync.
2. Run the `app` configuration on the S24, or use Build > Build APK(s).
3. Install with `adb install app/build/outputs/apk/debug/app-debug.apk` if you're sideloading.

## First-run setup

1. Open the app and allow notifications.
2. Settings > **Screen text access**: open Accessibility settings and turn on **Screen Notes**. On Android 13+, you may first need to allow restricted settings from the app's info page.
3. Settings > **Floating button**: turn on the switch and allow "Display over other apps".

## Limits

- Text from images, some games and canvas-drawn UIs isn't readable through Accessibility.
- Text that is scrolled off-screen isn't captured unless you scroll it into view first.
- Selection saving works only where the app shows the standard selection menu. Some browsers and WebViews don't.
- Password fields are always skipped.

## Before publishing

Google Play restricts Accessibility use to accessibility purposes. Keep this app sideloaded, or review the policy before submitting it.
