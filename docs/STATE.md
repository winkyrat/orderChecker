# STATE — donut-order-scanner

> Retomá desde acá. Una sesión nueva debe poder continuar leyendo solo este
> archivo y los documentos que referencia.

## Estado actual

- Fecha: 2026-11-16.
- Fase: **Fase 3 verificada en vivo**, con preset de throughput y filas
  incidentales. `0.10.1` corre `1-3 s` con sesgo corto y `pageDelay` 350-900 ms,
  `maxPages` 200. Corta en la primera página que contiene el ítem, **guarda toda
  la página** (las filas de otros ítems valen como dato) y siempre usa
  `Most Per Item`. `0.10.1` agrega un reintento cuando la página 1 abre vacía.
- Build/tests: `gradlew build` verde. **53 tests** (dumps, parser, snapshot,
  merge por página/búsqueda/incidental, config, ritmo, topes de ítems/páginas,
  cursor de ítems). Fixtures reales.
- Aprobación del owner para Fase 1: **dada** (eligió modo C y aceptó el riesgo).
- **Corrida 0.10.0 (2026-11-16, 18:17:58–18:22:06):** 103 búsquedas en 248 s
  (~2,4 s/ítem), 159 exports de página, 5.514 filas guardadas, snapshot de 2.632
  filas y 106 ítems. `diamond` necesitó 39 páginas; `feather` 7, `apple`/`bone` 5,
  `emerald` 3, `egg`/`quartz`/`redstone` 2, 94 ítems 1 página. Un solo menú vacío
  (`netherite_boots`) y ningún ítem mezcló filas incidentales con escaneo directo.
- **Hallazgo:** el buscador matchea también **lore y encantamientos** (ver
  `docs/AUTOSCAN.md`), por eso las materias primas baratas quedan profundo.

## Verificación en vivo de `0.7.0` (2026-11-16, 05:25)

La corrida de las 05:14 usó `0.6.0` y siguió hasta la página 75. La de las 05:25
ya tenía `0.7.0` y cortó donde corresponde:

```
Scan: /order diamond page 42 -> 45 rows [..., minecraft:diamond]
Scan: /order diamond done -> 42 pages, minecraft:diamond found
```

42 páginas en vez de 75, y `minecraft:diamond` quedó con **2 filas**:
`$11.300` (1.959 unidades) y `$11.200` (1.700), ambas en la página 42. Ninguna
orden de `$1`. El corte en una página funciona.

Esa misma corrida dejó a la vista un problema de datos que `0.8.0` corrige: al
paginar hacia `diamond` se guardaban también las filas de los otros ítems de cada
página. El snapshot quedó con 2.595 filas, de las cuales 339 eran de
`diamond_sword` repartidas en las páginas 2-42 de una búsqueda que no era la
suya, con precios de `$1.6M` a `$12.1K`. Al escanear `diamond_sword` de verdad,
su página 1 se fusionaba por página y esas 38 páginas viejas quedaban ahí,
inflando su escalera de demanda.

## Hallazgo de la corrida 1 (2026-11-16, 04:12–04:15)

Corrida autónoma real, cortada a mano con F9 después de 7 búsquedas:

```
Scan started: 60 items, 12-35 s, cap 40 searches / 30 min
Scan: /order diamond  (search 1/40)  -> 44 + 45 + 45 filas
Scan: /order emerald  (search 2/40)  -> 45 + 45 + 45
Scan: /order iron_ingot (3/40)       -> 45 + 45 + 45
Scan: /order gold_ingot (4/40)       -> 45 + 45 + 39
Scan: /order netherite_ingot (5/40)  -> 45 + 45 + 45
Scan: /order coal (6/40)             -> 45 + 45 + 45
Scan: /order redstone (7/40)         -> 45 + 45 + 45
Scan stopped: stopped by key (7 searches, 938 rows)
```

Validado contra DonutProfit (`npx tsx scripts/load-orders.ts`, dry run):
**1118 filas / 37 ítems, 0 descartadas, 1 warning**. Los 37 ítems incluyen las
capturas manuales anteriores (glass, obsidian, diamond_block).

Lo importante: **`/order <nombre>` matchea el id o el nombre como subcadena y
ordena de mayor a menor precio.** Consecuencias medidas:

| Búsqueda | Qué devolvió | ¿Apareció el ítem pedido? |
| --- | --- | --- |
| `diamond` | diamond_leggings/boots/spear/sword/helmet/axe/chestplate, 134 filas de 1M–5.8M | **No.** `minecraft:diamond` (~$1-3K) queda después de todo el gear. |
| `emerald` | deepslate_emerald_ore, emerald_ore, emerald_block | Sí, en la página 3 (27 filas). |
| `coal` | coal_block, coal_ore, deepslate_coal_ore | Sí, 55 filas. |
| `redstone` | redstone_block/ore/torch, deepslate_redstone_ore, **comparator, repeater** | Sí, 30 filas. |
| `iron_ingot` | solo iron_ingot | Sí, 135 filas (3 páginas). |

