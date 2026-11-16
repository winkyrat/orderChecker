# AGENTS.md — directivas para todos los agentes

> Fuente de verdad del **cómo**. Si otra instrucción choca con esta, gana esta.
> Léelo completo antes de tocar código.

## 1. Qué es este proyecto

`donut-order-scanner` es un mod **cliente** de Fabric para Minecraft 1.21.11 que:

- lee el menú `/order <ítem>` de DonutSMP (F8 manual, F9 autónomo);
- exporta JSON compatible con DonutProfit (`../oreboard`);
- mantiene `orders.json` como snapshot acumulado.

Repo remoto: <https://github.com/winkyrat/orderChecker>

## 2. Protocolo multi-agente

Trabajan varios agentes, **nunca en simultáneo**. Antes de empezar, en este orden:

1. `AGENTS.md` (este archivo).
2. `ROADMAP.md`: ubicar la fase pendiente.
3. `docs/STATE.md`: estado vivo, números reales, instalación.
4. **Solo la última entrada** de `SESSION.md` (regla 2.3).

### 2.1 ROADMAP.md

Checklist de todo lo necesario para que el proyecto funcione completo. Al cerrar
una tarea, marcar `[x]` y anotar la versión o el commit. Si aparece trabajo
nuevo, agregarlo como ítem; no borrar los completados.

### 2.2 LOG.md

Registro aditivo de decisiones **instrumentales** tomadas sin consultar al humano:
por qué se eligió una librería, un límite, un formato, un atajo. Cada entrada:
fecha, contexto, decisión, alternativa descartada y consecuencia. No es un chat.

### 2.3 SESSION.md — no leerlo entero

Es un registro aditivo de las interacciones con el humano y crece sin límite.

- **Prohibido** leer `SESSION.md` completo.
- Leer solo el índice (primeras ~40 líneas) y la última entrada:
  `Get-Content SESSION.md -Tail 60`.
- Para un tema puntual: `Select-String -Path SESSION.md -Pattern "<keyword>"`.
- Al cerrar una interacción, **agregar** una entrada al final y una fila al
  índice. Nunca reescribir ni resumir entradas viejas.

### 2.4 docs/STATE.md

Estado operativo: versión instalada, hashes, configuración viva, últimos
resultados medidos, próximo paso exacto y bloqueos. Se actualiza en cada sesión
que cambie el estado. Es el archivo que permite retomar sin leer todo.

## 3. Prioridades (en orden)

1. **Riesgo de ban**: menos clics, cadencia irregular, topes de sesión.
2. **Corrección de los datos**: precios reales, sin inventar ni redondear.
3. **Velocidad por eficiencia**, nunca por imprudencia.

## 4. Límites duros

- Solo cliente. Nada de servidor, plugins ni mods del lado servidor.
- Nunca commitear credenciales, tokens, `.env`, dumps del juego ni `.tools/`.
- **Nunca modificar `../oreboard` (DonutProfit)** sin aprobación explícita.
- No prometer indetectabilidad. No spoofear marca, no ocultar el mod, no
  falsificar paquetes, no generar movimiento sintético.
- No inventar mecánicas del servidor ni estructura de menús. Si algo no está
  medido, marcarlo como supuesto y capturar evidencia real (F8).

## 5. Stack

| Pieza | Versión |
| --- | --- |
| Minecraft | 1.21.11 |
| Fabric Loader | 0.20.1 |
| Fabric API | 0.141.6+1.21.11 |
| Loom | 1.19.4 |
| Java (release) | 21 |
| JDK de build | 25 (portable en `.tools/jdk-25.0.5+8`, gitignored) |
| Mappings | Mojang |

## 6. Comandos

```powershell
cd "C:\Users\winky\OneDrive\Documentos\Default Project\donut-order-scanner"
$env:JAVA_HOME="C:\Users\winky\OneDrive\Documentos\Default Project\donut-order-scanner\.tools\jdk-25.0.5+8"
.\gradlew.bat build --no-daemon --console=plain
.\gradlew.bat runClient
```

Instalación local (con el juego cerrado):

```powershell
$m="$env:APPDATA\.minecraft-donutscanner\mods"
Remove-Item "$m\donut-order-scanner-<viejo>.jar"
Copy-Item build\libs\donut-order-scanner-<nuevo>.jar $m
```

Nunca dejar dos jars del mod en `mods/`: Fabric falla por mod id duplicado.

## 7. Convenciones

- **Chat y docs en español.** **Código, nombres de archivo y commits en inglés.**
- Commits en imperativo, claros, con el *por qué* en el cuerpo.
- Tests con JUnit 5. Los fixtures son capturas reales; no inventar menús.
- Precios: truncar, nunca redondear (evita inflar profit).
- Fechas y horas en ISO o `YYYY-MM-DD`.

## 8. Decisiones de diseño transversales

- **Export compatible con `parseOrdersJson` de DonutProfit.** Claves extra las
  descarta Zod; no cambiar nombres ni tipos sin coordinarlo.
