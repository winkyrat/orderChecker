# Plan — donut-order-scanner

> Mod de cliente para leer valores del menú `/orders` de DonutSMP y alimentar
> DonutProfit. Documento vivo. El estado operativo vive en `STATE.md`.
>
> Última actualización: 2026-11-13 (Fase 0 cerrada).

## 1. Objetivo y prioridades

Reemplazar la API pública caída de DonutSMP como fuente de precios de orders.
Prioridades, en orden:

1. Riesgo de ban mínimo.
2. Datos correctos (un precio mal leído es peor que ninguno).
3. Velocidad y cobertura por eficiencia, no por agresividad.

Restricciones del proyecto:

- Solo cliente. Ningún paquete inventado, ningún bypass de anticheat.
- Sin credenciales de Microsoft/Minecraft.
- DonutProfit (`../oreboard`) se lee, no se modifica sin aprobación.

## 2. Resultado de la investigación (Fase 0)

### 2.1 Skills, MCP y documentación integrada

Búsqueda hecha el 2026-11-13. Skills disponibles en el entorno:

| Skill | Relevancia |
| --- | --- |
| `caveman` | Ninguna. Solo estilo de chat. |
| `opencode` | Ninguna. Documentación de OpenCode. |
| `report` | Ninguna. Reporte de bugs de OpenCode. |
| `web-vuln-scan` | Ninguna. Auditoría web ofensiva. |
| `browser` | Baja. Útil solo para leer documentación web. |

Servidores MCP: no hay recursos expuestos (`list_mcp_resources` vacío).
No se instaló ninguna skill, plugin ni servidor MCP de terceros.
No existe documentación integrada de Minecraft/Fabric.

Fuentes usadas: documentación oficial de Fabric, metadata de Fabric, wiki de
DonutSMP y documentos de DonutProfit. Detalle y nivel de confianza en §8.

### 2.2 DonutProfit: esquema y contrato de ingesta

Repositorio real: `../oreboard` (marca DonutProfit).

- Catálogo demo: 60 ítems en `src/data/seed/items.ts` (`minecraft:diamond`,
  `minecraft:iron_ingot`, etc.). Es la lista base del producto; confirmar si el
  escaneo debe cubrir exactamente esos 60 o un subconjunto.
- Contrato de orders (`src/data/adapters/contract.ts`):
  - `itemId: string`
  - `side: "buy" | "sell"`
  - `unitPrice: bigint` (entero, moneda del juego)
  - `quantityRemaining: int > 0`
  - `expiresAt?: ISO-8601`
  - `verified: boolean` (default `false`)
  - payload: `{ source, observedAt, orders[] }`
- Carga manual (`src/data/adapters/manual-orders.ts`): acepta JSON como array o
  como objeto con `orders: []`. Ignora claves desconocidas del objeto raíz.
  Alias aceptados: `item`, `itemId`, `price`, `unitPrice`, `qty`,
  `quantityRemaining`, `observedAt`, etc.
- `source` es un string libre; el loader manual usa `manual`. Para el mod se
  propondrá `donutsmp-mod` con un adaptador nuevo (Fase 4).
- Semántica de `verified`: el loader manual defaultea `false`. El mod no debe
  marcar `true` sin una verificación adicional; `true` implica más confianza.

Formato de export propuesto (compatible con el parser actual):

```json
{
  "schemaVersion": 1,
  "modVersion": "0.1.0",
  "source": "donutsmp-mod",
  "observedAt": "2026-11-13T12:00:00Z",
  "contributor": null,
  "orders": [
    {
      "itemId": "minecraft:diamond",
      "side": "buy",
      "unitPrice": 1250,
      "quantityRemaining": 64,
      "verified": false,
      "observedAt": "2026-11-13T12:00:00Z"
    }
  ]
}
```

El parser actual lee `orders` y descarta el resto del objeto raíz. El archivo
lateral NDJSON se reserva para auditoría y dumps crudos.

### 2.3 Mecánica de `/orders` (wiki, no verificada en juego)

- `/orders` abre el menú de orders. `/order <búsqueda>` busca.
- Una order es una solicitud de compra de un jugador: alguien ofrece dinero por
  ítems. El precio lo define el comprador.
- La wiki distingue precio por ítem de pago total, pero no documenta el layout
  del menú, los slots, la paginación ni el formato exacto de los precios.
- No hay endpoint público de orders. Confirmar si el menú muestra precio
  unitario, total, cantidad restante y expiración.

Todo lo anterior es **suposición hasta la Fase 1**. El parser no se escribe
antes de capturar dumps reales.

