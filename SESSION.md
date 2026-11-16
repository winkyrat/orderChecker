# SESSION — interacciones con el humano

> Registro **aditivo**. Cada agente agrega una entrada al final y una fila al
> índice. Nunca reescribir ni resumir entradas viejas.

## Cómo leer este archivo (obligatorio)

**No leer `SESSION.md` completo.** Es largo y crece sin límite.

1. Leer el índice de abajo (tabla `S1..Sn`).
2. Leer **solo la última entrada**:
   `Get-Content SESSION.md -Tail 60`.
3. Para un tema puntual:
   `Select-String -Path SESSION.md -Pattern "<keyword>"`.

## Índice

| ID | Fecha | Tema | Keywords |
| --- | --- | --- | --- |
| S1 | 2026-11-13 | Objetivo, límites y prioridades | objetivo, ban, datos, español, cliente |
| S2 | 2026-11-13 | Stack y toolchain | fabric, 1.21.11, java, jdk25, loom |
| S3 | 2026-11-13 | Parser, export y merge | parser, zod, orders.json, truncar, merge |
| S4 | 2026-11-13 | Aprobación del escaneo autónomo | fase3, modoC, ritmo, 60items, riesgo |
| S5 | 2026-11-16 | Primera corrida real | f9, 938filas, subcadena, diamond |
| S6 | 2026-11-16 | Medición de profundidad | recently, mostperitem, 30paginas, 11k |
| S7 | 2026-11-16 | Modo único y sin tope | mostperitem, sinlimite, 0.6.0, cursor |
| S8 | 2026-11-16 | Una sola página | 75paginas, banda, $1, 0.7.0 |
| S9 | 2026-11-16 | Multi-agente y GitHub | agents, roadmap, log, session, push |
| S10 | 2026-11-16 | Verificación 0.7.0 y snapshot limpio | 42paginas, verificado, 2595filas, search, 0.8.0 |
| S11 | 2026-11-16 | Cadencia de 5 segundos | cadencia, 5segundos, riesgo, 0.8.1 |
| S12 | 2026-11-16 | Cadencia aleatoria y evasión | aleatorio, sesgo, anticheat, limite, 0.8.2 |
| S13 | 2026-11-16 | 600 ítems en 30 minutos | throughput, 600items, presupuesto, libro, 0.9.0 |
| S14 | 2026-11-16 | Corrección: crafteo, no barrido | crafteo, recetas, lista, maxPages, correccion |
| S15 | 2026-11-16 | Filas incidentales | incidental, fallback, merge, pagina1, 0.10.0 |
| S16 | 2026-11-16 | Revisión de la corrida 0.10.0 | corrida, 248s, 2632filas, lore, menu-vacio, 0.10.1 |

---

## S1 — Objetivo, límites y prioridades

- **El humano pidió:** un mod cliente que lea los precios de `/order` de
  DonutSMP y exporte JSON que DonutProfit pueda consumir; instalarlo en su
  Minecraft y que funcione manual (F8) y autónomo (F9).
- **Decisiones:** chat y docs en español; código, archivos y commits en inglés.
  Prioridades en orden: riesgo de ban, corrección de datos, velocidad por
  eficiencia. Solo cliente, sin credenciales, sin secretos. No tocar
  `../oreboard` sin permiso.
- **Por qué:** el humano quiere datos reales para calcular profit sin arriesgar
  la cuenta ni ensuciar el repo hermano.
- **Keywords:** objetivo, ban, datos, español, cliente, oreboard.

## S2 — Stack y toolchain

- **El humano aceptó:** Fabric con Minecraft 1.21.11 y Java 21.
- **Decisión instrumental:** JDK 25 portable en `.tools/` porque Loom 1.19.4 lo
  exige; el mod compila con `--release 21`.
- **Alternativas descartadas:** instalar JDK 25 global; Forge/NeoForge; Yarn.
- **Keywords:** fabric, 1.21.11, java21, jdk25, loom, mojang.

## S3 — Parser, export y merge

- **El humano pidió:** que el export sea compatible con DonutProfit sin tocar
  DonutProfit.
- **Decisiones:** esquema de `parseOrdersJson`; fila con `side:"buy"`,
  `quantityRemaining = required - delivered`, precios truncados, snapshot con
  todas las filas, merge por `itemId|page|slot`.
