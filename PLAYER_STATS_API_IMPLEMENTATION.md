# Implementierung: Echte Spieler-Statistiken aus der API

## Problem
Die Confidence-Berechnung nutzte geschätzte oder falsche Werte:
- Vorher wurde `marketPlayer.number` (Trikotnummer!) für gespielte Spiele verwendet
- Oder es wurde aus `totalPoints / averagePoints` berechnet (ungenau)
- Resultat: 0% Confidence bei allen Spielern oder unrealistische Werte

## Lösung
Verwendung der echten Spieler-Statistik-Felder aus dem Player Details API-Endpunkt.

## API-Felder

**GET** `/v4/leagues/{leagueId}/players/{playerId}` liefert:

### Response enthält:
```json
{
  "smdc": 10,    // Season Matchday Count - Aktueller Spieltag
  "ismc": 8,     // In-Squad Match Count - Spiele auf dem Platz (Startelf + Einwechslung)
  "smd": 6,      // Starting Eleven Matchday - Spiele in der Startelf
  ...
}
```

### Bedeutung:
- **smdc**: Wie viele Spieltage sind bisher vergangen (z.B. 10)
- **ismc**: Wie oft stand der Spieler auf dem Platz (Startelf + Einwechslung, z.B. 8)
- **smd**: Wie oft stand er in der Startelf (z.B. 6)

## Implementierung

### 1. KickbasePlayerService.swift - Neue Funktion

```swift
/// Holt Spieltag-Informationen von einem Spieler
func getMatchDayStats(leagueId: String, playerId: String) async -> (smdc: Int, ismc: Int, smd: Int)? {
    do {
        let json = try await apiService.getPlayerDetails(leagueId: leagueId, playerId: playerId)
        
        guard let smdc = json["smdc"] as? Int else { return nil }
        
        let ismc = json["ismc"] as? Int ?? 0  // Spiele auf dem Platz
        let smd = json["smd"] as? Int ?? 0    // Spiele in Startelf
        
        return (smdc: smdc, ismc: ismc, smd: smd)
    } catch {
        return nil
    }
}
```

### 2. PlayerRecommendationService.swift - Cache-Struktur

```swift
private struct PlayerMatchStats {
    let smdc: Int      // Aktueller Spieltag
    let ismc: Int      // Spiele auf dem Platz (Startelf + Einwechslung)
    let smd: Int       // Spiele in Startelf
}

private var playerStatsCache: [String: PlayerMatchStats] = [:]
```

### 3. Zwei-Stufen-Ansatz

**Stufe 1: Schnelle Filterung**
- Alle Spieler werden mit geschätzten Stats gefiltert
- Confidence = 0.5 (niedrig, da geschätzt)
- Ermöglicht schnelles Pre-Filtering

**Stufe 2: Detaillierte Stats für Top-Kandidaten**
- Top 50 Spieler nach Score
- Lade echte Stats von der API (parallel, max 10 gleichzeitig)
- Neu-Berechnung der Confidence mit echten Werten
- Finale Sortierung nach aktualisiertem Score

### 4. Confidence-Berechnung mit echten Stats

```swift
func calculateSeasonProjectionWithStats(_ marketPlayer: MarketPlayer, stats: PlayerMatchStats) -> SeasonProjection {
    let gamesPlayed = stats.ismc  // Echte Anzahl Spiele auf dem Platz
    let currentMatchDay = stats.smdc  // Echter aktueller Spieltag
    
    if currentMatchDay > 0 && gamesPlayed > 0 {
        let playedRatio = Double(gamesPlayed) / Double(currentMatchDay)
        
        // Bonus für Stammkräfte (smd ~ ismc = meist Starter)
        let starterBonus = Double(stats.smd) / max(Double(stats.ismc), 1.0)
        
        // Confidence = Spielbeteiligung * (70% Base + 30% Starter-Bonus)
        confidence = min(playedRatio * (0.7 + starterBonus * 0.3), 1.0)
    }
}
```

## Beispiel-Berechnung

### Beispiel 1: Stammkraft
**Spieler:** Thomas Müller
- **smdc**: 10 (10 Spieltage vergangen)
- **ismc**: 10 (stand bei allen 10 Spielen auf dem Platz)
- **smd**: 10 (stand bei allen 10 in der Startelf)

**Berechnung:**
- playedRatio = 10/10 = 1.0 (100% Spielbeteiligung)
- starterBonus = 10/10 = 1.0 (100% Starter)
- confidence = 1.0 * (0.7 + 1.0 * 0.3) = 1.0 * 1.0 = **100%** ✅

