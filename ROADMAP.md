# ROADMAP — donut-order-scanner

> Checklist de todo lo necesario para que el proyecto funcione completo y
> correcto. Marcar `[x]` al cerrar; anotar versión o commit. No borrar ítems
> completados. El detalle operativo vive en `docs/STATE.md`.

## Fase 0 — Base del proyecto

- [x] Repo Gradle + Fabric Loom para Minecraft 1.21.11.
- [x] Java 21 como `--release`; JDK 25 portable en `.tools/` para Loom.
- [x] `.gitignore` para `build/`, `run/`, `logs/`, `dumps/`, `.tools/`, jars y secretos.
- [x] Documentación base: `README.md`, `docs/STATE.md`, `docs/AUTOSCAN.md`.
- [x] Perfil de juego aislado (`Donut Scanner 1.21.11`) para no cargar Wurst.
- [x] Instalación verificada por hash en `%APPDATA%\.minecraft-donutscanner\mods\`.

## Fase 1 — Captura manual (F8)

- [x] Keybind F8 y categoría compartida (fix del crash de `0.4.0`).
- [x] Dump crudo de pantalla con slots, lore y componentes.
- [x] Escritura de dumps en `config/donut_order_scanner/dumps/`.
- [x] Fixtures reales para tests (7 capturas).

## Fase 2 — Parser, export y snapshot

- [x] Parser puro de lore → órdenes (`$ 4K each`, `872/1k Delivered`).
- [x] Exclusión de filas de inventario (`~$ 275`).
- [x] `quantityRemaining = required - delivered`.
- [x] Precios truncados, nunca redondeados.
- [x] Export compatible con `parseOrdersJson` de DonutProfit.
- [x] Merge por `itemId|page|slot` en `orders.json`.
- [x] Tests de parser, números y store (fixtures reales).
- [x] Validación contra DonutProfit en dry run (`scripts/load-orders.ts`).

## Fase 3 — Escaneo autónomo (F9)

- [x] Máquina de estados: comando → espera → captura → página → cierre → pausa.
- [x] Cadencia irregular: 5-12 s con sesgo al extremo corto (0.8.2; antes
      12-35 s uniforme), 3 % de pausa de 1-2 min.
- [x] Preset de throughput (0.9.0): hueco 1-3 s con sesgo, cierre 150 ms,
      `pageDelay` 350-900 ms, `maxPages` 200. El hueco es la palanca barata.
- [x] Topes: 103 búsquedas / 30 min por sesión (clamp hasta 1000).
- [x] Auto-stop: 3 fallos, otro menú, movimiento/ataque/uso, pérdida de foco,
      salida del mundo.
- [x] Paginación dirigida: leer hasta que el ítem aparezca.
- [x] **Cortar en la primera página que contiene el ítem** (0.7.0), sin leer la
      banda completa.
- [x] `maxPages` 200 como tope por ítem; `maxPagesPerSession` 2000 como control
      real de clics.
- [x] Reporte honesto cuando un tope corta la búsqueda antes de encontrar el ítem.
- [x] `scan-cursor.txt` para rotar la lista entre sesiones.
- [x] Seed de 103 ítems observados en capturas reales (0.9.0), provisional.
- [x] Filas incidentales (0.10.0): la página completa se guarda; las filas de
      otros ítems valen como fallback y se retiran al escanear ese ítem directo.
- [x] Reintento de menú vacío en página 1 (0.10.1): 200 ms y una relectura antes
      de aceptar que la búsqueda no tiene resultados.
- [x] Excluir ítems encantados (0.10.2): el parser descarta filas con
      `enchantments`/`stored_enchantments`; el autoscan conserva el id para cortar
      la paginación y una página 1 vacía retira las filas viejas de esa búsqueda.
- [x] Re-escanear con 0.10.2 y `pnpm sync-orders --commit` (2026-11-16):
      snapshot de 1.989 filas / 95 ítems; +1.844 filas en Neon; la web quedó con
      5 crafteos y 0 flips, sin las espadas encantadas.
- [ ] **Lista definitiva** desde el grafo de recetas (materias primas + ítems
      crafteados). La aporta el owner; es el bloqueo B3.
- [ ] Investigar el filtro del menú (slot 47) para acortar la paginación de las
      materias primas.
- [x] ~~Barrido del libro sin búsqueda~~ descartado: ordena por precio
      descendente y las materias primas quedan a cientos de páginas.
- [x] Primera corrida autónoma real (7 búsquedas, 938 filas, sin fallos).
- [x] **Verificar en vivo `0.7.0`**: `diamond` cortó en la página 42
      (`done -> 42 pages`), sin seguir a la 75 (corrida de las 05:25).
- [ ] **Verificar en vivo `0.8.0`**: cada export debe guardar solo las filas del
      ítem buscado (`(N stored, snapshot ...)`) y `orders.json` debe quedar con
      ≤45 filas por ítem.

## Fase 4 — Calidad de datos y operación

- [ ] Validar el `orders.json` reconstruido contra DonutProfit (dry run) y
      confirmar que cada ítem tiene solo sus propias filas y que `diamond` trae
      `$11.200` y alrededores.
- [ ] Ajustar la lista de 60 ítems a lo que el humano realmente comercia.
- [ ] Medir páginas por ítem en una sesión completa y recalibrar
      `maxPagesPerSession` si sobra o falta.
- [x] Resolver la deriva del snapshot cuando la página de un ítem se corre
      (0.8.0: una búsqueda reemplaza todas sus filas, sin importar la página).
- [x] Decidir si se agrega un campo `search` a las filas (0.8.0: sí, opcional).
- [x] Dejar de guardar en el snapshot las filas de otros ítems vistas al paginar
      (0.8.0: solo se guarda el ítem buscado; la captura conserva la página).
- [ ] Documentar el procedimiento de recuperación si `orders.json` queda viejo.

## Fase 5 — Integración con DonutProfit

- [ ] Definir el contrato final de importación (sin tocar `../oreboard` sin
      aprobación).
- [ ] Probar el flujo completo: F9 → `orders.json` → carga en DonutProfit.
- [ ] Acordar frecuencia de escaneo y ventana de frescura de los datos.

## Fase 6 — Coordinación multi-agente y repo

- [x] `AGENTS.md` con directivas, stack, convenciones y reglas de git.
- [x] `ROADMAP.md` (este archivo).
- [x] `LOG.md` con decisiones instrumentales.
- [x] `SESSION.md` aditivo con índice y regla de lectura parcial.
- [x] Remoto `origin` → <https://github.com/winkyrat/orderChecker>.
- [x] Primer push de `main` al remoto (`931c418`, verificado con `git ls-remote`).
- [ ] Verificar que un agente nuevo pueda retomar leyendo solo
      `AGENTS.md` + `ROADMAP.md` + `docs/STATE.md` + última entrada de
      `SESSION.md`.

## Fase 7 — Futuro (no bloqueante)

- [ ] Precio propio para ítems encantados (hoy se descartan a propósito; haría
      falta separar el encantamiento del ítem base).
- [ ] HUD/status line con progreso de la sesión.
- [ ] Deduplicación de órdenes entre sesiones (misma orden vista dos veces).
- [ ] Editor/validación de la lista de ítems desde el juego.
- [ ] Toggle `stopWhenUnfocused` configurable.
- [ ] Empaquetado reproducible y versión en el nombre del jar.

## Definición de "funcionando completo"

1. F8 dumpea y F9 escanea sin fallos en el perfil aislado.
2. `orders.json` valida contra DonutProfit sin descartes inesperados.
3. Una sesión cubre el catálogo con precios reales (sin muestras ni $1).
4. `docs/STATE.md` permite retomar sin leer el historial.
5. `main` está pusheado y un agente nuevo puede seguir el roadmap.
