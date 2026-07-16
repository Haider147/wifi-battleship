# Plan: migración a WiFi P2P (WiFi Direct)

> **Estado (julio 2026):** Fases 1–6 implementadas y compilando. De la Fase 7 queda pendiente la **prueba en dos dispositivos físicos** (matriz abajo); la documentación ya fue actualizada.

Objetivo: cerrar la única brecha del contraste de requerimientos (**RF6**, ver `05-contraste-requerimientos.md`) migrando el establecimiento de conexión de **NSD + TCP en red local** a **WiFi Direct (`WifiP2pManager`)**.

**Idea clave:** WiFi Direct también termina en sockets TCP (el cliente se conecta a la IP del *group owner*), así que `GameConnection`, `Message`, `GameController` y todo `game/` no se tocan. Solo cambia *cómo* los dispositivos se descubren y se conectan.

## Fases

### Fase 1 — Permisos y manifiesto
- Agregar `CHANGE_WIFI_STATE` (obligatorio para P2P, hoy ausente) y `NEARBY_WIFI_DEVICES` (Android 13+, con `neverForLocation`).
- `ACCESS_FINE_LOCATION` queda limitado a `maxSdkVersion=32` (en 13+ lo reemplaza `NEARBY_WIFI_DEVICES`).
- Quitar permisos que dejan de usarse: `CHANGE_WIFI_MULTICAST_STATE` (era para mDNS), `ACCESS_NETWORK_STATE`, `ACCESS_COARSE_LOCATION`.
- Declarar `<uses-feature android.hardware.wifi.direct required=true />`.
- **Permisos en tiempo de ejecución** (hoy la app no pide ninguno): al entrar a las pantallas de Host y Cliente se solicita `NEARBY_WIFI_DEVICES` (13+) o `ACCESS_FINE_LOCATION` (8–12). Si se niega, mensaje y salida de la pantalla.
- En Android 8–12 el descubrimiento además exige la **ubicación del sistema encendida**: verificación + diálogo que abre los ajustes.

### Fase 2 — Nueva capa de transporte `net/WifiDirectHelper`
Reemplaza a `NsdHelper` con la misma forma de API (callbacks en hilo principal, errores en español):
- Envuelve `WifiP2pManager` + `Channel` + `BroadcastReceiver` (`WIFI_P2P_CONNECTION_CHANGED_ACTION`).
- **Host:** `startHost(puerto, nombre)` → `removeGroup` preventivo → `createGroup()` (fuerza al Host como *group owner*) → `addLocalService` con DNS-SD sobre P2P (`WifiP2pDnsSdServiceInfo`), anunciando el puerto TCP en el registro TXT.
- **Cliente:** `discoverGames()` → `setDnsSdResponseListeners` + `addServiceRequest` + `discoverServices`; cada respuesta DNS-SD se convierte en un `DiscoveredGame {nombre, dispositivo, puerto}`.
- **Conexión:** `connectTo(game)` → `manager.connect()` → al llegar `CONNECTION_CHANGED` con grupo formado, `requestConnectionInfo()` entrega la IP del group owner → se abre el socket TCP de siempre.
- `teardown()`: `cancelConnect`, `clearServiceRequests`, `clearLocalServices`, `stopPeerDiscovery`, `removeGroup`, y des-registro del receiver.

### Fase 3 — Lado Host (`HostWaitActivity`)
- Mantiene el `ServerSocket` + `GameConnection.acceptAsHost` intactos.
- Intenta el puerto fijo `GameConfig.SERVICE_PORT` y cae a puerto efímero si está ocupado (el puerto real siempre viaja en el TXT).
- Sustituye el registro NSD por `WifiDirectHelper.startHost(...)`.

### Fase 4 — Lado Cliente (`ClientDiscoverActivity`)
- El descubrimiento P2P alimenta el mismo `RecyclerView`; `DiscoveredGameAdapter` pasa de `NsdServiceInfo` a `DiscoveredGame`.
- Al elegir partida: `connectTo()` → IP del group owner + puerto del TXT → `GameConnection.connectAsClient` **sin cambios**.
- El DNS-SD sobre P2P no emite eventos de "servicio perdido": la lista se limpia con el botón «Reintentar».

### Fase 5 — Estado de la conexión
- `WifiStateMonitor` pasa de `ConnectivityManager` a escuchar `WIFI_P2P_STATE_CHANGED_ACTION` (broadcast sticky: entrega el estado actual al registrarse).
- La app ya no exige estar *conectado* a una red WiFi (WiFi Direct crea la suya); basta con WiFi encendido → `NetUtils.isWifiEnabled`.
- `GameActivity` reemplaza su `NetworkCallback` por un receiver de `WIFI_P2P_CONNECTION_CHANGED_ACTION` para el banner de conexión.

### Fase 6 — Ciclo de vida y limpieza
- `GameSession` guarda el `WifiDirectHelper` en lugar del `NsdHelper`; `reset()` hace el `teardown()` completo (sin esto, el grupo P2P queda vivo entre partidas y rompe «Jugar de nuevo»).
- Se elimina `NsdHelper` y las utilidades que solo servían para mDNS (MulticastLock, IP WiFi local).

### Fase 7 — Pruebas y documentación
- Actualizar `docs/04` (implementación WiFi) y `docs/05` (RF6 pasa a cumplido; H2 queda superado: WiFi Direct crea su propia red).
- **Pruebas solo en dispositivos físicos** (WiFi Direct no funciona en emuladores). Matriz mínima:
  - Un equipo Android ≤ 12 y uno ≥ 13 (los dos caminos de permisos).
  - Flujo completo: crear partida → descubrir → conectar → jugar → resultado → «Jugar de nuevo» encadenado.
  - Abandono a mitad de partida (BYE), pérdida de conexión (apagar WiFi), negar permisos, reintento tras fallo.

## Riesgos conocidos
- **Diálogo de invitación del sistema:** al conectarse el cliente, muchos dispositivos muestran en el Host un diálogo que el usuario debe aceptar. No se puede suprimir por código.
- **Fragilidad entre fabricantes:** descubrimientos que no arrancan, grupos zombis. Mitigación: `removeGroup` preventivo antes de crear grupo y `teardown()` agresivo; presupuestar depuración en hardware real.
- **H2 del PDF** ("misma red local") queda contradicho por diseño: WiFi Direct crea su propia red. Hay que actualizar esa línea del documento de requerimientos.
