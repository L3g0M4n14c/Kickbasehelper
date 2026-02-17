# GitHub Copilot Instructions für Kickbasehelper

> **Zielgruppe**: GitHub Copilot und andere KI-Coding-Agents, die Code-Änderungen in diesem Repository vornehmen.

## 🎯 Grundprinzipien

Als KI-Coding-Agent in diesem Projekt **MUSST** du folgende Regeln einhalten:

### 1. Architektur-Treue

**✅ IMMER befolgen:**
- Nutze das **MVVM-Pattern** (Model-View-ViewModel)
- **KickbaseManager** ist die zentrale ViewModel-Komponente
- Services enthalten Business Logic, Views binden an @Published Properties
- Keine Business Logic in Views
- State Management über Combine Framework (@Published, @StateObject, @EnvironmentObject)

**❌ NIEMALS:**
- UIKit/AppKit direkt verwenden (nur SwiftUI)
- Neue Architektur-Pattern ohne Abstimmung einführen
- Tight Coupling zwischen Views und Services
- Globale Variablen oder Singletons (außer wo bereits etabliert: BackgroundTaskManager.shared)

### 2. Code-Organisation

**Neue Dateien IMMER in korrekten Ordner:**
```
KickbaseCore/Sources/KickbaseCore/
├── Services/           # Neue Services hier
├── Views/              # Neue Views hier
├── ViewModels/         # Neue ViewModels hier
├── Models/             # Erweitere Models.swift, RecommendationModels.swift, oder LiveModels.swift
├── Utilities/          # Helper-Klassen hier
└── Compat/             # Platform-Adapter hier
```

**Namenskonventionen:**
- **Klassen/Structs**: `PascalCase` (z.B. `KickbaseManager`, `TransferRecommendation`)
- **Properties/Methoden**: `camelCase` (z.B. `teamPlayers`, `loadMarketPlayers()`)
- **Enums**: `PascalCase` mit `camelCase` Cases (z.B. `enum RiskLevel { case low, medium, high }`)
- **Dateien**: Matching mit Hauptklasse (z.B. `KickbaseManager.swift`)

### 3. Services erstellen

**Template für neue Services:**
```swift
/// <Beschreibung des Service-Zwecks>
class NewService {
    private let apiService: KickbaseAPIService
    private let parser: KickbaseDataParser
    
    // Dependency Injection für Testbarkeit
    init(apiService: KickbaseAPIService = KickbaseAPIService(),
         parser: KickbaseDataParser = KickbaseDataParser()) {
        self.apiService = apiService
        self.parser = parser
    }
    
    /// <Dokumentation der Methode>
    /// - Parameter xyz: Beschreibung
    /// - Returns: Beschreibung
    /// - Throws: APIError bei Fehlern
    func loadData() async throws -> DataType {
        let response = try await apiService.fetch(endpoint: "/v4/...")
        let parsed = try parser.parse(response)
        return parsed
    }
}
```

**Integration in KickbaseManager:**
```swift
class KickbaseManager: ObservableObject {
    private let newService: NewService
    @Published var newData: DataType?
    
    init(newService: NewService = NewService()) {
        self.newService = newService
    }
    
    func loadNewData() async {
        isLoading = true
        do {
            newData = try await newService.loadData()
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}
```

### 4. Views erstellen

**Template für neue Views:**
```swift
import SwiftUI

/// <Beschreibung der View>
struct NewFeatureView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var showSheet = false
    
    var body: some View {
        NavigationStack {
            content
                .navigationTitle("Titel")
                .toolbar {
                    toolbarContent
                }
        }
        .onAppear {
            loadData()
        }
    }
    
    @ViewBuilder
    private var content: some View {
        if kickbaseManager.isLoading {
            ProgressView()
        } else if let data = kickbaseManager.newData {
            dataView(data)
        } else if let error = kickbaseManager.errorMessage {
            errorView(error)
        }
    }
    
    private func loadData() {
        Task {
            await kickbaseManager.loadNewData()
        }
    }
}
```

**View-Extraktion bei Komplexität:**
```swift
// ✅ RICHTIG: Sub-Views bei >50 Zeilen
struct ComplexView: View {
    var body: some View {
        VStack {
            HeaderSection()
            ContentSection()
            FooterSection()
        }
    }
}

struct HeaderSection: View {
    var body: some View {
        // Header-Logik
    }
}
```

### 5. Async/Await richtig nutzen

**✅ IMMER:**
```swift
// Async-Methoden mit async/await
func loadData() async throws -> Data {
    let data = try await apiService.fetch()
    return data
}

// Task für UI-Calls
Task {
    await kickbaseManager.loadData()
}

// Error Handling mit do-catch
do {
    let result = try await someOperation()
} catch {
    errorMessage = error.localizedDescription
}
```