- Fila: `{itemId, side:"buy", unitPrice, quantityRemaining, observedAt,
  displayName, page, slot, verified:false, search}`. `search` es el id del ítem
  cuya búsqueda produjo la fila, o ausente en una captura manual.
- `872/1k Delivered` significa entregado/requerido:
  `quantityRemaining = required - delivered`.
- Se guardan **todas** las filas de la página del ítem buscado, no solo el mejor
  precio: DonutProfit arma la escalera de demanda.
- **El escaneo automático guarda toda la página, no solo el ítem buscado.**
  Paginar hacia `diamond` atraviesa 42 páginas de otro gear; esas filas son
  observaciones reales y le dan un nivel de precio a cada ítem sin gastar una
  búsqueda. Pero son un **fallback**: en cuanto ese ítem se escanea por su cuenta,
  sus filas incidentales se descartan y se reemplazan por el escaneo directo. Así
  nunca inflan una escalera. El archivo de captura conserva la página completa.
- Merge, con `search` (id del ítem buscado):
  - Página 1 de una búsqueda: borra **todas** las filas de esa búsqueda (la
    observación anterior) y las reemplaza. Se dispara solo en página 1 porque
    una búsqueda profunda captura página por página; si se disparara en cada
    página, la 2 borraría lo que guardó la 1.
  - Filas incidentales (`itemId != search`): se guardan salvo que el ítem ya
    tenga escaneo directo. Su identidad es `(itemId, unitPrice,
    quantityRemaining)`, así dos búsquedas que la muestran no la duplican.
  - Fila sin `search` (F8 manual): reemplaza solo su página.
- **Aviso de ingestión:** las filas incidentales traen ítems que pueden no estar
  en el catálogo de DonutProfit, y `orders.itemId` tiene FK a `items`. El dry run
  los muestra; `--commit` falla si falta el ítem. Sembrar el catálogo antes.
- El escaneo usa **siempre `Most Per Item`**. `Recently Listed` está descartado:
  da lo último publicado y cualquiera puede poner órdenes muy por debajo del
  precio real.
- El buscador matchea el **nombre visible como subcadena** y ordena por precio
  descendente. `/order minecraft:diamond` devuelve menú vacío.
- Se pagina hasta la **primera página que contiene el ítem** y se corta ahí:
  esa página ya trae sus órdenes más caras. No se lee la banda completa.
- Topes: `maxPages` 200 (tope por ítem, guarda anti-bucle), `maxPagesPerSession`
  2000, `maxItemsPerSession` 103 (tamaño de la lista viva) y 30 min por sesión.
  El clamp duro permite hasta 1000 búsquedas y un piso de 1 s entre búsquedas.
- Cadencia: dos presets. **Seguro** (defaults del código): `5-12 s` con sesgo al
  extremo corto y 3 % de pausa de 1-2 min. **Throughput** (el `scan.json` vivo):
  `1-3 s` con el mismo sesgo, sin pausas, `pageDelay` 350-900 ms. El owner eligió
  throughput el 2026-11-16 sabiendo que son ~21 comandos por minuto y que
  contradice la prioridad 1. Si otro agente lo cambia, anotarlo en `LOG.md`.
- `pageDelay` no baja de 350 ms: por debajo, la página puede no haber cargado y
  el escaneo la toma como "no hay más páginas", perdiendo el ítem en silencio.
- `scan-cursor.txt` guarda el próximo índice para cubrir la lista entre sesiones.
  La lista viva tiene **103 ítems provisionales**; la definitiva la define el
  owner desde el grafo de recetas (materias primas **y** ítems crafteados). Ver
  `docs/AUTOSCAN.md`.
- **Dominio:** DonutProfit compara el costo de las materias primas contra el
  valor del ítem crafteado. Por eso la lista incluye las dos puntas y es grande.
- **Descartado:** leer el libro sin búsqueda (slot 49). Ordena por precio
  descendente, las materias primas baratas quedan a cientos de páginas y no es
  `Most Per Item`. La vía es la búsqueda por ítem.
- Escaneo automático: solo export compacto + `orders.json`. Dump crudo solo F8.

## 9. Git y GitHub

- Rama única: `main`. Como máximo 1-2 ramas de prueba/experimento.
- **Sin PR**: los agentes no trabajan en simultáneo; push directo.
- Commitear en **cambios estructurales o breaking changes**, no en cada edición
  mínima. Un commit debe poder explicarse solo.
- Antes de cada commit: `gradlew build` verde (incluye tests).
- No commitear `build/`, `run/`, `logs/`, `dumps/`, `.tools/`, jars ni secretos.
- Remoto: `origin` → <https://github.com/winkyrat/orderChecker.git>.

```powershell
git add -A
git commit -m "<tipo>: <resumen en inglés>"
git push origin main
```

## 10. Seguridad

- No manejar credenciales de Microsoft/Minecraft ni de GitHub.
- No imprimir tokens ni volcar `.env`.
- Si un push pide credenciales, detenerse y pedirle al humano que las configure.
- El perfil de juego aislado (`Donut Scanner 1.21.11`) existe para no cargar
  Wurst junto al mod. No mezclar perfiles.
