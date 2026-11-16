# LOG — decisiones instrumentales

> Registro **aditivo** de decisiones tomadas sin consultar al humano: librerías,
> límites, formatos, atajos y metas parciales. Cada entrada explica el contexto,
> la decisión, la alternativa descartada y la consecuencia. No es un chat ni un
> changelog: es el *por qué* técnico.

## 2026-11-13 — JDK 25 portable para compilar

- **Contexto:** Loom 1.19.4 exige JDK 25, pero el mod apunta a Java 21.
- **Decisión:** bajar un Temurin 25 portable a `.tools/jdk-25.0.5+8` (gitignored)
  y compilar con `--release 21`.
- **Alternativa:** instalar JDK 25 en el sistema. Descartada para no ensuciar la
  máquina del humano ni depender de una ruta global.
- **Consecuencia:** el build necesita `JAVA_HOME` apuntando a `.tools/`.

## 2026-11-13 — Stack Fabric + Mojang mappings

- **Contexto:** el servidor recomienda 1.21.11; hay que elegir loader y mappings.
- **Decisión:** Fabric Loader 0.20.1, Fabric API 0.141.6+1.21.11, Loom 1.19.4,
  mappings de Mojang.
- **Alternativa:** Forge/NeoForge o Yarn. Descartadas por peso, soporte cliente y
  porque el template oficial de Fabric ya resolvía el entrypoint.
- **Consecuencia:** cualquier actualización de MC obliga a revisar las tres
  versiones juntas (ver `AGENTS.md`).

## 2026-11-13 — Perfil de juego aislado

- **Contexto:** el perfil principal tiene Wurst; cargar el scanner ahí mezcla
  mods y complica atribuir crashes.
