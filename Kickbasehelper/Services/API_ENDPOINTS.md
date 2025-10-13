# Kickbase API v4 - Vollständige Endpoint-Dokumentation

Diese Datei dokumentiert alle verfügbaren Endpoints der Kickbase API v4, die im `KickbaseAPIService` implementiert sind.

Quelle: [kickbase-api-doc auf GitHub](https://github.com/kevinskyba/kickbase-api-doc)

## Authentifizierung

### Login
- **POST** `/v4/user/login`
- **Body**: `{ "em": "email", "pass": "password", "loy": false, "rep": {} }`
- **Response**: Token und User-Objekt
- **Funktion**: `login(email:password:)`

### User Settings
- **GET** `/v4/user/settings`
- **Authentifizierung**: Required
- **Funktion**: `getUserSettings()`

## Liga-Endpoints

### Liga-Übersicht
- **GET** `/v4/leagues/selection`
- **Beschreibung**: Liste aller Ligen des Benutzers
- **Funktion**: `getLeagueSelection()`

### Liga-Details
- **GET** `/v4/leagues/{leagueId}/overview`
- **Query-Parameter**: `matchDay` (optional)
- **Funktion**: `getLeagueOverview(leagueId:matchDay:)`

### Meine Liga-Daten
- **GET** `/v4/leagues/{leagueId}/me`
- **Beschreibung**: Eigene Statistiken in der Liga
- **Funktion**: `getLeagueMe(leagueId:)`

### Budget
- **GET** `/v4/leagues/{leagueId}/me/budget`
- **Beschreibung**: Aktuelles Budget
- **Funktion**: `getMyBudget(leagueId:)`

### Kader
- **GET** `/v4/leagues/{leagueId}/squad`
- **Beschreibung**: Alle eigenen Spieler
- **Funktion**: `getMySquad(leagueId:)`

### Aufstellung
- **GET** `/v4/leagues/{leagueId}/lineup`
- **Beschreibung**: Aktuelle Aufstellung
- **Funktion**: `getMyLineup(leagueId:)`

- **POST** `/v4/leagues/{leagueId}/lineup`
- **Body**: Array von Spieler-IDs `[1, 2, 3, ...]`
- **Beschreibung**: Aufstellung aktualisieren
- **Funktion**: `updateMyLineup(leagueId:lineup:)`

### Team Center
- **GET** `/v4/leagues/{leagueId}/teamcenter/myeleven`
- **Beschreibung**: Startelf-Übersicht
- **Funktion**: `getMyEleven(leagueId:)`

### Rangliste
- **GET** `/v4/leagues/{leagueId}/ranking`
- **Query-Parameter**: `matchDay` (optional)
- **Funktion**: `getLeagueRanking(leagueId:matchDay:)`

### Liga-Einstellungen (Admin)
- **GET** `/v4/leagues/{leagueId}/settings`
- **Funktion**: `getLeagueSettings(leagueId:)`

### Liga-Mitglieder (Admin)
- **GET** `/v4/leagues/{leagueId}/settings/managers`
- **Funktion**: `getLeagueManagers(leagueId:)`

## Manager-Endpoints

### Manager-Profil
- **GET** `/v4/leagues/{leagueId}/managers/{userId}/dashboard`
- **Funktion**: `getManagerDashboard(leagueId:userId:)`

### Manager-Performance
- **GET** `/v4/leagues/{leagueId}/managers/{userId}/performance`
- **Funktion**: `getManagerPerformance(leagueId:userId:)`

### Manager-Kader
- **GET** `/v4/leagues/{leagueId}/managers/{userId}/squad`
- **Funktion**: `getManagerSquad(leagueId:userId:)`

### Manager-Transfers
- **GET** `/v4/leagues/{leagueId}/managers/{userId}/transfer`
- **Query-Parameter**: `matchDay` (optional)
- **Funktion**: `getManagerTransfers(leagueId:userId:matchDay:)`

### Team Center eines Managers
- **GET** `/v4/leagues/{leagueId}/users/{userId}/teamcenter`
- **Query-Parameter**: `matchDay` (optional)
- **Funktion**: `getTeamCenter(leagueId:userId:matchDay:)`

## Spieler-Endpoints

### Spieler-Details
- **GET** `/v4/leagues/{leagueId}/players/{playerId}`
- **Funktion**: `getPlayerDetails(leagueId:playerId:)`

### Spieler-Performance
- **GET** `/v4/leagues/{leagueId}/players/{playerId}/performance`
- **Funktion**: `getPlayerPerformance(leagueId:playerId:)`
- **Response**: `PlayerPerformanceResponse` (typisiert)

### Marktwert-Historie
- **GET** `/v4/leagues/{leagueId}/players/{playerId}/marketvalue/{timeframe}`
- **Parameter**: `timeframe` (z.B. 92, 365 Tage)
- **Funktion**: `getPlayerMarketValue(leagueId:playerId:timeframe:)`

### Transfer-Historie
- **GET** `/v4/leagues/{leagueId}/players/{playerId}/transferHistory`
- **Query-Parameter**: `matchDay` (optional)
- **Funktion**: `getPlayerTransferHistory(leagueId:playerId:matchDay:)`

### Spieler-Transfers
- **GET** `/v4/leagues/{leagueId}/players/{playerId}/transfers`
- **Funktion**: `getPlayerTransfers(leagueId:playerId:)`

## Transfermarkt-Endpoints

### Marktübersicht
- **GET** `/v4/leagues/{leagueId}/market`
- **Beschreibung**: Alle Spieler auf dem Transfermarkt
- **Funktion**: `getMarketPlayers(leagueId:)`

### Spieler auf Markt setzen
- **POST** `/v4/leagues/{leagueId}/market`
- **Body**: `{ "playerId": "...", "price": 1000000 }`
- **Funktion**: `setPlayerTransferPrice(leagueId:playerId:price:)`

### Spieler vom Markt nehmen
- **DELETE** `/v4/leagues/{leagueId}/market/{playerId}`
- **Funktion**: `removePlayerFromMarket(leagueId:playerId:)`

### Angebot abgeben
- **POST** `/v4/leagues/{leagueId}/market/{playerId}/offers`
- **Body**: `{ "price": 1000000 }`
- **Funktion**: `placeOffer(leagueId:playerId:price:)`

### Angebot zurückziehen
- **DELETE** `/v4/leagues/{leagueId}/market/{playerId}/offers/{offerId}`
- **Funktion**: `withdrawOffer(leagueId:playerId:offerId:)`

### Angebot annehmen
- **DELETE** `/v4/leagues/{leagueId}/market/{playerId}/offers/{offerId}/accept`
- **Funktion**: `acceptOffer(leagueId:playerId:offerId:)`

### Angebot ablehnen
- **DELETE** `/v4/leagues/{leagueId}/market/{playerId}/offers/{offerId}/decline`
- **Funktion**: `declineOffer(leagueId:playerId:offerId:)`

### Kickbase-Angebot annehmen
- **DELETE** `/v4/leagues/{leagueId}/market/{playerId}/sell`
- **Beschreibung**: Direktverkauf an Kickbase
- **Funktion**: `acceptKickbaseOffer(leagueId:playerId:)`

## Beobachtungsliste-Endpoints

### Beobachtete Spieler anzeigen
- **GET** `/v4/leagues/{leagueId}/scoutedplayers`
- **Funktion**: `getScoutedPlayers(leagueId:)`

### Spieler zur Beobachtungsliste hinzufügen
- **POST** `/v4/leagues/{leagueId}/scoutedplayers/{playerId}`
- **Funktion**: `addScoutedPlayer(leagueId:playerId:)`

### Spieler von Beobachtungsliste entfernen
- **DELETE** `/v4/leagues/{leagueId}/scoutedplayers/{playerId}`
- **Funktion**: `removeScoutedPlayer(leagueId:playerId:)`

### Beobachtungsliste leeren
- **DELETE** `/v4/leagues/{leagueId}/scoutedplayers`
- **Funktion**: `clearScoutedPlayers(leagueId:)`

## Aktivitäten-Feed-Endpoints

### Aktivitäten-Feed
- **GET** `/v4/leagues/{leagueId}/activitiesFeed`
- **Query-Parameter**: `start`, `max` (optional)
- **Funktion**: `getActivitiesFeed(leagueId:start:max:)`

### Einzelne Aktivität
- **GET** `/v4/leagues/{leagueId}/activitiesFeed/{activityId}`
- **Funktion**: `getFeedItem(leagueId:activityId:)`

### Kommentar senden
- **POST** `/v4/leagues/{leagueId}/activitiesFeed/{activityId}`
- **Body**: `{ "comment": "..." }`
- **Funktion**: `sendFeedItemComment(leagueId:activityId:comment:)`

### Kommentare abrufen
- **GET** `/v4/leagues/{leagueId}/activitiesFeed/{activityId}/comments`
- **Query-Parameter**: `start`, `max` (optional)
- **Funktion**: `getFeedItemComments(leagueId:activityId:start:max:)`

## Achievements-Endpoints

### Alle Achievements
- **GET** `/v4/leagues/{leagueId}/user/achievements`
- **Funktion**: `getUserAchievements(leagueId:)`

### Achievements nach Typ
- **GET** `/v4/leagues/{leagueId}/user/achievements/{type}`
- **Funktion**: `getUserAchievementsByType(leagueId:type:)`

### Battle nach Typ
- **GET** `/v4/leagues/{leagueId}/battles/{type}/users`
- **Funktion**: `getBattleByType(leagueId:type:)`

## Team-Endpoints

### Teamprofil
- **GET** `/v4/leagues/{leagueId}/teams/{teamId}/teamprofile`
- **Beschreibung**: Alle Spieler eines Bundesliga-Teams
- **Funktion**: `getTeamProfile(leagueId:teamId:)`
- **Response**: `TeamProfileResponse` (typisiert)

## Wettbewerb-Endpoints

### Wettbewerbs-Übersicht
- **GET** `/v4/competitions/{competitionId}/overview`
- **Funktion**: `getCompetitionOverview(competitionId:)`

### Wettbewerbs-Spieler
- **GET** `/v4/competitions/{competitionId}/players`
- **Query-Parameter**: `matchDay` (optional)
- **Funktion**: `getCompetitionPlayers(competitionId:matchDay:)`

### Spieler-Suche im Wettbewerb
- **GET** `/v4/competitions/{competitionId}/players/search`
- **Query-Parameter**: `query`, `sorting`, `start`, `max`
- **Funktion**: `searchCompetitionPlayers(competitionId:query:sorting:start:max:)`

### Wettbewerbs-Spieler-Performance
- **GET** `/v4/competitions/{competitionId}/players/{playerId}/performance`
- **Funktion**: `getCompetitionPlayerPerformance(competitionId:playerId:)`

### Wettbewerbs-Spieler-Marktwert
- **GET** `/v4/competitions/{competitionId}/players/{playerId}/marketvalue/{timeframe}`
- **Funktion**: `getCompetitionPlayerMarketValue(competitionId:playerId:timeframe:)`

### Spieler-Event-Historie
- **GET** `/v4/competitions/{competitionId}/playercenter/{playerId}`
- **Query-Parameter**: `matchDay` (optional)
- **Funktion**: `getPlayerEventHistory(competitionId:playerId:matchDay:)`

### Team-Rangliste
- **GET** `/v4/competitions/{competitionId}/ranking`
- **Funktion**: `getCompetitionRanking(competitionId:)`

### Wettbewerbs-Tabelle
- **GET** `/v4/competitions/{competitionId}/table`
- **Funktion**: `getCompetitionTable(competitionId:)`

### Spieltage
- **GET** `/v4/competitions/{competitionId}/matchdays`
- **Funktion**: `getCompetitionMatchdays(competitionId:)`

### Team-Center im Wettbewerb
- **GET** `/v4/competitions/{competitionId}/teams/{teamId}/teamcenter`
- **Query-Parameter**: `matchDay` (optional)
- **Funktion**: `getCompetitionTeamCenter(competitionId:teamId:matchDay:)`

### Teamprofil im Wettbewerb
- **GET** `/v4/competitions/{competitionId}/teams/{teamId}/teamprofile`
- **Funktion**: `getCompetitionTeamProfile(competitionId:teamId:)`

## Spiel-Endpoints

### Spiel-Details
- **GET** `/v4/matches/{matchId}/details`
- **Funktion**: `getMatchDetails(matchId:)`

### Wett-Link
- **GET** `/v4/matches/{matchId}/betlink`
- **Funktion**: `getMatchBetlink(matchId:)`

## Live-Endpoints

### Event-Typen
- **GET** `/v4/live/eventtypes`
- **Beschreibung**: Alle verfügbaren Live-Event-Typen
- **Funktion**: `getLiveEventTypes()`

## Sonstige Endpoints

### Basis-Übersicht
- **GET** `/v4/base/overview`
- **Funktion**: `getBaseOverview()`

### Bonus sammeln
- **GET** `/v4/bonus/collect`
- **Beschreibung**: Täglichen Bonus abholen
- **Funktion**: `collectBonus()`

### Konfiguration
- **GET** `/v4/config`
- **Beschreibung**: App-Konfiguration
- **Funktion**: `getConfig()`

## Chat-Endpoints

### Liga-Auswahl für Chat
- **GET** `/v4/chat/leagueselection`
- **Funktion**: `getChatLeagueSelection()`

### Chat-Token aktualisieren
- **GET** `/v4/chat/refreshtoken`
- **Funktion**: `getChatRefreshToken()`

## Challenge-Endpoints

Die Challenge-Endpoints sind für spezielle Wettbewerbe/Challenges in der App und werden derzeit nicht in der Hauptanwendung verwendet:

- `/v4/challenges/archive` - Vergangene Challenges
- `/v4/challenges/favorites` - Favoriten-Manager
- `/v4/challenges/overview` - Alle Challenges
- `/v4/challenges/recommended` - Empfohlene Challenges
- `/v4/challenges/selection` - Challenge-Auswahl
- `/v4/challenges/{challengeId}/...` - Challenge-spezifische Endpoints

## Nutzung

Alle Endpoints werden über die zentrale `KickbaseAPIService`-Klasse aufgerufen:

```swift
// Initialisierung
let apiService = KickbaseAPIService()

// Login
let loginResponse = try await apiService.login(email: "...", password: "...")
apiService.setAuthToken(loginResponse.tkn)

// Ligen abrufen
let leagues = try await apiService.getLeagueSelection()

// Spieler-Performance
let performance = try await apiService.getPlayerPerformance(leagueId: "...", playerId: "...")
```

## Fehlerbehandlung

Alle Endpoints werfen bei Fehlern einen `APIError`:

- `.noAuthToken` - Kein Authentifizierungstoken gesetzt
- `.invalidURL` - Ungültige URL
- `.noHTTPResponse` - Keine HTTP-Antwort
- `.authenticationFailed` - Authentifizierung fehlgeschlagen (401)
- `.networkError(String)` - Netzwerk- oder HTTP-Fehler

## Hinweise

- Alle Endpoints außer Login erfordern einen gesetzten Auth-Token
- Alle Responses sind JSON-Objekte (`[String: Any]`) oder typisierte Structs
- Query-Parameter sind optional und können als `nil` übergeben werden
- Die API verwendet konsequent verkürzte Feldnamen (z.B. `tkn` statt `token`)
