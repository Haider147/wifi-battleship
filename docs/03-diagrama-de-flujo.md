# Diagrama de flujo del software

Diagramas en formato **Mermaid** (se renderizan en GitHub, VS Code y Android Studio con plugin).

## 1. Flujo general de pantallas

```mermaid
flowchart TD
    A[MainActivity<br/>Selección de rol] -->|WiFi OK + rol Host| B[HostWaitActivity<br/>Crea partida y espera]
    A -->|WiFi OK + rol Cliente| C[ClientDiscoverActivity<br/>Busca partidas en la red]
    A -->|WiFi apagado| A2[Diálogo: activar WiFi<br/>abre ajustes del sistema]
    A2 --> A

    B -->|Crea grupo WiFi Direct,<br/>anuncia partida y acepta TCP| D[PlacementActivity<br/>Colocación de barcos]
    C -->|Selecciona partida,<br/>se une al grupo y conecta| D

    D -->|Ambos jugadores READY<br/>host sortea primer turno| E[GameActivity<br/>Partida por turnos]
    E -->|Victoria / derrota /<br/>desconexión / BYE| F[ResultActivity<br/>Ganaste / Perdiste]

    F -->|Jugar de nuevo<br/>GameSession.reset| A
    F -->|Salir| G((Fin))
```

## 2. Establecimiento de la conexión (Host vs Cliente)

```mermaid
sequenceDiagram
    participant H as Host (group owner)
    participant P2P as WiFi Direct (DNS-SD sobre P2P)
    participant C as Cliente

    H->>H: ServerSocket (puerto 50556 o efímero)
    H->>P2P: createGroup() — el host queda como group owner
    H->>P2P: addLocalService("_wifibattleship._tcp", TXT: puerto)
    H->>H: accept() en hilo "wbs-host-accept"

    C->>P2P: discoverServices() (DNS-SD)
    P2P-->>C: onGameFound (lista de partidas + puerto del TXT)
    C->>P2P: connect(dispositivo del host)
    Note over H: El sistema puede pedir<br/>aceptar la invitación
    P2P-->>C: CONNECTION_CHANGED → IP del group owner
    C->>H: Socket TCP connect (timeout 5 s)
    H-->>C: Conexión aceptada
    Note over H,C: Ambos crean GameConnection<br/>e inician el hilo lector
```

## 3. Desarrollo de la partida (protocolo de mensajes)

```mermaid
sequenceDiagram
    participant A as Jugador A (Host)
    participant B as Jugador B (Cliente)

    A->>B: READY (barcos colocados)
    B->>A: READY
    Note over A: El host sortea quién empieza
    A->>B: FIRST_TURN {role}
    Note over A,B: phase = PLAYING

    loop Turnos alternados
        A->>B: ATTACK {x, y}
        Note over B: Board.receiveAttack(x, y)
        B->>A: RESULT {x, y, WATER|HIT|SUNK}
        Note over A,B: Se actualizan tableros<br/>y cambia el turno
    end

    alt Toda la flota de B hundida
        B->>A: GAMEOVER {winner}
        Note over A,B: ResultActivity (A gana, B pierde)
    else Un jugador sale con "atrás"
        A->>B: BYE
        Note over B: Desconexión voluntaria, fin de partida
    end
```

## 4. Lógica de un ataque dentro de `GameController`

```mermaid
flowchart TD
    T[Toque en celda enemiga] --> V{¿Es mi turno y la celda<br/>no fue atacada?}
    V -->|No| M[Toast: turno enemigo /<br/>celda repetida]
    V -->|Sí| CF[Diálogo de confirmación]
    CF -->|Atacar| S[localAttack: cede turno<br/>y envía ATTACK]
    S --> R[Rival: receiveAttack<br/>en su tablero]
    R --> RES[Rival responde RESULT]
    RES --> U[handleAttackResult:<br/>marca tablero enemigo]
    U --> W{¿Resultado = SUNK y<br/>hundidos == TOTAL_SHIPS?}
    W -->|Sí| GO[phase = ENDED<br/>onGameOver: gané]
    W -->|No| TN[Recupero el turno<br/>cuando el rival ataca]

    R --> X{¿allSunk en el<br/>tablero del rival?}
    X -->|Sí| GO2[Rival envía GAMEOVER<br/>y pierde]
    X -->|No| TN2[El rival toma el turno]
```

## 5. Estados de la partida

```mermaid
stateDiagram-v2
    [*] --> PLACEMENT: Conexión establecida
    PLACEMENT --> PLAYING: READY de ambos + FIRST_TURN
    PLAYING --> ENDED: GAMEOVER (flota hundida)
    PLAYING --> ENDED: BYE (rival abandona)
    PLAYING --> ENDED: Desconexión (error de red)
    PLACEMENT --> ENDED: Desconexión
    ENDED --> [*]: ResultActivity y GameSession.reset()
```
