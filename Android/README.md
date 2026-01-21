# Kickbasehelper Android

Android-Version der Kickbasehelper App, erstellt mit [Skip](https://skip.tools).

## 🏗️ Architektur

```
Android/
├── app/                    # Android Application Module
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/kickbasehelper/
│   │   │   └── MainActivity.kt
│   │   └── res/
│   └── build.gradle.kts
│
├── build.gradle.kts        # Root Build Script
├── settings.gradle.kts     # Gradle Settings
└── gradle.properties       # Gradle Properties

KickbaseCore/              # Shared Swift/Kotlin Code
└── Sources/               # Swift Code (transpiliert zu Kotlin)
```

## 🚀 Quick Start

### Voraussetzungen
- Java 17+
- Android Studio Hedgehog (2023.1.1) oder neuer
- Android SDK API 34
- Gradle 8.2+

### Build & Run

1. **Öffne in Android Studio:**
   ```
   File > Open > [wähle diesen Android-Ordner]
   ```

2. **Gradle Sync:**
   - Wird automatisch gestartet
   - Kann beim ersten Mal 2-3 Minuten dauern

3. **Emulator einrichten:**
   - Tools > Device Manager > Create Device
   - Wähle Pixel 7, API 34

4. **App starten:**
   - Klicke ▶️ Run 'app'

## 📦 Dependencies

### Android Libraries
- **Jetpack Compose** - Modern Android UI
- **Material3** - Material Design Components
- **Navigation Compose** - Navigation
- **Lifecycle & ViewModel** - Architecture Components

### Skip Libraries
- **SkipUI** - SwiftUI → Compose Transpilation
- **SkipFoundation** - Foundation → Kotlin Standard Library

### Networking
- **OkHttp** - HTTP Client
- **Kotlinx Serialization** - JSON Parsing

## 🔧 Build-Varianten

### Debug
```bash
./gradlew assembleDebug
```
Output: `app/build/outputs/apk/debug/app-debug.apk`

### Release (Signed)
```bash
./gradlew assembleRelease
```
Benötigt Signing-Konfiguration in `app/build.gradle.kts`

## 🧪 Testing

### Unit Tests
```bash
./gradlew test
```

### Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

### Lint
```bash
./gradlew lint
```

## 📱 Permissions

Die App benötigt folgende Permissions:
- `INTERNET` - Für API-Calls zu Kickbase
- `ACCESS_NETWORK_STATE` - Network-Status prüfen

Konfiguriert in [AndroidManifest.xml](app/src/main/AndroidManifest.xml)

## 🎨 Theming

Das App-Theme nutzt Material3:
- Light & Dark Theme Support
- Dynamische Farben (Android 12+)
- Anpassbar in `res/values/themes.xml`

## 🔐 Signing (für Release)

1. Erstelle Keystore:
```bash
keytool -genkey -v -keystore kickbasehelper.keystore \
  -alias kickbasehelper -keyalg RSA -keysize 2048 -validity 10000
```

2. Erstelle `signing.properties`:
```properties
KEYSTORE_FILE=path/to/kickbasehelper.keystore
KEYSTORE_PASSWORD=your_password
KEY_ALIAS=kickbasehelper
KEY_PASSWORD=your_key_password
```

3. Update `app/build.gradle.kts` mit Signing Config

## 📊 Performance

### APK-Größe
- Debug: ~15-20 MB
- Release (mit ProGuard/R8): ~8-12 MB

### Startup-Zeit
- Cold Start: ~1-2 Sekunden
- Warm Start: ~0.5 Sekunden

## 🐛 Debugging

### Logcat
```bash
adb logcat | grep Kickbase
```

### Android Studio Profiler
- View > Tool Windows > Profiler
- CPU, Memory, Network Profiling

### Debug Build auf Device
```bash
./gradlew installDebug
adb shell am start -n com.kickbasehelper/.MainActivity
```

## 📚 Wichtige Dateien

- **MainActivity.kt** - Entry Point der App
- **AndroidManifest.xml** - App-Konfiguration, Permissions
- **build.gradle.kts** - Build-Konfiguration, Dependencies
- **proguard-rules.pro** - Code-Obfuscation für Release

## 🔄 Skip Transpilation Workflow

1. Swift Code in `KickbaseCore/Sources/` schreiben
2. Swift Build ausführen: `cd ../KickbaseCore && swift build`
3. Skip transpiliert automatisch zu Kotlin
4. Kotlin Code wird in Android Build integriert
5. Android App nutzt transpilierten Code

## 🌐 API Configuration

API-Basis-URL und Endpoints sind in `KickbaseCore` definiert.

Für Development-Modus:
```kotlin
// In local.properties (not in Git)
api.baseUrl=https://api.kickbase.com
api.debug=true
```

## 🎯 Release Checklist

- [ ] Version Code & Name in `build.gradle.kts` erhöhen
- [ ] ProGuard/R8 aktiviert und getestet
- [ ] All Features auf echtem Gerät getestet
- [ ] Keine Debug-Logs in Release-Build
- [ ] App-Icon in allen Größen vorhanden
- [ ] Screenshots für Play Store vorbereitet
- [ ] Signing Config korrekt
- [ ] APK/AAB gebaut und getestet

## 📖 Weitere Dokumentation

- [Android Quickstart](../ANDROID_QUICKSTART.md)
- [Skip Setup Guide](../SKIP_ANDROID_SETUP.md)
- [Compatibility Checklist](../SKIP_COMPATIBILITY_CHECKLIST.md)

## 💡 Troubleshooting

### "SDK not found"
→ Installiere Android SDK über Android Studio SDK Manager

### "Java version mismatch"
→ Stelle sicher Java 17 ist installiert: `brew install openjdk@17`

### "Gradle sync failed"
→ `./gradlew clean && ./gradlew --refresh-dependencies`

### "App crashes on launch"
→ Prüfe Logcat für Stack Traces: `adb logcat | grep AndroidRuntime`

## 🤝 Contributing

Bei Änderungen am Swift-Code:
1. Ändere Code in `KickbaseCore/`
2. Teste iOS-Build in Xcode
3. Teste Android-Build hier
4. Stelle sicher beide Plattformen funktionieren

## 📄 License

[Deine Lizenz hier]
