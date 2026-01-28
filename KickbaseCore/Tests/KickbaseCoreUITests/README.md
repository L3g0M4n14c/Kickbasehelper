KickbaseCore UI Tests (Phase B)

How to run:

1. Open the Xcode workspace/project (`Kickbasehelper.xcodeproj` / `.xcworkspace`).
2. Create or enable an iOS UI Test target (if not already present) and add the files under `Tests/KickbaseCoreUITests` to that target.
3. Select a simulator and run the UI tests from Xcode, or use xcodebuild:

   xcodebuild test -scheme "Kickbasehelper" -destination "platform=iOS Simulator,name=iPhone 14" -only-testing:KickbaseCoreUITests/KickbaseCoreUITests/test_launchAndOpenPlayerDetail_demoFlow

Notes and recommendations:
- The sample test uses launch environment flags to force demo data and a test mode (`KICKBASE_USE_DEMO_ACCOUNT=1` and `--uitesting`). Implement support in the app to honor these flags for stable UI tests.
- Add `accessibilityIdentifier` values to views you want to target for reliable UI tests (already added: `tab_team`, `player_row_<id>`, `player_firstname`, `player_lastname`).
- For CI, run UI tests on a macOS runner with simulators available, or use a cloud device farm.
