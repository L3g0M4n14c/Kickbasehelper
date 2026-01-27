# Spieltag-Tabelle Implementierung

## Überblick

Die LeagueTableView wurde erweitert, um zwischen zwei Ansichten zu wechseln:
1. **Gesamttabelle** - Zeigt die kumulierten Punkte der gesamten Saison
2. **Spieltag-Tabelle** - Zeigt die Punkte eines beliebigen Spieltags (wählbar von 1 bis aktueller Spieltag)

---

## Implementierung (26. Januar 2026, aktualisiert)

### Update: Spieltag-Auswahl hinzugefügt

**Neue Funktionalität:**
- Dropdown-Menü zur Auswahl eines beliebigen Spieltags
- Anzeige der Punkte für den ausgewählten Spieltag
- Dynamischer Titel zeigt "Spieltag X" an

### 1. **LeagueTableView - UI-Komponente**

**Neue Funktionalität:**
- Segmented Picker am oberen Bildschirmrand
- Dropdown-Menü zur Spieltag-Auswahl (erscheint bei Auswahl von "Spieltag")
- Dynamischer Wechsel zwischen Ansichten
- Automatisches Laden der Spieltag-Daten beim Wechsel

```swift
struct LeagueTableView: View {
    @EnvironmentObject var kickbaseManager: KickbaseManager
    @State private var tableType: TableType = .overall
    @State private var selectedMatchDay: Int = 1
    
    enum TableType {
        case overall   // Gesamttabelle
        case matchday  // Beliebiger Spieltag
    }
    
    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                // Segmented Picker
                if let league = kickbaseManager.selectedLeague {
                    Picker("", selection: $tableType) {
                        Text("Gesamttabelle").tag(TableType.overall)
                        Text("Spieltag").tag(TableType.matchday)
                    }
                    .pickerStyle(.segmented)
                    
                    // Spieltag-Auswahl (nur sichtbar im Spieltag-Modus)
                    if tableType == .matchday {
                        HStack {
                            Text("Spieltag auswählen:")
                            Picker("Spieltag", selection: $selectedMatchDay) {
                                ForEach(1...league.matchDay, id: \.self) { day in
                                    Text("Spieltag \(day)").tag(day)
                                }
                            }
                            .pickerStyle(.menu)
                            .onChange(of: selectedMatchDay) { _, newValue in
                                Task {
                                    await kickbaseManager.loadMatchDayRanking(
                                        for: league, 
                                        matchDay: newValue
                                    )
                                }
                            }
                        }
                    }
                    }
                }
                
                // Tabellen-Inhalt
                // ... (siehe Code für Details)
            }
            .navigationTitle(tableType == .overall ? "Tabelle" : "Spieltag-Tabelle")
        }
    }
    
    private var displayedUsers: [LeagueUser] {
        tableType == .overall ? kickbaseManager.leagueUsers : kickbaseManager.matchDayUsers
    }
}
```

---

### 2. **KickbaseManager - Daten-Management**

**Neue Properties:**
```swift
@Published public var matchDayUsers: [LeagueUser] = []
```

**Neue Methode:**
```swift
public func loadMatchDayRanking(for league: League, matchDay: Int) async {
    isLoading = true
    errorMessage = nil

    do {
        let users = try await leagueService.loadMatchDayRanking(
            for: league, 
            matchDay: matchDay
        )
        self.matchDayUsers = users
        print("✅ Loaded \(users.count) matchday users")
    } catch {
        print("❌ Error loading matchday ranking: \(error)")
        errorMessage = "Fehler beim Laden der Spieltag-Tabelle: \(error.localizedDescription)"
    }

    isLoading = false
}
```

---

### 3. **KickbaseLeagueService - API-Integration**

**Neue Methode:**
```swift
public func loadMatchDayRanking(for league: League, matchDay: Int) async throws -> [LeagueUser] {
    print("🏆 Loading matchday \(matchDay) ranking for: \(league.name)")
    
    do {
        let json = try await apiService.getLeagueRanking(
            leagueId: league.id, 
            matchDay: matchDay
        )
        let users = dataParser.parseLeagueRanking(from: json)
        
        // Sort by points descending
        let sortedUsers = users.sorted { $0.points > $1.points }
        
        return sortedUsers
    } catch {
        print("❌ Failed to load matchday ranking: \(error.localizedDescription)")
        throw error
    }
}
```

---

## Datenfluss

