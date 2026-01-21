# ğŸ”¥ Firebase Testing Guide

Ja, du kannst deine erstellte `.apk` Datei hervorragend in Firebase testen! Es gibt zwei Hauptwege:

## 1. Firebase Test Lab (Automatisierte Tests)
Hier wird deine App auf echten Geräten in der Google Cloud installiert und automatisch getestet ("Robo Test").

**Warum nutzen?**
- Um zu sehen, ob die App auf verschiedenen Android-Versionen (z.B. Android 9, 10, 14) und Geräten (Pixel, Samsung Galaxy) startet.
- Um "Crashes at Launch" zu finden, ohne ein Android-Gerät zu besitzen.

**Anleitung:**
1. Gehe zur [Firebase Console](https://console.firebase.google.com/).
2. Erstelle ein neues Projekt (oder wähle ein bestehendes).
3. Wähle im linken Menü **Build > Test Lab**.
4. Klicke auf **Run a test** > **Robo**.
5. Lade deine APK hoch.
   - **Pfad:** `Kickbasehelper/Android/app/build/outputs/apk/debug/app-debug.apk`
6. Firebase klickt sich automatisch durch deine App und zeichnet Screenshots & Logs auf.

## 2. Firebase App Distribution (Manuelles Testen)
Damit kannst du die APK an dich selbst oder Tester senden, um sie einfach auf einem echten Android-Handy zu installieren.

**Warum nutzen?**
- Einfacher Weg, die App "over-the-air" auf dein Handy zu bekommen.
- Verwalte Versionen und Tester-Gruppen.

**Anleitung:**
1. In der Firebase Console unter **Build > App Distribution**.
2. Akzeptiere die Nutzungsbedingungen.
3. Ziehe deine `app-debug.apk` in das Upload-Fenster.
4. Gib deine E-Mail-Adresse unter "Testers" ein.
5. Du erhältst eine E-Mail auf dem Handy, installierst den "App Tester" und kannst deine App laden.

## ğŸ› ï¸ Dein aktueller Status
Deine APK liegt bereit unter:
```
/Users/marcocorro/Documents/xCode/Kickbasehelper/Android/app/build/outputs/apk/debug/app-debug.apk
```

### Tipp für später (CLI Automation)
Du kannst das Hochladen auch automatisieren, direkt aus dem Terminal (via `fastlane` oder `firebase-tools`), aber für den Anfang ist der Web-Upload über die Console am einfachsten.
