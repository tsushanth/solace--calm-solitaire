# Solace: Calm Solitaire

A calm, distraction-free take on classic Klondike Solitaire for Android — built with Jetpack Compose and Material 3. No timers pressuring you, no ads interrupting play; just a felt table, a deck of cards, and a quiet undo button.

## Features

- **Classic Klondike Solitaire** with configurable draw-1 or draw-3 modes
- **Drag-and-drop card play** alongside tap-to-auto-move-to-foundation for quick single moves
- **Undo** (limited for free players, unlimited with Premium) and one-tap **auto-complete** for winnable games
- **Live stats bar** — score, moves, and elapsed time — plus a statistics sheet tracking games played, win rate, and (Premium) best time, best score, and streaks
- **Table felt themes** — Classic, Midnight, and Crimson — with Material You dynamic color support for app chrome on Android 12+
- **Sound effects and haptic feedback** for moves, drags, and wins, each independently toggleable
- **Solace Premium** — unlocks Draw-1 mode, unlimited undo, auto-complete, advanced statistics, and every felt theme, via Google Play Billing (subscriptions, lifetime purchase, and an optional tip)
- Fully accessible UI with TalkBack content descriptions on every pile and control, and edge-to-edge dark theming

## Requirements

- **Android 7.0 (API 24) or higher** — `minSdk 24`, `targetSdk`/`compileSdk 34`
- Android Studio (Koala or newer recommended) with the Android SDK Platform 34 installed
- JDK 17

## Build Instructions

1. Clone the repository and open it in Android Studio, or build from the command line.
2. Ensure `local.properties` points at your Android SDK (Android Studio generates this automatically):
   ```
   sdk.dir=/path/to/Android/sdk
   ```
3. Build and install a debug build:
   ```
   ./gradlew installDebug
   ```
4. Or just assemble an APK:
   ```
   ./gradlew assembleDebug
   ```
5. Run unit/instrumented tests:
   ```
   ./gradlew test
   ./gradlew connectedAndroidTest
   ```

A release build requires signing configuration and real Terms of Service / Privacy Policy URLs (see `app/src/main/res/values/strings.xml`) before Play Store submission.

## Project Structure

```
app/src/main/java/com/factory/solacecalmsolitaire/
├── engine/          Pure Klondike game rules and state transitions (GameState, SolitaireEngine)
├── model/           Card, Suit, Rank, and PileId domain types
├── viewmodel/       GameViewModel, StatsViewModel, PaywallViewModel — UI state holders
├── ui/
│   ├── screens/     GameScreen, SettingsSheet, StatsSheet, WinDialog, PaywallScreen
│   ├── components/  PlayingCardView, PileViews, DragController, ProBadge
│   └── theme/       Material 3 theme, felt table colors, dimens
├── data/
│   ├── local/       Room database and DAO for game results
│   ├── datastore/   Proto/Preferences DataStore for user settings
│   └── repository/  StatsRepository aggregating game history into StatsSummary
├── premium/         PremiumManager and feature/trigger definitions for gating
├── billing/         Google Play Billing integration (BillingManager, PurchaseVerifier)
└── util/            SoundPlayer and other small utilities
```
