# Neue Endpoints mit ähnlichen Namen - Kickbase API v4

Basierend auf der Analyse der dokumentierten Endpoints in `kickbasev4.json` wurden folgende neue, möglicherweise funktionsfähige Endpoints identifiziert.

## 📊 Erkannte Patterns aus dokumentierten Endpoints

| Pattern | Häufigkeit | Beispiele |
|---------|-----------|----------|
| `/{resource}/overview` | 10 | `/v4/base/overview`, `/v4/competitions/{competitionId}/overview` |
| `/{resource}/selection` | 6 | `/v4/challenges/selection`, `/v4/leagues/selection` |
| `/{resource}/{id}/profile` | 5 | `/v4/challenges/{challengeId}/profile` |
| `/{resource}/{id}/ranking` | 5 | `/v4/challenges/{challengeId}/ranking`, `/v4/leagues/{leagueId}/ranking` |
| `/{resource}/{id}/table` | 4 | `/v4/challenges/{challengeId}/table` |
| `/{resource}/{id}/performance` | 8 | `/v4/challenges/{challengeId}/performance` |
| `/{resource}/favorites` | 4 | `/v4/challenges/favorites` |

---

## 🎯 Kategorie 1: Symmetrie-basierte Endpoints

Diese Endpoints folgen der Logik: Wenn es `/{resource}/{id}/action` gibt, könnte es auch `/{resource}/action` geben.

```
✓ /v4/leagues/overview
  Grund: Es gibt /v4/leagues/{leagueId}/overview
  
✓ /v4/leagues/ranking
  Grund: Es gibt /v4/leagues/{leagueId}/ranking
  
✓ /v4/competitions/overview
  Grund: Es gibt /v4/competitions/{competitionId}/overview
  
✓ /v4/competitions/ranking
  Grund: Es gibt /v4/competitions/{competitionId}/ranking
```

---

## 👥 Kategorie 2: Manager/User-Endpoints

Symmetrisch zu den dokumentierten League-Manager-Endpoints:

```
✓ /v4/managers
  Grund: Es gibt /v4/leagues/{leagueId}/managers/{userId}/dashboard
  Status: Wahrscheinlich globale Manager-Liste
  
✓ /v4/managers/overview
  Grund: Passt zum /overview Pattern
  
✓ /v4/managers/{userId}
  Grund: Manager-Details ähnlich wie bei /v4/user/profile
  
✓ /v4/user/dashboard
  Grund: Es gibt /v4/leagues/{leagueId}/managers/{userId}/dashboard
```

---

## 📋 Kategorie 3: Squad/Team-Endpoints

```
✓ /v4/squads
  Grund: Es gibt /v4/leagues/{leagueId}/squad
  
✓ /v4/squads/{squadId}
  Grund: Symmetrisch zu anderen Resource-Endpoints
  
✓ /v4/squads/overview
  Grund: /overview Pattern
  
✓ /v4/teamcenters
  Grund: Es gibt /v4/leagues/{leagueId}/teamcenter/myeleven
  
✓ /v4/formations
  Grund: Logische Ergänzung zu Squad-Daten
  
✓ /v4/formations/{formationId}
  Grund: Formation-Details
```

---

## 💰 Kategorie 4: Market/Trading Endpoints

```
✓ /v4/market
  Grund: Es gibt /v4/leagues/{leagueId}/market aber kein globales Äquivalent
  
✓ /v4/market/overview
  Grund: /overview Pattern
  
✓ /v4/market/trending
  Grund: Beliebte Spieler auf globalem Markt
  
✓ /v4/market/favorites
  Grund: Es gibt /v4/challenges/favorites
  
✓ /v4/trading/offers
  Grund: Es gibt /v4/leagues/{leagueId}/market/{playerId}/offers
  
✓ /v4/trading/history
  Grund: Globale Trade-Historie
```

---

## 📊 Kategorie 5: Position/Formation Endpoints

```
✓ /v4/positions
  Grund: Nicht dokumentiert, aber essentiell für Squad-Management
  
✓ /v4/positions/{positionId}
  Grund: Position-Details
  
✓ /v4/positions/overview
  Grund: Alle verfügbaren Positionen
```

---

## 📈 Kategorie 6: Statistics/Analytics Endpoints

```
✓ /v4/statistics
  Grund: Keine globalen Stats dokumentiert
  
✓ /v4/statistics/overview
  Grund: Allgemeine API-Statistiken
  
✓ /v4/statistics/players
  Grund: Player-Statistiken aggregiert
  
✓ /v4/analytics
  Grund: Analytics-Dashboard
  
✓ /v4/analytics/trending
  Grund: Trend-Analyse
```

---

