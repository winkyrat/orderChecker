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
- [x] Cadencia irregular: 12-35 s, 5 % de pausa larga de 120-300 s.
- [x] Topes: 40 búsquedas / 30 min por sesión.
- [x] Auto-stop: 3 fallos, otro menú, movimiento/ataque/uso, pérdida de foco,
      salida del mundo.
- [x] Paginación dirigida: leer hasta que el ítem aparezca.
- [x] **Cortar en la primera página que contiene el ítem** (0.7.0), sin leer la
      banda completa.
- [x] `maxPages` 200 como guarda anti-bucle; `maxPagesPerSession` 400 como
      control real de clics.
- [x] Reporte honesto cuando un tope corta la búsqueda antes de encontrar el ítem.
- [x] `scan-cursor.txt` para rotar los 60 ítems entre sesiones.
- [x] Primera corrida autónoma real (7 búsquedas, 938 filas, sin fallos).
- [ ] **Verificar en vivo `0.7.0`**: `diamond` debe cortar en la página 43
      (`done -> 43 pages`), no seguir a la 75.

## Fase 4 — Calidad de datos y operación

- [ ] Validar el `orders.json` reconstruido contra DonutProfit (dry run) y
      confirmar que `diamond` tiene `$11.200` y ninguna orden de `$1`.
- [ ] Ajustar la lista de 60 ítems a lo que el humano realmente comercia.
- [ ] Medir páginas por ítem en una sesión completa y recalibrar
      `maxPagesPerSession` si sobra o falta.
- [ ] Resolver la deriva del snapshot cuando la página de un ítem se corre
      (hoy el merge por página deja filas viejas si el ítem cambia de página).
- [ ] Decidir si se agrega un campo `search` a las filas para poder podar por
      búsqueda sin ambigüedad.
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
- [ ] Primer push de `main` al remoto.
- [ ] Verificar que un agente nuevo pueda retomar leyendo solo
      `AGENTS.md` + `ROADMAP.md` + `docs/STATE.md` + última entrada de
      `SESSION.md`.

## Fase 7 — Futuro (no bloqueante)

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
