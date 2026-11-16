# Escaneo autónomo (Fase 3)

> Implementado en `0.4.0`. Pagina hasta encontrar el ítem desde `0.5.0` y sin
> límite práctico de páginas desde `0.6.0`. Tecla **F9** para prender/apagar.

## Qué hace, paso a paso

1. Toma el próximo ítem de `scan.json`, continuando donde terminó la sesión
   anterior (`scan-cursor.txt`).
2. Manda `/order <ítem>` como si lo escribieras vos.
3. Espera el menú (hasta `screenTimeoutMillis`). Si no aparece, salta el ítem.
4. Lee la página sin tocar nada y escribe el export.
5. Mira si el ítem pedido está en esa página. Si no está y hay **Next page**,
   lo clickea y repite hasta que aparezca o hasta que la lista se termine.
   `maxPages` (200) es solo una guarda contra un bucle.
6. Cuando el ítem aparece, **corta ahí**: esa página ya trae sus órdenes más
   caras porque la lista está ordenada por precio. No sigue leyendo la banda.
7. Cierra el menú.
8. Espera un tiempo al azar entre `minDelaySeconds` y `maxDelaySeconds`; con
   probabilidad `longPauseChancePercent` espera una pausa larga.
9. Repite hasta el tope de búsquedas, de páginas o de minutos.

## Ojo: la búsqueda no es exacta

`/order diamond` **no** devuelve `minecraft:diamond`. Devuelve todo ítem cuyo id
o nombre contenga "diamond", ordenado de mayor a menor precio: leggings, boots,
spear, sword, helmet, axe, chestplate. El diamante crudo (~$1-3K) queda al final
de la lista, después de ~134 filas de gear de 1M–5.8M.

Consecuencia: el ítem barato aparece **decenas de páginas** después. Medido con
`diamond` en `Most Per Item`: p1 `5.3M–2M`, p10 `500K`, p20 `200K`,
p30 `56K–50.5K`; el diamante crudo (`$11.2K`) apareció en la **página 43**.
Por eso el escaneo **pagina hasta encontrarlo**, sin un tope práctico, y corta
en esa misma página: al estar ordenada por precio, ya contiene sus órdenes más
caras. `maxPages` (200) es solo una guarda contra un bucle.

No conviene seguir leyendo la banda completa: en esa corrida la banda de
`diamond` siguió 32 páginas más, hasta órdenes de $1 (incluida una de 1.000
millones de diamantes a $1). La parte útil, `$11.200 → $8.000`, ya estaba en
las primeras páginas.

El modo es **siempre `Most Per Item`**. `Recently Listed` se descartó: encuentra
el ítem en 2-4 páginas, pero devuelve una muestra de lo último publicado y
cualquiera puede poner una orden muy por debajo del precio real. La escalera de
precios que usa DonutProfit necesita el orden por precio, no por fecha.

Solo cuatro nombres del catálogo son genéricos y caros de recorrer:
`iron`, `emerald`, `diamond` y `redstone`. El resto tiene nombre propio y
aparece en 1-3 páginas.

El log y la barra de acción dicen qué pasó:

```
[DOS] 1/40 diamond · next in 23s
[DOS] 1/40 diamond (no diamond in 200 pages) · next in 23s
```

Si un tope corta la búsqueda antes de terminar la banda, el log lo dice con
`cut after N pages by ...` y el ítem queda marcado como incompleto, no como
"no existe".

Atajos descartados con capturas reales (2026-11-16):

| Intento | Resultado |
| --- | --- |
| `/order minecraft:diamond` | Menú vacío: el id con namespace no matchea. |
| `/order` sin argumentos | Lista global sin filtrar: 45 elytras de $585M a $530M. |
| Filtro `Recently Listed` | No sirve: da lo último publicado, no la escalera de precios. |

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
| `maxPages` | 200 | Guarda contra un bucle. El escaneo corta antes si encuentra el ítem o si la lista termina. |
| `maxPagesPerSession` | 400 | Tope de páginas leídas en toda la sesión. Es el control real de cuántos clics se permiten. |
| `pageDelayMinMillis` | 350 | Espera mínima tras "Next page". |
| `pageDelayMaxMillis` | 900 | Espera máxima tras "Next page". |
| `screenTimeoutMillis` | 4000 | Cuánto esperar a que abra el menú. |

Valores fuera de rango se recortan solos (nunca menos de 3 s entre búsquedas,
nunca más de 240 min de sesión). Un `scan.json` roto vuelve a los defaults.

`scan-cursor.txt`, al lado de `scan.json`, guarda el índice del próximo ítem.
Así una sesión de 40 búsquedas sobre 60 ítems no deja los últimos 20 sin
escanear: la siguiente arranca donde terminó la anterior. Borralo si querés
volver a empezar por el primer ítem de la lista.

## Cuándo se corta solo

| Situación | Mensaje |
| --- | --- |
| Tres fallos seguidos | `Scan off: <motivo> three times in a row` |
| Tope de búsquedas o minutos | `Scan off: reached the 40 search cap` |
| Tope de páginas | `Scan off: reached the 400 page cap` |
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

## Qué guarda en el snapshot

El export de cada página conserva la página completa como evidencia, pero
`orders.json` **solo recibe las filas del ítem buscado**, y cada fila lleva el
campo `search` con ese ítem. Una búsqueda nueva reemplaza todas las filas de la
misma búsqueda, sin importar la página.

Motivo: para llegar a `diamond` hay que pasar 42 páginas de gear de diamante. Si
esas filas se guardaran, `diamond_sword` aparecería con 339 órdenes repartidas en
las páginas 2-42 de una búsqueda ajena, y al escanearlo de verdad su página 1 se
fusionaría por página dejando las viejas atrás. Su escalera de demanda quedaría
inflada con datos de hace media hora. Cada ítem se mide con su propia búsqueda.

## Cómo verificar que funcionó

En `logs/latest.log`:

```
Scan started: 60 items, 12-35 s, cap 40 searches / 30 min, starting at minecraft:diamond
Scan: /order diamond (search 1/40)
Scan: /order diamond page 1 -> 45 rows [minecraft:diamond_helmet, minecraft:diamond_block, ...]
Scan: /order diamond page 2 -> 45 rows [minecraft:diamond_boots, minecraft:diamond_pickaxe, ...]
...
Scan: /order diamond page 42 -> 45 rows [minecraft:diamond_pickaxe, minecraft:diamond_sword, ...]
Scan: /order diamond page 43 -> 45 rows [minecraft:diamond_pickaxe, minecraft:diamond_helmet, minecraft:diamond_block, minecraft:diamond_leggings, minecraft:diamond_chestplate, minecraft:diamond, ...]
Scan: /order diamond done -> 43 pages, minecraft:diamond found
...
Scan stopped: stopped by key (7 searches, 938 rows)
```

El log muestra **qué ítems vinieron**, no el que se pidió: si buscás `diamond` y
en la página hay leggings, el log lo dice. `done -> N pages` confirma cuánto
costó llegar al ítem; `never returned` avisa que la lista se terminó sin él.

Y en `config/donut_order_scanner/dumps/` aparecen `orders-p1-*.json`,
`orders-p2-*.json` y se actualiza `orders.json`.

La línea de export dice cuántas filas se guardaron de verdad:

```
Exported 45 orders to ...\orders-p43-1794816916504.json (2 stored, snapshot ...\orders.json)
```

`45 orders` es lo que había en la página; `2 stored` son las de `diamond` que
entraron al snapshot.
