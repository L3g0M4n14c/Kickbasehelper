# Score-Anpassungen: Transferempfehlungen

## Durchgeführte Änderungen (14. Oktober 2025)

### 1. ❌ Verletzungsrisiko-Score entfernt

**Grund:** Der Verletzungsrisiko-Score hat in der Bewertung keinen Mehrwert gebracht.

**Entfernte Komponenten:**

#### Aus `calculateRecommendationScore()`:
```swift
// ENTFERNT:
// 6. Verletzungsrisiko (verschärft)
switch analysis.injuryRisk {
case .low:
    score += 1.5
case .medium:
    score -= 0.5
case .high:
    score -= 3.0
}
```

#### Aus `generateReasons()`:
```swift
// ENTFERNT:
// Injury Risk Reasoning
switch analysis.injuryRisk {
case .high:
    reasons.append(RecommendationReason(
        type: .injury,
        description: "⚠️ Verletzungsrisiko beachten",
        impact: -5.0
    ))
case .medium:
    reasons.append(RecommendationReason(
        type: .injury,
        description: "Mittleres Verletzungsrisiko",
        impact: -2.0
    ))
case .low:
    break
}
```

**Hinweis:** Die `calculateInjuryRisk()` und `determineRiskLevel()` Methoden bleiben bestehen, da sie noch für andere Zwecke verwendet werden (z.B. RiskLevel-Badge in der UI).

---

### 2. ✅ Vertrauen-Score verbessert

**Problem:** Der alte Score berücksichtigte nur absolute Spielzahlen (≥10 Spiele = 1.0 Confidence), was in frühen Phasen der Saison unrealistisch war.

**Neue Berechnung:**

```swift
// VORHER:
confidence = min(Double(gamesPlayed) / 10.0, 1.0)

// NACHHER:
let estimatedCurrentMatchday = 34 - remainingGames
let possibleGames = Double(estimatedCurrentMatchday)

if possibleGames > 0 {
    let playedRatio = Double(gamesPlayed) / possibleGames
    confidence = min(playedRatio * 1.1, 1.0)
} else {
    confidence = 0.0
}
```

**Wie es funktioniert:**

1. **Geschätzter aktueller Spieltag berechnen:**
   - Beispiel: Spieler hat 8 Spiele, kann noch 26 spielen
   - Aktueller Spieltag = 34 - 26 = 8

2. **Verhältnis berechnen:**
   - Gespielte Spiele / Mögliche Spiele
   - Beispiel: 7 von 8 Spielen = 7/8 = 0.875

3. **Bonus-Faktor anwenden:**
   - Multiplikation mit 1.1 belohnt hohe Spielbeteiligung
   - 0.875 × 1.1 = 0.9625
   - Maximum bei 1.0 gedeckelt

**Beispiele:**

| Spieltag | Gespielte Spiele | Mögliche Spiele | Verhältnis | Confidence |
|----------|------------------|-----------------|------------|------------|
| 8 | 8 | 8 | 100% | **1.0** ✅ |
| 8 | 7 | 8 | 87.5% | **0.96** ⭐ |
| 8 | 6 | 8 | 75% | **0.83** ✔️ |
| 8 | 4 | 8 | 50% | **0.55** ⚠️ |
| 8 | 2 | 8 | 25% | **0.28** ❌ |

---

## Auswirkungen auf die Bewertung

### Score-Komponenten (nach Änderung):

1. **Punkte pro Spiel** (0-6 Punkte)
2. **Absolute Punkte Bonus** (0-3 Punkte)
3. **Value-for-Money** (0-4 Punkte)
4. **Form-Trend** (-2 bis +3 Punkte)
5. **Marktwert-Trend** (-1.5 bis +2 Punkte)
6. **Team-Need Bonus** (0.5-4 Punkte)
7. **Spiele-Konsistenz** (-1 bis +1 Punkte)
8. **Preis-Effizienz** (0-2 Punkte)

**Roher kombinierter Score:** ~0-24 Punkte (theoretisch)
**Finaler normalisierter Score:** **0-24 Punkte** (max-capped)
**Typischer guter Score:** 12-18 Punkte
**Sehr guter Score:** 18-22 Punkte
**Top-Tier Score:** 22-24 Punkte
**Empfehlungsschwelle:** ≥2.0 Punkte (vor Normalisierung)

---

## Vorteile der Änderungen

