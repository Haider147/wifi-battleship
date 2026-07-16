# Cómo implementamos la conectividad WiFi P2P (WiFi Direct)

> **Nota histórica:** la primera versión de la app usaba NSD (mDNS/DNS-SD) + TCP sobre la misma red WiFi local. En julio de 2026 se migró a **WiFi Direct (`WifiP2pManager`)** para cumplir literalmente el requerimiento RF6 ("WiFi P2P"). El plan de esa migración está en `06-plan-wifi-direct.md`.

## Qué es ahora

Los dos teléfonos forman un **grupo WiFi Direct propio** (no necesitan router ni estar en la misma red): el Host crea el grupo y queda como *group owner*; el Cliente lo descubre por DNS-SD sobre P2P y se une. Una vez formado el grupo, la partida viaja por un **socket TCP normal** hacia la IP del group owner — la capa de juego y el protocolo de mensajes no cambiaron.

## Arquitectura de red

```
   HOST (group owner)                        CLIENTE
┌───────────────────────────┐          ┌───────────────────────────┐
│ HostWaitActivity          │          │ ClientDiscoverActivity    │
│  1. ServerSocket          │          │  1. discoverGames()       │
│     (puerto 50556 o       │  DNS-SD  │     (DnsSd sobre P2P)     │
│      efímero)             │  sobre   │  2. lista de partidas     │
│  2. createGroup()         │  WiFi    │  3. connectTo(partida)    │
│  3. addLocalService() ────┼──Direct─►│  4. CONNECTION_CHANGED →  │
│     TXT: {port}           │          │     IP del group owner    │
│  4. accept() ◄────────────┼───TCP────┼── 5. Socket.connect()     │
└───────────────────────────┘          └───────────────────────────┘
          │                                      │
          └────────► GameConnection ◄────────────┘
              (JSON por línea, hilo lector,
               callbacks en el hilo principal)
```

## Piezas de la implementación

### 1. Permisos (`AndroidManifest.xml`)

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_WIFI_STATE" />
<uses-permission android:name="android.permission.CHANGE_WIFI_STATE" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"
    android:maxSdkVersion="32" />
<uses-permission android:name="android.permission.NEARBY_WIFI_DEVICES"
    android:usesPermissionFlags="neverForLocation" />
<uses-feature android:name="android.hardware.wifi.direct" android:required="true" />
```

- **Android 13+**: se pide `NEARBY_WIFI_DEVICES` en tiempo de ejecución (con `neverForLocation` no hace falta ubicación).
- **Android 8–12**: se pide `ACCESS_FINE_LOCATION` en tiempo de ejecución y además la **ubicación del sistema debe estar encendida** para descubrir; la app lo verifica y ofrece abrir los ajustes.
- La lógica de qué permiso pedir vive en `ui/P2pPermissions.java`; `HostWaitActivity` y `ClientDiscoverActivity` lo solicitan antes de tocar la API P2P.

### 2. La envoltura de la API: `net/WifiDirectHelper`

Encapsula `WifiP2pManager` + `Channel` (inicializado sobre el main looper, así los callbacks llegan al hilo principal) + un `BroadcastReceiver` de `WIFI_P2P_CONNECTION_CHANGED_ACTION`. Errores del sistema (`P2P_UNSUPPORTED`, `BUSY`, `ERROR`) se traducen a mensajes en español.

### 3. Lado Host (`HostWaitActivity`)

1. Abre el `ServerSocket`: intenta el puerto fijo `GameConfig.SERVICE_PORT` (50556) y cae a un puerto efímero si está ocupado.
2. `WifiDirectHelper.startHost(puerto, nombre)`:
   - `removeGroup()` **preventivo** (limpia grupos zombis de partidas anteriores),
   - `createGroup()` — fuerza que el Host sea el *group owner*, alineando el rol de la app con el rol de red,
   - `addLocalService()` con `WifiP2pDnsSdServiceInfo`: anuncia el nombre de la partida (`wifibattleship-N-Modelo`) con el tipo `_wifibattleship._tcp` y el **puerto TCP real en el registro TXT**.
3. `GameConnection.acceptAsHost()` espera al cliente exactamente igual que antes (solo se acepta una conexión).

### 4. Lado Cliente (`ClientDiscoverActivity`)

1. `WifiDirectHelper.discoverGames()`: registra `setDnsSdResponseListeners` + `addServiceRequest` + `discoverServices`. El listener de TXT guarda el puerto por dirección de dispositivo; el de servicio arma un `DiscoveredGame {nombre, dispositivo, puerto}` (filtrado por el prefijo `wifibattleship-`).
2. La lista alimenta el mismo `RecyclerView` de siempre (el DNS-SD sobre P2P no emite "servicio perdido", así que la lista se limpia al «Reintentar»).
3. Al elegir partida, `connectTo()`: `manager.connect()` con WPS PBC y `groupOwnerIntent = 0` (cede el rol de owner al Host). **En muchos dispositivos el Host verá un diálogo del sistema para aceptar la invitación** — es comportamiento de Android, no de la app.
4. Cuando llega `WIFI_P2P_CONNECTION_CHANGED_ACTION` con el grupo formado, `requestConnectionInfo()` entrega `groupOwnerAddress` → `GameConnection.connectAsClient(ip, puertoDelTXT)` — el mismo socket TCP de siempre, timeout de 5 s.

### 5. El canal de datos (`GameConnection` + `Message`) — sin cambios

JSON por línea en UTF-8, protocolo versionado (`"v": 1`), tipos `READY / FIRST_TURN / ATTACK / RESULT / GAMEOVER / BYE`, hilo lector daemon, escritura sincronizada, callbacks en el hilo principal, y cualquier error de E/S o parseo termina la partida por desconexión.

### 6. Estado de la conexión

- `net/WifiStateMonitor` escucha `WIFI_P2P_STATE_CHANGED_ACTION` (broadcast *sticky*: entrega el estado actual nada más registrarse). `MainActivity` lo usa para el banner y para habilitar el botón de inicio.
- Ya **no** se exige estar conectado a una red WiFi — basta el radio encendido (`NetUtils.isWifiEnabled`), porque WiFi Direct crea su propia red.
- `GameActivity` registra su propio receiver de `WIFI_P2P_CONNECTION_CHANGED_ACTION`: si el grupo se cae en plena partida, el banner pasa a "desconectado" (y la desconexión real la detecta el socket).

### 7. Ciclo de vida y limpieza

`GameSession.reset()` → `WifiDirectHelper.teardown()`: cancela conexión pendiente, limpia peticiones de servicio y servicios locales, detiene el descubrimiento y hace `removeGroup()`. Sin esto, el grupo P2P sobrevive entre partidas y rompe «Jugar de nuevo». Las activities de Host/Cliente limpian en `onDestroy` si el usuario cancela antes de conectar.

## Limitaciones conocidas

- **Solo dispositivos físicos:** WiFi Direct no funciona en emuladores.
- **Diálogo de invitación:** el sistema puede pedir confirmación en el Host al unirse el cliente; no se puede suprimir por código.
- **Fragilidad entre fabricantes:** si el descubrimiento no arranca, el botón «Reintentar» rehace la petición completa; el `removeGroup` preventivo del Host mitiga los grupos zombis.
