# Funciones implementadas

Resumen de la funcionalidad de **WiFi BattleShip**, organizada por capa. Las rutas son relativas a `app/src/main/java/app/wifibattleship/`.

## Funcionalidad de cara al usuario

| Función | Dónde vive |
|---|---|
| Seleccionar rol (Host / Cliente) e iniciar | `MainActivity` |
| Detección en vivo del estado de WiFi P2P (banner + botón deshabilitado si no hay WiFi) | `MainActivity` + `net/WifiStateMonitor` |
| Diálogo para activar el WiFi (abre ajustes del sistema) | `MainActivity`, `HostWaitActivity`, `ClientDiscoverActivity` |
| Solicitud de permisos en runtime (`NEARBY_WIFI_DEVICES` / ubicación) y aviso de ubicación apagada | `ui/P2pPermissions` + `HostWaitActivity`, `ClientDiscoverActivity` |
| Crear grupo WiFi Direct y anunciar la partida (host) | `ui/HostWaitActivity` + `net/WifiDirectHelper` |
| Buscar partidas por WiFi Direct y listarlas (cliente) | `ui/ClientDiscoverActivity` + `DiscoveredGameAdapter` |
| Reintentar búsqueda si falla o no aparece nada | `ui/ClientDiscoverActivity` (botón Reintentar) |
| Colocar barcos arrastrándolos al tablero (4, 3 y 2 casillas) | `ui/PlacementActivity` + `ui/view/BoardView` |
| Rotar orientación del barco (horizontal/vertical) | `ui/PlacementActivity` |
| Validación de colocación (dentro del tablero, sin superposición) | `game/Board.isValidPlacement` |
| Quitar un barco ya colocado tocándolo | `ui/PlacementActivity.onCellTap` |
| Botón «Listo» (se habilita solo con todos los barcos puestos) y aviso de rival listo | `ui/PlacementActivity` |
| Ver tablero propio y tablero enemigo simultáneamente | `ui/GameActivity` (dos `BoardView`) |
| Indicador de turno (banner propio/enemigo, tablero enemigo bloqueado fuera de turno) | `ui/GameActivity.updateTurnUI` |
| Atacar una celda del tablero enemigo con confirmación | `ui/GameActivity.onEnemyCellTap` |
| Bloqueo de ataques fuera de turno o a celdas ya atacadas (con mensaje) | `ui/GameActivity` + `game/GameController.localAttack` |
| Mostrar resultado del ataque: agua / impacto / hundido, con color y celda (p. ej. «Impacto — C5») | `ui/GameActivity.onAttackResult` |
| Resaltar el último disparo recibido/realizado | `BoardView.setLastHit` |
| Detectar fin de partida y mostrar ganador/perdedor | `ui/ResultActivity` |
| «Jugar de nuevo» o salir al terminar | `ui/ResultActivity` |
| Estado de conexión en pantalla y manejo de desconexión (rival se va o se cae la red) | `ui/GameActivity` |
| Salir de la partida con el botón atrás avisando al rival (mensaje BYE) | `ui/GameActivity` (OnBackPressedCallback) |

## Lógica del juego (`game/`)

- **`GameConfig`** — constantes de la partida: tablero 8×8, flota `{4, 3, 2}`, puerto y tipo de servicio NSD.
- **`Board`** — matriz de celdas (`WATER, SHIP, HIT, MISS, SUNK`):
  - `isValidPlacement` / `placeShip` / `removeShipAt` — colocación con validación.
  - `receiveAttack` — resuelve un ataque entrante y devuelve `WATER`, `HIT` o `SUNK` (marca todo el barco al hundirse).
  - `markShotResult` — registra en el tablero enemigo el resultado que reporta el rival.
  - `wasAlreadyAttacked` / `allSunk` — consultas para bloquear repetidos y detectar derrota.
- **`Ship`** — posición, orientación, conteo de impactos, `isSunk()`.
- **`GameController`** — máquina de estados central (implementa `MessageListener`):
  - Fases `PLACEMENT → PLAYING → ENDED`.
  - `setLocalReady()` — envía READY; cuando ambos están listos, **el host sortea quién empieza** y lo comunica con FIRST_TURN.
  - `localAttack(x, y)` — valida turno y celda, cede el turno y envía ATTACK.
  - `handleIncomingAttack` — resuelve el ataque en el tablero propio, responde RESULT y, si toda la flota está hundida, envía GAMEOVER.
  - `handleAttackResult` — marca el tablero enemigo; cuenta hundidos y declara victoria al llegar a `TOTAL_SHIPS`.
  - Notifica a la UI mediante `GameController.Listener` (turno, tableros, resultado, fin de juego, desconexión).

## Capa de red (`net/`)

- **`Message` / `MessageType`** — protocolo JSON por línea con **versionado** (`PROTOCOL_VERSION`): tipos `READY`, `FIRST_TURN`, `ATTACK`, `RESULT`, `GAMEOVER`, `BYE`. Coordenadas validadas (0–7) al construir y al parsear; versión incompatible ⇒ rechazo.
- **`GameConnection`** — conexión TCP:
  - `acceptAsHost` / `connectAsClient` — establecimiento asíncrono con timeout de 5 s en cliente.
  - Hilo lector en segundo plano; entrega de mensajes **siempre en el hilo principal**.
  - `send` sincronizado, keep-alive y TCP_NODELAY activados.
  - Cierre idempotente; cualquier error de E/S o de parseo dispara `onDisconnected`.
- **`WifiDirectHelper`** — WiFi Direct de punta a punta: `startHost` (removeGroup preventivo → `createGroup` → `addLocalService` DNS-SD con el puerto TCP en el TXT), `discoverGames` (listeners DNS-SD → `DiscoveredGame`), `connectTo` (join al grupo → IP del *group owner* vía `CONNECTION_CHANGED`) y `teardown()` (limpieza completa del grupo).
- **`DiscoveredGame`** — modelo de partida descubierta: nombre, dispositivo P2P y puerto.
- **`NetUtils`** — utilidades: `isWifiEnabled` (WiFi Direct solo necesita el radio encendido) y nombre de servicio único (`wifibattleship-N-Modelo`).
- **`WifiStateMonitor`** — observa el estado de WiFi P2P con el broadcast sticky `WIFI_P2P_STATE_CHANGED_ACTION` y avisa cambios en vivo.

## Sesión (`GameSession`)

Singleton que comparte entre activities: rol, conexión, controlador, `WifiDirectHelper` y `ServerSocket` del host. `reset()` cierra y limpia todo, incluido el grupo P2P (se usa al empezar una partida nueva y al salir).
