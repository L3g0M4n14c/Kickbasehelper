# Firebase Setup Checkliste für Bonus Collection Push Notifications

## Phase 1: Firebase Project Setup

### Schritt 1.1: Firebase Project erstellen oder verwenden
- [ ] Gehe zu https://console.firebase.google.com
- [ ] Erstelle ein neues Project oder wähle bestehendes aus
- [ ] Notiere die Project ID (z.B. `kickbasehelper-prod`)
- [ ] Aktiviere Blaze Plan (erforderlich für Cloud Functions)
  - Gehe zu `Upgrade` Button
  - Wähle "Blaze" Plan
  - Verbinde Kreditkarte

### Schritt 1.2: Cloud Functions aktivieren
- [ ] Gehe zu "Cloud Functions" im Firebase Console
- [ ] Klicke "Get Started"
- [ ] Bestätige API-Aktivierung (es öffnen sich mehrere APIs)
- [ ] Warte bis Status "✓ Cloud Functions API" anzeigt

### Schritt 1.3: Firestore Database Setup
- [ ] Gehe zu "Firestore Database"
- [ ] Klicke "Create Database"
- [ ] Wähle Region: `europe-west1` (oder deine bevorzugte Region)
- [ ] Starte im "Production Mode"
- [ ] Klicke "Create"

### Schritt 1.4: Firestore Security Rules setzen
- [ ] Gehe zu Firestore → Rules Tab
- [ ] Ersetze mit:

```firestore
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Device tokens - nur von Cloud Functions änderbar
    match /deviceTokens/{document=**} {
      allow read, write: if request.auth.uid != null;
    }
    
    // User documents - nur von Cloud Functions änderbar
    match /users/{userId} {
      allow read, write: if request.auth.uid == userId || request.auth.uid != null;
    }
    
    // Logs - nur von Cloud Functions
    match /bonusPushLogs/{document=**} {
      allow read: if request.auth.uid != null;
      allow write: if false; // Nur Cloud Functions
    }
  }
}
```

- [ ] Klicke "Publish"

### Schritt 1.5: Cloud Scheduler aktivieren (für Cron Jobs)
- [ ] Gehe zu Cloud Console → APIs & Services
- [ ] Suche "Cloud Scheduler API"
- [ ] Klicke "Enable"
- [ ] Warte auf Aktivierung

## Phase 2: Lokale Entwicklung Setup

### Schritt 2.1: Firebase CLI installieren
```bash
npm install -g firebase-tools
firebase login
firebase init
```

### Schritt 2.2: Project ID konfigurieren
```bash
cd /pfad/zu/project
firebase use --add
# Wähle dein Project aus der Liste
# Gib einen Alias ein (z.B. "production" oder "development")
```

### Schritt 2.3: Cloud Functions initialisieren
```bash
firebase init functions

# Wähle:
# - Language: TypeScript (empfohlen) oder JavaScript
# - ESLint: Yes (optional)
# - npm dependencies: Yes
```

### Schritt 2.4: Dependencies in functions/package.json
```bash
cd functions
npm install firebase-admin@latest firebase-functions@latest
npm install --save-dev @types/node
```

## Phase 3: Cloud Functions implementieren

### Schritt 3.1: Dateistruktur erstellen
```
functions/
├── src/
│   ├── index.ts (exports alle Functions)
│   ├── registerDeviceToken.ts
│   ├── sendDailyBonusPush.ts
│   └── cleanupBadTokens.ts
├── package.json
├── tsconfig.json
└── .eslintrc.json
```

### Schritt 3.2: functions/src/index.ts erstellen
```typescript
export { registerDeviceToken } from './registerDeviceToken';
export { sendDailyBonusPush } from './sendDailyBonusPush';
export { cleanupBadTokens } from './cleanupBadTokens';
```

### Schritt 3.3: Implementiere die 3 Cloud Functions
- [ ] Copy-paste Code aus [FIREBASE_CLOUD_FUNCTIONS_SETUP.md](./FIREBASE_CLOUD_FUNCTIONS_SETUP.md)
- [ ] In `registerDeviceToken.ts`
- [ ] In `sendDailyBonusPush.ts`
- [ ] In `cleanupBadTokens.ts`

### Schritt 3.4: Lokales Testen mit Emulator
```bash
# Starte den Emulator
firebase emulators:start --only functions,firestore

# In neuem Terminal: Teste registerDeviceToken
curl -X POST http://localhost:5001/YOUR-PROJECT/us-central1/registerDeviceToken \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer test-token-123" \
  -d '{
    "token": "test-ios-device-token-abc123",
    "platform": "iOS"
  }'

# Prüfe Firestore Emulator UI auf http://localhost:4000
```

## Phase 4: Firebase Authentication Setup (Apple Push Certificate)

### Schritt 4.1: APNs Certificate vom Apple Developer Account
- [ ] Gehe zu https://developer.apple.com/account
- [ ] Certificates, Identifiers & Profiles → Certificates
- [ ] Erstelle neues Certificate: "Apple Push Notification service SSL (Production)"
- [ ] Downloade `.cer` Datei
- [ ] Konvertiere zu `.p8` (wichtig für FCM):

```bash
# Export private key from .cer
openssl req -new -x509 -key /path/to/private_key.p8 -out /path/to/cert.pem -days 365

# Oder: Nutze die Key ID + Team ID direkt in Firebase
```

### Schritt 4.2: APNs in Firebase Cloud Messaging konfigurieren
- [ ] Gehe zu Firebase Console → Cloud Messaging Tab
- [ ] Scrolle zu "Apple Configuration"
- [ ] Klicke "Upload APNs Certificate"
- [ ] Lade dein APNs Certificate (.p8 oder .cer) hoch
- [ ] Speichere

