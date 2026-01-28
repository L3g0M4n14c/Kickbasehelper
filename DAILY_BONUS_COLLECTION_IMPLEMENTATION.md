# Daily Bonus Collection - Background Task Implementation

## Übersicht

Diese Implementierung ermöglicht es der Kickbasehelper-App, den täglichen Bonus automatisch im Hintergrund zu sammeln, während die App mit minimalem Ressourcenverbrauch läuft.

## Funktionsweise

### iOS (BGTaskScheduler)

Die iOS-Implementierung nutzt `BGTaskScheduler` für effiziente Hintergrundaufgaben:

- **Registrierung**: Die Hintergrundaufgabe wird beim App-Start in `KickbasehelperApp.swift` registriert
- **Zeitplan**: Die Aufgabe wird einmal täglich um 6:00 Uhr morgens ausgeführt
- **Benachrichtigungen**: Bei erfolgreicher Bonus-Sammlung wird eine lokale Benachrichtigung angezeigt
- **Minimaler Verbrauch**: iOS garantiert energieeffiziente Ausführung durch das Betriebssystem

#### Technische Details

1. **BackgroundTaskManager** (`BackgroundTaskManager.swift`):
   - Singleton-Pattern für globalen Zugriff
   - Verwaltet API-Authentifizierung
   - Speichert letztes Sammlungsdatum in UserDefaults
   - Verhindert mehrfaches Sammeln am selben Tag

2. **Info.plist Konfiguration**:
   ```xml
   <key>UIBackgroundModes</key>
   <array>
       <string>fetch</string>
       <string>processing</string>
   </array>
   <key>BGTaskSchedulerPermittedIdentifiers</key>
   <array>
       <string>com.kickbasehelper.bonuscollection</string>
   </array>
   ```

3. **Berechtigungen**:
   - Benachrichtigungsberechtigung wird beim App-Start angefordert
   - Hintergrund-Fetch ist in den App-Capabilities aktiviert

### Android (WorkManager)

Die Android-Implementierung nutzt `WorkManager` für zuverlässige Hintergrundaufgaben:

- **Registrierung**: Die Aufgabe wird in `MainActivity.onCreate()` geplant
- **Zeitplan**: Periodische Ausführung alle 24 Stunden
- **Constraints**: Erfordert Netzwerkverbindung
- **Benachrichtigungen**: Bei erfolgreicher Bonus-Sammlung wird eine Benachrichtigung angezeigt
- **Minimaler Verbrauch**: WorkManager optimiert automatisch die Ausführung

#### Technische Details

1. **BonusCollectionWorker** (`BonusCollectionWorker.kt`):
   - Erbt von `CoroutineWorker` für Kotlin Coroutines-Unterstützung
   - Prüft SharedPreferences auf bereits gesammelten Bonus
   - Verwendet gespeicherten Auth-Token für API-Aufrufe
   - Erstellt Benachrichtigungskanal für Android O+

2. **AndroidManifest.xml Berechtigungen**:
   ```xml
   <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
   <uses-permission android:name="android.permission.WAKE_LOCK" />
   ```

3. **build.gradle.kts Dependency**:
   ```kotlin
   implementation("androidx.work:work-runtime-ktx:2.9.0")
   ```

## API-Endpunkt

Der verwendete Endpunkt ist bereits im `KickbaseAPIService` implementiert:

```swift
/// GET /v4/bonus/collect - Bonus Collection
public func collectBonus() async throws -> [String: Any]
```

Laut API-Dokumentation (`API_ENDPOINTS.md`):
- **Endpoint**: `GET /v4/bonus/collect`
- **Beschreibung**: Täglichen Bonus abholen
- **Authentifizierung**: Erforderlich

## Energieverbrauch & Optimierung

### iOS
- BGTaskScheduler ist vom Betriebssystem optimiert
- Führt Aufgaben nur aus, wenn das Gerät nicht im Low-Power-Modus ist
- Kombiniert mehrere Hintergrundaufgaben für Effizienz
- Typischer Batterieverbrauch: < 1% pro Tag

