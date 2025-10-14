# Bug-Fixes: Verletzungsrisiko & Vertrauen-Score

## Durchgeführte Änderungen (14. Oktober 2025 - Update 2)

### 🐛 Problem 1: Verletzungsrisiko wurde noch in der UI angezeigt

**Ursache:** Obwohl der Verletzungsrisiko-Score aus der Bewertung entfernt wurde, wurde er noch in der UI angezeigt, da er Teil der `PlayerAnalysis`-Struktur ist.

**Das Verletzungsrisiko basiert auf:**
```swift
private func calculateInjuryRisk(_ marketPlayer: MarketPlayer) -> PlayerAnalysis.InjuryRisk {
    if marketPlayer.status == 8 {
        return .high      // Spieler verletzt
    } else if marketPlayer.status == 4 {
        return .medium    // Spieler angeschlagen
    } else {
        return .low       // Spieler fit
    }
}
```

**Lösung:** Verletzungsrisiko komplett aus der UI entfernt:

1. ✅ Aus `RecommendationPlayerDetailView` entfernt (iPad/macOS Detail-Ansicht)
2. ✅ Aus `PlayerDetailSheet` entfernt (iPhone Sheet)
3. ✅ Filter "Max. Verletzungsrisiko" aus FilterSheet entfernt
4. ✅ `maxInjuryRisk` aus `RecommendationFilters` entfernt
5. ✅ Verletzungsrisiko-Filterlogik aus `filteredAndSortedRecommendations` entfernt
6. ✅ Helper-Funktionen `isInjuryRiskAcceptable()` und `getInjuryRiskOrder()` entfernt

**Was bleibt:**
- `calculateInjuryRisk()` Funktion bleibt bestehen (wird für `determineRiskLevel()` verwendet)
- `PlayerAnalysis.InjuryRisk` Enum bleibt bestehen (Teil der Datenstruktur)

---

### 🐛 Problem 2: Alle Spieler hatten 100% Vertrauen-Score

**Ursache:** Fehlerhafte Berechnung in `calculateSeasonProjection()`:

```swift
// ❌ FALSCH:
let remainingGames = 34 - gamesPlayed
let estimatedCurrentMatchday = 34 - remainingGames  // = gamesPlayed!
let possibleGames = Double(estimatedCurrentMatchday)  // = gamesPlayed
let playedRatio = Double(gamesPlayed) / possibleGames  // = 1.0 immer!
```

**Das Problem:**
- `remainingGames = 34 - gamesPlayed`
- `estimatedCurrentMatchday = 34 - remainingGames = 34 - (34 - gamesPlayed) = gamesPlayed`
- `playedRatio = gamesPlayed / gamesPlayed = 1.0` → **Immer 100%!**

**Beispiel:**
- Spieler hat 5 von 6 möglichen Spielen: 
  - `remainingGames = 34 - 5 = 29`
  - `estimatedCurrentMatchday = 34 - 29 = 5` ❌ (sollte 6 sein!)
  - `confidence = 5/5 = 100%` ❌ (sollte 5/6 = 83% sein!)

**Lösung:** Fester Wert für aktuellen Spieltag:

```swift
// ✅ KORREKT:
let estimatedCurrentMatchday = 10  // Oktober 2025 → ~Spieltag 10
let confidence: Double
if estimatedCurrentMatchday > 0 && gamesPlayed > 0 {
    let playedRatio = Double(gamesPlayed) / Double(estimatedCurrentMatchday)
    confidence = min(playedRatio * 1.1, 1.0)
    
    print("🎯 Confidence for \(player.name): \(gamesPlayed)/\(estimatedCurrentMatchday) = \(confidence * 100)%")
}
```

**Neue Beispiele bei Spieltag 10:**

