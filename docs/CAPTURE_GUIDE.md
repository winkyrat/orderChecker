# Guía de captura — Fase 1

> El mod no hace clic, no manda comandos y no modifica el menú. Solo copia lo
> que el cliente ya recibió cuando apretás F8.

## 1. Requisitos

- Minecraft Java **1.21.11** con **Fabric Loader 0.20.1** o compatible.
- **Fabric API** `0.141.6+1.21.11` en la carpeta `mods`.
- El jar del mod: `build/libs/donut-order-scanner-0.3.0.jar`.

Ruta típica en Windows: `%APPDATA%\.minecraft\mods\`.

## 2. Instalación

Ya está instalado en un perfil aislado:

- Perfil del launcher: **Donut Scanner 1.21.11** (`fabric-loader-0.20.1-1.21.11`).
- Carpeta de juego: `%APPDATA%\.minecraft-donutscanner\`.
- Mods en `%APPDATA%\.minecraft-donutscanner\mods\`.

Si hay que reinstalar a mano:

1. Copiá el jar a `%APPDATA%\.minecraft-donutscanner\mods\`.
2. Abrí el juego con el perfil **Donut Scanner 1.21.11**.
3. Verificá en **Options > Controls > Key Binds** que exista la categoría
   **Donut Order Scanner** y la tecla **Dump open screen** (F8 por defecto).
   Si F8 choca con otro mod, cambiala.
4. Entrá a DonutSMP.

## 3. Capturas que necesito

Hacé cada captura con el menú abierto y apretá **F8**. Suena un clic agudo. Si
falla, mostrame el mensaje del chat y `logs/latest.log`.

El comando confirmado es **`/order <ítem>`** (singular).

| # | Qué abrir | Qué quiero ver |
| --- | --- | --- |
| 1 | `/order diamond_block`, página 2 | Paginación: qué slot es `Next page` y si la página 2 sigue ordenada por precio. |
| 2 | `/order zzzzz` | Caso borde: búsqueda sin resultados. |
| 3 | `/order diamond` | Si la búsqueda es exacta o trae variantes (`diamond` + `diamond_block`). |
| 4 | `/order spawner` o un ítem con nombre custom | Que el `itemId` siga siendo el real aunque el nombre sea raro. |

Ya tengo tres capturas reales (glass, diamond_block, obsidian). No hace falta
repetirlas: están en `src/test/resources/fixtures/`.

> Estado 2026-11-16: 1–3 **hechas**. Falta solo la 4 (ítem con nombre custom),
> que no bloquea nada.

## 4. Qué hace F8

- Funciona **con el menú abierto** (no solo en el mundo). F8 se intercepta en
  la pantalla actual y el menú no lo recibe.
- Lee la pantalla actual: clase, título, tamaño, menú y slots.
- Por slot: índice, posición, item id, cantidad, nombre, lore y componentes.
- Escribe tres archivos en
  `%APPDATA%\.minecraft-donutscanner\config\donut_order_scanner\dumps\`:
  - `orders_page_1-<ms>.json`: dump crudo, evidencia.
  - `orders-p1-<ms>.json`: export de esa captura.
  - `orders.json`: snapshot con todas las filas; una captura reemplaza solo su página.
- No incluye tu contraseña, token ni datos de la cuenta.
- Con un contenedor abierto el chat no se dibuja, así que la confirmación
  inmediata es un **sonido agudo** (clic). El mensaje `[DOS] Dump saved: ...`
  queda en el chat y en `logs/latest.log`.

## 5. Qué pasarme

1. `orders.json` (el que se le pasa a DonutProfit) y los `orders-p*.json`.
2. Los dumps crudos `orders_page_*.json`, sin editar, para los fixtures.
3. El resultado de `logs/latest.log` si algo falla.

## 6. Seguridad

- No compartas capturas con tu nombre real, saldo o coordenadas.
- Los dumps pueden incluir nombres de jugadores; se usan solo como fixtures
  locales y no se publican.
- Si el servidor advierte por mods, cerrá el juego y no sigas capturando.
