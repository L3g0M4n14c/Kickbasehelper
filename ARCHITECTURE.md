# Kickbasehelper - Technische Architektur Dokumentation

> **Zielgruppe**: Entwickler und KI-Coding-Agents, die neue Features implementieren oder bestehenden Code verstehen möchten.

## 📋 Inhaltsverzeichnis

1. [Architektur-Überblick](#architektur-überblick)
2. [Projekt-Struktur](#projekt-struktur)
3. [Architektur-Pattern](#architektur-pattern)
4. [Module und Komponenten](#module-und-komponenten)
5. [Datenmodelle](#datenmodelle)
6. [Services und Manager](#services-und-manager)
7. [UI-Layer und Navigation](#ui-layer-und-navigation)
8. [API-Integration](#api-integration)
9. [State Management](#state-management)
10. [Testing-Strategie](#testing-strategie)
11. [Wie füge ich neue Features hinzu?](#wie-füge-ich-neue-features-hinzu)
12. [Best Practices und Konventionen](#best-practices-und-konventionen)

---

## 🏗️ Architektur-Überblick

### Architektur-Pattern
**MVVM (Model-View-ViewModel) mit reaktiver Architektur**

- **Models**: Datenstrukturen (`Models.swift`, `RecommendationModels.swift`, `LiveModels.swift`)
- **Views**: SwiftUI Views (alle `*View.swift` Dateien)
- **ViewModels**: Teilweise dedizierte ViewModels (z.B. `LeagueTableViewModel`), meist wird `KickbaseManager` als zentrale ViewModel genutzt
- **Services**: Business Logic und API-Integration (`Services/` Ordner)

### Technologie-Stack

| Technologie | Verwendung |
|-------------|------------|
| **Swift 5.9+** | Haupt-Programmiersprache |
| **SwiftUI** | UI-Framework (100% SwiftUI, kein UIKit/AppKit) |
| **Combine** | Reaktive Programmierung |
| **Swift Concurrency** | async/await für asynchrone Operationen |
| **SwiftData** | Datenpersistierung (minimal genutzt) |
| **URLSession** | Netzwerk-Kommunikation |
| **Swift Package Manager** | Dependency Management |
| **SkipUI** | Cross-Platform Transpiler (iOS → Android) |

### Plattform-Kompatibilität

- **iOS 15.0+** (iPhone, iPad)
- **macOS 12.0+**
- **Android** (via Skip Framework Transpilation)

---

## 📁 Projekt-Struktur

```
Kickbasehelper/
│
├── KickbaseCore/                          # ⭐ Haupt-SPM-Library (Core Logic)
│   ├── Sources/KickbaseCore/
│   │   ├── Services/                      # Business Logic Layer
│   │   │   ├── KickbaseAPIService.swift       # API v4 Client
│   │   │   ├── KickbaseDataParser.swift       # JSON Parser
│   │   │   ├── KickbasePlayerService.swift    # Spieler-Daten
│   │   │   ├── KickbaseLeagueService.swift    # Liga-Daten
│   │   │   ├── KickbaseUserStatsService.swift # User-Statistiken
│   │   │   ├── PlayerRecommendationService.swift  # Empfehlungen
│   │   │   ├── LigainsiderService.swift       # Web Scraping
│   │   │   └── BackgroundTaskManager.swift    # iOS Background Tasks
│   │   │
│   │   ├── Managers/                      # Orchestration Layer
│   │   │   ├── KickbaseManager.swift          # ⭐ Zentrale Koordination
│   │   │   └── AuthenticationManager.swift    # Auth & Session
│   │   │
│   │   ├── Models/                        # Data Models
│   │   │   ├── Models.swift                   # Core Models (User, League, Player)
│   │   │   ├── RecommendationModels.swift     # Transfer/Sale Empfehlungen
│   │   │   ├── LiveModels.swift               # Live-Daten Strukturen
│   │   │   └── Item.swift                     # SwiftData Model
│   │   │
│   │   ├── Views/                         # SwiftUI Views
│   │   │   ├── ContentView.swift              # Root View
│   │   │   ├── LoginView.swift                # Login UI
│   │   │   ├── MainDashboardView.swift        # ⭐ Haupt-Dashboard
│   │   │   ├── TeamManagementViews.swift      # Team-Verwaltung
│   │   │   ├── TransferRecommendationsView.swift  # Kauf-Empfehlungen
│   │   │   ├── SaleRecommendationsView.swift      # Verkauf-Empfehlungen
│   │   │   ├── PlayerDetailView.swift         # Spieler-Details
│   │   │   ├── LineupComparisonView.swift     # Aufstellungs-Manager
│   │   │   ├── LiveView.swift                 # Live-Daten
│   │   │   ├── LeagueTableView.swift          # Liga-Tabelle
│   │   │   ├── LigainsiderView.swift          # Ligainsider Integration
│   │   │   ├── PlayerMatchDetailView.swift    # Match-Details
│   │   │   ├── UserDetailView.swift           # User-Profil
│   │   │   └── BonusCollectionSettingsView.swift  # Bonus-Einstellungen
│   │   │
│   │   ├── ViewModels/                    # Dedizierte ViewModels
│   │   │   └── LeagueTableViewModel.swift     # Liga-Tabelle VM
│   │   │
│   │   ├── Utilities/                     # Helper Classes
│   │   │   ├── ImageHelpers.swift             # Async Image Loading
│   │   │   ├── SearchUtils.swift              # Such-Algorithmen
│   │   │   ├── ScalingUtility.swift           # macOS Scaling
│   │   │   └── SessionSanitizer.swift         # URLSession Config
│   │   │
│   │   ├── Compat/                        # Platform Compatibility
│   │   │   └── NavigationCompat.swift         # Navigation Adapter
│   │   │
│   │   ├── Inspectables/                  # Testing Utilities
│   │   │   └── InspectableSheet.swift         # ViewInspector Helpers
│   │   │
│   │   ├── EnhancedMarketParser.swift     # Advanced Parser
│   │   ├── DebugMarketParser.swift        # Debug Parser
│   │   └── PlatformUtils.swift            # Platform Extensions
│   │   │
│   │   └── Resources/                     # Assets & Config
│   │       ├── Assets.xcassets/
│   │       └── kickbasev4.json                # API Field Mappings
│   │
│   ├── Tests/KickbaseCoreTests/           # Unit & Integration Tests
│   │   ├── KickbaseAPIServiceTests.swift
│   │   ├── KickbasePlayerServiceTests.swift
│   │   ├── PlayerRecommendationServiceTests.swift
│   │   ├── LigainsiderServiceTests.swift
│   │   └── ... (13+ Test-Dateien)
│   │
│   └── Package.swift                      # SPM Package Definition
│
├── Kickbasehelper/                        # iOS/macOS App Target
│   ├── KickbasehelperApp.swift            # App Entry Point
│   ├── Assets.xcassets/                   # App-spezifische Assets
│   └── Services/
│       └── API_ENDPOINTS.md               # API Dokumentation
│
├── KickbasehelperTests/                   # App-Level Tests
├── KickbasehelperUITests/                 # UI Tests
│
├── Android/                               # Android Transpilation (Skip)
│   └── app/                               # Android App Target
│
├── scripts/                               # Build & Deploy Scripts
│   ├── build_android.sh
│   └── deploy_android.sh
│
└── docs/                                  # Zusätzliche Dokumentation
    └── SKIP_SKILL.md
```

---

## 🎯 Architektur-Pattern

### MVVM mit zentralem Manager

```
┌─────────────────────────────────────────────────────────────┐
│                        SwiftUI Views                         │
│  (ContentView, MainDashboardView, TransferRecommendationsView)│
└────────────────────────┬────────────────────────────────────┘
                         │ @EnvironmentObject
                         │ @StateObject
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    KickbaseManager                           │
│            (Zentrale ViewModel & Orchestrator)               │
│  • @Published Properties für UI-Binding                      │
│  • Koordiniert alle Services                                 │
│  • Hält App-State (User, League, Players, Recommendations)   │
└────────────────────────┬────────────────────────────────────┘
                         │
         ┌───────────────┼───────────────┬──────────────┐
         ▼               ▼               ▼              ▼
┌─────────────┐ ┌─────────────┐ ┌──────────────┐ ┌──────────┐
│ APIService  │ │PlayerService│ │LeagueService │ │ Auth Mgr │
└─────────────┘ └─────────────┘ └──────────────┘ └──────────┘
         │               │               │              │
         └───────────────┴───────────────┴──────────────┘
                         ▼
                ┌─────────────────┐
                │  Kickbase API   │
                │ (REST v4)       │
                └─────────────────┘
```

### Datenfluss

1. **View** → Action (User klickt Button)
2. **ViewModel** (KickbaseManager) → Service-Methode aufrufen
3. **Service** → API Request
4. **Service** → Response parsen (via KickbaseDataParser)
5. **ViewModel** → @Published Property aktualisieren
6. **View** → Auto-Update durch SwiftUI Binding

---

## 🧩 Module und Komponenten

### Core Services

#### 1. KickbaseAPIService
**Zweck**: Low-Level HTTP Client für Kickbase API v4  
**Verantwortlichkeiten**:
- API-Requests mit Authentifizierung
- Error Handling
- Response Decoding
- Token Management

**Wichtige Methoden**:
```swift
func login(email: String, password: String) async throws -> LoginResponse
func fetchLeagues() async throws -> LeaguesResponse
func fetchTeamPlayers(leagueId: String) async throws -> TeamPlayersResponse
func fetchMarketPlayers(leagueId: String) async throws -> MarketPlayersResponse
func updateLineup(leagueId: String, playerIds: [String]) async throws
```

#### 2. KickbaseDataParser
**Zweck**: JSON-Parsing mit Kickbase-spezifischen Kürzeln  
**Verantwortlichkeiten**:
- Mapping von Kurzform-Keys zu vollständigen Properties
- Fehlertolerante Parsing-Logik
- Validierung von Daten

**Field Mappings**:
```swift
"i"   → id
"n"   → name
"tn"  → teamName
"em"  → email
"b"   → budget
"tv"  → teamValue
"p"   → points
"pl"  → placement
"md"  → matchDay
"lp"  → lineupPlayerIds
```

#### 3. KickbasePlayerService
**Zweck**: Spieler-Daten laden und verwalten  
**Features**:
- Team-Spieler laden
- Markt-Spieler laden
- Ligainsider-Integration für Live-Daten
- Caching von Spieler-Statistiken

#### 4. PlayerRecommendationService
**Zweck**: KI-gestützte Transfer-Empfehlungen  
**Algorithmus**:
1. Analysiere aktuelles Team
2. Analysiere verfügbare Markt-Spieler
3. Berechne Empfehlungs-Score basierend auf:
   - Punktedurchschnitt
   - Trend (Form)
   - Marktwert vs. Potential
   - Team-Balance (Positionen)
   - Risiko-Faktor
4. Sortiere und filtere Empfehlungen
5. Cache für 5 Minuten

**Scoring-Faktoren**:
```swift
recommendationScore = 
    pointsAverage * 0.4 +
    trendScore * 0.3 +
    valueForMoney * 0.2 +
    positionNeed * 0.1
```

#### 5. LigainsiderService
**Zweck**: Web Scraping für Echtzeit-Aufstellungen  
**Datenquelle**: ligainsider.de  
**Features**:
- HTML-Parsing
- Squad-Caching
- Verletzungs- und Sperr-Status
- Match-Predictions

#### 6. BackgroundTaskManager
**Zweck**: iOS Background Task Scheduling  
**Use Case**: Tägliche Bonus-Collection  
**Pattern**: Singleton (`shared`)

### Managers (Orchestration Layer)

#### KickbaseManager
**⭐ Zentrale Komponente der App**

**Verantwortlichkeiten**:
- App-State verwalten
- Alle Services koordinieren
- UI-Bindings via @Published Properties
- Error Handling & User Feedback

**Wichtige Properties**:
```swift
@Published var user: User?
@Published var currentLeague: League?
@Published var teamPlayers: [TeamPlayer] = []
@Published var marketPlayers: [MarketPlayer] = []
@Published var transferRecommendations: [TransferRecommendation] = []
@Published var saleRecommendations: [SaleRecommendation] = []
@Published var isLoading: Bool = false
@Published var errorMessage: String?
```

**Lifecycle**:
1. App Start → `KickbaseManager` wird initialisiert
2. Login → User & Token werden gespeichert
3. League Selection → Lade Liga-Daten
4. Background → Regelmäßige Updates

#### AuthenticationManager
**Verantwortlichkeiten**:
- Login/Logout
- Token-Speicherung (Keychain/UserDefaults)
- Session-Persistierung
- Auto-Login

---

## 📊 Datenmodelle

### Core Models (Models.swift)

#### User
```swift
struct User: Codable, Identifiable {
    let id: String
    let name: String
    let email: String
    let profile: String?  // Profilbild URL
}
```

#### League
```swift
struct League: Codable, Identifiable {
    let id: String
    let name: String
    let creatorId: String
    let matchDay: Int
    let users: [LeagueUser]
}
```

#### LeagueUser
```swift
struct LeagueUser: Codable, Identifiable {
    let userId: String
    let name: String
    let placement: Int
    let points: Int
    let teamValue: Int
    let budget: Int
}
```

#### Player (Base Model)
```swift
struct Player: Codable, Identifiable {
    let id: String
    let firstName: String
    let lastName: String
    let teamName: String
    let position: Int  // 1=TW, 2=ABW, 3=MF, 4=STU
    let number: Int?
    let profileBig: String  // Image URL
    let points: Int
    let averagePoints: Int
    let marketValue: Int
    let status: Int  // 0=OK, 2=Verletzt, 4=Gesperrt
}
```

#### TeamPlayer (Owned)
```swift
struct TeamPlayer: Player {
    let buyPrice: Int
    let expiry: String?
}
```

#### MarketPlayer (Available for Transfer)
```swift
struct MarketPlayer: Player {
    let offers: [Offer]?
    let expiry: String?
}
```

### Recommendation Models (RecommendationModels.swift)

#### TransferRecommendation
```swift
struct TransferRecommendation: Identifiable {
    let id: UUID
    let player: MarketPlayer
    let recommendationScore: Double  // 0-100
    let reasons: [RecommendationReason]
    let analysis: PlayerAnalysis
    let riskLevel: RiskLevel  // low, medium, high
    let priority: Priority    // essential, recommended, optional
}
```

#### SaleRecommendation
```swift
struct SaleRecommendation: Identifiable {
    let id: UUID
    let player: TeamPlayer
    let goal: SaleRecommendationGoal
    let estimatedProfit: Int
    let replacementSuggestion: MarketPlayer?
}
```

#### SaleRecommendationGoal
```swift
enum SaleRecommendationGoal {
    case balanceBudget      // Budget ausgleichen
    case underperforming    // Schwache Spieler
    case rebuild            // Team-Umbau
    case maximizeProfit     // Gewinn maximieren
}
```

---

## 🔧 Services und Manager

### Service-Architektur

Alle Services folgen diesem Pattern:

```swift
class XYZService {
    private let apiService: KickbaseAPIService
    private let parser: KickbaseDataParser
    
    init(apiService: KickbaseAPIService, parser: KickbaseDataParser) {
        self.apiService = apiService
        self.parser = parser
    }
    
    func loadData() async throws -> DataType {
        let response = try await apiService.fetch()
        let parsed = try parser.parse(response)
        return parsed
    }
}
```

### Dependency Injection

KickbaseManager nutzt Constructor Injection für Testbarkeit:

```swift
class KickbaseManager: ObservableObject {
    private let apiService: KickbaseAPIService
    private let playerService: KickbasePlayerService
    
    init(
        apiService: KickbaseAPIService = KickbaseAPIService(),
        playerService: KickbasePlayerService = KickbasePlayerService()
    ) {
        self.apiService = apiService
        self.playerService = playerService
    }
}
```

**Testbarkeit**: Mock-Services können injiziert werden:
```swift
let mockAPI = MockKickbaseAPIService()
let manager = KickbaseManager(apiService: mockAPI)
```

---

## 🎨 UI-Layer und Navigation

### Navigation-Pattern

#### iOS/iPad
```swift
TabView {
    DashboardView()
        .tabItem { Label("Dashboard", systemImage: "house") }
    
    TeamManagementView()
        .tabItem { Label("Team", systemImage: "person.3") }
    
    // ... weitere Tabs
}
```

#### macOS
```swift
NavigationSplitView {
    List {
        NavigationLink("Dashboard", destination: DashboardView())
        NavigationLink("Team", destination: TeamManagementView())
        // ... weitere Links
    }
} detail: {
    selectedView
}
```

### View-Hierarchie

```
ContentView
├─ if user == nil
│  └─ LoginView
└─ else
   └─ MainDashboardView
      ├─ TabView (iOS)
      │  ├─ Tab 0: Dashboard Overview
      │  ├─ Tab 1: Team Management
      │  ├─ Tab 2: Market Analysis
      │  ├─ Tab 3: Transfer Recommendations
      │  ├─ Tab 4: Sale Recommendations
      │  ├─ Tab 5: Lineup Manager
      │  ├─ Tab 6: Live View
      │  └─ Tab 7: League Table
      │
      └─ NavigationSplitView (macOS)
         └─ Sidebar + Detail
```

### State Management in Views

Views nutzen `@EnvironmentObject` für KickbaseManager:

```swift
struct TransferRecommendationsView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        List(kickbaseManager.transferRecommendations) { recommendation in
            RecommendationRow(recommendation: recommendation)
        }
        .onAppear {
            Task {
                await kickbaseManager.loadTransferRecommendations()
            }
        }
    }
}
```

### Sheet Präsentation

```swift
.sheet(isPresented: $showPlayerDetail) {
    PlayerDetailView(player: selectedPlayer)
        .environmentObject(kickbaseManager)
}
```

---

## 🌐 API-Integration

### Kickbase API v4

**Base URL**: `https://api.kickbase.com`

**Authentifizierung**: Bearer Token im Authorization Header

```swift
var request = URLRequest(url: url)
request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
```

### Wichtige Endpoints

| Endpoint | Methode | Beschreibung |
|----------|---------|--------------|
| `/v4/user/login` | POST | Login & Token erhalten |
| `/v4/leagues` | GET | Alle Ligen des Users |
| `/v4/leagues/:id` | GET | Liga-Details |
| `/v4/leagues/:id/players` | GET | Team-Spieler |
| `/v4/leagues/:id/market` | GET | Markt-Spieler |
| `/v4/leagues/:id/lineupex` | POST | Aufstellung aktualisieren |
| `/v4/leagues/:id/stats` | GET | Liga-Statistiken |
| `/v4/users/:id` | GET | User-Details |
| `/v4/players/:id` | GET | Spieler-Details |
| `/v4/leagues/:id/collectgift` | POST | Täglichen Bonus abholen |

### API Response Handling

```swift
func fetchData<T: Decodable>(_ endpoint: String) async throws -> T {
    let url = URL(string: "\(baseURL)\(endpoint)")!
    var request = URLRequest(url: url)
    request.setValue("Bearer \(token)", forHTTPHeaderField: "Authorization")
    
    let (data, response) = try await URLSession.shared.data(for: request)
    
    guard let httpResponse = response as? HTTPURLResponse else {
        throw APIError.invalidResponse
    }
    
    guard httpResponse.statusCode == 200 else {
        throw APIError.httpError(statusCode: httpResponse.statusCode)
    }
    
    let decoded = try JSONDecoder().decode(T.self, from: data)
    return decoded
}
```

### Error Handling

```swift
enum APIError: Error, LocalizedError {
    case invalidURL
    case invalidResponse
    case httpError(statusCode: Int)
    case decodingError(Error)
    case networkError(Error)
    case unauthorized
    
    var errorDescription: String? {
        switch self {
        case .invalidURL: return "Ungültige URL"
        case .invalidResponse: return "Ungültige Antwort"
        case .httpError(let code): return "HTTP Fehler: \(code)"
        case .decodingError: return "Fehler beim Parsen der Daten"
        case .networkError: return "Netzwerkfehler"
        case .unauthorized: return "Nicht autorisiert"
        }
    }
}
```

---

## 🔄 State Management

### Published Properties Pattern

```swift
class KickbaseManager: ObservableObject {
    // UI bindet an diese Properties
    @Published var user: User?
    @Published var currentLeague: League?
    @Published var teamPlayers: [TeamPlayer] = []
    @Published var isLoading: Bool = false
    @Published var errorMessage: String?
    
    func loadTeamPlayers() async {
        isLoading = true
        errorMessage = nil
        
        do {
            teamPlayers = try await playerService.loadTeamPlayers(leagueId: currentLeague!.id)
        } catch {
            errorMessage = error.localizedDescription
        }
        
        isLoading = false
    }
}
```

### Loading States

Jede Datenoperation folgt diesem Pattern:
1. `isLoading = true`
2. Daten laden
3. Properties aktualisieren
4. `isLoading = false`
5. Fehler → `errorMessage` setzen

### Caching Strategy

#### PlayerRecommendationService
```swift
private var cachedRecommendations: [TransferRecommendation]?
private var cacheTimestamp: Date?
private let cacheValidityDuration: TimeInterval = 300 // 5 Minuten

func getRecommendations() async -> [TransferRecommendation] {
    if let cached = cachedRecommendations,
       let timestamp = cacheTimestamp,
       Date().timeIntervalSince(timestamp) < cacheValidityDuration {
        return cached
    }
    
    let fresh = await calculateRecommendations()
    cachedRecommendations = fresh
    cacheTimestamp = Date()
    return fresh
}
```

---

## 🧪 Testing-Strategie

### Test-Struktur

```
Tests/KickbaseCoreTests/
├── Unit Tests (Service-Level)
│   ├── KickbaseAPIServiceTests.swift
│   ├── KickbaseDataParserTests.swift
│   ├── KickbasePlayerServiceTests.swift
│   └── PlayerRecommendationServiceTests.swift
│
├── Integration Tests (Multi-Service)
│   ├── KickbaseManagerIntegrationTests.swift
│   └── PlayerRecommendationIntegrationTests.swift
│
└── UI Tests (ViewInspector)
    └── ViewInspectorTests.swift
```

### Testing Patterns

#### 1. Unit Tests mit Mocks
```swift
class KickbasePlayerServiceTests: XCTestCase {
    var sut: KickbasePlayerService!
    var mockAPIService: MockKickbaseAPIService!
    var mockParser: MockKickbaseDataParser!
    
    override func setUp() {
        super.setUp()
        mockAPIService = MockKickbaseAPIService()
        mockParser = MockKickbaseDataParser()
        sut = KickbasePlayerService(apiService: mockAPIService, parser: mockParser)
    }
    
    func testLoadTeamPlayers_Success() async throws {
        // Given
        mockAPIService.mockResponse = mockTeamPlayersData
        
        // When
        let players = try await sut.loadTeamPlayers(leagueId: "123")
        
        // Then
        XCTAssertEqual(players.count, 15)
        XCTAssertTrue(mockAPIService.fetchTeamPlayersCalled)
    }
}
```

#### 2. Network Mocking
```swift
class URLProtocolMock: URLProtocol {
    static var mockData: Data?
    static var mockResponse: URLResponse?
    static var mockError: Error?
    
    override class func canInit(with request: URLRequest) -> Bool {
        return true
    }
    
    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }
    
    override func startLoading() {
        if let error = URLProtocolMock.mockError {
            client?.urlProtocol(self, didFailWithError: error)
            return
        }
        
        if let response = URLProtocolMock.mockResponse {
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
        }
        
        if let data = URLProtocolMock.mockData {
            client?.urlProtocol(self, didLoad: data)
        }
        
        client?.urlProtocolDidFinishLoading(self)
    }
}
```

#### 3. SwiftUI View Testing (ViewInspector)
```swift
import ViewInspector

class ViewInspectorTests: XCTestCase {
    func testTransferRecommendationsView_DisplaysRecommendations() throws {
        let manager = KickbaseManager()
        manager.transferRecommendations = mockRecommendations
        
        let view = TransferRecommendationsView()
            .environmentObject(manager)
        
        let list = try view.inspect().find(ViewType.List.self)
        XCTAssertEqual(try list.count(), mockRecommendations.count)
    }
}
```

### Test-Driven Development Workflow

1. **Test schreiben** (Red)
2. **Minimal-Implementierung** (Green)
3. **Refactoring** (Refactor)

### Coverage-Ziele

- **Services**: 80%+ Coverage
- **Managers**: 70%+ Coverage
- **Views**: 50%+ Coverage (kritische UI-Flows)

---

## 🚀 Wie füge ich neue Features hinzu?

### Schritt-für-Schritt Anleitung

#### Beispiel: Neue Liga-Statistik hinzufügen

**Schritt 1: Datenmodell erweitern**
```swift
// Models.swift
struct League: Codable, Identifiable {
    let id: String
    let name: String
    // ... bestehende Properties
    let averageTeamValue: Int?  // ⭐ NEU
}
```

**Schritt 2: API Service erweitern**
```swift
// KickbaseAPIService.swift
func fetchLeagueStats(leagueId: String) async throws -> LeagueStatsResponse {
    let endpoint = "/v4/leagues/\(leagueId)/stats"
    return try await fetchData(endpoint)
}
```

**Schritt 3: Parser aktualisieren (falls nötig)**
```swift
// KickbaseDataParser.swift
func parseLeagueStats(_ data: Data) throws -> LeagueStatsResponse {
    let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
    // Mapping-Logik
}
```

**Schritt 4: KickbaseManager erweitern**
```swift
// KickbaseManager.swift
@Published var leagueStats: LeagueStatsResponse?

func loadLeagueStats() async {
    guard let leagueId = currentLeague?.id else { return }
    
    do {
        leagueStats = try await apiService.fetchLeagueStats(leagueId: leagueId)
    } catch {
        errorMessage = error.localizedDescription
    }
}
```

**Schritt 5: View hinzufügen/aktualisieren**
```swift
// MainDashboardView.swift
struct StatsView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    
    var body: some View {
        VStack {
            if let stats = kickbaseManager.leagueStats {
                Text("Durchschnittlicher Teamwert: \(stats.averageTeamValue)")
            }
        }
        .onAppear {
            Task {
                await kickbaseManager.loadLeagueStats()
            }
        }
    }
}
```

**Schritt 6: Tests schreiben**
```swift
// KickbaseManagerTests.swift
func testLoadLeagueStats_Success() async throws {
    // Given
    mockAPIService.mockLeagueStats = mockData
    
    // When
    await sut.loadLeagueStats()
    
    // Then
    XCTAssertNotNil(sut.leagueStats)
    XCTAssertEqual(sut.leagueStats?.averageTeamValue, 50000000)
}
```

**Schritt 7: Dokumentation aktualisieren**
- Diese ARCHITECTURE.md Datei aktualisieren
- Kommentare im Code hinzufügen
- CHANGELOG.md aktualisieren (falls vorhanden)

### Feature-Kategorien

#### 1. Neue API-Integration
- Service-Klasse erstellen
- Parser erweitern
- KickbaseManager integrieren
- Tests schreiben

#### 2. Neue UI-View
- View-Datei erstellen
- Navigation hinzufügen (Tab/Sidebar)
- EnvironmentObject binden
- Loading/Error States implementieren

#### 3. Neuer Algorithmus/Service
- Service-Klasse erstellen
- Business Logic implementieren
- Caching-Strategy definieren
- Unit Tests mit Mocks

#### 4. Neues Datenmodell
- Model in Models.swift definieren
- Codable conformance
- Parser-Mappings hinzufügen
- Beispiel-JSON dokumentieren

---

## 📏 Best Practices und Konventionen

### Code-Style

#### Naming Conventions
```swift
// Classes & Structs: PascalCase
class KickbaseManager { }
struct TransferRecommendation { }

// Properties & Methods: camelCase
var teamPlayers: [TeamPlayer]
func loadMarketPlayers() { }

// Constants: camelCase
let apiBaseURL = "https://api.kickbase.com"

// Enums: PascalCase (Cases: camelCase)
enum RiskLevel {
    case low
    case medium
    case high
}
```

#### Async/Await Best Practices
```swift
// ✅ RICHTIG
func loadData() async throws -> Data {
    let data = try await apiService.fetch()
    return data
}

// ❌ FALSCH (unnötiges withCheckedContinuation)
func loadData() async throws -> Data {
    return try await withCheckedThrowingContinuation { continuation in
        apiService.fetch { result in
            continuation.resume(with: result)
        }
    }
}
```

#### Error Handling
```swift
// ✅ RICHTIG: Spezifische Errors
enum APIError: Error {
    case invalidResponse
    case unauthorized
}

// ✅ RICHTIG: Try-Catch mit spezifischem Error-Handling
do {
    let data = try await apiService.fetch()
} catch APIError.unauthorized {
    // Handle auth error
} catch {
    // Handle generic error
}

// ❌ FALSCH: Ignore errors
try? apiService.fetch()
```

#### SwiftUI Best Practices
```swift
// ✅ RICHTIG: Extraktion in Sub-Views
struct PlayerListView: View {
    let players: [Player]
    
    var body: some View {
        List(players) { player in
            PlayerRow(player: player)
        }
    }
}

struct PlayerRow: View {
    let player: Player
    
    var body: some View {
        HStack {
            Text(player.name)
            Spacer()
            Text("\(player.points)")
        }
    }
}

// ❌ FALSCH: Alles in einer View
struct PlayerListView: View {
    let players: [Player]
    
    var body: some View {
        List(players) { player in
            HStack {
                Text(player.name)
                Spacer()
                Text("\(player.points)")
            }
        }
    }
}
```

### Performance-Optimierung

#### 1. Lazy Loading
```swift
// Listen nur laden, wenn sichtbar
.onAppear {
    Task {
        await kickbaseManager.loadTeamPlayers()
    }
}
```

#### 2. Image Caching
```swift
// ImageHelpers.swift nutzen für gecachte Bilder
AsyncCachedImage(url: player.profileBig)
```

#### 3. Debouncing
```swift
// Bei Search-Feldern
@State private var searchText = ""
.onChange(of: searchText) { newValue in
    // Debounce search
    Task {
        try? await Task.sleep(nanoseconds: 300_000_000) // 300ms
        await performSearch(newValue)
    }
}
```

### Security Best Practices

#### 1. Token Storage
```swift
// ✅ RICHTIG: Keychain für sensitive Daten
KeychainHelper.save(token, forKey: "auth_token")

// ❌ FALSCH: UserDefaults für Tokens
UserDefaults.standard.set(token, forKey: "auth_token")
```

#### 2. HTTPS Only
```swift
// Alle API-Calls über HTTPS
let baseURL = "https://api.kickbase.com"
```

#### 3. Input Validation
```swift
// Email & Passwort validieren
func isValidEmail(_ email: String) -> Bool {
    let emailRegex = "[A-Z0-9a-z._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,64}"
    return NSPredicate(format:"SELF MATCHES %@", emailRegex).evaluate(with: email)
}
```

### Accessibility

```swift
// Labels für Screen Reader
Image(systemName: "star")
    .accessibilityLabel("Favorit")

// Semantic Headers
Text("Transfer-Empfehlungen")
    .accessibilityAddTraits(.isHeader)
```

### Platform-Spezifische Anpassungen

```swift
#if os(iOS)
    // iOS-spezifischer Code
    TabView { }
#elseif os(macOS)
    // macOS-spezifischer Code
    NavigationSplitView { }
#endif
```

### Code-Kommentare

```swift
// ✅ RICHTIG: Dokumentation für komplexe Logik
/// Berechnet den Empfehlungs-Score basierend auf mehreren Faktoren.
/// 
/// - Parameters:
///   - player: Der zu bewertende Spieler
///   - teamContext: Kontext des aktuellen Teams
/// - Returns: Score zwischen 0 und 100
func calculateRecommendationScore(for player: MarketPlayer, teamContext: TeamContext) -> Double {
    // Complex logic...
}

// ❌ FALSCH: Offensichtliche Kommentare
// Increment counter
counter += 1
```

---

## 🔗 Wichtige Links & Ressourcen

- **Kickbase API Dokumentation**: Siehe `Kickbasehelper/Services/API_ENDPOINTS.md`
- **Skip Framework**: https://skip.tools (Cross-Platform Transpilation)
- **SwiftUI Dokumentation**: https://developer.apple.com/documentation/swiftui
- **ViewInspector**: https://github.com/nalexn/ViewInspector

---

## 📝 Changelog

Diese Dokumentation sollte bei größeren Architektur-Änderungen aktualisiert werden:

- **2024-02-17**: Initiale Version erstellt
- Features hinzufügen hier...

---

## 🤝 Beitragende

Wenn du Code zu diesem Projekt beiträgst:
1. ✅ Folge den Architektur-Pattern (MVVM)
2. ✅ Schreibe Tests für neue Features
3. ✅ Aktualisiere diese Dokumentation
4. ✅ Kommentiere komplexe Logik
5. ✅ Nutze async/await für asynchrone Operationen
6. ✅ Folge den Naming Conventions

---

**Letzte Aktualisierung**: 17.02.2024  
**Version**: 1.0.0
