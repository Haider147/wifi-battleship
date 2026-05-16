# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

WiFi Battleship — juego Android multijugador local (2 jugadores) vía WiFi Direct o Bluetooth. Tablero y barcos configurables. UI en español.

- Requerimientos completos: `C:\Users\jpjur\Desktop\APP\Análisis de Requerimientos-WiFiBattleShip.pdf`
- Repositorio remoto: https://github.com/Haider147/wifi-battleship

## Build & Run

```bash
# Build debug APK
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Install on device
./gradlew installDebug
```

> WiFi P2P y Bluetooth requieren **dispositivos físicos** — el emulador no soporta estas APIs.

Primera vez en Android Studio: `File > Open` → seleccionar esta carpeta → Android Studio descarga el Gradle wrapper automáticamente.

## Stack

| Capa | Tecnología |
|------|-----------|
| Plataforma | Android minSdk 30 (Android 11), compileSdk 35 |
| Lenguaje | **Java** |
| UI | XML Views + ViewBinding |
| Navegación | Jetpack Navigation Component (Single Activity) + SafeArgs |
| Arquitectura | MVVM — ViewModel + LiveData |
| Red | WiFi P2P (`WifiP2pManager`) + Bluetooth Classic (RFCOMM) |
| Serialización | Gson (mensajes de protocolo) |

## Arquitectura

### Paquetes

```
com.wifibattleship/
├── ui/
│   ├── MainActivity.java            # Single activity, host del NavController
│   ├── fragments/
│   │   ├── MenuFragment             # Selección WiFi / BT / Configuración
│   │   ├── LobbyFragment            # Discovery y conexión entre dispositivos
│   │   ├── SettingsFragment         # Tamaño de tablero y lista de barcos
│   │   ├── ShipPlacementFragment    # Colocación de barcos en el tablero
│   │   ├── GameFragment             # Tablero propio + tablero enemigo, turnos
│   │   └── ResultFragment           # Pantalla de victoria/derrota
│   └── viewmodels/
│       ├── GameViewModel            # Compartido entre Placement/Game/Result
│       └── LobbyViewModel           # Discovery y estado de conexión
├── game/
│   ├── GameConfig.java             # BoardSize + lista de ShipConfig (configurable)
│   ├── Ship.java                   # Posición, orientación, hits, isSunk()
│   ├── Board.java                  # Grid 2D, placeShip(), receiveAttack(), allSunk()
│   └── GameEngine.java             # Turno, ataque local/remoto, isGameOver()
└── network/
    ├── ConnectionManager.java       # Interface común para ambos transportes
    ├── WifiDirectManager.java       # WifiP2pManager + sockets TCP sobre grupo P2P
    ├── BluetoothConnectionManager.java # BluetoothAdapter + RFCOMM sockets
    └── protocol/
        └── Message.java            # Mensajes JSON: ATTACK, ATTACK_RESULT, GAME_CONFIG…
```

### Flujo de navegación

```
MenuFragment → LobbyFragment(connectionType: "WIFI"|"BLUETOOTH")
            → ShipPlacementFragment
            → GameFragment
            → ResultFragment → MenuFragment (popUpTo)
```

`GameViewModel` se comparte entre `ShipPlacementFragment`, `GameFragment` y `ResultFragment` via `new ViewModelProvider(requireActivity()).get(GameViewModel.class)`.

### Capa de red

`ConnectionManager` abstrae ambos transportes. `LobbyFragment` instancia `WifiDirectManager` o `BluetoothConnectionManager` según el argumento `connectionType`.

- **WiFi Direct**: `WifiDirectManager.openSocket()` abre un `ServerSocket` en el host (Group Owner, IP `192.168.49.1`, puerto 8888) y un `Socket` en el cliente.
- **Bluetooth**: `BluetoothConnectionManager.listenAsHost()` usa RFCOMM con UUID fijo. El cliente llama a `connect()`.

Ambos envían/reciben líneas de texto JSON en un `ExecutorService` de hilo único. Los callbacks se reciben en el thread del executor — hacer `runOnUiThread()` o `postValue()` antes de actualizar LiveData.

### Protocolo de mensajes (`Message.Type`)

| Tipo | Dirección | Payload |
|------|-----------|---------|
| `GAME_CONFIG` | host → cliente | JSON de `GameConfig` |
| `SHIP_PLACEMENT_READY` | ambos | vacío |
| `ATTACK` | turno activo → pasivo | `"row,col"` |
| `ATTACK_RESULT` | pasivo → activo | `"row,col,HIT\|MISS\|SUNK"` |
| `GAME_OVER` | cualquiera | `"LOCAL"\|"REMOTE"` |
| `REMATCH_REQUEST` / `REMATCH_ACCEPT` | cualquiera | vacío |

### Configuración del juego

`GameConfig` se crea en `SettingsFragment`, se guarda en `SharedPreferences`, y se envía al dispositivo remoto como `GAME_CONFIG` antes de la colocación de barcos. Tamaño de tablero: 5–15 (default 10). Barcos configurables en nombre, tamaño y cantidad.

## Permisos en runtime requeridos

- `ACCESS_FINE_LOCATION` — WiFi P2P discovery en Android 11–12
- `NEARBY_WIFI_DEVICES` — WiFi P2P en Android 13+
- `BLUETOOTH_SCAN`, `BLUETOOTH_CONNECT`, `BLUETOOTH_ADVERTISE` — Bluetooth en Android 12+

Solicitar todos antes de llamar a `startDiscovery()`. Usar `ActivityResultContracts.RequestMultiplePermissions`.
