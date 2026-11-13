# STATE — donut-order-scanner

> Retomá desde acá. Una sesión nueva debe poder continuar leyendo solo este
> archivo y los documentos que referencia.

## Estado actual

- Fecha: 2026-11-13.
- Fase: **Fase 1 instalada y cargando en el juego**. Falta probar F8 y capturar.
- Build/tests: `gradlew build` verde. 2 tests unitarios del escritor de dumps.
- Aprobación del owner para Fase 1: **dada** (eligió modo C y aceptó el riesgo).

## Instalación en la máquina del owner (2026-11-13)

Perfil aislado, para no mezclar Wurst con DonutSMP:

| Qué | Dónde |
| --- | --- |
| Perfil del launcher | `Donut Scanner 1.21.11` → versión `fabric-loader-0.20.1-1.21.11` |
| Carpeta de juego | `%APPDATA%\.minecraft-donutscanner\` |
| Mods | `donut-order-scanner-0.1.0.jar`, `fabric-api-0.141.6+1.21.11.jar` |
| Dumps | `%APPDATA%\.minecraft-donutscanner\config\donut-order-scanner\dumps\` |
| Fabric Loader | `0.20.1` (instalado con installer 1.1.2 en `.minecraft/versions`) |

- Se copiaron `options.txt`, `optionsof.txt`, `servers.dat` y `servers.dat_old`
  al game dir para no perder ajustes ni servidores.
- `%APPDATA%\.minecraft\mods\` **no se tocó**: sigue con `fabric-api-0.140.0`
  y `Wurst-Client-v7.51.4`. El perfil aislado no carga esa carpeta.
- Antes de escribir `launcher_profiles.json` se guardó
  `launcher_profiles.json.bak-donutscanner`. El launcher llegó a reescribir el
  archivo dejando solo 2 perfiles; se restauró desde el backup y quedó con 33.
  Si algo raro pasa con los perfiles, ese `.bak` es la referencia.
- Log de la primera corrida: `Loading Minecraft 1.21.11 with Fabric Loader 0.20.1`,
  `donut_order_scanner 0.1.0` entre los 49 mods, sin `ERROR` ni `WARN`.

## Hecho

- Proyecto creado en `donut-order-scanner/`, repo git inicializado en `main`.
- `docs/PLAN.md`: investigación, contrato de DonutProfit, decisión de loader,
  fases, arquitectura y fuentes.
- `docs/RISKS.md`: ban, menú, datos, dependencia, privacidad, legal.
- `docs/OPEN_QUESTIONS.md`: bloqueantes B1, B2, B8 y resto.
- Identificado DonutProfit real en `../oreboard` (no se modificó).
- Contrato de orders y lista demo de 60 ítems documentados.
- Mod Fabric 1.21.11 con keybind F8 que vuelca la pantalla abierta a JSON.
- **Fix 0.1.1:** vanilla ignora los keybinds mientras hay una pantalla abierta,
  así que F8 no podía dumpear con el menú abierto (síntoma: "No screen open"
  en el chat justo después de cerrar el menú). Ahora el mod escucha
  `ScreenKeyboardEvents.allowKeyPress` por pantalla y consume la tecla. Sonido
  de confirmación porque el chat no se ve con un contenedor abierto.
- `docs/CAPTURE_GUIDE.md` con los pasos exactos de captura.
- Toolchain local: JDK 25 en `.tools/` (Loom 1.19.4 lo exige para compilar;
  el mod compila con `--release 21` y corre en Java 21).

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

1. En el juego (perfil `Donut Scanner 1.21.11`), abrir `Options > Controls >
   Key Binds` y confirmar que existe la categoría **Donut Order Scanner** con
   **Dump open screen** en F8. Si F8 choca, reasignar.
2. Prueba rápida: abrir el inventario (E) y apretar F8. Debe aparecer un
   `.json` en `%APPDATA%\.minecraft-donutscanner\config\donut-order-scanner\dumps\`.
3. Seguir `docs/CAPTURE_GUIDE.md`: `/orders`, búsqueda, página 2, menú vacío,
   cantidad grande y cantidad 1.
4. Pasar los `.json` sin editar. Con eso: fixtures, parser puro y tests (Fase 2).
5. Si el cliente no entra con 1.21.11: recompilar para 26.3 (Fabric API
   `0.166.2+26.3`, Java 25, mappings de Mojang). El código de dump es portable.

## Bloqueos

- B1: confirmar que el cliente entra con 1.21.11. Si no, portar a 26.3.
- B2: resuelto. El owner aceptó el riesgo y eligió modo autónomo (C).
- B3: lista de ítems. Provisional: los 60 del seed de DonutProfit.
- B8: resuelto. Modo C, con jitter/pausas/topes y sin evasión.
- Dumps reales: sin ellos no se escribe el parser (Fase 2).

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
