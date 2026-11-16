# STATE — donut-order-scanner

> Retomá desde acá. Una sesión nueva debe poder continuar leyendo solo este
> archivo y los documentos que referencia.

## Estado actual

- Fecha: 2026-11-16.
- Fase: **Fase 2 implementada y validada contra DonutProfit**. F8 dumpea la
  pantalla y, si es el menú de órdenes, además escribe el export.
- Build/tests: `gradlew build` verde. **21 tests**. Fixtures reales de
  `/order glass`, `/order diamond_block` y `/order obsidian`.
- Aprobación del owner para Fase 1: **dada** (eligió modo C y aceptó el riesgo).

## Instalación en la máquina del owner (2026-11-13)

Perfil aislado, para no mezclar Wurst con DonutSMP:

| Qué | Dónde |
| --- | --- |
| Perfil del launcher | `Donut Scanner 1.21.11` → versión `fabric-loader-0.20.1-1.21.11` |
| Carpeta de juego | `%APPDATA%\.minecraft-donutscanner\` |
| Mods | `donut-order-scanner-0.3.0.jar`, `fabric-api-0.141.6+1.21.11.jar` |
| Dumps y exports | `%APPDATA%\.minecraft-donutscanner\config\donut_order_scanner\dumps\` |
| Fabric Loader | `0.20.1` (instalado con installer 1.1.2 en `.minecraft/versions`) |

> `0.3.0` está compilado en `build/libs/` pero **todavía no está instalado**: la
> instancia abierta tiene el jar viejo bloqueado. Al cerrar el juego, borrar
> `donut-order-scanner-0.2.0.jar` de `mods/` y copiar el `0.3.0`. Nunca dejar los
> dos: Fabric falla por mod id duplicado.

- Se copiaron `options.txt`, `optionsof.txt`, `servers.dat` y `servers.dat_old`
  al game dir para no perder ajustes ni servidores.
- `%APPDATA%\.minecraft\mods\` **no se tocó**: sigue con `fabric-api-0.140.0`
  y `Wurst-Client-v7.51.4`. El perfil aislado no carga esa carpeta.
- Antes de escribir `launcher_profiles.json` se guardó
  `launcher_profiles.json.bak-donutscanner`. El launcher llegó a reescribir el
  archivo dejando solo 2 perfiles; se restauró desde el backup y quedó con 33.
  Si algo raro pasa con los perfiles, ese `.bak` es la referencia.
- Log de la primera corrida: `Loading Minecraft 1.21.11 with Fabric Loader 0.20.1`,
  `donut_order_scanner 0.1.0` entre los 49 mods, sin `ERROR` ni `WARN`.

## Hecho

- Proyecto creado en `donut-order-scanner/`, repo git inicializado en `main`.
- `docs/PLAN.md`: investigación, contrato de DonutProfit, decisión de loader,
  fases, arquitectura y fuentes.
- `docs/RISKS.md`: ban, menú, datos, dependencia, privacidad, legal.
- `docs/OPEN_QUESTIONS.md`: bloqueantes B1, B2, B8 y resto.
- Identificado DonutProfit real en `../oreboard` (no se modificó).
- Contrato de orders y lista demo de 60 ítems documentados.
- Mod Fabric 1.21.11 con keybind F8 que vuelca la pantalla abierta a JSON.
- **Fix 0.1.1:** vanilla ignora los keybinds mientras hay una pantalla abierta,
  así que F8 no podía dumpear con el menú abierto (síntoma: "No screen open"
  en el chat justo después de cerrar el menú). Ahora el mod escucha
  `ScreenKeyboardEvents.allowKeyPress` por pantalla y consume la tecla. Sonido
  de confirmación porque el chat no se ve con un contenedor abierto.
- `docs/CAPTURE_GUIDE.md` con los pasos exactos de captura.
- **Fase 2 (0.2.0):** parser puro + export. Ver "Datos reales observados".
- Toolchain local: JDK 25 en `.tools/` (Loom 1.19.4 lo exige para compilar;
  el mod compila con `--release 21` y corre en Java 21).

## Datos reales observados (2026-11-13)

Evidencia en `src/test/resources/fixtures/`:

| Fixture | Captura |
| --- | --- |
| `orders-glass-page1.json` | `/order glass` (45 filas, 8 variantes de vidrio) |
| `orders-diamond-block-page1.json` | `/order diamond_block` (45 filas, un solo ítem) |
| `orders-obsidian-page1.json` | `/order obsidian` (obsidian + crying_obsidian) |

- Menú de 90 slots: **0–44 órdenes**, 45–46 vacíos, 47 filtro (hopper),
  48 Shard Shop, 49 Orders, 50 Search, 51 Your Orders, 53 Next page,
  54–89 inventario del jugador.
- Título: `Orders (Page 1)`. No incluye el ítem buscado.
- El slot **sí muestra el ítem real** (`minecraft:glass`, `tinted_glass`,
  `minecraft:diamond_block`…), no un placeholder. `count` del stack es siempre
  1: no sirve como cantidad.
- Lore de una orden:
  - `$ 4K each` → precio por unidad (el comprador paga).
  - `872/1k Delivered` → **entregado / necesario** (confirmado por el owner).
  - `Click to deliver items` → la orden es un pedido de entrega ⇒ `side: "buy"`.
- El inventario del jugador usa `~$ 275` (valor estimado total). El prefijo `~`
  se usa para excluirlo; no es precio unitario.
- Formato numérico: `4K`, `1.9K`, `6.2k`, `38.1k`, `128k`, `1m`.
- **Precios con decimales**: `$ 511.2 each`. El parser trunca (511) porque
  DonutProfit guarda unidades enteras y redondear hacia abajo nunca infla el
  profit.
- Las filas vienen **ordenadas por precio descendente** (filtro por defecto
  `Most Paid`). La página 1 tiene los mejores precios.
- Una búsqueda puede llenar la página con **un solo ítem** (45 órdenes de
  `diamond_block`), así que "mejor precio por ítem" perdía la profundidad del
  libro: el snapshot guarda **todas** las filas.
- El slot 53 (`arrow`, `Next page`) confirma que hay más de una página.
- El filtro (slot 47) ofrece `Most Per Item`, `Most Paid`, `Recently Listed`.
  Hoy no se toca: se asume el orden por defecto.
- `quantityRemaining = necesario − entregado`. Si es ≤ 0 la orden ya está
  completa y se descarta con un warning.

### Archivos que escribe F8 en el menú de órdenes

| Archivo | Contenido |
| --- | --- |
| `orders_page_1-<ms>.json` | Dump crudo (evidencia, todos los slots). |
| `orders-p1-<ms>.json` | Export de esa captura, formato DonutProfit. |
| `orders.json` | Snapshot: todas las filas conocidas; una captura reemplaza solo su página. |

`orders.json` es el archivo que se le pasa a DonutProfit. Cada fila conserva su
`observedAt`, así que DonutProfit puede descartar lo viejo por ventana de
frescura. Reemplazar por página evita mezclar una fila vieja con una nueva del
mismo slot, y a la vez permite juntar página 1 y página 2 del mismo ítem.

### Validación del contrato (2026-11-16, sin tocar DonutProfit)

Los tres dumps reales se reprocesaron sin el juego y el `orders.json` resultante
se pasó por el cargador de DonutProfit en **dry run**:

```
Orders: 135 across 11 items.
  minecraft:diamond_block: 45 rows, 153585 units
  minecraft:crying_obsidian: 40 rows, 262777 units
  minecraft:glass: 22 rows, 2308093 units
  ...
