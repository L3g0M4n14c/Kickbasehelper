# Skip Skill — Arbeitsregeln und Referenz (Kurz)

Zweck
- Kurze, präzise Zusammenfassung der relevanten Teile der Skip‑Dokumentation, damit im Projekt künftig strikt nach Docs gearbeitet wird.
- Enthält Beispiele für SKIP‑Kommentare, empfohlenen Workflow und CI‑Checks.

Grundprinzipien
- Niemals langfristig breitflächig in generierte Dateien eingreifen: generierte Dateien werden beim nächsten Transpilieren überschrieben.
- Stattdessen Use‑Case‑gerechte SKIP‑Mechanismen in Swift‑Quellcode oder `skip.yml`‑Optionen nutzen.

Wichtige Konzepte (Kurz)
- SKIP Kommentare: `// SKIP DECLARE:`, `// SKIP REPLACE:`, `// SKIP INSERT:`, `// SKIP NOWARN` — damit lässt sich das Kotlin‑Output gezielt steuern.
- Platform Guards / Direktiven: `#if os(Android)` / `#if !os(Android)` / `#if SKIP` — für Plattform‑spezifischen Code.
- Bridging Optionen: `skip.yml` kann `bridging: true` und `options: 'kotlincompat'` enthalten, um Kotlin‑freundliche Typen/Signaturen zu erzeugen.
- `.sref()` / `wrappedValue` / Binding Semantik: Verstehen, wie Skip Structs/Bindings in Kotlin abgebildet werden, bevor man Names/Access ändert.

Konkrete Beispiele
- SKIP DECLARE Beispiel (Swift):
```swift
// SKIP DECLARE: public fun navigationPath(): NavigationPath
```
- SKIP REPLACE Beispiel (Swift):
```swift
// SKIP REPLACE:
// internal fun problematicFunction() { /* temporary stub for Android */ }
```
- Platform guard (Swift):
```swift
#if os(Android)
// Android‑spezifischer Stub
#else
// Original iOS Implementation
#endif
```

Empfohlener Workflow (Kurz)
1. Bei Compile‑Fehlern nach Transpile: identifiziere die betroffene erzeugte Kotlin‑Datei (Fehlerstelle).  
2. Finde die korrespondierende Swift‑Quelle (unter `Sources/...`).  
3. Versuche eine minimale SKIP‑Comment/`#if`‑Änderung in Swift (oder `skip.yml`), regeneriere Transpilation (z. B. `skip export`/`xcodebuild`) und prüfe `./gradlew :app:compileDebugKotlin`.  
4. Falls nötig, erstelle temporären Kotlin‑Shim in Quell‑Code (nicht in `.build/`) als Stop‑gap.  
5. Langfristig: Issue/PR an Generator/Skip stellen, um saubere Generator‑Fixes zu erhalten.

CI Checkliste (Minimal)
- Job A: Transpile (xcodebuild / skip export) → Commit artifacts as build step.  
- Job B: Android build: `./gradlew :app:compileDebugKotlin` (fail on error).  
- Job C (optional): Snapshot diff gating for generator output.

Wartung & Hinweise
- Jede SKIP‑Änderung muss dokumentiert (Commit‑Message + TODO mit Issue/PR‑Referenz) werden, damit temporäre Stubs nicht langfristig verbleiben.
- Automatisiere lokale Regenerate/Tests in `scripts/` (z. B. `scripts/regenerate_and_check.sh`).

Migration: Sanitizer → SKIP
- Entferne navigator‑spezifische globale Umbenennungen aus `scripts/sanitize_skipstone.sh` (fragile) und ersetze sie durch gezielte SKIP‑Kommentare oder `#if`‑Guards in Swift.
- Vorgehensweise:
  1. Finde die Kotlin‑Fehlermeldung (z. B. Navigation.kt: Unresolved reference 'boundPath').
  2. Suche die korrespondierende Swift‑Quelle, die das Feature erzeugt (z. B. Views, Types in `Sources/`).
  3. Ergänze dort einen `// SKIP DECLARE:` oder `// SKIP REPLACE:` Kommentar oder `#if os(Android)` Guard mit einer einfachen, sicheren Android‑Fallback‑Impl.
  4. Regeneriere und prüfe mit `xcodebuild`/`skip export` → `./gradlew :app:compileDebugKotlin`.

Beispiel: Navigation‑Fallback (Swift)
```swift
// Wenn Navigation intern komplexe Bindings erzeugt, offerieren wir einen einfachen Android‑Fallback:
#if os(Android)
// SKIP REPLACE:
// import skip.ui
// public class NavigationStack { /* small, stable Kotlin API for Android */ }
#endif
```

Referenzen
- Skip Docs: https://skip.dev/docs/ (Platform customization, Transpilation Reference, Modes / Bridging)