## 🔍 Kategorie 7: Scouting/Recommendations

```
✓ /v4/scouting
  Grund: Es gibt /v4/leagues/{leagueId}/scoutedplayers aber keine globale Version
  
✓ /v4/scouting/recommendations
  Grund: Empfeilte Spieler
  
✓ /v4/scouting/prospects
  Grund: Nachwuchs-Spieler
  
✓ /v4/recommendations/players
  Grund: Globale Spieler-Empfehlungen
  
✓ /v4/recommendations/transfers
  Grund: Transfer-Empfehlungen
```

---

## 🏆 Kategorie 8: Badge/Achievement Endpoints

```
✓ /v4/badges
  Grund: Es gibt /v4/leagues/{leagueId}/user/achievements/{type}
  
✓ /v4/badges/overview
  Grund: Alle verfügbaren Badges
  
✓ /v4/trophies
  Grund: Trophäen-System
  
✓ /v4/achievements
  Grund: Globale Achievements
```

---

## 🎲 Kategorie 9: Betting/Challenges Erweiterungen

```
✓ /v4/betting
  Grund: Es gibt nur /v4/challenges/* keine globalen Betting-Endpoints
  
✓ /v4/betting/overview
  Grund: /overview Pattern
  
✓ /v4/bets
  Grund: Liste aller Wetten
  
✓ /v4/bets/{betId}
  Grund: Wetten-Details
  
✓ /v4/wagers
  Grund: Separate Wetten-Kategorie möglich
```

---

## 🔴 Kategorie 10: Livescores/Real-time Daten

```
✓ /v4/live/matches
  Grund: Es gibt /v4/live/eventtypes aber nicht matches
  
✓ /v4/live/scores
  Grund: Live-Ergebnisse
  
✓ /v4/live/updates
  Grund: Real-time Updates
  
✓ /v4/livescores
  Grund: Alternative Endpoint-Struktur
  
✓ /v4/live/standings
  Grund: Live-Tabellen
```

---

## 📱 Kategorie 11: Zusätzliche Resource-Endpoints

```
✓ /v4/players
  Grund: Globale Player-Liste (analog zu /v4/competitions/{competitionId}/players)
  
✓ /v4/players/overview
  Grund: /overview Pattern
  
✓ /v4/players/trending
  Grund: Trend-Spieler
  
✓ /v4/teams
  Grund: Es gibt Teams nur unter Competitions
  
✓ /v4/teams/overview
  Grund: Alle Teams
  
✓ /v4/matchdays
  Grund: Es gibt /v4/competitions/{competitionId}/matchdays
  
✓ /v4/matchdays/current
  Grund: Aktueller Spieltag
  
✓ /v4/seasons
  Grund: Saison-Management
```

---

## 📡 Kategorie 12: API-Management Endpoints

```
✓ /v4/status
  Grund: API-Status
  
✓ /v4/health
  Grund: Health-Check
  
✓ /v4/info
  Grund: API-Informationen
  
✓ /v4/settings
  Grund: Es gibt /v4/leagues/{leagueId}/settings
```

---

## 📋 Zusammenfassung

| Kategorie | Endpoints | Status |
|-----------|-----------|--------|
| Symmetrie | 4 | 🟡 Wahrscheinlich |
| Manager | 4 | 🟡 Wahrscheinlich |
| Squad/Team | 6 | 🟡 Wahrscheinlich |
| Market | 6 | 🟡 Wahrscheinlich |
| Position | 3 | 🔴 Unwahrscheinlich |
| Statistics | 5 | 🟡 Wahrscheinlich |
| Scouting | 5 | 🟡 Wahrscheinlich |
| Badges | 4 | 🟡 Wahrscheinlich |
| Betting | 5 | 🟡 Wahrscheinlich |
| Live-Daten | 5 | 🟡 Wahrscheinlich |
| Resources | 8 | 🟡 Wahrscheinlich |
| API-Mgmt | 4 | 🟢 Möglich |

**Gesamt: ~59 neue potenzielle Endpoints**

---

## 🧪 Empfohlene Test-Strategie

1. **Phase 1 - Symmetrie testen:** Endpoints mit direktem Symmetrie-Pattern testen
2. **Phase 2 - Manager-Endpoints:** /v4/managers/* testen
3. **Phase 3 - Market-Endpoints:** /v4/market/* testen
4. **Phase 4 - Scouting-Endpoints:** /v4/scouting/* testen

---

## 📝 Notizen

- Die Analyse basiert auf REST-API-Design-Patterns
- Endpoints sollten mit Authentication getestet werden
- Einige Endpoints könnten mit Query-Parametern erweitert sein
- POST/PUT/DELETE Varianten könnten zusätzlich existieren