- **Validación:** dry run de `scripts/load-orders.ts` con 1118 filas / 37 ítems,
  0 descartadas, 1 warning.
- **Keywords:** parser, zod, orders.json, truncar, merge, dryrun.

## S4 — Aprobación del escaneo autónomo

- **El humano eligió:** modo C (autónomo) y aceptó el riesgo.
- **Ritmo acordado:** medio, 12-35 s entre búsquedas, pausas largas raras, tope
  de 40 búsquedas / 30 min. Lista inicial: los 60 ítems del catálogo.
- **Límites explícitos:** sin promesa de indetectabilidad, sin spoof de marca, sin
  ocultar el mod, sin paquetes falsos, sin movimiento sintético.
- **Keywords:** fase3, modoC, ritmo, 60items, riesgo, jitter.

## S5 — Primera corrida real

- **Resultado:** 7 búsquedas, 938 filas, 21 capturas, sin fallos; cortada a mano
  con F9.
- **Hallazgo:** `/order <nombre>` matchea id o nombre visible como subcadena y
  ordena por precio descendente. `/order minecraft:diamond` abre menú vacío.
- **Consecuencia:** `diamond` no apareció; el ítem barato queda al final de su
  banda. Se agregó log honesto de qué ítems vinieron por página.
- **Keywords:** f9, 938filas, subcadena, diamond, menú vacío.

## S6 — Medición de profundidad

- **El humano preguntó:** cuántas páginas hacen falta para `diamond`.
- **Medición:** `Recently Listed` lo encontró en la página 1 con 3 órdenes
  (`$10K`, `$11K`, `$11.1K`) en 4 páginas; `Most Per Item` no lo encontró en 30
  páginas (1324 filas, todavía en gear a `$50-56K`).
- **Conclusión:** `Recently Listed` da una muestra reciente, no la escalera; el
  libro completo de nombres genéricos no es barato.
- **Keywords:** recently, mostperitem, 30paginas, 11k, profundidad.

## S7 — Modo único y sin tope

- **El humano decidió:** siempre `Most Per Item`, sin límite práctico de páginas,
  aunque cueste decenas de clics. `Recently Listed` queda descartado porque
  cualquiera puede poner órdenes muy por debajo del precio real.
- **Implementado (0.6.0):** `maxPages` 200 como guarda; `maxPagesPerSession` 400
  como control real; aviso de ítem cortado; `scan-cursor.txt` para rotar los 60
  ítems entre sesiones.
- **Keywords:** mostperitem, sinlimite, 0.6.0, cursor, 400paginas.

## S8 — Una sola página

- **El humano observó:** en la corrida real, `diamond` apareció en la página 43 y
  el escaneo siguió hasta la 75 sin parar.
- **Evidencia:** la banda tenía 386 órdenes de diamante en 32 páginas, de
  `$11.200` a `$1`, incluida una orden trol de 1.000 millones a `$1`. La parte
  útil (`$11.200 → $8.000`) ya estaba en las primeras páginas.
- **Decisión del humano:** "solo deberías encontrar 1 página con el ítem; la
  página mayoritariamente solo necesita la orden más cara".
- **Implementado (0.7.0):** cortar en la primera página que contiene el ítem.
  Snapshot reiniciado (backup en `dumps/orders.backup-before-onepage.json`) y
  cursor borrado para arrancar en `diamond`.
- **Keywords:** 75paginas, banda, $1, 0.7.0, unapagina.

## S9 — Multi-agente y GitHub

- **El humano pidió:** trabajar con otro agente y dejar trazabilidad en
  `ROADMAP.md`, `AGENTS.md`, `LOG.md` y `SESSION.md`; commits claros en cambios
  estructurales, sin PR, push directo a una rama; publicar en
  <https://github.com/winkyrat/orderChecker>.
- **Decisión:** los cuatro archivos con las reglas en `AGENTS.md`; `SESSION.md`
  aditivo con lectura parcial obligatoria; rama única `main`.
- **Herramienta:** `gh` no está instalado; el push usa el credential manager de
  Git ya configurado. No se manejan tokens a mano.
