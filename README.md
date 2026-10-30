# AuraCraft Toolkit - Android WebView App

A modern, production-grade Android WebView application for **AuraCraft Toolkit** (`https://harivinder129-art.github.io/AuraCraft/`).

## 📌 Configured Details:
- **App Name:** `AuraCraft Toolkit` ([strings.xml](app/src/main/res/values/strings.xml))
- **Target URL:** `https://harivinder129-art.github.io/AuraCraft/` ([MainActivity.kt](app/src/main/java/com/example/webviewapp/MainActivity.kt))

---

## 🚀 How to build the APK:

### Option 1: In Android Studio (Local)
1. Open **Android Studio** and click **Open**.
2. Select this folder (`c:\Users\hariv\OneDrive\Desktop\app`).
3. Wait for Gradle sync to finish.
4. Click on top menu: **Build** > **Build Bundle(s) / APK(s)** > **Build APK(s)**.
5. The `.apk` will be generated in:
   `app/build/outputs/apk/debug/app-debug.apk`.

### Option 2: Free Cloud Build (GitHub Actions)
If you don't have Android Studio installed:
1. Push this folder to a GitHub repository (public or private).
2. Go to the **Actions** tab on your GitHub repository.
3. The "Build Android APK" workflow will run automatically.
4. Download the compiled `app-debug.apk` directly from the workflow run!