### 2.4 Versión de Minecraft y loader

Evidencia:

- La página de servidor de DonutSMP en Modrinth dice "joinable on any version"
  (actualizada 2026-03-04) y recomienda 1.21.11 / 1.21.x.
- Listados de terceros reportan 26.3 para el servidor, pero son metadata
  automática y contradictoria.
- Fabric soporta 1.21.11 (última versión obfuscada, Yarn disponible) y 26.x
  (sin obfuscación, mappings de Mojang). 26.1+ exige Java 25.
- El entorno tiene JDK 21 (Temurin 21.0.9), que alcanza para 1.21.11.

Decisión provisional: **Fabric + Minecraft 1.21.11 + Java 21**.

Motivos: es la versión recomendada por la ficha del servidor, permite entrar a
DonutSMP aunque el backend sea más nuevo (el servidor acepta varias versiones),
Yarn sigue disponible y el JDK instalado alcanza. Riesgo: si el cliente del
owner no puede entrar con 1.21.11, hay que recompilar para 26.3 con Java 25 y
mappings de Mojang. El código de lectura de pantalla es portable; el costo es
de toolchain, no de diseño.

Versiones a fijar en Fase 1 (verificadas hoy en metadata oficial):

- Fabric Loader `0.20.1` (última estable).
- Fabric API `0.141.6+1.21.11` (última para esa versión).
- Loom y Gradle: los del template oficial para 1.21.11; no hardcodear a mano.

Pendiente bloqueante: confirmar con el owner la versión exacta con la que entra
al servidor. Se registra en `OPEN_QUESTIONS.md`.

## 3. Arquitectura

Paquetes/módulos separados, con el dominio testeable sin el juego:

| Módulo | Responsabilidad | Depende de Minecraft |
| --- | --- | --- |
| `ScreenReader` | Captura título, tipo de contenedor, slots, item, nombre, lore y componentes. Solo lectura. | Sí |
| `Parser` | Funciones puras: slots crudos → observaciones. Validación estricta. | No |
| `ItemQueue` | Cola con prioridad, TTL y presupuesto por sesión. | No |
| `Navigator` | Lista blanca estricta de acciones. | Sí |
| `SafetyGuard` | Kill switch, condiciones de aborto, límites. | Parcial |
| `Exporter` | JSON compatible con DonutProfit + NDJSON de auditoría. | No |
| `Config` | Defaults conservadores; perfiles. | No |

Regla: el `Parser` nunca importa clases de Minecraft. Los fixtures son dumps
reales serializados a JSON.

## 4. Fases

### Fase 0 — Investigación (completada)

Entregables: este plan, `RISKS.md`, `OPEN_QUESTIONS.md`, `STATE.md`.
Sin código de mod. Gate: revisión del owner.

### Fase 1 — Herramienta de reconocimiento

Mod mínimo, solo lectura, sin interacción automática:

- Tecla de debug (sin comando de servidor) que vuelca la pantalla actual.
- Dump: clase de pantalla, título (texto y JSON), tamaño, y por slot: índice,
  item id, count, nombre, lore completo (texto plano + JSON), NBT si existe.
- Archivo por captura en `config/donut-order-scanner/dumps/`.
- Sin clics, sin teclas simuladas, sin paquetes propios.

DoD: build reproducible, instrucciones de captura, dumps de los casos pedidos.

### Fase 2 — Lectura pasiva (modo por defecto)

- Captura al abrir/actualizar el menú visible. Cero automatización.
- Parser puro con tests sobre los dumps reales.
- Validaciones: descartar slot sin item, precio 0/negativo, cantidad 0,
  formato ambiguo, item fuera de la lista, outliers, duplicados.
- Cada registro: timestamp, versión del mod, evidencia (slot, texto crudo).
- Export JSON compatible + NDJSON de auditoría.
- Kill switch: tecla que apaga toda captura y export hasta reiniciar.

DoD: tests verdes con fixtures reales; export cargado por
`pnpm db:load-orders` en dry run sin filas inválidas.

### Fase 3 — Escaneo activo (opt-in, desactivado por defecto)

**Advertencia de reglas.** Las reglas oficiales de DonutSMP prohíben "macros,
scripts, auto-clickers, mouse tweaks y scrollers". Un recorrido autónomo del
menú entra en esa categoría aunque no compre ni venda. Por eso la Fase 3 no se
implementa como robot de clics sin una decisión explícita del owner.

Diseño propuesto y seguro: **modo guiado**.

- El mod elige el próximo ítem y resalta el slot de búsqueda/paginación.
- El jugador hace cada clic.
- El mod lee y exporta lo que aparece.
- Sin clics sintéticos, sin movimiento, sin rotación, sin paquetes.