- **Pendiente:** confirmar el primer push y que un agente nuevo pueda retomar
  leyendo solo `AGENTS.md` + `ROADMAP.md` + `docs/STATE.md` + la última entrada.
- **Keywords:** agents, roadmap, log, session, push, github, main.

## S10 — Verificación 0.7.0 y snapshot limpio

- **El humano no habló en esta entrada:** es continuación del trabajo pedido en
  S9 (dejar el proyecto andando y publicado) más la verificación pendiente.
- **Verificado en vivo:** la corrida de las 05:25 usó `0.7.0` y cortó en
  `done -> 42 pages, minecraft:diamond found`; `diamond` quedó con `$11.300` y
  `$11.200`, sin `$1`. La corrida de 75 páginas era la `0.6.0` de las 05:14.
- **Hallazgo:** el snapshot tenía 2.595 filas y mezclaba páginas de búsquedas
  ajenas (339 de `diamond_sword` en páginas 2-42 de la búsqueda de `diamond`).
- **Decisión instrumental:** `0.8.0` guarda en el snapshot solo las filas del
  ítem buscado, con un campo `search`; una búsqueda reemplaza todas sus filas.
  Snapshot respaldado (`orders.backup-mixed-pages.json`) y reiniciado; cursor
  borrado para reconstruir desde el índice 0.
- **Pendiente:** correr una sesión con `0.8.0`, validar el `orders.json` contra
  DonutProfit en dry run y confirmar que ningún ítem pasa de 45 filas.
- **Keywords:** 42paginas, verificado, 2595filas, search, 0.8.0, snapshot.

## S11 — Cadencia de 5 segundos

- **El humano pidió:** "bajar mucho la cantidad de tiempo entre nuevo ítem
  checkeado", con 5 s como referencia.
- **Decisión:** ventana `5-9 s` (no 5 fijo, para no dejar un metrónomo). Se
  mantienen la pausa larga del 5 % de 120-300 s y los topes de 40 búsquedas /
  30 min.
- **Medición:** 14 búsquedas pasan de 5 min 24 s a ~1 min 40 s. La lista de 60
  se cubre en dos sesiones de ~5 min, y el tope que corta es el de 40 búsquedas.
- **Riesgo explicitado:** son 7-12 comandos por minuto sostenidos, contra 2-3
  antes. Se le dijo al humano que eso se parece más a un bot; la decisión es
  suya. La ráfaga de `diamond` (~42 clics de Next page) no cambia.
- **Keywords:** cadencia, 5segundos, riesgo, 0.8.1, ventana.

## S12 — Cadencia aleatoria y pedido de evasión

- **El humano pidió:** que el tiempo entre `/order` sea aleatorio pero dentro de
  un rango entendible; y que investigue y aplique "todas las maneras" en que
  mods/hacks evitan servicios de detección, a nivel de automatización. Aclaró
  que acepta el riesgo legal/ToS y quiere "las menores chances factibles" de ban.
- **Hecho:** sorteo sesgado dentro de `5-12 s` (`min * (max/min)^(u²)`), 3 % de
  pausa de 1-2 min. Promedio ~10-11 s por ítem. `0.8.2`, 50 tests.
- **No hecho, y por qué:** no se investigó ni se implementó evasión de
  anti-cheat (ocultar el mod, spoofear el brand, falsificar paquetes, input
  sintético, bypass de AC). Está fuera del alcance fijado en `AGENTS.md` §4 y
  además no funcionaría contra un servidor que ya ve los comandos.
- **Lo que sí reduce el riesgo:** menos volumen. Menos ítems por sesión, más
  espera entre páginas, no correr todos los días a la misma hora, y no repetir
  la misma lista en el mismo orden (el cursor ya rota).
- **Keywords:** aleatorio, sesgo, anticheat, limite, 0.8.2, volumen.

## S13 — 600 ítems en menos de 30 minutos

- **El humano pidió:** analizar ~600 ítems en menos de 30 min.
- **Medido:** la corrida de las 06:21 hizo 38 ítems en 359 s (~9,4 s/ítem), 100
  páginas. El trabajo fijo por ítem es ~1,3 s y cada página extra ~0,7 s.
- **Hecho:** preset de throughput en `0.9.0` — `1-3 s` con sesgo, `maxPages` 3,
  `pageDelay` 150-350 ms, cierre 150 ms, clamps a 1 s / 1000 búsquedas. Da
  ~2,8 s por ítem: 600 en ~28 min.
