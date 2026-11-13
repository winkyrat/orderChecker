# STATE — donut-order-scanner

> Retomá desde acá. Una sesión nueva debe poder continuar leyendo solo este
> archivo y los documentos que referencia.

## Estado actual

- Fecha: 2026-11-13.
- Fase: **Fase 2 implementada**. F8 dumpea la pantalla y, si es el menú de
  órdenes, además escribe el export compatible con DonutProfit.
- Build/tests: `gradlew build` verde. **16 tests** (dumps, números, parser,
  snapshot). Fixture real: `src/test/resources/fixtures/orders-glass-page1.json`.
- Aprobación del owner para Fase 1: **dada** (eligió modo C y aceptó el riesgo).

## Instalación en la máquina del owner (2026-11-13)

Perfil aislado, para no mezclar Wurst con DonutSMP:

| Qué | Dónde |
| --- | --- |
| Perfil del launcher | `Donut Scanner 1.21.11` → versión `fabric-loader-0.20.1-1.21.11` |
| Carpeta de juego | `%APPDATA%\.minecraft-donutscanner\` |
| Mods | `donut-order-scanner-0.2.0.jar`, `fabric-api-0.141.6+1.21.11.jar` |
| Dumps y exports | `%APPDATA%\.minecraft-donutscanner\config\donut_order_scanner\dumps\` |
| Fabric Loader | `0.20.1` (instalado con installer 1.1.2 en `.minecraft/versions`) |

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

## Datos reales observados (`/order glass`, página 1, 2026-11-13)

Evidencia en `src/test/resources/fixtures/orders-glass-page1.json`.

- Menú de 90 slots: **0–44 órdenes**, 45–46 vacíos, 47 filtro (hopper),
  48 Shard Shop, 49 Orders, 50 Search, 51 Your Orders, 53 Next page,
  54–89 inventario del jugador.
- Título: `Orders (Page 1)`. No incluye el ítem buscado.
- El slot **sí muestra el ítem real** (`minecraft:glass`, `tinted_glass`,
  `black_stained_glass`…), no un placeholder. `count` del stack es siempre 1:
  no sirve como cantidad.
- Lore de una orden:
  - `$ 4K each` → precio por unidad (el comprador paga).
  - `872/1k Delivered` → **entregado / necesario** (confirmado por el owner).
  - `Click to deliver items` → la orden es un pedido de entrega ⇒ `side: "buy"`.
- El inventario del jugador usa `~$ 275` (valor estimado total). El prefijo `~`
  se usa para excluirlo; no es precio unitario.
- Formato numérico: `4K`, `1.9K`, `6.2k`, `38.1k`, `128k`, `1m`.
- `quantityRemaining = necesario − entregado`. Si es ≤ 0 la orden ya está
  completa y se descarta con un warning.

### Archivos que escribe F8 en el menú de órdenes

| Archivo | Contenido |
| --- | --- |
| `orders_page_1-<ms>.json` | Dump crudo (evidencia, todos los slots). |
| `orders-p1-<ms>.json` | Export de esa captura, formato DonutProfit. |
| `orders.json` | Snapshot acumulado: mejor precio por ítem, cada fila con su `observedAt`. |

`orders.json` es el archivo que se le pasa a DonutProfit.

### Validación del contrato (2026-11-13, sin tocar DonutProfit)

El export generado desde el fixture real se pasó por el cargador de DonutProfit
en **dry run** (`pnpm db:load-orders <archivo>`, sin `--commit`):

```
Orders: 45 across 8 items.
Observed at: 2026-11-13T04:24:17.195577Z
  minecraft:glass: 22 rows, 2308093 units
  ...
Dry run: nothing written.
```

45 filas, **0 filas descartadas**. `orders.json` es aceptado tal cual. Se puede
repetir con cualquier captura nueva:

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

## Próximo paso exacto

1. Reiniciar el juego (el jar pasó a `0.2.0`; el que estaba cargado era `0.1.1`).
2. Capturar en DonutSMP, con F8 en cada menú:
   - `/order diamond` (ítem no-vidrio: confirma que el `itemId` es el real),
   - `/order diamond` página 2 (confirma paginación y el slot "Next page"),
   - `/order zzzzz` o búsqueda sin resultados (caso borde).
3. Verificar `orders.json` y pasarlo a DonutProfit.
4. Con más dumps: ampliar fixtures y cubrir ítems con nombre custom
   (spawners, libros, pociones) por si el icono deja de ser el ítem real.
5. Fase 3 (escaneo autónomo, modo C): diseño con jitter, pausas y topes.
6. Si el cliente no entra con 1.21.11: recompilar para 26.3 (Fabric API
   `0.166.2+26.3`, Java 25, mappings de Mojang). El código es portable.

## Bloqueos

- B1: confirmar que el cliente entra con 1.21.11. Si no, portar a 26.3.
- B2: resuelto. El owner aceptó el riesgo y eligió modo autónomo (C).
- B3: lista de ítems. Provisional: los 60 del seed de DonutProfit.
- B8: resuelto. Modo C, con jitter/pausas/topes y sin evasión.
- Dumps reales: sin ellos no se escribe el parser (Fase 2).

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