Dos datos nuevos del servidor:

- El match también usa el **nombre visible**: `redstone` devolvió `comparator` y
  `repeater` ("Redstone Comparator", "Redstone Repeater").
- Los ítems baratos son los últimos de la lista, porque el orden es por precio
  descendente. Los ítems que más interesan para crafteo son justo esos.

### Pruebas de búsqueda exacta (04:27–04:28)

Dos capturas manuales con F8 para ver si hay una forma barata de llegar al ítem
pedido. **Las dos fallaron:**

| Comando | Resultado |
| --- | --- |
| `/order minecraft:diamond` | Menú **vacío**: 0 filas, sin Next page. El id con namespace **no matchea**; el servidor busca sin prefijo. |
| `/order` sin argumentos | **Lista global sin filtrar** con todas las órdenes del servidor, ordenada por precio descendente: 45 elytras de `$ 585M` a `$ 530M`, con Next page. |

Conclusión: mandar el id con `minecraft:` no sirve (el mod ya lo quita, está
bien). El listado global existe y se abre con **un solo comando**, pero al estar
ordenado por precio descendente los ítems baratos quedan a cientos de páginas de
distancia. El filtro `Recently Listed` tampoco sirve: da lo último publicado, no
la escalera de precios.

Decisión del owner: **paginar hasta encontrar el ítem**. Implementado en
`0.5.0`: el escaneo lee páginas hasta que aparece el ítem pedido, sigue una
página más mientras su banda de precios continúe, y corta. `maxPages` (12) es el
tope duro por ítem y `maxPagesPerSession` (250) el tope de clics de la sesión.
Falta medir en vivo cuántas páginas necesita `diamond`.

Efecto secundario útil: `$ 585M each` parsea bien a `585000000`. Quedó el
fixture `orders-global-page1.json` con esa página.

### Medición de profundidad (04:48)

Dos corridas de sonda con un solo ítem (`minecraft:diamond`), `maxPages: 30`,
`maxItemsPerSession: 1`:

| Modo del filtro | Páginas | Filas | ¿Apareció `minecraft:diamond`? |
| --- | --- | --- | --- |
| `Recently Listed` | 4 | 178 | **Sí, en la página 1.** 3 órdenes: `$11.000`, `$11.100`, `$10.000`. Cortó en la p4 al no haber más diamantes. |
| `Most Per Item` | 30 | 1324 | **No.** |

Progresión de precios en `Most Per Item`: p1 `5.3M–2M`, p10 `500K–451K`,
p20 `200K–150K`, p30 `56K–50.5K`. Como el diamante está en `$10-11K`, caería
cerca de la **página 40-50**: unos 45 clics para un solo ítem, contra 4 en
`Recently Listed`.

Conclusiones:

- La escalera completa de precios para nombres genéricos **no es alcanzable con
  pocos clics**: son decenas de páginas. Ni siquiera el gear de diamante entra
  completo en 30 páginas (1350 filas) y seguía en gear. El owner aceptó ese
  costo para tener el precio real.
- `Recently Listed` llega al ítem barato enseguida, pero devuelve una **muestra
  de órdenes recientes**, no el libro completo. Para `diamond` fueron 3 filas.
- `Most Per Item` sí da el **libro completo** de los ítems cuyo nombre no se
  comparte (por ejemplo `iron_ingot`: 135 filas en 3 páginas) y el mejor precio
  arriba, que es lo que usa el motor de flips.
- Aparecieron ítems nuevos del servidor: `minecraft:diamond_nautilus_armor` y
  `minecraft:diamond_hoe`.

Decisión del owner (2026-11-16): **siempre `Most Per Item`**, sin tope práctico
de páginas para *encontrar* el ítem. `Recently Listed` se descarta porque
devuelve lo último publicado y cualquiera puede poner una orden muy por debajo
del precio real. Solo `iron`, `emerald`, `diamond` y `redstone` son caros de
recorrer.

Implementado en `0.6.0` y ajustado en `0.7.0`:

- `maxPages` subió a **200** por ítem y es solo una guarda contra un bucle.
- Se pagina hasta que el ítem aparece y **se corta en esa misma página**: al
  estar ordenada por precio, ya trae sus órdenes más caras. No se lee la banda
  completa.
- `maxPagesPerSession` (**400**) es el control real de cuántos clics permite
  una sesión.
- Si un tope corta la búsqueda antes de encontrar el ítem, el log lo dice
  (`cut after N pages by ...`) en vez de reportarlo como inexistente.
- `scan-cursor.txt` guarda el próximo índice, así una sesión de 40 búsquedas
  sobre 60 ítems no deja los últimos 20 sin escanear.

