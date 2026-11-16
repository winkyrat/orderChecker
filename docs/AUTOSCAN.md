# Escaneo autónomo (Fase 3)

> Implementado en `0.4.0`. Tecla **F9** para prender/apagar.

## Qué hace, paso a paso

1. Toma el próximo ítem de `scan.json`.
2. Manda `/order <ítem>` como si lo escribieras vos.
3. Espera el menú (hasta `screenTimeoutMillis`). Si no aparece, salta el ítem.
4. Lee la página sin tocar nada y escribe el export.
5. Si hay botón **Next page** y no llegó a `maxPages`, lo clickea una vez y
   espera `pageDelay*` antes de leer la página nueva.
6. Cierra el menú.
7. Espera un tiempo al azar entre `minDelaySeconds` y `maxDelaySeconds`; con
   probabilidad `longPauseChancePercent` espera una pausa larga.
8. Repite hasta el tope de búsquedas o de minutos.

## Ojo: la búsqueda no es exacta

`/order diamond` **no** devuelve `minecraft:diamond`. Devuelve todo ítem cuyo id
o nombre contenga "diamond", ordenado de mayor a menor precio: leggings, boots,
spear, sword, helmet, axe, chestplate. El diamante crudo (~$1-3K) queda al final
de la lista, después de ~134 filas de gear de 1M–5.8M.

Consecuencia: con `maxPages: 3` el ítem pedido puede no aparecer nunca. El log y
la barra de acción lo dicen explícitamente:

```
[DOS] 1/40 diamond (no diamond in 3 pages) · next in 23s
```

Cuando pasa, esas filas de gear igual se guardan (sirven para esos ítems), pero
el ítem pedido queda sin datos. Para bajar precios exactos de materiales baratos
hay que paginar más profundo o cambiar el orden.

Atajos descartados con capturas reales (2026-11-16):

| Intento | Resultado |
| --- | --- |
| `/order minecraft:diamond` | Menú vacío: el id con namespace no matchea. |
| `/order` sin argumentos | Lista global sin filtrar: 45 elytras de $585M a $530M. |

## Cómo se prende

- Entrá al mundo (no sirve en el menú principal).
- Cerrá cualquier menú abierto.
- **F9**. Suena un clic agudo y el chat dice `[DOS] Scan on: 60 items…`.
- **F9** otra vez para cortar. También se corta solo (ver abajo).

Mientras corre, después de cada ítem aparece en la barra de acción:
`[DOS] 7/40 diamond_block · next in 23s`.

## Configuración

Archivo: `%APPDATA%\.minecraft-donutscanner\config\donut_order_scanner\scan.json`.
Se crea solo la primera vez con los valores de abajo. Se lee **al apretar F9**:
editá el archivo, apretá F9 dos veces y toma los cambios sin reiniciar.

| Campo | Default | Qué controla |
| --- | --- | --- |
| `items` | los 60 del catálogo | Qué buscar. Acepta `minecraft:glass` o `glass`. |
| `minDelaySeconds` | 12 | Espera mínima entre búsquedas. |
| `maxDelaySeconds` | 35 | Espera máxima entre búsquedas. |
| `longPauseChancePercent` | 5 | Probabilidad de una pausa larga. |
| `longPauseMinSeconds` | 120 | Pausa larga mínima. |
| `longPauseMaxSeconds` | 300 | Pausa larga máxima. |
| `maxItemsPerSession` | 40 | Tope de búsquedas por sesión. |
| `maxSessionMinutes` | 30 | Tope de duración de la sesión. |
| `maxPages` | 3 | Páginas por ítem. |
| `pageDelayMinMillis` | 350 | Espera mínima tras "Next page". |
| `pageDelayMaxMillis` | 900 | Espera máxima tras "Next page". |
| `screenTimeoutMillis` | 4000 | Cuánto esperar a que abra el menú. |

Valores fuera de rango se recortan solos (nunca menos de 3 s entre búsquedas,
nunca más de 240 min de sesión). Un `scan.json` roto vuelve a los defaults.

## Cuándo se corta solo

| Situación | Mensaje |
| --- | --- |
| Tres fallos seguidos | `Scan off: <motivo> three times in a row` |
| Tope de búsquedas o minutos | `Scan off: reached the 40 search cap` |
| Aparece otro menú que no es el de órdenes | `Scan off: another menu is open` |
| Movés, atacás o usás el ítem en mano | `Scan off: you started playing` |
| La ventana pierde el foco | `Scan off: the window lost focus` |
| Salís del mundo | `Scan off: left the world` |

## Qué NO hace

- No falsifica paquetes: usa el comando y el click reales del cliente.
- No mueve al jugador ni gira la cámara.
- No se oculta de la lista de mods ni del servidor.
- No promete ser indetectable. Manda un comando cada 12–35 s durante 30 min;
  un servidor puede detectar ese patrón. Los topes reducen la exposición, no la
  eliminan.
- No escribe el dump crudo de cada página automática (serían ~400 KB por
  página). Solo escribe el export compacto y `orders.json`. El dump crudo queda
  para F8 manual.

## Cómo verificar que funcionó

En `logs/latest.log`:

```
Scan started: 60 items, 12-35 s, cap 40 searches / 30 min
Scan: /order diamond (search 1/40)
Scan: /order diamond page 1 -> 44 rows [minecraft:diamond_leggings, minecraft:diamond_boots, minecraft:diamond_spear]
Scan: /order diamond page 2 -> 45 rows [minecraft:diamond_spear, minecraft:diamond_leggings, minecraft:diamond_boots, minecraft:diamond_sword]
Scan: /order diamond page 3 -> 45 rows [minecraft:diamond_sword, minecraft:diamond_helmet, ...]
WARN Scan: /order diamond never returned minecraft:diamond within 3 pages
...
Scan stopped: stopped by key (7 searches, 938 rows)
```

El log muestra **qué ítems vinieron**, no el que se pidió: si buscás `diamond` y
en la página hay leggings, el log lo dice. `never returned` avisa que el ítem
pedido quedó fuera de las páginas leídas.

Y en `config/donut_order_scanner/dumps/` aparecen `orders-p1-*.json`,
`orders-p2-*.json` y se actualiza `orders.json`.