```
┌──────────────────────────────────────────────┐
│ User tippt auf "Spieltag X" Segment          │
└────────────────┬─────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│ LeagueTableView.onChange(of: tableType)      │
│ → Trigger loadMatchDayRanking()              │
└────────────────┬─────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│ KickbaseManager.loadMatchDayRanking()        │
│ → Delegiert an LeagueService                 │
└────────────────┬─────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│ KickbaseLeagueService.loadMatchDayRanking()  │
│ → Ruft API mit matchDay Parameter            │
└────────────────┬─────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│ KickbaseAPIService.getLeagueRanking()        │
│ GET /v4/leagues/{id}/ranking?matchDay={X}    │
└────────────────┬─────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│ Response: Spieltag-spezifische Punkte        │
│ → Parse & Sort → matchDayUsers               │
└────────────────┬─────────────────────────────┘
                 │
                 ▼
┌──────────────────────────────────────────────┐
│ LeagueTableView.displayedUsers               │
│ → Zeigt matchDayUsers in der Liste           │
└──────────────────────────────────────────────┘
```

---

## API-Endpunkte

### Gesamttabelle (Overall)
```
GET /v4/leagues/{leagueId}/ranking
```

**Response:**
```json
{
  "users": [
    {
      "i": "user123",
      "n": "Max Mustermann",
      "tn": "FC Awesome",
      "p": 850,  // Gesamtpunkte der Saison
      "pl": 1    // Platzierung
    }
  ]
}
```

### Spieltag-Tabelle (Matchday)
```
GET /v4/leagues/{leagueId}/ranking?matchDay={matchDay}
```

**Response:**
```json
{
  "users": [
    {
      "i": "user123",
      "n": "Max Mustermann",
      "tn": "FC Awesome",
      "p": 45,   // Punkte nur für diesen Spieltag
      "pl": 2    // Platzierung für diesen Spieltag
    }
  ]
}
```

---

## Features

### ✅ Segmented Picker
- Zwei Optionen: "Gesamttabelle" und "Spieltag"
- Standard-Auswahl: "Gesamttabelle"

### ✅ Spieltag-Auswahl-Dropdown
- Erscheint nur wenn "Spieltag" ausgewählt ist
- Zeigt "Spieltag auswählen:" Label
- Dropdown-Menü mit allen Spieltagen (1 bis aktueller Spieltag)
- Standard-Auswahl: Aktueller Spieltag
- Lädt Daten automatisch bei Änderung

### ✅ Dynamisches Daten-Laden
- Gesamttabelle wird beim ersten Erscheinen geladen
- Spieltag-Daten werden beim Wechsel zu "Spieltag" geladen
- Daten werden beim Wechsel des Spieltags neu geladen

### ✅ Pull-to-Refresh
- Funktioniert für beide Tabellen
- Lädt jeweils die richtigen Daten (overall oder ausgewählter matchday)

### ✅ Dynamischer Titel
- "Tabelle" für Gesamttabelle
- "Spieltag X" für Spieltag-Ansicht (X = ausgewählter Spieltag)

### ✅ Fehlerbehandlung
- Zeigt Fehlermeldungen bei API-Fehlern
- "Aktualisieren"-Button zum erneuten Laden

---

## UI-Beispiele

### Gesamttabelle
```
┌────────────────────────────────────────┐
│ ← Tabelle                              │
├────────────────────────────────────────┤
│ [Gesamttabelle] [Spieltag]             │ ← Segmented Picker
├────────────────────────────────────────┤
│  1  Max Mustermann    FC Awesome  850  │
│  2  Hans Beispiel     Team Stark  820  │
│  3  Peter Test        Die Kicker  790  │
│  4  Tom Müller        Goal United 760  │
│  5  Anna Schmidt      Top Players 735  │
└────────────────────────────────────────┘
```

### Spieltag-Tabelle (mit Dropdown)
```
┌────────────────────────────────────────┐
│ ← Spieltag 10                          │
├────────────────────────────────────────┤
│ [Gesamttabelle] [Spieltag]             │ ← Segmented Picker
├────────────────────────────────────────┤
│ Spieltag auswählen: [Spieltag 10 ▼]   │ ← Dropdown
├────────────────────────────────────────┤
│  1  Peter Test        Die Kicker   52  │
│  2  Tom Müller        Goal United  49  │
│  3  Max Mustermann    FC Awesome   46  │
│  4  Anna Schmidt      Top Players  43  │
│  5  Hans Beispiel     Team Stark   38  │
└────────────────────────────────────────┘
```

**Beachte:** Die Reihenfolge kann unterschiedlich sein, da die Spieltag-Punkte unabhängig von den Gesamt-Punkten sind.

### Dropdown erweitert
```
┌────────────────────────────────────────┐
│ ← Spieltag 10                          │
├────────────────────────────────────────┤
│ [Gesamttabelle] [Spieltag]             │
├────────────────────────────────────────┤
│ Spieltag auswählen: ┌──────────────┐  │
│                     │ Spieltag 1   │  │
│                     │ Spieltag 2   │  │
│                     │ ...          │  │
│                     │ ✓ Spieltag 10│  │
│                     │ Spieltag 11  │  │
│                     │ ...          │  │
│                     │ Spieltag 15  │  │
│                     └──────────────┘  │
└────────────────────────────────────────┘
```