Si el owner acepta el riesgo de un modo autónomo, se implementará solo con:

- Lista blanca de acciones (abrir `/orders`, buscar, paginar, volver).
- Prohibido clickear slots de compra/venta/confirmación.
- Máquina de estados con timeouts, backoff y aborto seguro.
- Perfiles `safe` (default) y `normal`; jitter, pausas y topes por sesión/hora.
- Kill switch y parada ante chat inesperado, teleport, menú distinto,
  desconexión o lag anómalo.

Aun así, ningún diseño garantiza ausencia de ban.

### Fase 4 — Integración con DonutProfit

- Entrega local por archivo (default) y HTTP opt-in con token.
- Comparativa de opciones y propuesta de endpoint/adaptador, sin tocar
  DonutProfit hasta aprobación.
- Campos preparados para multi-contribuidor: `contributor` anónimo,
  `observedAt`, `modVersion`, `schemaVersion`.

## 5. Formato de datos y reglas de validación

- Dinero: entero (`long`), nunca `double`. Si el menú muestra `1.2M`, se
  convierte con una tabla explícita y se conserva el texto crudo como evidencia.
- Precio unitario: solo si el menú lo declara; si muestra total, se divide con
  `ceil` únicamente cuando la cantidad es conocida y el redondeo está claro.
- Un parseo dudoso se descarta y se registra como `unparsed`, no se adivina.
- `side`: `buy` mientras la evidencia diga que son órdenes de compra.
- `verified`: `false` por defecto. No se auto-promueve.
- `expiresAt`: solo si el menú lo muestra de forma inequívoca.

## 6. Seguridad y privacidad

- Sin telemetría, sin auth de Microsoft, sin tokens en el jar.
- HTTP solo a un endpoint configurado por el usuario, con token en archivo
  local ignorado por git.
- Dumps pueden contener nombres de jugadores; se guardan localmente y no se
  publican.
- `.gitignore` cubre `run/`, `dumps/`, `.env`, logs.

## 7. Riesgo de las fases

Ver `RISKS.md`. Resumen: la Fase 2 es la de menor riesgo; la Fase 3 autónoma
es incompatible con las reglas publicadas y no se hará sin aprobación explícita.

## 8. Fuentes

Consultadas 2026-11-13 salvo indicación.

| Fuente | URL | Qué aporta | Confianza |
| --- | --- | --- | --- |
| DonutSMP Server Rules (wiki) | https://donutsmp.wiki/server-rules | Prohíbe macros, scripts, auto-clickers, mouse tweaks/scrollers; enforcement a cuentas asociadas. | Media-alta (wiki que cita los ToS; ToS directo da 403) |
| Client Modifications (wiki) | https://donutsmp.wiki/client-modifications | Categorías prohibidas; no hay lista blanca pública; "unfair advantage" es criterio abierto. | Media-alta |
| Orders (wiki) | https://donutsmp.wiki/orders | `/orders`, `/order <search>`, orders son solicitudes de compra; precio por ítem vs total. | Media |
| DonutSMP en Modrinth | https://modrinth.com/server/donutsmp | "Joinable on any version"; recomienda 1.21.11 / 1.21.x. | Media |
| Fabric 1.21.11 | https://fabricmc.net/2025/12/05/12111.html | Última versión obfuscada; Loom 1.14; Loader estable al momento. | Alta |
| Fabric Loader metadata | https://meta.fabricmc.net/v2/versions/loader | Loader estable actual `0.20.1`. | Alta |
| Fabric API (Modrinth) | https://api.modrinth.com/v2/project/fabric-api/version | `0.141.6+1.21.11` para 1.21.11; `0.166.2+26.3` para 26.3. | Alta |
| Fabric Example Mod | https://github.com/FabricMC/fabric-example-mod | Template oficial; rama `26.3`; se usará el equivalente 1.21.11. | Alta |
| Documentación Fabric | https://docs.fabricmc.net/develop/ | Pantallas, eventos, keybinds, networking. | Alta |
| DonutProfit | `../oreboard/docs/PROJECT.md`, `SOURCES.md`, `contract.ts`, `manual-orders.ts` | Esquema, lista de 60 ítems, parser de orders. | Alta (código local) |

No verificado: estructura real del menú `/orders`, formato exacto de precios,
paginación, expiración, fees, comportamiento del anticheat con mods de lectura,
y si DonutSMP permite este caso concreto. Se resuelve con dumps (Fase 1) y
consulta a staff (owner).