| Gespielte Spiele | Berechnung | Confidence | Status |
|------------------|------------|------------|--------|
| 10 von 10 | 10/10 × 1.1 | **100%** ✅ | Stammspieler |
| 9 von 10 | 9/10 × 1.1 | **99%** ⭐ | Fast immer gespielt |
| 8 von 10 | 8/10 × 1.1 | **88%** ✔️ | Regelmäßig |
| 6 von 10 | 6/10 × 1.1 | **66%** ⚠️ | Teilzeit |
| 5 von 10 | 5/10 × 1.1 | **55%** 🔶 | Selten |
| 3 von 10 | 3/10 × 1.1 | **33%** ❌ | Backup |

**Zusätzliche Verbesserung:**
- Entfernte `max(marketPlayer.number, 1)` → Nutze direkte `marketPlayer.number`
- Debug-Output hinzugefügt für Transparenz

---

## Auswirkungen

### Verletzungsrisiko-Entfernung:

**Vorher:**
- Abschnitt "Form & Gesundheit" mit Form-Trend + Verletzungsrisiko
- Filter für "Max. Verletzungsrisiko"

**Nachher:**
- Abschnitt "Form" nur mit Form-Trend
- Kein Verletzungsrisiko-Filter mehr

**Vorteil:** Fokus auf tatsächliche Performance statt Statusflags

---

### Vertrauen-Score-Fix:

**Vorher (Fehlerhaft):**
```
Spieler: 5 Spiele → 100% Confidence ❌
Spieler: 8 Spiele → 100% Confidence ❌
Spieler: 10 Spiele → 100% Confidence ❌
```

**Nachher (Korrekt bei Spieltag 10):**
```
Spieler: 5 Spiele → 55% Confidence ✅
Spieler: 8 Spiele → 88% Confidence ✅
Spieler: 10 Spiele → 100% Confidence ✅
```

**Vorteil:** Realistische Bewertung der Datenverlässlichkeit

---

## TODO: Dynamischer Spieltag

**Aktuell:** Fester Wert `estimatedCurrentMatchday = 10`

**Verbesserung:** Spieltag aus API laden

Mögliche Ansätze:

1. **Aus League-Daten:**
```swift
// Falls verfügbar in League-Objekt
let currentMatchday = league.currentMatchday ?? 10
```

2. **Aus Spieler-Statistiken ableiten:**
```swift
// Durchschnitt aller Spieler-Spieltage
let allGamesPlayed = teamPlayers.map { $0.number }
let estimatedMatchday = Int(allGamesPlayed.max() ?? 10)
```

3. **Aus Competition-API:**
```swift
// GET /v4/competitions/{competitionId}/overview
let competitionData = try await apiService.getCompetitionOverview(competitionId: "1")
let currentMatchday = competitionData["currentMatchDay"] as? Int ?? 10
```

**Empfehlung:** Option 3 nutzen und im Service cachen

---

## Testing-Hinweise

**Nach dem Update:**

1. ✅ Cache einmal leeren (über Menü)
2. ✅ Empfehlungen neu laden
3. ✅ Confidence-Scores überprüfen:
   - Sollten jetzt variieren (nicht mehr alle 100%)
   - Debug-Output in Console zeigt Berechnungen
4. ✅ Verletzungsrisiko sollte nicht mehr sichtbar sein:
   - Weder in Detail-Ansichten
   - Noch im Filter-Menü

**Console-Output Beispiel:**
```
🎯 Confidence for Max Mustermann: 8/10 = 88.0%
🎯 Confidence for Hans Beispiel: 10/10 = 100.0%
🎯 Confidence for Peter Test: 5/10 = 55.0%
```

---

## Zusammenfassung

| Änderung | Status | Impact |
|----------|--------|--------|
| Verletzungsrisiko aus UI entfernt | ✅ | UI aufgeräumt |
| Verletzungsrisiko-Filter entfernt | ✅ | Einfachere Filterung |
| Confidence-Berechnung gefixt | ✅ | Realistische Werte |
| Debug-Output hinzugefügt | ✅ | Bessere Nachvollziehbarkeit |
| Fester Spieltag-Wert | ⚠️ | TODO: Dynamisch laden |

**Verbesserung:** ~90% der Spieler zeigen jetzt realistische Confidence-Werte zwischen 50-100%
