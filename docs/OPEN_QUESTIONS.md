# Preguntas abiertas — donut-order-scanner

> Responder antes de la fase indicada. Las marcadas **bloqueante** impiden
> avanzar sin inventar datos.
>
> Última actualización: 2026-11-13.

## Antes de la Fase 1

### B1. Versión de Minecraft con la que entrás a DonutSMP — bloqueante

La ficha del servidor dice "joinable on any version" y recomienda 1.21.11, pero
otros listados reportan 26.3. Necesito el dato real:

- ¿Qué versión elegís en el launcher al entrar?
- ¿DonutSMP te muestra algún mensaje de versión al conectar?

Plan provisional: Fabric + 1.21.11 + Java 21. Si entrás con 26.x, se recompila
para 26.3 (requiere Java 25).

### B2. Aceptación del riesgo de ban — bloqueante

Las reglas prohíben macros/scripts, y hay reportes de inspección del cliente.
¿Aceptás el riesgo de usar un mod de lectura pasiva? ¿Consultaste al staff o
pensás hacerlo? Sin esta respuesta no se instala nada en tu cuenta.

### B3. Lista de ítems objetivo

DonutProfit tiene 60 ítems demo en `oreboard/src/data/seed/items.ts`.
¿Escaneamos exactamente esos 60, un subconjunto, o la lista completa de orders
del servidor? Si es un subconjunto, pasame la lista.

### B4. Dónde correr el cliente

¿Minecraft vanilla/Fabric en Windows con el `.minecraft` habitual?
¿Qué ruta usa tu launcher? El mod escribirá en `config/donut_order_scanner/`.

## Antes de la Fase 2

### B5. Evidencia del menú

Necesito capturas o dumps de:

- pantalla principal de `/orders`;
- resultado de `/order <item>` o de la búsqueda interna;
- una página con muchas orders y otra vacía;
- una order con cantidad grande;
- el tooltip/lore de una order (precio, cantidad, expiración si aparece).

Los dumps de la Fase 1 los genera el mod; si algo no se puede abrir, una captura
de pantalla sirve como respaldo.

### B6. Semántica del precio

¿El menú muestra precio por ítem, pago total, o ambos? ¿La cantidad es la
restante o la original? ¿Hay comisión? No lo voy a suponer.

### B7. Órdenes de compra y de venta

¿`/orders` muestra solo órdenes de compra (buy) o también de venta (sell)?
¿Hay una pestaña o comando distinto para cada lado?

## Antes de la Fase 3

### B8. Alcance del escaneo activo — bloqueante

Las reglas prohíben macros/scripts/auto-clickers. Opciones:

- **A (recomendada):** modo guiado, el humano cliquea, el mod resalta y lee.
- **B:** modo autónomo opt-in, con lista blanca y perfil seguro, asumiendo riesgo
  alto de sanción.
- **C:** no implementar Fase 3.

¿Cuál elegís? Sin respuesta, se implementa A o C, nunca B.

### B9. Límites aceptables

Si hay modo activo: ¿cuántos ítems por sesión y por hora considerás razonable?
¿Horario de uso? ¿Pausa entre ítems?

## Antes de la Fase 4

### B10. Entrega de datos

¿Archivo local solamente, o también HTTP a DonutProfit? Si HTTP:

- ¿endpoint y método?
- ¿token compartido o por usuario?
- ¿TLS y hosting?
- ¿qué hacés con datos de otros contribuidores?

### B11. Permiso para tocar DonutProfit

La Fase 4 probablemente requiera un adaptador nuevo (`donutsmp-mod`) o un
endpoint. ¿Autorizás modificarlo cuando llegue el momento? Hasta entonces solo
se lee.

### B12. Contribuidores

¿El proyecto va a aceptar datos de varios usuarios? Si sí, hay que definir
consentimiento, anonimato, rate limit y validación anti-poisoning antes de
implementarlo.

## Supuestos registrados (no confirmados)

- S1. Las orders del menú son órdenes de compra (`side: "buy"`).
- S2. El menú expone un precio interpretable como unitario o total con cantidad.
- S3. El título del menú contiene "Orders" o similar y se puede detectar sin
  ambigüedad.
- S4. La paginación usa slots estables dentro del contenedor.
- S5. Los ítems del menú se pueden mapear a los IDs de `minecraft:*` del seed.

Cada supuesto se valida o se descarta con los dumps de la Fase 1.
