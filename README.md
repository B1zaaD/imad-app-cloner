# Imad Clone

Android app that creates a second isolated space (Android Work Profile, same
technique as Shelter / Island) where you can run a separate copy of installed apps.

## Build the APK
1. Install Android Studio and open this folder (File > Open).
2. Wait for Gradle sync (it downloads Gradle 8.7 + Android SDK 34 on first run).
3. Build > Build Bundle(s)/APK(s) > Build APK(s). Output:
   app/build/outputs/apk/debug/imad-clone-debug.apk
4. Copy it to your phone and install (allow "install unknown apps").

## Use it
1. Open Imad Clone on the phone > "Create Imad Clone" > accept the prompt.
2. A second "Imad Clone" app (briefcase badge) appears: open it.
3. Tap any app in the list to clone it. Optionally enable Google Play there.
4. The cloned app shows on the home screen with a briefcase badge; log into a second account.

## Notes
- Requires Android 8.0+ (API 26). Some phones (a few Chinese ROMs) block work profiles.
- Only one work profile can exist at a time; if you already use Shelter/Island/
  Work apps, remove that one first.

## Build without Android Studio (GitHub)
Push this folder to a new GitHub repo. Open the Actions tab > "Build APK" run >
download the "CloneSpace-apk" artifact (a zip containing imad-clone-debug.apk).