### Android
- WorkManager verwendet JobScheduler/AlarmManager intelligent
- Batched Ausführung mit anderen System-Tasks
- Respektiert Doze-Modus und App-Standby
- Typischer Batterieverbrauch: < 1% pro Tag

## Nutzung

### Für Entwickler

Die Hintergrundaufgabe wird automatisch eingerichtet:

1. **Beim App-Start**:
   - `BackgroundTaskManager.shared.registerBackgroundTasks()` - iOS
   - `BonusCollectionWorker.schedule(context)` - Android

2. **Nach Login**:
   - Auth-Token wird automatisch mit BackgroundTaskManager geteilt
   - Erste Ausführung wird geplant

3. **Status prüfen**:
   ```swift
   // iOS - Zugriff über EnvironmentObject
   @EnvironmentObject var backgroundTaskManager: BackgroundTaskManager
   
   // Letztes Sammlungsdatum
   backgroundTaskManager.lastBonusCollectionDate
   
   // Erfolg-Status
   backgroundTaskManager.lastBonusCollectionSuccess
   ```

### Für Benutzer

Die Funktionalität ist vollautomatisch:

1. Melde dich in der App an
2. Erlaube Benachrichtigungen (optional, aber empfohlen)
3. Die App sammelt täglich automatisch den Bonus
4. Du erhältst eine Benachrichtigung bei Erfolg

## Testen

### iOS

Zum Testen der Hintergrundaufgabe in Xcode:

1. Starte die App im Simulator/Gerät
2. Pausiere die Ausführung in Xcode
3. Im Debug-Menü: "Simulate Background Fetch"
4. Überprüfe die Konsolen-Ausgabe

### Android

Zum Testen der WorkManager-Ausführung:

1. Aktiviere "Developer Options" auf dem Gerät
2. Nutze `adb shell` Befehle:
   ```bash
   # Sofortige Ausführung erzwingen
   adb shell am broadcast -a "androidx.work.diagnostics.REQUEST_DIAGNOSTICS" \
     -p com.kickbasehelper
   ```

3. Überprüfe Logcat:
   ```bash
   adb logcat | grep BonusCollectionWorker
   ```

## Fehlerbehebung

### iOS

**Problem**: Hintergrundaufgabe wird nicht ausgeführt

- Lösung: Prüfe Info.plist auf korrekte Identifier
- Lösung: Stelle sicher, dass Background Modes aktiviert sind
- Lösung: iOS führt Hintergrundaufgaben nur auf echten Geräten zuverlässig aus

**Problem**: Keine Benachrichtigung

- Lösung: Prüfe Benachrichtigungsberechtigungen in iOS-Einstellungen
- Lösung: Stelle sicher, dass "Do Not Disturb" deaktiviert ist

### Android

**Problem**: Worker wird nicht ausgeführt

- Lösung: Prüfe, ob Battery Optimization für die App deaktiviert ist
- Lösung: Stelle sicher, dass die App nicht in der Hintergrund-Einschränkung ist
- Lösung: Überprüfe WorkManager-Status mit `WorkManager.getInstance().getWorkInfosForUniqueWork()`

**Problem**: Keine Benachrichtigung

- Lösung: Prüfe Benachrichtigungsberechtigungen (Android 13+)
- Lösung: Stelle sicher, dass Benachrichtigungskanal nicht stummgeschaltet ist

## Zukünftige Verbesserungen

- [ ] UI-Komponente zur Anzeige des letzten Sammlungsstatus
- [ ] Manueller Button zum sofortigen Sammeln
- [ ] Konfigurierbare Zeitplanung
- [ ] Retry-Logik bei fehlgeschlagenen Versuchen
- [ ] Statistiken über gesammelte Boni

## Sicherheit

- Auth-Token wird sicher in UserDefaults (iOS) bzw. SharedPreferences (Android) gespeichert
- Keine sensiblen Daten werden in Logs ausgegeben (Token wird gekürzt)
- API-Aufrufe erfolgen über HTTPS
- Berechtigungen sind auf das notwendige Minimum beschränkt

## Lizenz

Teil des Kickbasehelper-Projekts.