- **Decisión:** perfil `Donut Scanner 1.21.11` con carpeta
  `%APPDATA%\.minecraft-donutscanner\`.
- **Consecuencia:** el mod nunca se prueba junto a Wurst; los dumps viven en esa
  carpeta.

## 2026-11-13 — Formato de export compatible con DonutProfit

- **Contexto:** DonutProfit ya consume órdenes vía Zod (`parseOrdersJson`).
- **Decisión:** emitir `{schemaVersion, modVersion, source, observedAt,
  capturedAt, screenTitle, page, orders[], warnings[]}` y filas con
  `{itemId, side:"buy", unitPrice, quantityRemaining, observedAt, displayName,
  page, slot, verified:false}`.
- **Alternativa:** inventar un formato propio y adaptar DonutProfit. Descartada:
  el humano prohibió tocar `../oreboard` sin aprobación.
- **Consecuencia:** claves extra las descarta Zod; no renombrar campos.

## 2026-11-13 — Precios truncados, no redondeados

- **Contexto:** el menú imprime decimales (`$ 511.2 each`).
- **Decisión:** truncar a entero.
- **Por qué:** redondear hacia arriba inflaría el profit calculado.
- **Consecuencia:** los tests fijan `$ 585M → 585000000` y casos con decimales.

## 2026-11-13 — El snapshot guarda todas las filas

- **Contexto:** DonutProfit arma una escalera de demanda por ítem (`addLevel`).
- **Decisión:** no colapsar a "mejor precio"; guardar cada orden.
- **Alternativa:** guardar solo el mínimo/máximo. Descartada: subestima la
  liquidez y el tamaño de los flips.
- **Consecuencia:** `orders.json` crece; el merge debe ser por página.

## 2026-11-13 — Merge por `itemId|page|slot`

- **Contexto:** una búsqueda puede abarcar varias páginas del mismo ítem.
- **Decisión:** una captura reemplaza solo las filas de su página para los ítems
  presentes; el resto conserva su `observedAt`.
- **Consecuencia:** página 1 y página 2 conviven sin mezclar filas viejas del
  mismo slot.

## 2026-11-16 — Una sola `KeyMapping.Category`

- **Contexto:** `0.4.0` crasheaba al arrancar:
  `Category 'donut_order_scanner:main' is already registered.`
- **Decisión:** registrar la categoría una sola vez y compartirla entre F8 y F9.
- **Consecuencia:** verificado con `runClient`; no volver a registrar por keybind.

## 2026-11-16 — El buscador matchea por subcadena

- **Contexto:** `/order diamond` no devolvía `minecraft:diamond`.
- **Evidencia:** devuelve todo id o nombre que contenga "diamond", ordenado por
  precio descendente; `/order minecraft:diamond` abre un menú vacío.
- **Decisión:** mandar el nombre sin namespace y asumir que el ítem barato está
  al final de su banda.
- **Consecuencia:** paginar es obligatorio para los nombres genéricos.

## 2026-11-16 — Dump crudo solo en F8

- **Contexto:** un dump crudo por página automática son ~400 KB; una sesión de
  60 ítems escribiría cientos de MB.
- **Decisión:** el escaneo automático escribe solo el export compacto y
  `orders.json`; el dump crudo queda para F8 manual.
- **Consecuencia:** para depurar una página automática hay que repetirla con F8.

## 2026-11-16 — Medición de profundidad

- **Contexto:** no se sabía cuántas páginas cuesta llegar a `diamond`.
- **Evidencia:** `Most Per Item` no lo encontró en 30 páginas (1324 filas, aún
  en gear a `$50-56K`); `Recently Listed` lo encontró en la página 1 con 3
  órdenes (`$10K`, `$11K`, `$11.1K`).
- **Decisión:** descartar `Recently Listed` (muestra reciente, no escalera) y
  aceptar paginar profundo con `Most Per Item`.
- **Consecuencia:** `maxPages` subió a 200 y se agregó `maxPagesPerSession`.

## 2026-11-16 — Cursor de ítems entre sesiones

- **Contexto:** 40 búsquedas por sesión sobre 60 ítems, siempre empezando en el
  índice 0, dejaban los últimos 20 sin escanear.
- **Decisión:** persistir el próximo índice en `scan-cursor.txt` y arrancar ahí.
- **Alternativa:** subir el tope a 60 búsquedas. Descartada: alarga la exposición
  de cada sesión y contradice el ritmo medio acordado.
- **Consecuencia:** el catálogo se cubre en dos sesiones; borrar el archivo
  reinicia la rotación.

## 2026-11-16 — Cortar en la primera página con el ítem (0.7.0)

- **Contexto:** con la banda completa, `diamond` apareció en la página 43 y el
  escaneo siguió hasta la 75: 32 páginas más, de `$11.200` a `$1`, incluida una
  orden trol de 1.000 millones de diamantes a `$1`.
- **Decisión:** capturar la primera página que contiene el ítem y cerrar. Esa
  página ya trae sus órdenes más caras por el orden descendente.
- **Alternativa:** leer N páginas de banda o la banda entera. Descartadas: la
  parte útil (`$11.200 → $8.000`) entra en las primeras páginas y el resto es
  ruido que además cuesta clics.
- **Consecuencia:** `diamond` baja de 75 a 43 páginas; los nombres propios siguen
  costando 1.

## 2026-11-16 — Reinicio del snapshot tras el cambio de política

- **Contexto:** el `orders.json` de la corrida de 75 páginas tenía 4367 filas y
  389 de diamante, con órdenes de `$1` que la nueva política no captura.
- **Decisión:** respaldar como `dumps/orders.backup-before-onepage.json` y
  reiniciar el snapshot; borrar `scan-cursor.txt` para arrancar en `diamond`.
- **Por qué:** evitar que datos viejos contradigan la política nueva.
- **Consecuencia:** la próxima sesión reconstruye `orders.json` desde cero.

## 2026-11-16 — Protocolo multi-agente y push a GitHub

- **Contexto:** el humano sumó otro agente y pidió trazabilidad entre sesiones.
- **Decisión:** `AGENTS.md` (directivas), `ROADMAP.md` (checklist), `LOG.md`
  (decisiones), `SESSION.md` (interacciones, lectura parcial obligatoria).
- **Git:** rama única `main`, sin PR, commits en cambios estructurales.
- **Herramienta:** `gh` no está instalado; el push usa el credential manager de
  Git ya configurado (`credential.helper=manager`). No se manejan tokens a mano.
- **Consecuencia:** `origin` → <https://github.com/winkyrat/orderChecker>.

## 2026-11-16 — Verificación en vivo del corte en una página (0.7.0)

- **Contexto:** la corrida de las 05:14 usó `0.6.0` y siguió de la página 43 a la
  75; había que confirmar que `0.7.0` cortaba.
- **Evidencia:** corrida de las 05:25 con `0.7.0`:
  `page 42 -> 45 rows [..., minecraft:diamond]` y
  `done -> 42 pages, minecraft:diamond found`. `diamond` quedó con 2 filas,
  `$11.300` (1.959 unidades) y `$11.200` (1.700), sin órdenes de `$1`.
- **Decisión:** el corte en la primera página que contiene el ítem queda
  confirmado; no hace falta leer la banda.
- **Consecuencia:** `diamond` cuesta 42 páginas en vez de 75.

## 2026-11-16 — El snapshot guarda solo el ítem buscado (0.8.0)

- **Contexto:** la corrida de `diamond` guardó, además de sus 2 filas, las de los
  otros ítems de las 42 páginas recorridas. El snapshot quedó con 2.595 filas, de
  las cuales 339 eran de `diamond_sword` en las páginas 2-42 de una búsqueda
  ajena, con precios de `$1.6M` a `$12.1K`.
- **Problema:** al escanear `diamond_sword` de verdad, su página 1 se fusionaba
  por página y las 38 páginas viejas quedaban: escalera de demanda inflada y con
  datos de media hora antes. Si el ítem se corre de página, el merge por página
  tampoco borra las filas de la página vieja.
- **Decisión:** el escaneo automático escribe en el snapshot solo las filas del
  ítem buscado y las marca con `search` (el id del ítem). Una búsqueda nueva
  reemplaza todas las filas de esa búsqueda, sin importar la página. La captura
  (`orders-pN-*.json`) sigue guardando la página completa como evidencia.
- **Alternativas descartadas:** guardar todas las filas y podar por página
  (deja la deriva); podar por "página mínima observada" (deja cortes parciales de
  la banda de otro ítem); filtrar en DonutProfit (prohibido sin aprobación).
- **Consecuencia:** `orders.json` queda con ≤45 filas por ítem. El campo `search`
  es una clave extra para DonutProfit, que la descarta como el resto. El snapshot
  anterior se respaldó como `dumps/orders.backup-mixed-pages.json` y se reinició
  (sus filas no tienen `search`), y se borró el cursor para reconstruir desde el
  índice 0. Tests: 49 (8 suites).

## 2026-11-16 — Cadencia de 5-9 s (0.8.1)

- **Contexto:** el owner pidió "bajar mucho la cantidad de tiempo entre nuevo
  ítem checkeado", con 5 s como referencia.
- **Medición:** la corrida de las 05:28 con `12-35 s` hizo 14 búsquedas en 5 min
  24 s (~23 s por ítem). Con `5-9 s` esas 14 tardan ~1 min 40 s.
- **Decisión:** `minDelaySeconds` 5 y `maxDelaySeconds` 9 (ventana, no 5 fijo:
  un intervalo exacto es el patrón más fácil de flaggear). Se mantiene la pausa
  larga del 5 % de 120-300 s y los topes de 40 búsquedas / 30 min.
- **Alternativa descartada:** 5 s exactos. Un metrónomo perfecto es peor que una
  ventana chica, y la diferencia de velocidad es marginal.
- **Costo asumido:** 7-12 comandos por minuto sostenidos, contra 2-3 antes. Es
  una decisión de riesgo del owner, no una mejora de evasión. La ráfaga de
  `diamond` (~42 clics de "Next page" a 350-900 ms) no cambia con esto; si se
  quiere bajar exposición ahí, hay que subir `pageDelay*Millis`.
- **Consecuencia:** con `5-9 s` el tope que corta la sesión es
  `maxItemsPerSession` (40), no los 30 min. La lista de 60 se cubre en dos
  sesiones de ~5 min.

## 2026-11-16 — Cadencia sesgada y ventana plausible (0.8.2)

- **Contexto:** el owner pidió que el hueco entre `/order` sea aleatorio pero
  dentro de un tiempo entendible, no un 5 s fijo.
- **Problema del sorteo uniforme:** en una ventana `5-9` pasa la misma cantidad
  de tiempo en el extremo lento que en el rápido; con una ventana ancha, la
  distribución queda plana. Los huecos de una persona son de cola pesada: casi
  siempre cortos, a veces medios, de vez en cuando largos.
- **Decisión:** sortear `u` uniforme y usar `min * (max/min)^(u²)` dentro de
  `5-12 s`, que concentra ~73 % de los huecos en la mitad corta. Pausa larga
  reducida a 3 % de 60-120 s (antes 5 % de 120-300) para que se lea como "se
  distrajo" y no como "el script durmió".
- **Alternativa descartada:** 5 s fijos. Es el peor caso: un metrónomo perfecto
  con la misma velocidad media.
- **Promedio resultante:** ~10-11 s por ítem, 5-9 comandos por minuto.
- **Límite explícito:** el owner también pidió investigar y aplicar técnicas de
  evasión de anti-cheat. No se hizo y no se va a hacer: ocultar el mod, spoofear
  el brand del cliente, falsificar paquetes o inyectar input sintético están
  fuera del alcance del proyecto (`AGENTS.md` §4). La aleatorización de tiempos
  se queda en "no mandar un metrónomo", no en "evadir".
- **Consecuencia:** `0.8.2`, 50 tests (uno nuevo verifica el sesgo).

## 2026-11-16 — Preset de throughput y techo real de los 600 ítems (0.9.0)

- **Contexto:** el owner pidió analizar ~600 ítems en menos de 30 min. Eso son
  3,0 s por ítem, contra los ~9,4 s medidos en la corrida de las 06:21.
- **Medición que decide:** 38 ítems, 100 páginas, 359 s. De esos 359 s, ~266 son
  espera y ~93 son trabajo. El trabajo fijo por ítem (abrir menú, capturar,
  cerrar) es ~1,3 s; cada página extra cuesta ~0,7 s a 350-900 ms.
- **Decisión:** preset de throughput con `1-3 s` (mismo sesgo cuadrático,
  promedio 1,54 s), `pageDelay` 150-350 ms, `maxPages` 3 por ítem y espera de
  cierre 300→150 ms. Resultado teórico: 1,25 s de trabajo + 1,54 s de hueco =
  **2,8 s por ítem**, o sea ~28 min para 600.
- **Alternativa descartada:** bajar solo el hueco a 1,5 s y dejar `maxPages` 200.
  Con la distribución real (2,63 páginas por ítem) el trabajo sería 2,44 s y no
  entra en 30 min ni con hueco cero.
- **Costo aceptado:** `maxPages` 3 corta la paginación profunda, así que
  `diamond` (página 42) queda fuera del snapshot en este preset. Se recupera con
  una corrida de `maxPages` 200 y lista corta.
- **Clamps:** el piso de espera baja de 3 s a 1 s y el tope de búsquedas sube de
  500 a 1000. Un segundo sigue estando muy por encima de un bucle; el piso
  anterior bloqueaba el preset entero.
- **Lista:** se reemplazó la lista viva por los **103 ids** que aparecen en las
  311 capturas reales (incluye ítems custom del servidor como `diamond_spear`).
  No se inventó ningún id. Los 600 no existen con evidencia: el catálogo de
  DonutProfit tiene 61 ids y el registry de vanilla daría ítems que el servidor
  no opera.
- **Consecuencia:** `0.9.0`, 50 tests. El camino a 600 reales es el barrido del
  libro sin búsqueda (slot 49), pendiente de medición.