**Oder (empfohlen - Token-based):**
- [ ] Gehe zu Apple Developer Account
- [ ] Keys → App IDs → Erstelle neue Key für "Apple Push Notifications"
- [ ] Downloade `.p8` Datei
- [ ] In Firebase: Nutze "APNs Authentication Key"
- [ ] Lade `.p8` hoch + gib Key ID und Team ID an

## Phase 5: Deployment auf Firebase

### Schritt 5.1: Teste Functions lokal
```bash
cd functions
npm run build  # wenn TypeScript
npm run serve  # lokal testen
```

### Schritt 5.2: Deploy zu Firebase
```bash
firebase deploy --only functions

# Output sollte zeigen:
# ✔ functions[registerDeviceToken(us-central1)]: Successful create
# ✔ functions[sendDailyBonusPush(us-central1)]: Successful create
# ✔ functions[cleanupBadTokens(us-central1)]: Successful create
```

### Schritt 5.3: Überprüfe Deployment
```bash
firebase functions:list

# Sollte zeigen:
# registerDeviceToken (HTTP) - https://...
# sendDailyBonusPush (Pubsub/Scheduled)
# cleanupBadTokens (Pubsub/Scheduled)
```

## Phase 6: Cloud Scheduler Setup (Cron Jobs)

### Schritt 6.1: Prüfe ob Scheduler aktiviert
```bash
gcloud scheduler jobs list --location=europe-west1
# oder über Firebase Console → Cloud Scheduler
```

### Schritt 6.2: Jobs sollten automatisch erstellt sein
Nach Deploy von `sendDailyBonusPush` und `cleanupBadTokens`:
- [ ] `sendDailyBonusPush` sollte täglich um 12:00 UTC laufen
- [ ] `cleanupBadTokens` sollte täglich um 01:00 UTC laufen

### Schritt 6.3: Jobs manuell überprüfen
```bash
gcloud scheduler jobs describe sendDailyBonusPush --location=europe-west1

# Anpassen der Zeit (falls nötig):
gcloud scheduler jobs update sendDailyBonusPush \
  --location=europe-west1 \
  --schedule="0 12 * * *" \
  --time-zone="UTC"
```

## Phase 7: Monitoring & Logging

### Schritt 7.1: Logs in Firebase Console
- [ ] Firebase Console → Functions → Logs
- [ ] Wähle `sendDailyBonusPush` aus
- [ ] Sollte täglich um 12:00 UTC ausgeführt werden

### Schritt 7.2: Cloud Logging Setup
```bash
# Sehe alle Logs
gcloud functions describe sendDailyBonusPush --runtime=nodejs20

# Live Log Streaming
gcloud functions logs read sendDailyBonusPush --limit 50 --follow
```

### Schritt 7.3: Error Alerts erstellen
- [ ] Gehe zu Cloud Logging
- [ ] Erstelle Alert für:
  - Function Execution Errors
  - Quota Exceeded
  - High Error Rate

## Phase 8: iOS App Integration Testen

### Schritt 8.1: Device Token von Test-iPhone
- [ ] Öffne App auf Testgerät
- [ ] Logs sollten zeigen: `🔑 Device token stored: abc123...`
- [ ] Prüfe Firestore Console → `deviceTokens` Collection
- [ ] Sollte einen Eintrag mit dem Token zeigen

### Schritt 8.2: Teste manuellen Push
- [ ] Firebase Console → Cloud Messaging
- [ ] Wähle die App aus
- [ ] "Send your first message"
- [ ] Wähle "Send to a device"
- [ ] Paste das Device Token vom iPhone
- [ ] Erstelle Test-Nachricht mit:
```json
{
  "notification": {
    "title": "Test Push",
    "body": "Test Message"
  },
  "data": {
    "bonus_id": "test_push"
  }
}
```
- [ ] Drücke "Send"
- [ ] iPhone sollte die Benachrichtigung empfangen

### Schritt 8.3: Teste Scheduled Push (12:00 UTC)
- [ ] Warte bis 12:00 UTC (oder trigger manuell)
- [ ] Prüfe Firestore Logs (`bonusPushLogs` Collection)
- [ ] Sollte Eintrag mit `successCount > 0` zeigen
- [ ] Prüfe Cloud Functions Logs

## Troubleshooting

### Problem: "APNs certificate not configured"
**Lösung:**
- [ ] Firebase Console → Cloud Messaging
- [ ] Lade APNs Certificate neu hoch
- [ ] Stelle sicher, dass Certificate für Production (nicht Sandbox)

### Problem: "Device token not registered"
**Lösung:**
- [ ] Überprüfe dass App `registerForRemoteNotifications()` aufgerufen hat
- [ ] Überprüfe dass `didRegisterForRemoteNotificationsWithDeviceToken` aufgerufen wird
- [ ] Logs sollten zeigen: `🔑 Device token stored`

### Problem: "Cloud Scheduler job fails"
**Lösung:**
- [ ] Prüfe Cloud Functions Logs: `firebase functions:log`
- [ ] Überprüfe Firestore Security Rules
- [ ] Überprüfe Cloud Scheduler Permissions

### Problem: "Function quota exceeded"
**Lösung:**
- [ ] Upgrade zum Blaze Plan
- [ ] Oder reduziere Frequency der Pushes

## Nächste Schritte

1. ✅ Firebase Cloud Functions deployed
2. ✅ Cloud Scheduler konfiguriert
3. ✅ iOS App sendet Device Tokens
4. ✅ Tägliche Pushes um 12:00 UTC
5. Monitoring & Alerts aufsetzen
6. Load Testing mit vielen Device Tokens
7. Production Deployment