Medición que motivó el corte en una página (corrida del 2026-11-16, 05:14):
`diamond` apareció en la **página 43** con `$11.200`; su banda seguía **32
páginas más** hasta órdenes de `$1`, incluida una de 1.000 millones de
diamantes a `$1`. La parte útil (`$11.200 → $8.000`) estaba en las primeras
páginas de la banda.

## Instalación en la máquina del owner (2026-11-13)

Perfil aislado, para no mezclar Wurst con DonutSMP:

| Qué | Dónde |
| --- | --- |
| Perfil del launcher | `Donut Scanner 1.21.11` → versión `fabric-loader-0.20.1-1.21.11` |
| Carpeta de juego | `%APPDATA%\.minecraft-donutscanner\` |
| Mods | `donut-order-scanner-0.10.0.jar` (instalado), `fabric-api-0.141.6+1.21.11.jar` |
| Dumps y exports | `%APPDATA%\.minecraft-donutscanner\config\donut_order_scanner\dumps\` |
| Config del escaneo | `%APPDATA%\.minecraft-donutscanner\config\donut_order_scanner\scan.json` |
| Cursor de ítems | `%APPDATA%\.minecraft-donutscanner\config\donut_order_scanner\scan-cursor.txt` |
| Fabric Loader | `0.20.1` (instalado con installer 1.1.2 en `.minecraft/versions`) |

> `0.10.0` instalado el 2026-11-16 (hash `55899CC3…3284` verificado contra
> `build/libs/`). Cambia el merge: guarda la página completa y trata las filas de
> otros ítems como fallback hasta que ese ítem se escanee directo. `scan.json`
> vivo: **103 ítems provisionales**, `1-3 s`, sin pausas largas, `maxPages: 200`,
> `pageDelay` 350-900 ms, `maxPagesPerSession: 2000`, `maxItemsPerSession: 103`.
> El `orders.json` que mezclaba páginas de búsquedas ajenas quedó respaldado
> como `dumps/orders.backup-mixed-pages.json` y se reinició el snapshot: sus
> filas no tienen el campo `search` y no se pueden podar sin ambigüedad. También
> se borró `scan-cursor.txt` para reconstruir el catálogo desde el índice 0.
> Nunca dejar dos jars del mod en `mods/`: Fabric falla por mod id duplicado.
>
> `0.10.1` **construido** el 2026-11-16 (hash `7D6FCE28…51A8`) pero **todavía no
> instalado**: el juego estaba abierto y bloqueaba el jar viejo. Al cerrarlo:
> borrar `donut-order-scanner-0.10.0.jar` de `mods\` y copiar el `0.10.1`. Agrega
> el reintento de menú vacío en página 1.

### Incidente 0.4.0 (2026-11-16)

`0.4.0` crasheaba al arrancar:

```
java.lang.IllegalArgumentException: Category 'donut_order_scanner:main' is already registered.
```

Causa: `KeyMapping.Category.register(...)` se llamaba una vez por keybind (F8 y
F9) y en 1.21.11 registrar la misma categoría dos veces lanza. Fix en `0.4.1`:
una sola categoría compartida. Verificado con `gradlew runClient`: el cliente
carga `donut_order_scanner 0.4.1` y llega al menú sin error de entrypoint.
Lección: los cambios en el entrypoint cliente se prueban con `runClient` antes
de instalar.

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
| `orders-diamond-block-page2.json` | la misma búsqueda, página 2 (70K → 20K) |
| `orders-obsidian-page1.json` | `/order obsidian` (obsidian + crying_obsidian) |
| `orders-diamond-search-page1.json` | `/order diamond` (armadura; una orden ya completa) |
| `orders-empty-search.json` | `/order zzzzz` (sin resultados) |

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
- Paginación confirmada: `/order diamond_block` página 2 tiene 45 filas más,
  precios 70K → 20K, sin solaparse con la página 1 (92K → 73K). El título pasa a
  `Orders (Page 2)` y el parser lo lee de ahí.
- `/order diamond` **no** es búsqueda exacta: devuelve variantes por nombre
  (`diamond_leggings`, `diamond_boots`, `diamond_spear`). `diamond_block` no
  aparece en la página 1 porque sus órdenes (~92K) son mucho más baratas que la
  armadura (2M–5.8M) y caen en páginas siguientes. Ordenado por precio
  descendente, lo barato queda al final.
- `/order zzzzz` abre el menú igual, con **0 órdenes**: el parser no inventa
  filas y deja un warning (`No order rows found`).
- Una orden con `36/36 Delivered` se descarta con warning (ya está completa).
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
| `orders.json` | Snapshot: todas las filas conocidas. |

`orders.json` es el archivo que se le pasa a DonutProfit. Cada fila conserva su
`observedAt`, así que DonutProfit puede descartar lo viejo por ventana de
frescura.

Cómo se fusiona:

- **Escaneo automático (F9):** solo se guardan las filas del ítem buscado, y cada
  fila lleva `search` (el id del ítem). Una búsqueda nueva reemplaza **todas** las
  filas de esa misma búsqueda, sin importar la página: si `diamond` pasa de la
  página 43 a la 41, las filas viejas de la 43 desaparecen.
- **Captura manual (F8):** las filas van sin `search` y la página capturada
  reemplaza solo esa página.

Por qué no se guardan las filas de los otros ítems de la página: al paginar hacia
un ítem barato se ven decenas de páginas de otros ítems. Esas filas son reales
pero son un corte de la banda de precios de otro ítem, en una página que no
significa nada para la búsqueda propia de ese ítem. Guardarlas inflaba su
escalera de demanda y dejaba filas viejas cuando el ítem se escaneaba de verdad.
El archivo de captura (`orders-pN-*.json`) sí conserva la página completa como
evidencia.

### Validación del contrato (2026-11-16, sin tocar DonutProfit)

Las siete capturas reales se reprocesaron sin el juego y el `orders.json`
resultante se pasó por el cargador de DonutProfit en **dry run**:

```
Orders: 224 across 14 items.
  minecraft:diamond_block: 90 rows, 232404 units
  minecraft:crying_obsidian: 40 rows, 262777 units
  minecraft:glass: 22 rows, 2308093 units
  ...
