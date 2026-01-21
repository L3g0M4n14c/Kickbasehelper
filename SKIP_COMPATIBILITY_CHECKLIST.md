# Skip Compatibility Anpassungen

## ⚠️ Notwendige Code-Anpassungen für Android

### 1. View Modifiers entfernen/anpassen

#### ContentView.swift
Die macOS-spezifischen Modifiers müssen entfernt oder mit Conditional Compilation gesichert werden:

```swift
// VORHER:
.macOSScaled()
.macOSOptimized()

// NACHHER (Option 1 - entfernen):
// Einfach weglassen

// NACHHER (Option 2 - conditional):
#if !SKIP
.macOSScaled()
.macOSOptimized()
#endif
```

### 2. UserDefaults → SharedPreferences

#### AuthenticationManager.swift
UserDefaults funktioniert mit Skip, aber für bessere Android-Integration:

```swift
// AKTUELL (funktioniert):
private func storeToken(_ token: String) {
    UserDefaults.standard.set(token, forKey: "kickbase_token")
}

// FÜR SICHERE SPEICHERUNG (optional):
#if SKIP
// Android EncryptedSharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private func storeToken(_ token: String) {
    // Implementierung mit EncryptedSharedPreferences
}
#else
private func storeToken(_ token: String) {
    UserDefaults.standard.set(token, forKey: "kickbase_token")
}
#endif
```

### 3. Networking - URLSession

URLSession sollte mit Skip funktionieren, aber prüfe:

```swift
// Diese Patterns sind kompatibel:
let (data, response) = try await URLSession.shared.data(for: request)

// Falls Probleme auftreten, nutze plattformspezifischen Code:
#if SKIP
// Android OkHttp Implementation
import okhttp3.*
#else
// iOS URLSession
#endif
```

### 4. SwiftData → Room Database

SwiftData wird nicht von Skip unterstützt. Alternativen:

```swift
// OPTION 1: In-Memory Storage für MVP
@Published var cachedData: [Player] = []

// OPTION 2: Plattformspezifisches Persistence
#if SKIP
// Android Room Database
@Database(entities = [Player::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun playerDao(): PlayerDao
}
#else
// iOS SwiftData
import SwiftData
@Model class Player { ... }
#endif
```

### 5. Environment & StateObject

Diese funktionieren mit Skip:
- `@State`, `@Binding`, `@StateObject`, `@ObservedObject` ✅
- `@EnvironmentObject` ✅
- `@Published` ✅

ABER:
- `@AppStorage` ⚠️ (funktioniert, aber limitiert)
- `@FetchRequest` ❌ (SwiftData)
- `@SceneStorage` ❌

### 6. GeometryReader & Layout

GeometryReader funktioniert mit Skip, aber:

```swift
// VERMEIDEN (kann Probleme machen):
GeometryReader { geometry in
    // Komplexe Berechnungen
}

// BESSER:
VStack(spacing: 20) {
    // Nutze feste Layouts oder Spacer()
}
```

### 7. Navigation

```swift
// SKIP UNTERSTÜTZT:
NavigationStack { ... }
NavigationLink { ... }
.navigationDestination()

// NICHT UNTERSTÜTZT:
NavigationView // Veraltet, nutze NavigationStack
```

### 8. Async/Await & Task

```swift
// FUNKTIONIERT:
Task {
    await loadData()
}

// FUNKTIONIERT:
async/await mit URLSession

// ACHTUNG:
// Ensure proper error handling auf beiden Plattformen
```

## 🔧 Konkrete TODOs für dein Projekt

### Sofort erforderlich:

1. **ContentView.swift** - Zeile 35-36 anpassen:
```swift
// Entferne:
.macOSScaled()
.macOSOptimized()
```

2. **AuthenticationManager.swift** - Token-Storage ist OK
   - UserDefaults funktioniert mit Skip
   - Optional: Später auf EncryptedSharedPreferences upgraden

3. **SwiftData Migration** (wenn genutzt):
   - Prüfe ob SwiftData in deinem Projekt verwendet wird
   - Falls ja: Migration auf alternative Persistence-Lösung

### Optional (später):

4. **Bessere Android-Integration:**
   - Material3 Theming anpassen
   - Android-spezifische UI-Patterns
   - Deep Linking & Notifications

5. **Performance-Optimierungen:**
   - Lazy Loading für Listen
   - Image Caching
   - Background Tasks

## 📋 Checkliste vor erstem Build

- [ ] `.macOSScaled()` und `.macOSOptimized()` entfernen
- [ ] SwiftData-Abhängigkeiten prüfen
- [ ] Alle Custom View-Extensions auf Skip-Kompatibilität prüfen
- [ ] Third-Party Dependencies prüfen (nur Skip-kompatible nutzen)
- [ ] Networking-Code testen

## 🧪 Testing-Strategie

1. **Phase 1: Swift Build**
```bash
cd KickbaseCore
swift build
# Prüfe auf Transpilations-Fehler
```

2. **Phase 2: Basic UI**
```bash
# Erstelle eine minimale Test-View
# Teste einfache Navigation und State-Management
```

3. **Phase 3: Networking**
```bash
# Teste API-Calls
# Prüfe JSON-Parsing
```

4. **Phase 4: Full Integration**
```bash
# Integration aller Features
# Performance-Tests
```

## 🆘 Häufige Probleme & Lösungen

### Problem: "Unknown modifier macOSScaled"
**Lösung:** Conditional Compilation oder entfernen

### Problem: "SwiftData not supported"
**Lösung:** In-Memory Storage oder plattformspezifische DB

### Problem: "URLSession Error"
**Lösung:** Prüfe Android Network Permissions (bereits konfiguriert)

### Problem: "View not rendering correctly"
**Lösung:** 
- Vereinfache Layout
- Nutze Standard SwiftUI Components
- Vermeide komplexe GeometryReader

## 📚 Weitere Ressourcen

- [Skip SwiftUI Support](https://skip.tools/docs/swiftui/)
- [Skip Foundation Support](https://skip.tools/docs/foundation/)
- [Platform-specific Code](https://skip.tools/docs/platform-differences/)