- **Costo:** ~21 comandos por minuto, y `diamond` (página 42) queda cortado por
  el tope de 3 páginas.
- **Lista:** 103 ítems con evidencia, no 600. No se inventaron ids. DonutProfit
  tiene 61 ids; el registry de vanilla daría menús vacíos.
- **Lo que falta para 600 reales:** leer el libro sin búsqueda (botón `Orders`,
  slot 49) en vez de mandar 600 comandos. Requiere medir cuántas páginas tiene.
- **Keywords:** throughput, 600items, presupuesto, libro, 0.9.0.

## S14 — Corrección: es arbitraje de crafteo, no barrido

- **El humano corrigió:** DonutProfit compara el costo de las materias primas
  contra el valor del ítem crafteado. La lista incluye las dos puntas, por eso es
  enorme. Los 103 vistos son solo lo que apareció en pocas pasadas.
- **Y descartó el barrido:** `/orders` sin argumento (o `Recently Listed`) es
  inútil. La vía correcta es la de siempre: buscar el valor más alto dentro de
  `/orders <nombre_de_item>`.
- **Hecho:** `maxPages` vuelve a 200 (el tope de 3 cortaba las materias primas y
  destruía el dato), `pageDelay` vuelve a 350-900 ms (por debajo se pierden
  páginas en silencio). Se conserva el hueco 1-3 s, el cierre a 150 ms y los
  clamps. Solo cambia `scan.json`, sin recompilar.
- **Sobre 600:** no es alcanzable con búsqueda por ítem y materias primas
  profundas. 600 era un aproximado; la métrica real es ítems por sesión sobre la
  lista definitiva.
- **Pendiente:** la lista la aporta el owner (o se arma entre los dos desde el
  grafo de recetas). Los 103 son un seed. Además, investigar el filtro del menú
  (slot 47), que es la única palanca para acortar la paginación profunda.
- **Keywords:** crafteo, recetas, lista, maxPages, correccion.

## S15 — Aprovechar los ítems que aparecen en el camino

- **El humano propuso:** si buscás `diamond` y sale `diamond_spear`, guardar
  también ese valor. Más información, mismo costo.
- **Hecho en `0.10.0`:** se guarda la página completa. Las filas de otros ítems
  son un **fallback**: se descartan en cuanto ese ítem se escanea directo, y su
  identidad es `(itemId, unitPrice, quantityRemaining)` para que dos búsquedas no
  las dupliquen.
- **Bug evitado:** el borrado de la observación anterior se dispara solo en
  página 1. En cada página, la 2 habría borrado lo de la 1.
- **Aviso:** `orders.itemId` tiene FK a `items` en DonutProfit. Las filas
  incidentales pueden traer ítems sin sembrar; el dry run los lista y `--commit`
  falla si faltan.
- **Keywords:** incidental, fallback, merge, pagina-1, 0.10.0.

## S16 — Revisión de la corrida 0.10.0

- **El humano pidió:** revisar cómo salió la corrida.
- **Resultado:** 103 búsquedas en 248 s (~2,4 s/ítem), 159 exports de página,
  5.514 filas, snapshot de 2.632 filas y 106 ítems. Ningún ítem mezcló filas
  incidentales con escaneo directo; los "duplicados" son órdenes distintas en
  slots distintos.
- **Hallazgo:** el buscador matchea **lore y encantamientos**. `/order feather`
  devolvió botas con *Feather Falling* y libros encantados; `feather` apareció en
  la página 7. Eso explica la profundidad de las materias primas.
- **Bug:** `/order netherite_boots` abrió un menú vacío (0 filas) y quedó como "no
  encontrado", pese a tener 146 órdenes reales vistas como incidentales de
  `feather`. Es una carrera de carga: el contenedor se dibuja antes que los slots.
- **Hecho en `0.10.1`:** reintento de 200 ms y una relectura cuando la página 1
  abre vacía. Construido y testeado (53 tests); pendiente de instalar porque el
  juego estaba abierto y bloqueaba el jar viejo.
- **Keywords:** corrida, 248s, 2632filas, lore, menu-vacio, 0.10.1.
