# Riesgos — donut-order-scanner

> Evaluación honesta. Ningún diseño de este documento garantiza que la cuenta
> no sea sancionada. Las reglas y la detección pueden cambiar sin aviso.
>
> Última actualización: 2026-11-13.

## 1. Riesgo de ban

### 1.1 Estado de las reglas

Las reglas publicadas de DonutSMP prohíben, entre otras cosas:

- hacked clients y modificaciones con ventaja injusta;
- modificaciones de inventario (*inventory modifications*);
- macros, scripts, auto-clickers, mouse tweaks y scrollers;
- cualquier mod que dé una ventaja no disponible en el juego normal.

Fuente: DonutWiki, `Server Rules` y `Client Modifications` (actualizados
2026-09-06, citando los ToS oficiales). Los ToS directos de la tienda
respondieron 403; la wiki es la mejor fuente accesible. El criterio de "ventaja
injusta" es abierto y no existe lista blanca pública.

### 1.2 Escaneo del cliente

Reportes de la comunidad y fragmentos indexados de los ToS de la tienda indican
que DonutSMP hace comprobaciones de integridad del cliente al conectar y durante
la partida. No se pudo abrir la página oficial (403), así que esto es confianza
media. Implicación: no basta con que el mod no envíe paquetes raros. El servidor
puede inspeccionar la presencia de mods, el brand del cliente o el comportamiento.

No se implementará nada para ocultar el mod, falsificar el brand ni evadir el
escaneo: el owner lo prohibió y además aumenta el riesgo reputacional y de ban.

### 1.3 Comparación de riesgo por fase

| Fase | Qué hace | Riesgo relativo | Motivo |
| --- | --- | --- | --- |
| 1. Reconocimiento | Volcado local de una pantalla abierta | Bajo-medio | Sigue siendo un mod cargado; no automatiza nada. |
| 2. Lectura pasiva | Lee lo que el jugador ya ve | Bajo-medio | Cero clics del mod; el patrón es humano por construcción. |
| 3. Guiada | Resalta el próximo paso; el humano cliquea | Medio | Sigue siendo un mod de cliente; no viola la prohibición de macros. |
| 3. Autónoma | El mod navega y clickea | **Alto** | Es un macro/script de automatización, prohibido por las reglas. |

Recomendación original: no implementar la Fase 3 autónoma sin aprobación
explícita del owner y, preferentemente, sin consulta previa al staff.

**Excepción registrada (2026-11-13):** el owner eligió el modo autónomo y aceptó
el riesgo de ban. Se implementará con variación de ritmo, pausas y topes. No se
hará spoof de brand, ocultamiento del mod, paquetes inventados ni movimiento
sintético. La probabilidad de sanción sigue siendo real y no se puede medir
desde este proyecto.

### 1.3.1 Qué quedó implementado (0.4.0) y qué no mitiga

Medidas presentes: delay al azar 5–9 s (0.8.1; antes 12–35 s), pausa larga
ocasional (5%), topes de 40 búsquedas y 30 min, corte si el jugador se mueve, si
la ventana pierde foco o si aparece otro menú, y `maxPages` como guarda contra un
bucle.

Lo que **no** cambia el fondo del problema: el servidor ve un cliente que manda
`/order` y clickea "Next page" en un patrón de máquina. Eso es exactamente lo que
las reglas llaman macro. Ningún ajuste de ritmo lo convierte en humano. Los topes
limitan la exposición y el daño si algo sale mal; no la eliminan.

### 1.4 Vectores de detección a asumir

Aun en modo pasivo:

- El jugador usa Fabric; el servidor puede detectar la plataforma.
- Un mod desconocido puede activar una revisión manual.
- Abrir y cerrar el menú `/orders` con una frecuencia alta es observable.
- Un patrón de clicks perfectamente regular es observable.
- La lectura de contenedores en cliente no genera paquetes extra, pero el mod
  puede delatarse por metadata, crash reports o comportamiento.