Dry run: nothing written.
```

224 filas, **0 descartadas**. `diamond_block` junta las 90 filas de página 1 y 2
sin pisarse. Se puede repetir con cualquier captura nueva:

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
| D8 | Reemplazo por búsqueda cuando la fila tiene `search`; por página cuando es manual | Una búsqueda es una sola observación de un ítem: así un ítem que se corre de página no deja filas viejas, y una captura manual sigue reemplazando solo su página. |
| D9 | Precio truncado, no redondeado | El menú imprime decimales (`$ 511.2 each`); truncar nunca infla el profit. |

## Próximo paso exacto

1. **Instalar `0.10.1`** (construido, pendiente por el juego abierto): cerrar el
   juego, borrar `donut-order-scanner-0.10.0.jar` de `mods\`, copiar
   `donut-order-scanner-0.10.1.jar`. Es la única versión que reintenta un menú
   vacío en página 1.
2. **Validar el `orders.json` de la corrida 0.10.0** contra DonutProfit
   (`npx tsx scripts/load-orders.ts`, dry run). Revisar dos cosas: que cada ítem
   escaneado directo tenga solo las filas de su escaneo (sin incidentales
   colgadas) y que el dry run no liste ítems fuera del catálogo que vayan a
   romper la FK en `--commit`. Ya se vio que `glass_bottle`,
   `gray_stained_glass` y `purple_stained_glass` llegaron solo como incidentales
   de `glass`, y `netherite_boots` tiene 146 filas incidentales de `feather`.
3. **Ingerir la lista definitiva** cuando el owner la entregue (materias primas +
   ítems crafteados, del grafo de recetas). Solo hay que reemplazar el array
   `items` de `scan.json`; no hace falta recompilar.
4. **Investigar el filtro del menú (slot 47).** Es la única palanca conocida para
   acortar la paginación de las materias primas: si filtra por categoría, un
   `diamond` con menos coincidencias podría aparecer en 5 páginas en vez de 42.
   Requiere un F8 con el filtro abierto para ver qué opciones tiene.
5. Si el cliente no entra con 1.21.11: recompilar para 26.3 (Fabric API
   `0.166.2+26.3`, Java 25, mappings de Mojang). El código es portable.

## Bloqueos

- B1: confirmar que el cliente entra con 1.21.11. Si no, portar a 26.3.
- B2: resuelto. El owner aceptó el riesgo y eligió modo autónomo (C).
- B3: **abierto**. La lista final no existe todavía: el owner la va a definir con
  DeepSeek, o se arma entre los dos desde el grafo de recetas. Los 103 del
  `scan.json` son un seed provisional. 600 es un aproximado, no un objetivo duro.
- B10: **descartado**. Leer el libro sin búsqueda (botón `Orders`, slot 49) no
  sirve: ordena por precio descendente y las materias primas baratas quedan a
  cientos de páginas, además de no ser `Most Per Item`. La vía es la búsqueda por
  ítem, como está implementado.
- B8: resuelto. Modo C, con jitter/pausas/topes y sin evasión.
- B9: **resuelto**. `Most Per Item` es el modo único. `diamond` vive cerca de la
  página 40-50 y se llega paginando profundo; el costo se acota con
  `maxPagesPerSession`. `Recently Listed` descartado por dar datos no
  representativos.
- Dumps reales: **desbloqueado**. Siete fixtures reales.

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
