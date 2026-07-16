# Contraste: requerimientos vs. implementación

Contraste entre el documento **"Análisis de Requerimientos – WiFi BattleShip"** y el estado actual del código.

## Requerimientos funcionales

| # | Requerimiento | Estado | Evidencia en el código |
|---|---|---|---|
| RF1 | Iniciar una partida de batalla naval entre dos jugadores | ✅ Cumplido | Flujo completo Host/Cliente: `MainActivity` → `HostWaitActivity` / `ClientDiscoverActivity` → partida. |
| RF2 | Ubicar los barcos en un tablero | ✅ Cumplido | `PlacementActivity` + `BoardView` (arrastrar y soltar, rotación H/V, quitar barco tocándolo). Validación en `Board.isValidPlacement`. |
| RF3 | Atacar posiciones del tablero enemigo | ✅ Cumplido | `GameActivity.onEnemyCellTap` → confirmación → `GameController.localAttack` → mensaje `ATTACK`. |
| RF4 | Mostrar si el ataque fue agua, impacto o hundido | ✅ Cumplido | `AttackResult {WATER, HIT, SUNK}`; la UI muestra texto y color por resultado y marca las celdas (`onAttackResult`, `markShotResult`). |
| RF5 | Determinar un ganador cuando todos los barcos de un jugador sean destruidos | ✅ Cumplido | `Board.allSunk()` en el defensor envía `GAMEOVER`; el atacante también detecta victoria contando hundidos (`enemySunkCount >= TOTAL_SHIPS`). Resultado en `ResultActivity`. |
| RF6 | Conectar dos dispositivos mediante la misma red **WiFi P2P** con un host y cliente | ✅ Cumplido | Migrado a **WiFi Direct (`WifiP2pManager`)** en julio de 2026: el Host crea el grupo P2P (*group owner*) y anuncia la partida por DNS-SD sobre P2P; el Cliente la descubre, se une al grupo y abre el socket TCP contra la IP del group owner. Ver `docs/04-implementacion-wifi.md` y el plan en `docs/06-plan-wifi-direct.md`. |
| RF7a | Mostrar el tablero propio y el del enemigo | ✅ Cumplido | `GameActivity` muestra dos `BoardView` (modos `OWN` y `ENEMY`) a la vez. |
| RF7b | Indicar el turno del jugador | ✅ Cumplido | Banner de turno con color propio/enemigo (`updateTurnUI`); el tablero enemigo se deshabilita fuera de turno. |
| RF7c | Mostrar el estado de la conexión | ✅ Cumplido | Banner WiFi en `MainActivity` (en vivo con `WifiStateMonitor`); indicador `tvConnection` en `GameActivity` que reacciona a pérdida de red o desconexión del rival. |

## Requerimientos no funcionales

| # | Requerimiento | Estado | Evidencia |
|---|---|---|---|
| S1 | Desarrollado para la plataforma Android | ✅ Cumplido | Proyecto Android Studio, Java, minSdk 26, targetSdk 35. |
| H1 | Equipo móvil con capacidad WiFi | ✅ Cumplido | La app exige WiFi activo (`NetUtils.isWifiReady`) y bloquea el inicio sin él. |
| H2 | Dispositivos conectados a la misma red local | ✅ Superado | Con WiFi Direct los dispositivos **ya no necesitan** estar en la misma red: forman su propio grupo P2P. La "red local" es ahora la que crea el propio grupo. Conviene actualizar la redacción de H2 en el documento de requerimientos. |
| H3 | Pantalla táctil | ✅ Cumplido | Toda la interacción es táctil (arrastrar barcos, tocar celdas, botones). |

## Casos de uso

| Caso de uso | Estado | Notas |
|---|---|---|
| **Seleccionando el rol** — muestra roles Host/Cliente y botón de inicio; si no hay WiFi, inhabilita el botón y muestra mensaje | ✅ Cumplido | `MainActivity`: RadioGroup de rol, botón deshabilitado y banner rojo clicable cuando no hay WiFi (detección en vivo). |
| **Iniciando el rol** — Host ve pantalla de espera; Cliente ve lista de partidas; si el WiFi está apagado se pide activarlo | ✅ Cumplido | `HostWaitActivity` (espera + crea partida disponible), `ClientDiscoverActivity` (lista), diálogos que abren los ajustes de WiFi. |
| **Seleccionando una partida** — lista, selección, conexión al host, interfaz de juego; si falla, error y reintento | ✅ Cumplido | `RecyclerView` de partidas, `resolveService` + `connectAsClient` con timeout, mensaje de error y botón «Reintentar». |
| **Posicionando los barcos** — ubicar, validar (sin superposición), confirmar, notificar al rival | ✅ Cumplido | `Board.isValidPlacement` rechaza superposición/fuera de tablero con Toast de error; «Listo» envía `READY`; el rival recibe aviso (`onOpponentReady`). |
| **Realizando un ataque** — seleccionar posición, confirmar, enviar jugada, mostrar agua/impacto/hundido, actualizar turno; si no es su turno, bloquear con mensaje | ✅ Cumplido | Diálogo de confirmación de ataque, envío `ATTACK`/`RESULT`, resultado con color y celda, alternancia de turno; fuera de turno: Toast «turno del enemigo» y tablero deshabilitado. |
| **Finalizando la partida** — detectar flota destruida, mostrar ganador/perdedor, salir o jugar de nuevo; si se pierde la conexión, mensaje y fin de partida | ✅ Cumplido | `GAMEOVER` + `ResultActivity` (ganaste/perdiste, «Jugar de nuevo» y «Salir»); desconexión → Toast + fin de partida (`onDisconnected`). |

## Resumen

- **Todos los requerimientos funcionales (RF1–RF7), los no funcionales y los 6 casos de uso están cumplidos**, incluidas las alternativas (WiFi apagado, error de conexión con reintento, colocación inválida, ataque fuera de turno, desconexión).
- **RF6 se cerró** con la migración a WiFi Direct (julio de 2026), siguiendo el plan de `docs/06-plan-wifi-direct.md`. La primera versión usaba NSD + TCP en red local; la actual usa la API WiFi P2P real de Android.
- **Pendiente en el documento de requerimientos:** ajustar la redacción de H2 ("misma red local"), porque WiFi Direct crea su propia red y ya no exige un router común.
- **Pendiente de verificación:** pruebas en dos dispositivos físicos (WiFi Direct no funciona en emuladores) según la matriz de la Fase 7 del plan.

Detalle técnico completo en `docs/04-implementacion-wifi.md`.

## Cosas implementadas que el documento no pide (extras)

- **Protocolo versionado** (`PROTOCOL_VERSION`): evita partidas entre versiones incompatibles de la app.
- **Detección de WiFi en vivo** en la pantalla principal y durante la partida (no solo al iniciar).
- **Salida voluntaria notificada** (mensaje `BYE` con el botón atrás) — el rival se entera de que abandonaste en vez de ver un error.
- **Protección contra jugadas repetidas** (celda ya atacada) y contra doble conexión al host.
- **Resaltado del último disparo** en ambos tableros y etiquetas de celda tipo A1–H8.
