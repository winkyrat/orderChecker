# Guía de captura — Fase 1

> El mod no hace clic, no manda comandos y no modifica el menú. Solo copia lo
> que el cliente ya recibió cuando apretás F8.

## 1. Requisitos

- Minecraft Java **1.21.11** con **Fabric Loader 0.20.1** o compatible.
- **Fabric API** `0.141.6+1.21.11` en la carpeta `mods`.
- El jar del mod: `build/libs/donut-order-scanner-0.1.0.jar`.

Ruta típica en Windows: `%APPDATA%\.minecraft\mods\`.

## 2. Instalación

1. Copiá el jar a `mods/`.
2. Abrí el juego con el perfil Fabric 1.21.11.
3. Verificá en **Options > Controls > Key Binds** que exista la categoría
   **Donut Order Scanner** y la tecla **Dump open screen** (F8 por defecto).
   Si F8 choca con otro mod, cambiala.
4. Entrá a DonutSMP.

## 3. Capturas que necesito

Hacé cada captura con el menú abierto y apretá **F8**. El chat debe mostrar
`[DOS] Dump saved: ...`. Si falla, mostrame el mensaje y el archivo
`logs/latest.log`.

| # | Qué abrir | Qué quiero ver |
| --- | --- | --- |
| 1 | `/orders` | Pantalla principal, sin filtro. |
| 2 | `/orders diamond` (o `/order diamond`) | Resultado de búsqueda de un ítem. |
| 3 | Misma búsqueda, página 2 | Cómo se pagina y qué slot es "siguiente". |
| 4 | `/orders iron_ingot` | Otro ítem, precio y cantidad distintos. |
| 5 | Búsqueda sin resultados (`/orders zzzzz`) | Caso borde: menú vacío. |
| 6 | Una order con cantidad grande | Formato de cantidades > 1.000. |
| 7 | Una order con cantidad 1, si existe | Caso borde: una sola unidad. |

Si el comando correcto es `/order <item>` y no `/orders <item>`, usá el que
funcione y anotá cuál fue.

## 4. Qué hace F8

- Lee la pantalla actual: clase, título, tamaño, menú y slots.
- Por slot: índice, posición, item id, cantidad, nombre, lore y componentes.
- Escribe un JSON en:
  `%APPDATA%\.minecraft\config\donut-order-scanner\dumps\`
- No incluye tu contraseña, token ni datos de la cuenta.

## 5. Qué pasarme

1. Todos los `.json` de esa carpeta, sin editar.
2. Una captura de pantalla de cada menú (opcional pero útil).
3. El resultado de `logs/latest.log` si algo falla.
4. Anotá si el precio que muestra el menú es por ítem o total, si lo sabés.

## 6. Seguridad

- No compartas capturas con tu nombre real, saldo o coordenadas.
- Los dumps pueden incluir nombres de jugadores; se usan solo como fixtures
  locales y no se publican.
- Si el servidor advierte por mods, cerrá el juego y no sigas capturando.