### Beispiel 2: Joker
**Spieler:** Leroy Sané
- **smdc**: 10 (10 Spieltage vergangen)
- **ismc**: 8 (stand bei 8 Spielen auf dem Platz)
- **smd**: 4 (nur 4x in der Startelf, 4x eingewechselt)

**Berechnung:**
- playedRatio = 8/10 = 0.8 (80% Spielbeteiligung)
- starterBonus = 4/8 = 0.5 (50% Starter)
- confidence = 0.8 * (0.7 + 0.5 * 0.3) = 0.8 * 0.85 = **68%** ✅

### Beispiel 3: Bankdrücker
**Spieler:** Ersatzspieler
- **smdc**: 10 (10 Spieltage vergangen)
- **ismc**: 3 (nur 3x auf dem Platz)
- **smd**: 1 (nur 1x in der Startelf)

**Berechnung:**
- playedRatio = 3/10 = 0.3 (30% Spielbeteiligung)
- starterBonus = 1/3 = 0.33 (33% Starter)
- confidence = 0.3 * (0.7 + 0.33 * 0.3) = 0.3 * 0.8 = **24%** ✅

## Performance-Optimierung

### Paralleles Laden
```swift
// Lade Stats parallel, max 10 gleichzeitig
await withTaskGroup(of: (String, PlayerMatchStats?).self) { group in
    for recommendation in batch {
        group.addTask {
            let stats = await self.kickbaseManager.authenticatedPlayerService.getMatchDayStats(...)
            ...
        }
    }
}
```

### Batching
- Nur Top 50 Spieler bekommen detaillierte Stats
- In Batches von 10 parallel geladen
- Reduziert API-Calls von potentiell 100+ auf ~5

### Caching
- Stats werden in `playerStatsCache` gespeichert
- Bei erneutem Laden werden gecachte Werte wiederverwendet
- Cache wird zusammen mit Empfehlungen (5 Min) geleert

## Vorteile

✅ **100% Genau**: Verwendet echte API-Daten statt Schätzungen  
✅ **Stammkraft-Bonus**: Berücksichtigt, ob Spieler regelmäßig in Startelf steht  
✅ **Performance**: Nur Top-Kandidaten bekommen detaillierte Stats  
✅ **Parallel**: Stats werden parallel geladen (max 10 gleichzeitig)  
✅ **Cached**: Wiederverwendung von bereits geladenen Stats  
✅ **Fallback**: Funktioniertauch ohne Stats (niedrige Confidence 0.5)  

## Console Output

### Erfolgreicher Abruf:
```
🎯 Generating transfer recommendations for league: Meine Liga
✅ Current matchday from API: 10
✅ Loaded 15 team players and 142 market players in parallel
📊 Pre-filtered from 142 to 89 quality players
📦 Processed batch 1: 18 recommendations added
📦 Processed batch 2: 15 recommendations added
✅ Generated 33 recommendations
📊 Loading detailed stats for top 33 players...
📊 Stats for player 123: matchday=10, gamesPlayed=8, gamesStarted=6
📊 Stats for player 456: matchday=10, gamesPlayed=10, gamesStarted=10
...
✅ Loaded stats for 33 players total
🎯 Confidence for Thomas Müller: 10 games / 10 matchdays (started: 10) = 100.0%
🎯 Confidence for Leroy Sané: 8 games / 10 matchdays (started: 4) = 68.0%
✅ Final 20 recommendations ready
```

### Bei Cache-Hit:
```
🎯 Generating transfer recommendations for league: Meine Liga
✅ Returning cached recommendations (20 players)
```

## Testing

1. Öffne die App und navigiere zu "Verkaufsempfehlungen"
2. Wähle eine Liga aus
3. Warte auf das Laden (erste Mal etwas länger wegen Stats-API-Calls)
4. Schau in die Xcode-Konsole für Debug-Ausgaben
5. Prüfe die Confidence-Werte in der UI - sollten jetzt realistisch sein!

## Nächste Schritte

- ✅ Implementiert und kompiliert
- ✅ Echte API-Felder verwendet (smdc, ismc, smd)
- ✅ Zwei-Stufen-Ansatz für Performance
- ✅ Paralleles Laden mit Batching
- ✅ Caching-Mechanismus
- ✅ Starter-Bonus in Confidence-Berechnung
- ✅ Ausführliches Logging
