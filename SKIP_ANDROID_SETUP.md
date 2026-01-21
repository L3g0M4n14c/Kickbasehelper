# Skip Android Integration Guide

## ✅ Setup abgeschlossen

Die grundlegende Android-Struktur wurde erfolgreich eingerichtet!

## 📁 Projektstruktur

```
Kickbasehelper/
├── KickbaseCore/           # Swift Package mit Skip
│   ├── Package.swift       # Skip Dependencies konfiguriert
│   ├── build.gradle.kts    # Android Library Build
│   ├── gradle.properties
│   └── Sources/
│       └── KickbaseCore/   # Swift Code (wird nach Kotlin transpiliert)
│
└── Android/                # Android App
    ├── app/
    │   ├── build.gradle.kts
    │   └── src/main/
    │       ├── AndroidManifest.xml
    │       ├── java/com/kickbasehelper/
    │       │   └── MainActivity.kt
    │       └── res/
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── gradle.properties
```

## 🚀 Nächste Schritte

### 1. Skip Dependencies installieren
```bash
cd KickbaseCore
swift package update
```

### 2. Swift Code transpilieren
Skip transpiliert automatisch beim Build. Teste das mit:
```bash
swift build
```

### 3. Android Studio öffnen
```bash
cd Android
# Öffne den Android-Ordner in Android Studio
```

### 4. Android Build testen
In Android Studio:
- Sync Gradle Files
- Build > Make Project
- Run > Run 'app'

## 🔍 Wichtige Hinweise

### Skip-Kompatibilität prüfen

Nicht alle SwiftUI-Features werden von Skip unterstützt. Prüfe:

1. **Unterstützte SwiftUI Views:**
   - Text, VStack, HStack, ZStack
   - List, ScrollView, LazyVStack
   - Button, TextField, Toggle
   - NavigationStack, NavigationLink
   - Sheet, Alert

2. **Eingeschränkte Features:**
   - Komplexe Gestures
   - Einige PropertyWrappers (@AppStorage, etc.)
   - Platform-spezifische APIs

3. **Anpassungen nötig:**
   - Keychain-Zugriff (iOS) → EncryptedSharedPreferences (Android)
   - UserDefaults → SharedPreferences
   - URLSession ist meist kompatibel

### Code-Anpassungen

Für plattformspezifischen Code:
```swift
#if SKIP
// Android-spezifischer Kotlin Code
#else
// iOS-spezifischer Swift Code
#endif
```

## 📱 Testing

### iOS Testing
```bash
cd Kickbasehelper
xcodebuild -scheme Kickbasehelper -destination 'platform=iOS Simulator,name=iPhone 15'
```

### Android Testing
```bash
cd Android
./gradlew assembleDebug
# oder in Android Studio: Run 'app'
```

## 🐛 Troubleshooting

### Problem: "Skip plugin not found"
```bash
swift package reset
swift package update
```

### Problem: Gradle Sync Fehler
- Prüfe Java Version (min. Java 17)
- Update Android Studio
- Invalidate Caches & Restart

### Problem: Transpilation Fehler
- Prüfe Skip-Dokumentation für unterstützte APIs
- Nutze `#if SKIP` für plattformspezifischen Code
- Vereinfache komplexe SwiftUI-Konstrukte

## 📚 Ressourcen

- [Skip Documentation](https://skip.tools/docs/)
- [Skip GitHub](https://github.com/skiptools/skip)
- [Supported SwiftUI](https://skip.tools/docs/swiftui/)
- [Supported Foundation](https://skip.tools/docs/foundation/)

## 🎯 Android-spezifische Konfiguration

### MainActivity
Die MainActivity lädt die transpilierte SwiftUI-View. Nach erfolgreicher Transpilation:

```kotlin
// MainActivity.kt
setContent {
    MaterialTheme {
        ContentView()  // Deine SwiftUI ContentView, transpiliert
    }
}
```

### Permissions
Android-Permissions sind bereits in AndroidManifest.xml konfiguriert:
- INTERNET
- ACCESS_NETWORK_STATE

Weitere Permissions nach Bedarf hinzufügen.

### Theme & Styling
Das Material3-Theme ist vorkonfiguriert. Anpassungen in:
- `Android/app/src/main/res/values/themes.xml`
- `Android/app/src/main/res/values/colors.xml` (erstellen bei Bedarf)

## ⚙️ Build-Varianten

### Debug Build
```bash
cd Android
./gradlew assembleDebug
```

### Release Build (signiert)
1. Keystore erstellen
2. `signing.properties` konfigurieren
3. `./gradlew assembleRelease`

## 📊 Performance

Skip-transpilierter Code ist in der Regel performant, aber beachte:
- Erste Transpilation dauert länger
- Inkrementelle Builds sind schnell
- Native Compose Performance