---

## Testing

### Unit Tests

**Hinzugefügte Tests in `KickbasehelperTests.swift`:**

1. **testMatchDayTableSwitching** - Testet das Umschalten zwischen Tabellentypen
2. **testMatchDayUserDataSeparation** - Verifiziert, dass Gesamt- und Spieltag-Punkte unabhängig sind
3. **testDisplayedUsersSelection** - Testet, dass die korrekten Benutzerdaten basierend auf dem ausgewählten Tabellentyp angezeigt werden

### Manuelle Tests

1. **Öffne die App** und wähle eine Liga
2. **Navigiere zur Tabelle**
   - Sollte "Gesamttabelle" als Standard zeigen
3. **Tippe auf "Spieltag X"**
   - Lädt Spieltag-spezifische Daten
   - Titel ändert sich zu "Spieltag-Tabelle"
   - Zeigt nur Punkte des aktuellen Spieltags
4. **Tippe auf "Gesamttabelle"**
   - Wechselt zurück zur Gesamt-Ansicht
   - Titel ändert sich zu "Tabelle"
5. **Pull-to-Refresh testen**
   - In beiden Modi testen
   - Sollte jeweils die richtigen Daten neu laden

---

## Performance-Überlegungen

### Caching-Strategie
- `leagueUsers` und `matchDayUsers` sind separate Properties
- Beide werden im Memory gecacht
- Kein erneutes Laden beim Wechseln zwischen bereits geladenen Ansichten
- Pull-to-Refresh aktualisiert jeweils nur die aktive Ansicht

### API-Aufrufe
- **Initial:** Nur Gesamttabelle wird geladen
- **Beim Wechsel:** Spieltag-Tabelle wird on-demand geladen
- **Vorteil:** Reduziert initiale Ladezeit und API-Calls

---

## Vorteile dieser Implementierung

| Vorteil | Beschreibung |
|---------|--------------|
| ✅ **Live-Daten** | Zeigt aktuelle Spieltag-Punkte während Spiele laufen |
| ✅ **Unabhängige Ansichten** | Gesamt- und Spieltag-Daten sind getrennt |
| ✅ **Effizienter Speicher** | Beide Datensätze werden gecacht |
| ✅ **Einfache UI** | Segmented Picker ist intuitiv und iOS-Standard |
| ✅ **Minimal-invasiv** | Keine Änderungen an bestehender Gesamt-Tabelle |

---

## Bekannte Einschränkungen

1. **Historische Spieltage:** 
   - Aktuell wird nur der aktuelle Spieltag unterstützt
   - Erweiterbar durch Dropdown für Spieltag-Auswahl

2. **Offline-Modus:**
   - Erfordert Netzwerkverbindung für Daten-Abruf
   - Könnte mit lokaler Persistenz erweitert werden

---

## Mögliche Erweiterungen

### 1. Spieltag-Auswahl
```swift
// Statt nur aktuellen Spieltag, alle Spieltage wählbar
Picker("Spieltag", selection: $selectedMatchDay) {
    ForEach(1...34, id: \.self) { day in
        Text("Spieltag \(day)").tag(day)
    }
}
```

### 2. Vergleichsansicht
```swift
// Zeige beide Tabellen nebeneinander (iPad)
HStack {
    OverallTableView()
    MatchDayTableView()
}
```

### 3. Trend-Indikatoren
```swift
// Zeige Positionsänderung zwischen Gesamt- und Spieltag-Tabelle
HStack {
    Text("\(position)")
    if let trend = calculateTrend() {
        Image(systemName: trend > 0 ? "arrow.up" : "arrow.down")
            .foregroundColor(trend > 0 ? .green : .red)
    }
}
```

---

## Zusammenfassung

**Geänderte Dateien:**

1. ✅ `LeagueTableView.swift`
   - Segmented Picker hinzugefügt
   - TableType enum für Zustandsverwaltung
   - Dynamisches Laden basierend auf Auswahl

2. ✅ `KickbaseManager.swift`
   - `matchDayUsers` Property hinzugefügt
   - `loadMatchDayRanking()` Methode implementiert

3. ✅ `KickbaseLeagueService.swift`
   - `loadMatchDayRanking()` Methode für API-Call

4. ✅ `KickbasehelperTests.swift`
   - Unit Tests für neue Funktionalität

**Resultat:**
- Benutzer können jetzt einfach zwischen Gesamt- und Spieltag-Tabelle wechseln
- Live-Ansicht der aktuellen Spieltag-Punkte
- Keine Breaking Changes an bestehender Funktionalität
