# 🚀 Android Build - Schnellstart

## ✅ Setup Status
- [x] Skip Dependencies konfiguriert
- [x] Android-Projektstruktur erstellt
- [x] macOS-spezifische Modifiers angepasst
- [x] Gradle-Build-Dateien erstellt

## 📦 Voraussetzungen

### 1. Java Development Kit
```bash
# Prüfen ob Java 17+ installiert ist
java -version

# Falls nicht installiert (macOS):
brew install openjdk@17
```

### 2. Android Studio
- Download: https://developer.android.com/studio
- Installiere Android SDK (API 34)
- Installiere Android Command Line Tools

### 3. Swift Package Dependencies
```bash
cd KickbaseCore
swift package update
```

## 🔨 Build-Prozess

### Schritt 1: Swift Package bauen
```bash
cd /Users/marcocorro/Documents/xCode/Kickbasehelper/KickbaseCore
swift build
```

**Erwartetes Ergebnis:**
- Skip transpiliert Swift → Kotlin
- Generierter Kotlin-Code in `.build/plugins/outputs/`
- ✅ "Build complete!" Nachricht

**Bei Fehlern:**
- Prüfe Skip-Installation: `swift package describe`
- Reset Package: `swift package clean && swift package update`

### Schritt 2: Android Studio öffnen

1. **Android Studio starten**
2. **"Open" auswählen**
3. **Navigiere zu:** `/Users/marcocorro/Documents/xCode/Kickbasehelper/Android`
4. **Warte auf Gradle Sync** (beim ersten Mal dauert es länger)

### Schritt 3: Gradle Sync

Android Studio führt automatisch Gradle Sync durch:

```
✓ Resolving dependencies...
✓ Configuring projects...
✓ Building...
```

**Mögliche Fehler:**
- **"SDK not found"**: Installiere Android SDK über SDK Manager
- **"Java version"**: Stelle sicher Java 17 ist installiert
- **"Gradle version"**: Android Studio aktualisiert automatisch

### Schritt 4: Android Emulator einrichten

1. **Tools > Device Manager**
2. **Create Device**
3. **Wähle ein Phone (z.B. Pixel 7)**
4. **System Image: API 34 (Android 14)** herunterladen und auswählen
5. **Finish**

### Schritt 5: App ausführen

**Option A: Über Android Studio**
1. Wähle Device/Emulator in der Toolbar
2. Klicke ▶️ Run 'app'
3. App wird gebaut und gestartet

**Option B: Kommandozeile**
```bash
cd /Users/marcocorro/Documents/xCode/Kickbasehelper/Android

# Debug Build
./gradlew assembleDebug

# Installieren auf verbundenem Gerät
./gradlew installDebug

# App starten
adb shell am start -n com.kickbasehelper/.MainActivity
```

## 🧪 Ersten Test durchführen

### 1. Basis-Build testen
```bash
cd Android
./gradlew build
```

### 2. Run auf Emulator
- Starte den Emulator
- In Android Studio: Run > Run 'app'
- App sollte starten (zunächst mit leerer MainActivity)

### 3. Transpilierung prüfen
Nach dem Swift Build, prüfe:
```bash
cd KickbaseCore
find .build -name "*.kt" | head -10
```
Sollte transpilierte Kotlin-Dateien zeigen.

## 📱 Erwartetes Verhalten

### Erster Start
- App öffnet mit Material3-Theme
- Leere MainActivity wird angezeigt
- Keine Crashes

### Nach Integration der Views
- Login-Screen wird angezeigt
- Navigation funktioniert
- API-Calls funktionieren (wenn INTERNET Permission aktiv)

## 🐛 Troubleshooting

### Problem: Gradle Sync failed
```bash
cd Android
./gradlew clean
./gradlew --refresh-dependencies
```

### Problem: Skip transpilation failed
```bash
cd KickbaseCore
swift package clean
swift package reset
swift package update
swift build
```

### Problem: Android SDK not found
- Öffne Android Studio > Settings
- Appearance & Behavior > System Settings > Android SDK
- Installiere SDK Platform 34
- Installiere Android SDK Build-Tools

### Problem: App crashed on launch
- Prüfe Logcat in Android Studio
- View > Tool Windows > Logcat
- Filter auf "AndroidRuntime" für Crash-Logs

### Problem: Java Version Fehler
```bash
# Java Version prüfen
java -version

# Sollte zeigen: openjdk version "17.x.x"

# Falls falsch, setze JAVA_HOME:
export JAVA_HOME=$(/usr/libexec/java_home -v 17)
```

## 📊 Build-Zeiten (ungefähr)

- **Erster Build:** 3-5 Minuten
  - Download Dependencies
  - Skip Transpilation
  - Android Gradle Build

- **Inkrementelle Builds:** 10-30 Sekunden
  - Nur geänderte Dateien
  - Schnellere Gradle-Builds

- **Clean Build:** 2-3 Minuten

## ✅ Checkliste vor Release

- [ ] Debug Build erfolgreich auf Emulator
- [ ] Debug Build erfolgreich auf echtem Gerät
- [ ] Alle Features getestet
- [ ] Keine Crashes in Logcat
- [ ] Performance ist akzeptabel
- [ ] Network Calls funktionieren
- [ ] Login/Logout funktioniert
- [ ] Navigation funktioniert
- [ ] Listen scrollen smooth

## 🎯 Nächste Schritte nach erfolgreichem Build

1. **UI-Integration:**
   - Transpilierte ContentView in MainActivity integrieren
   - Navigation testen
   - Theme anpassen

2. **Feature-Testing:**
   - Login-Flow
   - API-Calls
   - Daten-Anzeige

3. **Optimierung:**
   - Performance-Profiling
   - Memory-Leaks checken
   - UI/UX für Android anpassen

4. **Release-Vorbereitung:**
   - App-Icon
   - Signing-Config
   - ProGuard/R8
   - Play Store Listing

## 📚 Hilfreiche Commands

```bash
# Gradle Tasks anzeigen
./gradlew tasks

# Dependencies anzeigen
./gradlew :app:dependencies

# Lint Check
./gradlew lint

# Unit Tests
./gradlew test

# APK-Größe prüfen
ls -lh app/build/outputs/apk/debug/*.apk

# Logcat live view
adb logcat | grep -i kickbase
```

## 🎓 Lernressourcen

- [Skip Docs](https://skip.tools/docs/)
- [Android Developer Guide](https://developer.android.com/guide)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Kotlin Docs](https://kotlinlang.org/docs/home.html)

## 💡 Tipps

1. **Inkrementell vorgehen:** Starte mit einer einfachen View, dann erweitere
2. **Loggen:** Nutze `print()` in Swift, wird zu `println()` in Kotlin
3. **Hot Reload:** Android Studio unterstützt Live Edit für Compose
4. **Debugging:** Nutze Breakpoints sowohl in Xcode als auch Android Studio