Mitigación: instalar el mod solo cuando se vaya a usar, mantener el perfil
seguro, no escanear en bucle, no dejar el mod corriendo AFK y no automatizar
compras ni ventas.

### 1.5 Cuenta y sanciones

Las reglas permiten sancionar cuentas asociadas. Un ban puede afectar más que
la cuenta que corre el mod. El owner debe decidir si acepta el riesgo antes de
la Fase 1.

## 2. Riesgo de cambios en el menú

El layout de `/orders` no está documentado y puede cambiar sin aviso:

- título, tamaño del inventario o cantidad de filas;
- posición de los slots de ítems, búsqueda y paginación;
- lore con precio unitario, total, cantidad o expiración en distinto orden;
- formato de dinero (`$1,250`, `$1.2K`, `$1.25M`, sufijos locales);
- ítems con NBT/encantamientos que no deben agregarse entre sí;
- páginas vacías, una sola order, cantidades enormes;
- cambios de comisión o de redondeo.

Mitigación:

- Parser estricto y versionado; si no coincide el patrón esperado, se descarta.
- Dumps de referencia por versión del menú.
- Test de regresión por cada cambio observado.
- El mod nunca “adivina” con heurísticas peligrosas.

## 3. Riesgo de datos incorrectos

Un precio mal leído contamina DonutProfit y puede causar pérdidas al usuario.

- Precio total confundido con precio unitario.
- Sufijos (`K`, `M`) mal expandidos.
- Cantidad de stack confundida con cantidad de la order.
- Órdenes de compra y de venta mezcladas.
- Órdenes expiradas o parcialmente llenadas.
- Ítems con el mismo nombre y distinto NBT.

Mitigación:

- Conservar siempre el texto crudo y el slot como evidencia.
- Descartar ante ambigüedad; no completar campos faltantes.
- `verified: false` por defecto.
- Tests con casos límite y revisión manual de una muestra.

## 4. Riesgo de dependencia y mantenimiento

- El mod depende de una versión de Minecraft y de Fabric; una actualización del
  servidor o del cliente puede romperlo.
- DonutProfit puede cambiar su contrato de orders; el export debe versionarse.
- La API oficial de DonutSMP ya cayó una vez; este mod es un reemplazo frágil.
- La wiki y las reglas pueden cambiar.

Mitigación: versión de schema explícita, adapter aislado en DonutProfit,
documentación de migración y tests de contrato.

## 5. Riesgo operativo y de privacidad

- Los dumps pueden incluir nombres de jugadores y datos de mercado.
- Un archivo exportado con token en un repo sería un secreto filtrado.
- Un mod de terceros descargado de fuente dudosa es un vector de malware.

Mitigación: `.gitignore` para dumps, `run/` y `.env`; sin telemetría; el mod se
compila localmente desde este repo; no se instalan jars de dudosos.

## 6. Riesgo legal y de marca

DonutProfit no es oficial de Mojang ni de DonutSMP. El mod tampoco debe
presentarse como aprobado por el servidor. No se usarán assets del servidor ni
de Mojang. La EULA de Minecraft permite herramientas no oficiales, pero no
autoriza incumplir las reglas del servidor.

## 7. Riesgo de negocio

Si DonutProfit cobra por datos derivados de un mod que viola las reglas del
servidor, el riesgo se traslada al producto. La decisión de monetizar este canal
requiere revisión del owner y, idealmente, contacto con el staff.

## 8. Riesgos residuales aceptados

Aun con todas las mitigaciones:

- el servidor puede banear por detectar Fabric o un mod desconocido;
- el menú puede cambiar y el mod quedarse sin datos;
- un parseo puede ser incorrecto pese a los tests;
- el owner puede perder acceso a la cuenta si usa el mod.

Estos riesgos no se eliminan; solo se reducen. La decisión de usarlo es del owner.
