# STATE — donut-order-scanner

> Retomá desde acá. Una sesión nueva debe poder continuar leyendo solo este
> archivo y los documentos que referencia.

## Estado actual

- Fecha: 2026-11-13.
- Fase: **Fase 0 cerrada**. Sin código de mod todavía.
- Build/tests: no aplica; el repo solo contiene documentación.
- Aprobación del owner para pasar a Fase 1: **pendiente**.

## Hecho

- Proyecto creado en `donut-order-scanner/`, repo git inicializado en `main`.
- `docs/PLAN.md`: investigación, contrato de DonutProfit, decisión de loader,
  fases, arquitectura y fuentes.
- `docs/RISKS.md`: ban, menú, datos, dependencia, privacidad, legal.
- `docs/OPEN_QUESTIONS.md`: bloqueantes B1, B2, B8 y resto.
- Identificado DonutProfit real en `../oreboard` (no se modificó).
- Contrato de orders y lista demo de 60 ítems documentados.

## Decisiones tomadas

| ID | Decisión | Motivo |
| --- | --- | --- |
| D1 | Fabric, no Forge/NeoForge | Menor peso, mejor soporte cliente, template oficial, mixins solo si hacen falta. |
| D2 | Minecraft 1.21.11 (provisional) | Ficha del servidor lo recomienda; JDK 21 instalado; Yarn disponible. Revisar B1. |
| D3 | Export JSON compatible con `parseOrdersJson` | DonutProfit ya lo consume; evita tocar el repo hermano. |
| D4 | Fase 3 autónoma **aprobada por el owner** (2026-11-13), riesgo aceptado | El owner eligió opción C. Se implementa con jitter, pausas y topes; sin evasión (brand, ocultamiento, paquetes, movimiento falso). |
| D5 | Parser puro, sin imports de Minecraft | Testeable con fixtures reales. |
| D6 | Intervalo fijo de 3 s descartado | Un patrón perfecto es más detectable. Se usa jitter con cola larga. |

## Próximo paso exacto

1. Owner responde B1, B2 y B3 (versión, riesgo, lista de ítems).
2. Si B1 = 1.21.11: clonar/adaptar el template oficial de Fabric para 1.21.11,
   fijar Loader `0.20.1`, Fabric API `0.141.6+1.21.11`, Java 21.
3. Implementar el volcado de Fase 1 (keybind + escritura de dump JSON).
4. Compilar, ejecutar `./gradlew build`, commit `feat: add screen dump tool`.
5. Probar en el juego y adjuntar dumps; actualizar `STATE.md`.

Si B1 = 26.3: instalar JDK 25, Loom actual, mappings de Mojang, Fabric API
`0.166.2+26.3`; el resto del plan no cambia.

## Bloqueos

- B1: versión real del cliente.
- B2: aceptación del riesgo de ban.
- B3: lista de ítems.
- B8 (solo Fase 3): alcance del modo activo.

## Comandos previstos

```powershell
cd "C:\Users\winky\OneDrive\Documentos\Default Project\donut-order-scanner"
.\gradlew build
.\gradlew runClient
```

## Archivos que no se deben tocar sin permiso

- `../oreboard/**` (DonutProfit).
- Cualquier `.env`, token o credencial.
- `run/` y `config/` generados por el cliente.
