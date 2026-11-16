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
