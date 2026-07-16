# Estructura del proyecto — paso a paso

Este documento describe cómo se estructuró **WiFi BattleShip** y el orden en que se construyó, siguiendo el historial real del repositorio.

## Estructura final

```
wifi-battleship/
├── build.gradle                  # Plugin Android (AGP 8.13.2)
├── settings.gradle
├── app/
│   ├── build.gradle              # minSdk 26, targetSdk 35, Java 17
│   └── src/main/
│       ├── AndroidManifest.xml   # Permisos de red y declaración de activities
│       ├── java/app/wifibattleship/
│       │   ├── MainActivity.java     # Pantalla inicial (selección de rol)
│       │   ├── GameSession.java      # Singleton que comparte estado entre activities
│       │   ├── game/                 # Lógica pura del juego (sin dependencias de UI)
│       │   │   ├── GameConfig.java   # Constantes: tablero 8x8, barcos 4/3/2, puerto, tipo NSD
│       │   │   ├── GameController.java  # Máquina de estados de la partida
│       │   │   ├── Board.java / Ship.java / Cell.java
│       │   │   └── GamePhase / Role / Orientation / AttackResult (enums)
│       │   ├── net/                  # Capa de red
│       │   │   ├── GameConnection.java  # Socket TCP + hilo lector
│       │   │   ├── Message.java / MessageType.java  # Protocolo JSON versionado
│       │   │   ├── MessageSender.java / MessageListener.java  # Interfaces de desacople
│       │   │   ├── WifiDirectHelper.java # WiFi Direct: grupo P2P + DNS-SD + conexión
│       │   │   ├── DiscoveredGame.java   # Partida descubierta (nombre, dispositivo, puerto)
│       │   │   ├── NetUtils.java        # Utilidades WiFi (radio encendido, nombre de servicio)
│       │   │   └── WifiStateMonitor.java # Vigila el estado de WiFi P2P en vivo
│       │   └── ui/                   # Activities y vistas
│       │       ├── P2pPermissions.java  # Permisos runtime de WiFi Direct
│       │       ├── HostWaitActivity.java
│       │       ├── ClientDiscoverActivity.java / DiscoveredGameAdapter.java
│       │       ├── PlacementActivity.java
│       │       ├── GameActivity.java
│       │       ├── ResultActivity.java
│       │       └── view/BoardView.java  # Vista personalizada que dibuja el tablero
│       └── res/                  # Layouts, drawables, strings (en español), colores, temas
```

## Principio de diseño: tres capas

El proyecto se organizó en **tres capas** con responsabilidades separadas:

1. **`game/` — lógica del juego.** No importa nada de UI de Android. Se puede razonar (y probar) sin dispositivo. `GameController` es el corazón: recibe mensajes de red y notifica a la UI mediante la interfaz `GameController.Listener`.
2. **`net/` — transporte.** Sockets TCP, WiFi Direct (grupo P2P + descubrimiento DNS-SD) y protocolo de mensajes. Todo el trabajo de red ocurre en hilos en segundo plano, pero **todos los callbacks se entregan en el hilo principal**.
3. **`ui/` — presentación.** Cada pantalla es una Activity; reaccionan a los eventos del controlador y nunca tocan el socket directamente.

Las capas se conectan a través del singleton **`GameSession`**, que guarda el rol (HOST/CLIENT), la conexión, el controlador y el `WifiDirectHelper`, de modo que las activities comparten estado sin pasar objetos por Intents.

## Paso a paso de la construcción

El historial de commits refleja el orden real de desarrollo:

### Paso 1 — Creación del proyecto y estructura base (`59ac470`)
- Proyecto Android Studio en Java, un solo módulo `app`.
- Configuración de Gradle: `compileSdk 35`, `minSdk 26`, Java 17, ViewBinding.
- Permisos en el Manifest: `INTERNET`, `ACCESS_WIFI_STATE`, `ACCESS_NETWORK_STATE`, `CHANGE_WIFI_MULTICAST_STATE` y ubicación (necesaria para descubrimiento en algunas versiones de Android).
- Esqueleto de paquetes: `game/`, `net/`, `ui/`.

### Paso 2 — Lógica del juego y capa de red (`df3bb45`)
- Modelo del juego: `Board` (matriz 8x8 de `Cell`), `Ship` (tamaño, orientación, impactos), reglas de colocación y resolución de ataques (`AttackResult`: agua / impacto / hundido).
- `GameController`: máquina de estados `PLACEMENT → PLAYING → ENDED`, manejo de turnos y condición de victoria.
- Protocolo de mensajes: JSON por línea sobre TCP (`Message`, `MessageType`: READY, FIRST_TURN, ATTACK, RESULT, GAMEOVER, BYE).
- `GameConnection`: conexión TCP con hilo lector; `NsdHelper`: registro y descubrimiento de partidas en la red local.

### Paso 3 — Interfaz de usuario (`02fc0b6`)
- Flujo de pantallas: selección de rol → espera/descubrimiento → colocación de barcos → juego → resultado.
- `BoardView`: vista personalizada que dibuja el tablero con Canvas, soporta arrastrar-y-soltar barcos, previsualización de colocación y toque para atacar.

### Paso 4 — Mejoras visuales (`61171bb`)
- Banners de turno y de estado de conexión, chips de barcos, colores por resultado (agua/impacto/hundido), etiquetas de celdas (A1–H8).

### Paso 5 — Ajustes finales y correcciones (`d00de0f`)
- Confirmación de ataque con diálogo, mensajes de error en español, manejo del botón atrás (envía BYE al rival).

### Paso 6 — Robustez general (`b152534`)
- Protección contra dobles conexiones, banderas `destroyed` para evitar tocar la UI tras cerrar la Activity, limpieza de recursos en `onDestroy`, cierre ordenado de sockets y del `ServerSocket` del host.

### Paso 7 — Versionado del protocolo (`c6e9d61`)
- Campo `v` (`PROTOCOL_VERSION`) en todos los mensajes; versiones incompatibles se rechazan al parsear y provocan la desconexión.

### Paso 8 — Detección de WiFi en vivo (`26e321f`)
- `WifiStateMonitor` con `ConnectivityManager.NetworkCallback`: la pantalla principal habilita/deshabilita el botón de inicio según el estado real del WiFi y ofrece abrir los ajustes si está apagado.

### Paso 9 — Migración a WiFi P2P / WiFi Direct (julio 2026)
- Se reemplazó el descubrimiento NSD (mDNS en red local) por **WiFi Direct** (`WifiP2pManager`) para cumplir literalmente el requerimiento RF6: el Host crea el grupo P2P y anuncia la partida por DNS-SD sobre P2P; el Cliente se une al grupo y conecta el mismo socket TCP contra la IP del *group owner*.
- Se añadieron los permisos en tiempo de ejecución (`NEARBY_WIFI_DEVICES` / `ACCESS_FINE_LOCATION`) — ver `06-plan-wifi-direct.md`.
- La lógica del juego y el protocolo de mensajes no cambiaron.