Dry run: nothing written.
```

135 filas, **0 descartadas**. Se puede repetir con cualquier captura nueva:

```powershell
cd "..\oreboard"
npx tsx scripts/load-orders.ts "<ruta>\orders.json"
```

## Decisiones tomadas

| ID | Decisión | Motivo |
| --- | --- | --- |
| D1 | Fabric, no Forge/NeoForge | Menor peso, mejor soporte cliente, template oficial, mixins solo si hacen falta. |
| D2 | Minecraft 1.21.11 (provisional) | Ficha del servidor lo recomienda; JDK 21 instalado; Yarn disponible. Revisar B1. |
| D3 | Export JSON compatible con `parseOrdersJson` | DonutProfit ya lo consume; evita tocar el repo hermano. |
| D4 | Fase 3 autónoma **aprobada por el owner** (2026-11-13), riesgo aceptado | El owner eligió opción C. Se implementa con jitter, pausas y topes; sin evasión (brand, ocultamiento, paquetes, movimiento falso). |
| D5 | Parser puro, sin imports de Minecraft | Testeable con fixtures reales. |
| D6 | Intervalo fijo de 3 s descartado | Un patrón perfecto es más detectable. Se usa jitter con cola larga. |
| D7 | El snapshot guarda **todas** las filas, no el mejor precio | DonutProfit arma una escalera de demanda por ítem (`addLevel`); colapsar a una fila subestima cuánto compra el mercado. |
| D8 | Reemplazo por (ítem, página) al fusionar | Permite juntar página 1 y 2 del mismo ítem sin mezclar filas viejas del mismo slot. |
| D9 | Precio truncado, no redondeado | El menú imprime decimales (`$ 511.2 each`); truncar nunca infla el profit. |

## Próximo paso exacto

1. Capturar con el juego ya abierto (`0.3.0` en `mods/`). F8 en cada menú:
   - página 2 de `/order diamond_block` (botón `Next page`, slot 53),
   - `/order zzzzz` (búsqueda sin resultados),
   - `/order diamond` (¿trae `diamond` solo, o también `diamond_block`?),
   - opcional: un ítem con nombre custom (spawner, libro, poción).
2. Reprocesar los dumps sin el juego y volver a pasar `orders.json` por el
   cargador de DonutProfit en dry run.
3. Fase 3 (escaneo autónomo, modo C): diseño con jitter, pausas y topes.
4. Si el cliente no entra con 1.21.11: recompilar para 26.3 (Fabric API
   `0.166.2+26.3`, Java 25, mappings de Mojang). El código es portable.

## Bloqueos

- B1: confirmar que el cliente entra con 1.21.11. Si no, portar a 26.3.
- B2: resuelto. El owner aceptó el riesgo y eligió modo autónomo (C).
- B3: lista de ítems. Provisional: los 60 del seed de DonutProfit.
- B8: resuelto. Modo C, con jitter/pausas/topes y sin evasión.
- Dumps reales: **desbloqueado**. Hay tres capturas reales como fixtures.

## Comandos previstos

```powershell
cd "C:\Users\winky\OneDrive\Documentos\Default Project\donut-order-scanner"
.\gradlew build
.\gradlew runClient
```

## Archivos que no se deben tocar sin permiso

- `../oreboard/**` (DonutProfit).
- Cualquier `.env`, token o credencial.
- `run/` y `config/` generados por el cliente.
