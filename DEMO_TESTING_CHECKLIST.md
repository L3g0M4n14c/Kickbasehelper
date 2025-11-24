# Demo-Account Testplan

## 🧪 Pre-Review Testing Checklist

### 1. Demo-Button anzeigen
- [ ] App starten
- [ ] Login-View wird angezeigt
- [ ] Button **"📱 Demo ausprobieren"** ist sichtbar (blau, unter dem grünen Login-Button)
- [ ] Button funktioniert (nicht disabled)

### 2. Demo-Login durchführen
- [ ] Auf "Demo ausprobieren" klicken
- [ ] Loading-Indicator erscheint: "Anmeldung läuft..."
- [ ] Nach ~1 Sekunde verschwindet Loading
- [ ] Dashboard wird angezeigt
- [ ] Keine Fehlermeldungen

### 3. Dashboard-Daten validieren
- [ ] Benutzername: "Demo User" wird angezeigt
- [ ] Teamname: "Demo Team" wird angezeigt
- [ ] Budget: €2.500.000 wird angezeigt
- [ ] Teamwert: €45.000.000 wird angezeigt
- [ ] Punkte: 287 wird angezeigt

### 4. Liga-Auswahl testen
- [ ] Mindestens 2 Ligen sind verfügbar:
  - "🏆 Bundesliga Classic"
  - "⚽ Friends Challenge"
- [ ] Ligen können gewechselt werden
- [ ] Daten aktualisieren sich korrekt

### 5. Team-Spieler anzeigen
- [ ] Team-Spieler werden korrekt geladen
- [ ] Mindestens 5 Spieler angezeigt
- [ ] Spielerdaten enthalten:
  - Name (z.B. "Manuel Neuer")
  - Position (Torwart, Abwehr, Mittelfeld, Stürmer)
  - Team (z.B. "FC Bayern")
  - Marktwert
  - Durchschnittspunkte
  - Trend (positiv/negativ)

### 6. Marktplatz testen
- [ ] Markt-Spieler werden angezeigt
- [ ] Mindestens 5 Premium-Spieler verfügbar:
  - Florian Wirtz
  - Vinícius Júnior
  - Lamine Yamal
  - Florent Inzaghi
  - Joshua Kimmich
- [ ] Angebotszahlen sind korrekt
- [ ] Verkäufer-Informationen angezeigt

### 7. Statistiken validieren
- [ ] Marktwert-Verlauf wird angezeigt
- [ ] 3-Tage-History mit Trends:
  - 24. Nov: 45.500.000 € (+0,44%)
  - 23. Nov: 45.300.000 € (+0,22%)
  - 22. Nov: 45.200.000 € (-0,66%)
- [ ] Prozentuale Änderungen realistisch

### 8. Empfehlungen laden
- [ ] Verkaufsempfehlungen können geladen werden
- [ ] Transferempfehlungen können geladen werden
- [ ] Keine Fehler bei der Generierung

### 9. Logout testen
- [ ] Logout funktioniert
- [ ] Zurück zum Login-Screen
- [ ] Demo-Daten werden geleert
- [ ] Login-Form ist wieder leer

### 10. Re-Login mit Demo testen
- [ ] Erneutes Klicken auf "Demo ausprobieren"
- [ ] Neue Demo-Session wird gestartet
- [ ] Neue Token wird generiert (eindeutig)
- [ ] Alle Daten sind wieder verfügbar

## 🔍 Edge Cases testen

### UI-Tests
- [ ] App auf iPhone testen
- [ ] App auf iPad testen (Landscape/Portrait)
- [ ] Dark Mode aktivieren
- [ ] Light Mode testen

### Performance
- [ ] Demo-Daten laden schnell (< 2 Sekunden)
- [ ] Keine Freezes oder Lags
- [ ] Memory-Usage ist normal

### Error Handling
- [ ] Logout während Demo möglich
- [ ] Re-Login nach Logout funktioniert
- [ ] Keine Crashes

## 📝 Zu überprüfende Logs (Xcode Console)

Nach dem Demo-Login solltest du sehen:
```
🎮 Starting demo mode...
👤 Demo User: Demo User - Demo Team
✅ Demo mode activated!
```

## ✅ Finaler Check vor Apple-Einreichung

```
☐ Alle Tests bestanden
☐ Keine Crash-Reports
☐ Keine Debug-Prints in Release-Build
☐ Code-Review durchgeführt
☐ Demo-Button ist intuitiv und auffindbar
☐ Demo-Daten sind realistisch
☐ Performance ist optimal
```

## 🎯 Apple Review Keywords

Diese Features solltest du in der App-Beschreibung für Apple erwähnen:
- "Demo-Mode für schnelle Vorschau"
- "Keine Registrierung erforderlich"
- "Realistische Beispieldaten"
- "Alle Features sofort testbar"

## 📞 Support für Apple

Falls Apple Fragen hat:
- Demo-Daten werden lokal generiert (keine API-Abhängigkeit)
- Token ist eindeutig pro Session
- Keine Datenschutz-Bedenken (keine echten Nutzerdaten)
- Performant und zuverlässig