**❌ NIEMALS:**
```swift
// ❌ Completion Handler (deprecated)
func loadData(completion: @escaping (Result<Data, Error>) -> Void) {
    // ...
}

// ❌ DispatchQueue direkt
DispatchQueue.main.async {
    // Use @MainActor instead
}
```

### 6. API-Integration

**Neuer API Endpoint:**
```swift
// 1. In KickbaseAPIService.swift hinzufügen:
func fetchNewData(leagueId: String) async throws -> NewDataResponse {
    let endpoint = "/v4/leagues/\(leagueId)/newdata"
    return try await fetchData(endpoint)
}

// 2. Response Model in Models.swift:
struct NewDataResponse: Codable {
    let data: [DataItem]
}

// 3. Falls Kurzform-Keys: In KickbaseDataParser.swift mappen
func parseNewData(_ json: [String: Any]) -> NewDataResponse {
    // Mapping von "nd" → newData, etc.
}
```

### 7. Datenmodelle erweitern

**✅ Best Practice:**
```swift
// In Models.swift, RecommendationModels.swift, oder LiveModels.swift
struct NewModel: Codable, Identifiable {
    let id: String
    let name: String
    // ... weitere Properties
    
    // Computed Properties OK
    var displayName: String {
        return name.uppercased()
    }
}

// Enums für typsichere Werte
enum Status: Int, Codable {
    case active = 0
    case injured = 2
    case suspended = 4
}
```

### 8. Testing ist PFLICHT

**Für jeden neuen Service/Manager:**
```swift
// Tests/KickbaseCoreTests/NewServiceTests.swift
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
        let expectedData = mockData
        mockAPIService.mockResponse = expectedData
        
        // When
        let result = try await sut.loadData()
        
        // Then
        XCTAssertEqual(result.count, 5)
        XCTAssertTrue(mockAPIService.fetchCalled)
    }
    
    func testLoadData_Failure() async {
        // Given
        mockAPIService.shouldFail = true
        
        // When/Then
        do {
            _ = try await sut.loadData()
            XCTFail("Should throw error")
        } catch {
            XCTAssertTrue(error is APIError)
        }
    }
}
```

**Für Views (optional, aber empfohlen):**
```swift
import ViewInspector
@testable import KickbaseCore

class NewViewTests: XCTestCase {
    func testView_DisplaysData() throws {
        let manager = KickbaseManager()
        manager.newData = mockData
        
        let view = NewFeatureView()
            .environmentObject(manager)
        
        let list = try view.inspect().find(ViewType.List.self)
        XCTAssertEqual(try list.count(), mockData.count)
    }
}
```

### 9. Dokumentation IMMER aktualisieren

**Bei neuen Features:**
1. ✅ Docstrings für Klassen/Methoden hinzufügen
2. ✅ `ARCHITECTURE.md` aktualisieren (Sektion "Module und Komponenten")
3. ✅ Beispiel-Code in Dokumentation einfügen
4. ✅ API-Endpoints in `Services/API_ENDPOINTS.md` dokumentieren

**Docstring-Template:**
```swift
/// Kurze Beschreibung in einem Satz.
///
/// Detaillierte Beschreibung des Zwecks und Verhaltens.
/// Mehrere Absätze möglich für komplexe Logik.
///
/// - Parameters:
///   - param1: Beschreibung von Parameter 1
///   - param2: Beschreibung von Parameter 2
/// - Returns: Beschreibung des Rückgabewerts
/// - Throws: `APIError` wenn API-Call fehlschlägt
func complexMethod(param1: String, param2: Int) async throws -> Result {
    // Implementation
}
```

### 10. Error Handling Pattern

**Standard Error Handling:**
```swift
class SomeManager: ObservableObject {
    @Published var isLoading = false
    @Published var errorMessage: String?
    @Published var data: DataType?
    
    func loadData() async {
        isLoading = true
        errorMessage = nil
        
        do {
            data = try await service.fetch()
        } catch let error as APIError {
            errorMessage = error.localizedDescription
        } catch {
            errorMessage = "Ein unbekannter Fehler ist aufgetreten"
        }
        
        isLoading = false
    }
}
```

**In Views:**
```swift
var body: some View {
    VStack {
        if manager.isLoading {
            ProgressView("Lädt...")
        } else if let error = manager.errorMessage {
            ErrorView(message: error, retry: loadData)
        } else {
            ContentView(data: manager.data)
        }
    }
}
```

### 11. Performance-Bewusst Coden

