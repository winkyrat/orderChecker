# donut-order-scanner

Mod de cliente (Fabric) para leer los valores del menú `/orders` de DonutSMP y
exportarlos en el formato que consume DonutProfit.

Estado: Fase 1 implementada. El mod vuelca la pantalla abierta a JSON con F8.
Falta probarlo en el juego y capturar los dumps reales.

- Plan y fuentes: `docs/PLAN.md`
- Riesgos: `docs/RISKS.md`
- Preguntas pendientes: `docs/OPEN_QUESTIONS.md`
- Estado operativo: `docs/STATE.md`
- Cómo capturar: `docs/CAPTURE_GUIDE.md`

## Build

Requiere JDK 25 para Gradle/Loom. El mod compila con `--release 21` y corre en
Java 21. El JDK 25 local vive en `.tools/` y no se versiona.

```powershell
$env:JAVA_HOME="C:\Users\winky\OneDrive\Documentos\Default Project\donut-order-scanner\.tools\jdk-25.0.5+8"
.\gradlew.bat build
```

Salida: `build/libs/donut-order-scanner-0.3.0.jar`.

Tests: `.\gradlew.bat test`.

Advertencia: las reglas de DonutSMP prohíben macros, scripts y auto-clickers.
El owner aprobó el modo autónomo para una fase posterior y aceptó el riesgo.
El escaneo autónomo no se implementa todavía. No afiliado a Mojang, Microsoft
ni DonutSMP.
