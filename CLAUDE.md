# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Android app (Java, minSdk 26, targetSdk 35) for two-player Battleship over **WiFi Direct (WiFi P2P)**. The host creates the P2P group (group owner) and advertises the game via DNS-SD over P2P; the client joins the group and connects to a TCP server on the group owner's IP. No backend, no external dependencies beyond AndroidX/Material. UI strings, commit messages, and log/error text are in Spanish — keep that convention.

## Commands

```
.\gradlew.bat assembleDebug     # build debug APK
.\gradlew.bat installDebug      # install on connected device/emulator
.\gradlew.bat lint              # Android lint
```

There are no unit or instrumented tests in the project. Real verification requires two physical devices — WiFi Direct does not work on emulators.

## Architecture

Three layers, wired together by a singleton:

- **`GameSession`** (`app/wifibattleship/GameSession.java`) — process-wide singleton holding the `Role` (HOST/CLIENT), the `GameConnection`, the `GameController`, the `WifiDirectHelper`, and the host's `ServerSocket`. Activities share game state through it instead of intent extras. `GameSession.reset()` tears everything down (including the P2P group); it lazily wires the controller as the connection's `MessageListener` on first `getController()` call.

- **`net/`** — transport, all callbacks delivered on the main thread:
  - `GameConnection` — newline-delimited JSON over a TCP socket; background reader thread; static `acceptAsHost`/`connectAsClient` factories. Any parse error or IO error closes the connection and fires `onDisconnected`.
  - `Message` / `MessageType` — versioned protocol (`PROTOCOL_VERSION` in `Message`). Types: READY, FIRST_TURN, ATTACK, RESULT, GAMEOVER, BYE. Coordinates are validated against the 8x8 board on both build and parse. Bump `PROTOCOL_VERSION` on any wire-format change — mismatched versions are rejected at parse time.
  - `WifiDirectHelper` — WiFi Direct wrapper: host side does `removeGroup` (preventive) → `createGroup` → `addLocalService` (DNS-SD over P2P, TCP port in the TXT record); client side does DNS-SD discovery (`DiscoveredGame` items) and `connect`, delivering the group owner's IP via the `WIFI_P2P_CONNECTION_CHANGED_ACTION` broadcast. `teardown()` must run between games or the stale group breaks "play again".
  - `WifiStateMonitor` — live WiFi P2P state watching via the sticky `WIFI_P2P_STATE_CHANGED_ACTION` broadcast (used by MainActivity).
  - P2P calls need `NEARBY_WIFI_DEVICES` (API 33+) or `ACCESS_FINE_LOCATION` (API 26–32) at runtime — `ui/P2pPermissions` centralizes this; on API < 33 discovery also requires system location to be ON.

- **`game/`** — pure game logic, no Android UI dependencies:
  - `GameController` — state machine (`GamePhase`: PLACEMENT → PLAYING → ENDED) implementing `MessageListener`; translates network messages into `GameController.Listener` UI callbacks. The HOST decides the first turn randomly once both sides send READY. Each player tracks only hits/misses on the enemy board (opponent ship positions are never transmitted); win detection for the attacker counts SUNK results against `GameConfig.TOTAL_SHIPS`.
  - `GameConfig` — the constants that define the game: 8x8 board, ships of size 4/3/2, TCP port, NSD service type. Board size 8 is also hardcoded in `Message` validation.
  - `Board` / `Ship` / `Cell` — placement and attack resolution.

- **`ui/`** — activity flow: `MainActivity` (choose role) → `HostWaitActivity` or `ClientDiscoverActivity` (establish connection) → `PlacementActivity` (place ships, send READY) → `GameActivity` (play) → `ResultActivity`. `BoardView` (`ui/view/`) is a custom View that renders both boards.

## Conventions

- Threading: network work on daemon threads; every listener/callback into UI or controller code is posted to the main looper. Keep that invariant when touching `net/`.
- `GameController` and `game/` classes must stay free of Android UI imports (only `android.util.Log`-level deps at most) — UI reacts via `GameController.Listener`.