**✅ Caching implementieren:**
```swift
private var cachedData: DataType?
private var cacheTimestamp: Date?
private let cacheValidityDuration: TimeInterval = 300 // 5 Minuten

func getData() async -> DataType {
    if let cached = cachedData,
       let timestamp = cacheTimestamp,
       Date().timeIntervalSince(timestamp) < cacheValidityDuration {
        return cached
    }
    
    let fresh = await fetchFreshData()
    cachedData = fresh
    cacheTimestamp = Date()
    return fresh
}
```

**✅ Lazy Loading:**
```swift
.onAppear {
    Task {
        await loadDataIfNeeded()
    }
}
```

**✅ Debouncing bei Search:**
```swift
@State private var searchText = ""
@State private var searchTask: Task<Void, Never>?

var body: some View {
    TextField("Suche", text: $searchText)
        .onChange(of: searchText) { newValue in
            searchTask?.cancel()
            searchTask = Task {
                try? await Task.sleep(nanoseconds: 300_000_000)
                await performSearch(newValue)
            }
        }
}
```

### 12. Platform-Kompatibilität

**iOS, macOS & Android Support:**
```swift
#if os(iOS)
// iOS-spezifischer Code
TabView {
    // Tabs
}
#elseif os(macOS)
// macOS-spezifischer Code
NavigationSplitView {
    // Sidebar
}
#endif
```

**Skip Framework für Android:**
- Code in `KickbaseCore` ist automatisch für Android transpiliert
- Vermeide plattform-spezifische APIs ohne Guards
- Nutze `PlatformUtils.swift` für Abstractions

### 13. Security Guidelines

**✅ IMMER:**
- Tokens in Keychain speichern (nicht UserDefaults)
- HTTPS für alle API-Calls
- Input validieren (Email, Passwort, etc.)
- Keine hardcoded Credentials

**❌ NIEMALS:**
- API-Keys im Code committen
- Sensitive Daten in Logs ausgeben
- Unsichere HTTP-Verbindungen

### 14. Code Review Checklist

Vor dem Commit prüfe:
- [ ] Code folgt MVVM-Pattern
- [ ] Neue Services haben Dependency Injection
- [ ] Tests für neue Funktionalität geschrieben
- [ ] Dokumentation aktualisiert (ARCHITECTURE.md)
- [ ] Docstrings für neue Klassen/Methoden
- [ ] Error Handling implementiert
- [ ] Loading States in UI
- [ ] Keine Compiler Warnings
- [ ] Keine force unwraps (`!`) ohne Rechtfertigung
- [ ] Platform-Kompatibilität berücksichtigt

---

## 🚀 Quick Reference: Feature hinzufügen

### Neue API-Daten anzeigen

1. **Model** definieren/erweitern (`Models.swift`)
2. **API Service** Methode hinzufügen (`KickbaseAPIService.swift`)
3. **KickbaseManager** Property + Load-Methode
4. **View** erstellen mit EnvironmentObject binding
5. **Tests** schreiben für Service & Manager
6. **Dokumentation** aktualisieren

### Neue Empfehlungs-Logik

1. **Service** erstellen in `Services/`
2. **Algorithm** implementieren mit Scoring-System
3. **Caching** für Performance (5 Min)
4. **Integration** in KickbaseManager
5. **View** für UI mit Filtering/Sorting
6. **Tests** mit Mock-Daten
7. **Dokumentation** des Algorithmus

### Neuer Tab/Screen

1. **View** erstellen in `Views/`
2. **Navigation** hinzufügen:
   - iOS: `MainDashboardView.swift` TabView erweitern
   - macOS: Sidebar NavigationLink hinzufügen
3. **Icon** auswählen (SF Symbols)
4. **EnvironmentObject** binden
5. **Tests** für View-Logic

---

## 📚 Weitere Ressourcen

- **Architektur-Dokumentation**: [`ARCHITECTURE.md`](../ARCHITECTURE.md)
- **Beitrags-Richtlinien**: [`CONTRIBUTING.md`](../CONTRIBUTING.md)
- **API-Endpoints**: [`Kickbasehelper/Services/API_ENDPOINTS.md`](../Kickbasehelper/Services/API_ENDPOINTS.md)

---

## ⚠️ Kritische Regeln für KI-Agents

1. **NIEMALS** existierende Architektur brechen
2. **IMMER** Tests für neue Features schreiben
3. **IMMER** Dokumentation aktualisieren
4. **NIEMALS** sensible Daten committen
5. **IMMER** async/await statt Completion Handler
6. **IMMER** SwiftUI (kein UIKit/AppKit)
7. **IMMER** Dependency Injection nutzen
8. **IMMER** Error Handling implementieren
9. **IMMER** Code in korrekten Ordner platzieren
10. **IMMER** Naming Conventions befolgen

---

**Bei Unsicherheit**: Schaue bestehenden Code an und folge dem etablierten Pattern!

**Letzte Aktualisierung**: 17.02.2024
