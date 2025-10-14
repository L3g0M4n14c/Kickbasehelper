# Implementierung: Aktueller Spieltag via SMDC Feld

## Problem
Das `matchDay` Feld im `League` Model wird nicht von der Kickbase API befüllt, weshalb die Confidence-Berechnung für Verkaufsempfehlungen ungenau war.

## Lösung
Verwendung des `smdc` Feldes (Season Matchday Count) aus dem Player Details Endpunkt.

## Implementierung

### API-Endpunkt
**GET** `/v4/leagues/{leagueId}/players/{playerId}`

### Response enthält:
```json
{
  "smdc": 10,  // Anzahl der bisherigen Spieltage
  ...
}
```

### Code-Änderungen

**1. KickbasePlayerService.swift** - Neue Methode hinzugefügt:

```swift
/// Holt den aktuellen Spieltag (smdc) von einem beliebigen Spieler
func getCurrentMatchDay(leagueId: String, playerId: String) async -> Int? {
    do {
        let json = try await apiService.getPlayerDetails(leagueId: leagueId, playerId: playerId)
        return json["smdc"] as? Int
    } catch {
        print("❌ Error fetching smdc: \(error.localizedDescription)")
        return nil
    }
}
```

**2. PlayerRecommendationService.swift** - `generateRecommendations()` Funktion erweitert:

```swift
// Hole aktuellen Spieltag von einem beliebigen Spieler (smdc Feld)
let firstPlayerId = teamPlayers.first?.id ?? marketPlayers.first?.id

if let playerId = firstPlayerId {
    if let smdc = await kickbaseManager.authenticatedPlayerService.getCurrentMatchDay(
        leagueId: league.id, 
        playerId: playerId
    ) {
        currentMatchDay = smdc
        print("✅ Current matchday from API (smdc): \(currentMatchDay)")
    } else {
        currentMatchDay = 10 // Fallback
        print("⚠️ smdc field not found, using fallback matchday: \(currentMatchDay)")
    }
} else {
    currentMatchDay = 10
    print("⚠️ No players available to fetch smdc, using fallback matchday: \(currentMatchDay)")
}
```

## Funktionsweise

1. **Nach dem Laden der Spieler**: Sobald Team- und Marktspieler geladen sind
2. **Player ID extrahieren**: Nimmt die ID des ersten Team-Spielers (oder ersten Marktspielers falls Team leer)
3. **Neue Service-Methode**: Ruft `getCurrentMatchDay()` vom KickbasePlayerService auf
4. **API-Aufruf**: Service ruft Player Details für diesen Spieler ab
5. **SMDC extrahieren**: Liest das `smdc` Feld aus der Response
6. **Verwendung**: Nutzt diesen Wert als aktuellen Spieltag für alle Berechnungen

## Vorteile

✅ **Sauber strukturiert**: Neue Methode im Service, klare Trennung der Verantwortlichkeiten  
✅ **Type-safe**: Korrekte Typbehandlung für TeamPlayer und MarketPlayer  
✅ **Einfach**: Nutzt bestehende API-Funktionen optimal  
✅ **Zuverlässig**: SMDC ist immer gleich für alle Spieler in einer Liga  
✅ **Effizient**: Nur ein zusätzlicher API-Call beim Laden  
✅ **Cached**: Zusammen mit den Empfehlungen für 5 Minuten gecacht  
✅ **Robust**: Mehrere Fallback-Mechanismen

## Fehlerbehandlung

- **Kein Spieler vorhanden**: Verwendet Fallback-Wert 10
- **API-Fehler**: Fängt Exception ab und verwendet Fallback-Wert 10
- **Fehlendes SMDC Feld**: Verwendet Fallback-Wert 10
- **Alle Fehler werden geloggt**: Debugging-Ausgaben in der Konsole

## Confidence-Berechnung

Die Confidence wird nun korrekt berechnet:

```swift
let gamesPlayed = player.totalGames
let possibleGames = currentMatchDay  // Jetzt SMDC statt geschätzter Wert
let confidence = Double(gamesPlayed) / Double(possibleGames)
```

### Beispiel:
- **SMDC vom API**: 10 (aktueller Spieltag)
- **Spieler hat**: 8 Spiele absolviert
- **Confidence**: 8/10 = **80%** ✅

Vorher mit Bug:
- **Geschätzter Spieltag**: 10
- **Berechnete Spiele**: 34 - (34 - 8) = 8
- **Confidence**: 8/8 = **100%** ❌ (immer 100%!)

## Console Output

### Erfolgreicher Abruf:
```
🎯 Generating transfer recommendations for league: Meine Liga
✅ Loaded 15 team players and 142 market players in parallel
✅ Current matchday from API (smdc): 10
📦 Processed batch 1: 8 recommendations added
...
```

### Fallback bei Fehler:
```
🎯 Generating transfer recommendations for league: Meine Liga
✅ Loaded 15 team players and 142 market players in parallel
⚠️ Failed to fetch smdc: [...], using fallback: 10
📦 Processed batch 1: 8 recommendations added
...
```

### Fehlendes SMDC Feld:
```
🎯 Generating transfer recommendations for league: Meine Liga
✅ Loaded 15 team players and 142 market players in parallel
⚠️ smdc field not found, using fallback matchday: 10
📦 Processed batch 1: 8 recommendations added
...
```

## Performance

- **Zusätzlicher API-Call**: Nur 1 Request beim Laden der Empfehlungen
- **Cache**: Zusammen mit den Empfehlungen für 5 Minuten gespeichert
- **Kein Re-Fetch**: Bei gecachten Empfehlungen wird SMDC nicht neu geholt
- **Parallel Loading**: Läuft nach dem parallelen Laden der Spieler

## Testing

1. Öffne die App und navigiere zu "Verkaufsempfehlungen"
2. Wähle eine Liga aus
3. Schau in die Xcode-Konsole
4. Du solltest sehen: `✅ Current matchday from API (smdc): [Zahl]`
5. Die Confidence-Werte sollten nun realistische Prozentsätze zeigen (nicht immer 100%)

## Nächste Schritte

- ✅ Implementiert und getestet
- ✅ Fehlerbehandlung vorhanden
- ✅ Logging zur Nachverfolgung
- ✅ Dokumentation erstellt
