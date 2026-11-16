# donut-order-scanner

Mod de cliente (Fabric) para leer los valores del menú `/orders` de DonutSMP y
exportarlos en el formato que consume DonutProfit.

Estado: Fase 3 implementada en `0.7.0`. **F8** vuelca la pantalla abierta y
exporta las órdenes; **F9** prende el escaneo autónomo.

- Coordinación de agentes: `AGENTS.md`
- Checklist del proyecto: `ROADMAP.md`
- Decisiones instrumentales: `LOG.md`
- Interacciones con el humano: `SESSION.md` (leer solo el índice y la última
  entrada)
- Estado operativo: `docs/STATE.md`
- Plan y fuentes: `docs/PLAN.md`
- Riesgos: `docs/RISKS.md`
- Preguntas pendientes: `docs/OPEN_QUESTIONS.md`
- Cómo capturar: `docs/CAPTURE_GUIDE.md`
- Escaneo autónomo: `docs/AUTOSCAN.md`

Repo: <https://github.com/winkyrat/orderChecker>

## Build

Requiere JDK 25 para Gradle/Loom. El mod compila con `--release 21` y corre en
Java 21. El JDK 25 local vive en `.tools/` y no se versiona.

```powershell
$env:JAVA_HOME="C:\Users\winky\OneDrive\Documentos\Default Project\donut-order-scanner\.tools\jdk-25.0.5+8"
.\gradlew.bat build
```

Salida: `build/libs/donut-order-scanner-0.10.0.jar`.

Tests: `.\gradlew.bat test`.

Advertencia: las reglas de DonutSMP prohíben macros, scripts y auto-clickers.
El owner aprobó el modo autónomo y aceptó el riesgo de ban. El escaneo manda
comandos y clickea páginas reales; no falsifica paquetes ni oculta el mod, y no
hay garantía de que pase desapercibido. No afiliado a Mojang, Microsoft ni
DonutSMP.
