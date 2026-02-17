# Beitragsrichtlinien für Kickbasehelper

Vielen Dank für dein Interesse, zu Kickbasehelper beizutragen! Diese Richtlinien helfen dir, schnell produktiv zu werden.

## 📋 Inhaltsverzeichnis

- [Schnellstart für Entwickler](#schnellstart-für-entwickler)
- [Entwicklungsumgebung einrichten](#entwicklungsumgebung-einrichten)
- [Projekt-Struktur verstehen](#projekt-struktur-verstehen)
- [Wie man beiträgt](#wie-man-beiträgt)
- [Code-Standards](#code-standards)
- [Testing](#testing)
- [Pull Request Prozess](#pull-request-prozess)
- [Hilfe bekommen](#hilfe-bekommen)

---

## 🚀 Schnellstart für Entwickler

### Voraussetzungen

- **macOS 12.0+** (für iOS/macOS Entwicklung)
- **Xcode 14.0+**
- **Swift 5.9+**
- Git
- Optional: Android Studio (für Android-Builds via Skip)

### Repository klonen und starten

```bash
# Repository klonen
git clone https://github.com/L3g0M4n14c/Kickbasehelper.git
cd Kickbasehelper

# Xcode öffnen
open Kickbasehelper.xcodeproj

# In Xcode: Wähle Target "Kickbasehelper" und drücke Cmd+R zum Starten
```

### Erste Schritte

1. **Architektur verstehen**: Lies [`ARCHITECTURE.md`](./ARCHITECTURE.md) für einen detaillierten Überblick
2. **Code erkunden**: Starte mit `KickbaseCore/Sources/KickbaseCore/`
3. **Tests ausführen**: Cmd+U in Xcode oder `swift test` im Terminal
4. **Lokales Feature entwickeln**: Erstelle einen Feature-Branch und leg los!

---

## 🛠️ Entwicklungsumgebung einrichten

### Xcode-Konfiguration

1. **Öffne das Projekt**:
   ```bash
   open Kickbasehelper.xcodeproj
   ```

2. **Dependencies prüfen**:
   - Swift Package Manager löst automatisch alle Dependencies auf
   - Hauptabhängigkeiten: SkipUI, SkipFoundation, ViewInspector

3. **Build Settings**:
   - Target: iOS 15.0+ / macOS 12.0+
   - Swift Language Version: 5.9

### Tests ausführen

```bash
# Alle Tests
swift test

# Spezifische Test-Suite
swift test --filter KickbaseAPIServiceTests

# In Xcode
Cmd+U (alle Tests)
Cmd+Opt+U (Test-Navigator öffnen)
```

### Build-Befehle

```bash
# iOS Build
xcodebuild -scheme Kickbasehelper -destination 'platform=iOS Simulator,name=iPhone 15'

# macOS Build
xcodebuild -scheme Kickbasehelper -destination 'platform=macOS'

# Android Build (requires Skip)
./build_android.sh
```

---

## 📁 Projekt-Struktur verstehen

### Wichtigste Ordner

```
KickbaseCore/Sources/KickbaseCore/
├── Services/           ← Business Logic, API-Integration
├── Views/              ← SwiftUI UI-Komponenten
├── ViewModels/         ← Dedizierte ViewModels
├── Models/             ← Datenmodelle (Codable Structs)
├── Utilities/          ← Helper-Funktionen
└── Managers/           ← Orchestration (KickbaseManager)
```

### Wo füge ich was hinzu?

| Was?                  | Wo?                                      |
|-----------------------|------------------------------------------|
| **Neuer API Endpoint**| `Services/KickbaseAPIService.swift`      |
| **Neue UI-Ansicht**   | `Views/NeueView.swift`                   |
| **Datenmodell**       | `Models.swift` oder neue Model-Datei     |
| **Business Logic**    | Neuer Service in `Services/`             |
| **Helper-Funktion**   | `Utilities/`                             |
| **Tests**             | `Tests/KickbaseCoreTests/`               |

**Details siehe**: [`ARCHITECTURE.md`](./ARCHITECTURE.md)

---

## 🤝 Wie man beiträgt

### 1. Issue finden oder erstellen

- Schau dir die [offenen Issues](https://github.com/L3g0M4n14c/Kickbasehelper/issues) an
- Oder erstelle ein neues Issue für dein Feature/Bug

### 2. Branch erstellen

```bash
# Feature Branch
git checkout -b feature/mein-neues-feature

# Bugfix Branch
git checkout -b fix/bugfix-beschreibung
```

### 3. Code ändern

Folge den [Code-Standards](#code-standards) (siehe unten)

### 4. Tests schreiben

Jede neue Funktionalität braucht Tests:
- Unit Tests für Services
- Integration Tests für Manager
- Optional: UI Tests für Views

### 5. Commit

```bash
git add .
git commit -m "feat: Füge neue Transfer-Empfehlungslogik hinzu"
```

**Commit Message Format**:
- `feat:` für neue Features
- `fix:` für Bugfixes
- `docs:` für Dokumentation
- `test:` für Tests
- `refactor:` für Refactoring
- `style:` für Code-Style Änderungen

### 6. Push und Pull Request

```bash
git push origin feature/mein-neues-feature
```

Dann erstelle einen Pull Request auf GitHub.

---

## 📏 Code-Standards

### Architektur-Pattern

**MVVM (Model-View-ViewModel)**:
- **Models**: Datenstrukturen (Codable Structs)
- **Views**: SwiftUI Views (nur UI-Logik)
- **ViewModels**: Business Logic (meist `KickbaseManager`)
- **Services**: API-Calls, Datenverarbeitung

### Naming Conventions

```swift
// Klassen/Structs: PascalCase
class KickbaseManager { }
struct TransferRecommendation { }

// Properties/Methoden: camelCase
var teamPlayers: [TeamPlayer]
func loadMarketPlayers() async { }

// Constants: camelCase
let apiBaseURL = "https://api.kickbase.com"

// Enums: PascalCase, Cases: camelCase
enum RiskLevel {
    case low
    case medium
    case high
}
```

### Swift-Stil

#### ✅ Bevorzugt

```swift
// Async/Await
func loadData() async throws -> Data {
    try await apiService.fetch()
}

// Optional Binding
if let user = currentUser {
    print(user.name)
}

// Guard für Early Exit
guard let leagueId = currentLeague?.id else { return }

// Computed Properties
var displayName: String {
    "\(firstName) \(lastName)"
}
```

#### ❌ Vermeiden

```swift
// Force Unwrap (!)
let name = user!.name  // ❌ Crash-Gefahr

// Completion Handler (deprecated)
func loadData(completion: @escaping (Data) -> Void) { }

// Nested Callbacks (Pyramid of Doom)
loadA { a in
    loadB { b in
        loadC { c in
            // ...
        }
    }
}
```

### SwiftUI Best Practices

```swift
// ✅ View-Extraktion für Übersichtlichkeit
struct MainView: View {
    var body: some View {
        VStack {
            HeaderView()
            ContentView()
            FooterView()
        }
    }
}

// ✅ EnvironmentObject für shared State
@EnvironmentObject var kickbaseManager: KickbaseManager

// ✅ @State für lokalen View State
@State private var isShowingSheet = false

// ✅ Loading/Error States
if isLoading {
    ProgressView()
} else if let error = errorMessage {
    ErrorView(error: error)
} else {
    ContentView(data: data)
}
```

### Dokumentation

Komplexe Methoden **MÜSSEN** dokumentiert werden:

```swift
/// Berechnet den Empfehlungs-Score für einen Spieler.
///
/// Der Score basiert auf mehreren Faktoren:
/// - Durchschnittspunkte (40%)
/// - Trend/Form (30%)
/// - Preis-Leistungs-Verhältnis (20%)
/// - Positions-Bedarf (10%)
///
/// - Parameters:
///   - player: Der zu bewertende Spieler
///   - teamContext: Kontext des aktuellen Teams
/// - Returns: Score zwischen 0 und 100
/// - Throws: `APIError` bei Datenfehlern
func calculateRecommendationScore(
    for player: MarketPlayer,
    teamContext: TeamContext
) async throws -> Double {
    // Implementation
}
```

---

## 🧪 Testing

### Test-Strategie

Wir nutzen **3 Test-Ebenen**:

1. **Unit Tests**: Isolierte Service-Tests mit Mocks
2. **Integration Tests**: Multi-Service Koordination
3. **UI Tests**: SwiftUI View Tests (optional)

### Unit Test Beispiel

```swift
import XCTest
@testable import KickbaseCore

class NewServiceTests: XCTestCase {
    var sut: NewService!
    var mockAPIService: MockKickbaseAPIService!
    
    override func setUp() {
        super.setUp()
        mockAPIService = MockKickbaseAPIService()
        sut = NewService(apiService: mockAPIService)
    }
    
    override func tearDown() {
        sut = nil
        mockAPIService = nil
        super.tearDown()
    }
    
    func testLoadData_Success() async throws {
        // Given
        let expectedData = createMockData()
        mockAPIService.mockResponse = expectedData
        
        // When
        let result = try await sut.loadData()
        
        // Then
        XCTAssertEqual(result.count, 5)
        XCTAssertTrue(mockAPIService.fetchWasCalled)
    }
}
```

### Test Coverage Ziele

- Services: **80%+**
- Managers: **70%+**
- Views: **50%+** (kritische Flows)

### Tests ausführen

```bash
# Alle Tests
swift test

# Mit Coverage
swift test --enable-code-coverage

# Spezifische Test-Klasse
swift test --filter KickbaseAPIServiceTests
```

---

## 🔄 Pull Request Prozess

### Vor dem PR

1. **Tests ausführen**: Alle Tests müssen grün sein
2. **Code formatieren**: Nutze Xcode Auto-Format (Ctrl+I)
3. **Dokumentation aktualisieren**: `ARCHITECTURE.md` bei Bedarf
4. **Commit Messages prüfen**: Folge dem Format (`feat:`, `fix:`, etc.)

### PR erstellen

1. **Titel**: Klare Beschreibung (z.B. "feat: Add player comparison view")
2. **Beschreibung**: 
   - Was wurde geändert?
   - Warum wurde es geändert?
   - Wie wurde es getestet?
3. **Screenshots**: Bei UI-Änderungen
4. **Related Issues**: Verlinke relevante Issues

### PR Template

```markdown
## Änderungen
- Füge XYZ Feature hinzu
- Behebe Bug in ABC

## Motivation
Warum war diese Änderung notwendig?

## Tests
- [ ] Unit Tests geschrieben
- [ ] Integration Tests geschrieben
- [ ] Manuell getestet auf iOS
- [ ] Manuell getestet auf macOS

## Screenshots (bei UI-Änderungen)
[Füge Screenshots ein]

## Checklist
- [ ] Code folgt Stil-Richtlinien
- [ ] Dokumentation aktualisiert
- [ ] Tests hinzugefügt/aktualisiert
- [ ] Alle Tests bestehen
- [ ] Keine Compiler Warnings
```

### Review-Prozess

1. **Automatische Checks**: CI/CD läuft automatisch
2. **Code Review**: Ein Maintainer reviewed deinen Code
3. **Änderungen**: Feedback adressieren
4. **Merge**: Nach Approval wird der PR gemergt

---

## 📚 Wichtige Ressourcen

- **[ARCHITECTURE.md](./ARCHITECTURE.md)**: Detaillierte technische Dokumentation
- **[.github/copilot-instructions.md](./.github/copilot-instructions.md)**: GitHub Copilot Guidelines
- **[API_ENDPOINTS.md](./Kickbasehelper/Services/API_ENDPOINTS.md)**: API Dokumentation
- **[Kickbase API](https://api.kickbase.com)**: Offizielle Kickbase API

---

## 🎯 Feature-Ideen für Einsteiger

Suche nach einem guten ersten Beitrag? Hier sind einige Ideen:

### Einfach (Good First Issue)
- [ ] Verbessere UI-Texte (z.B. Fehlermeldungen)
- [ ] Füge neue SF Symbols Icons hinzu
- [ ] Dokumentations-Verbesserungen
- [ ] Kleine UI-Tweaks (Spacing, Colors)

### Medium
- [ ] Neue Filteroption in Markt-Ansicht
- [ ] Export-Funktion für Spieler-Listen
- [ ] Dark Mode Optimierungen
- [ ] Neue Statistik-Visualisierung

### Fortgeschritten
- [ ] Neue Empfehlungs-Algorithmus-Variante
- [ ] Push-Notifications für wichtige Events
- [ ] Watchlist für Spieler
- [ ] Advanced Analytics Dashboard

---

## ❓ Hilfe bekommen

### Fragen stellen

- **GitHub Issues**: Für Bug Reports und Feature Requests
- **GitHub Discussions**: Für allgemeine Fragen
- **Jira Board**: https://l3g0m4n14c.atlassian.net/jira/software/projects/ECS/boards/1

### Debugging-Tipps

#### API-Probleme
```swift
// Aktiviere Logging in KickbaseAPIService
let apiService = KickbaseAPIService(enableLogging: true)
```

#### UI-Probleme
```swift
// View Hierarchy Debugger in Xcode
Debug → View Debugging → Capture View Hierarchy
```

#### Crash beim Start
- Prüfe: Ist `KickbaseManager` korrekt initialisiert?
- Prüfe: Sind alle `@EnvironmentObject` richtig übergeben?

---

## 🏆 Code of Conduct

- Sei respektvoll und konstruktiv
- Hilf anderen Entwicklern
- Akzeptiere Feedback professionell
- Folge den etablierten Patterns

---

## 📝 Cheat Sheet

### Häufige Befehle

```bash
# Tests
swift test                              # Alle Tests
swift test --filter ServiceTests        # Spezifische Tests

# Build
xcodebuild -scheme Kickbasehelper       # iOS/macOS Build
./build_android.sh                      # Android Build

# Git
git checkout -b feature/xyz             # Neuer Branch
git commit -m "feat: ..."               # Commit
git push origin feature/xyz             # Push
```

### Häufige Dateien

- `KickbaseManager.swift`: Zentrale ViewModel
- `Models.swift`: Core Datenmodelle
- `KickbaseAPIService.swift`: API Client
- `MainDashboardView.swift`: Haupt-UI

### Quick Links

- [Architektur](./ARCHITECTURE.md)
- [Copilot Guidelines](./.github/copilot-instructions.md)
- [Issues](https://github.com/L3g0M4n14c/Kickbasehelper/issues)
- [PRs](https://github.com/L3g0M4n14c/Kickbasehelper/pulls)

---

**Viel Erfolg beim Beitragen! 🚀**

Bei Fragen: Erstelle ein Issue oder schaue in die Dokumentation.

**Letzte Aktualisierung**: 17.02.2024