### Verletzungsrisiko entfernt:
- ✅ Weniger negative Bewertung durch Statusflags
- ✅ Fokus auf tatsächliche Leistung statt Gesundheitsstatus
- ✅ Einfachere Interpretation der Scores

### Verbesserter Confidence Score:
- ✅ Realistischere Bewertung in frühen Phasen der Saison
- ✅ Berücksichtigt Spielbeteiligung relativ zum Spieltag
- ✅ Belohnt Stammspieler angemessen
- ✅ Bestraft häufig ausgewechselte/verletzte Spieler

---

## Technische Details

### Confidence-Berechnung im Detail:

```swift
// Beispiel: Spieltag 8, Spieler hat 7 von 8 Spielen absolviert

let gamesPlayed = 7
let remainingGames = 26  // 34 - 8 = 26
let estimatedCurrentMatchday = 34 - 26 = 8  // Aktueller Spieltag
let possibleGames = 8.0  // Maximale Spiele bis Spieltag 8

let playedRatio = 7.0 / 8.0 = 0.875  // 87.5% Spielbeteiligung
let confidence = min(0.875 * 1.1, 1.0) = 0.9625  // 96.25% Confidence

// Gerundet in UI: 96%
```

### Warum der 1.1 Faktor?

Der Faktor 1.1 gibt einen Bonus für hohe Spielbeteiligung:
- **90%+ Beteiligung** → nahe 100% Confidence
- **80%+ Beteiligung** → ~88% Confidence
- **70%+ Beteiligung** → ~77% Confidence
- **<50% Beteiligung** → <55% Confidence

Dies spiegelt wider, dass Spieler mit hoher Beteiligung verlässlichere Datenpunkte liefern.

---

## Migration & Kompatibilität

**Keine Breaking Changes:**
- Die `PlayerAnalysis`-Struktur bleibt unverändert
- Alle UI-Komponenten funktionieren weiterhin
- Cache wird automatisch mit neuen Berechnungen gefüllt

**Empfehlung nach Update:**
- Cache einmal manuell leeren für sofortige neue Bewertungen
- Oder 5 Minuten warten (automatische Cache-Invalidierung)

---

## Priority-Schwellwerte (29. November 2025 - Update)

Nach der Anpassung auf 24er-Skala wurden auch die Priority-Schwellwerte neu kalibriert:

**Neue Schwellwerte (24er-Skala):**
- **Essential** 🔴: Schwache Position + Score ≥ **19.2** (80%)
- **Recommended** 🟠: Score ≥ **12.0** (50%)
- **Optional** 🔵: Score < 12.0

**Begründung:**
- Essentiell = wirklich Top-Spieler (obere 20%) auf benötigten Positionen
- Empfohlen = gute Spieler (obere 50%) unabhängig von Position
- Optional = alle anderen Empfehlungen

---

**Problem (v1):** Der Empfehlungsscore konnte über 10 hinausgehen (z.B. 16/10), obwohl die UI ihn als "/10" anzeigte.

**Erste Lösung (v1):** Score wurde auf 0-10 Range normalisiert.

**Problem (v2):** Mit 0-10 Normalisierung bekommen zu viele Spieler 10/10, Unterschiede nicht erkennbar.

**Aktuelle Lösung (v2):** Score wird auf theoretisches Maximum von ~24 Punkten begrenzt mit `min(max(score, 0.0), 24.0)`.

**Auswirkung:**
- ✅ Beste Spieler bekommen Maximum 24.0 (statt 10.0 oder 16+)
- ✅ Mittelmäßige Spieler: 12-16
- ✅ Gute Spieler: 18-22
- ✅ Unterschiede zwischen Spielern sind jetzt deutlich erkennbar
- ✅ Schwellwert von 2.0 Punkten bleibt erhalten

---

## Zukünftige Optimierungen

### Mögliche weitere Verbesserungen:

1. **Confidence-Bonus im Score:**
   - Spieler mit hohem Confidence könnten zusätzliche Punkte erhalten
   - Beispiel: Confidence ≥ 0.9 → +1.0 Punkte

2. **Dynamische Spieltags-Erkennung:**
   - API-basierte Ermittlung des aktuellen Spieltags
   - Genauere Berechnung der möglichen Spiele

3. **Positions-spezifische Confidence:**
   - Torwarte benötigen weniger Spiele für hohe Confidence
   - Stürmer könnten strengere Kriterien haben

4. **Historische Verletzungsdaten:**
   - Falls verfügbar, Verletzungshistorie in separatem Score
   - Nicht als Abzug, sondern als Informations-Badge
