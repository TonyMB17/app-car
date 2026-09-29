# Validación de V0.6.1 — Confirmación del widget

29/09/2026: confirmación verde «Guardado: S/ …» durante aproximadamente 1.8 segundos después de guardar, con toast adicional cuando Android lo permite. Revisión visual en QA: dos registros consecutivos de S/ 5 muestran la confirmación; entre ambos vuelve el estado normal. Capturas `app/build/feedback-first.png` y `app/build/feedback-repeat.png`. El emulador suprime los toasts de fondo por configuración del sistema; la franja permite confirmar sin depender de ellos.

Compilación y Lint correctos. Prueba focalizada con PendingIntent real: registro, rechazo de repetición del mismo evento, Deshacer y bloqueo del teléfono. Un primer intento agotó el tiempo de espera; se añadieron mensajes de diagnóstico y las dos ejecuciones posteriores pasaron. No se reejecutó toda la batería histórica. El restablecimiento visual consulta el estado actual para conservar registros posteriores y limpiar también tras cambiar de página.

0.6.1 instalada como actualización en el emulador; QA retirado por el ejecutor de pruebas. La app diaria muestra 6 servicios, S/ 33 de ingresos y S/ 50 de combustible. Los registros de esta validación manual se hicieron exclusivamente en QA.

---

# Validación de V0.6 — Widget

29/09/2026: compilación correcta y Lint sin incidencias. Cobertura acumulada: 36 pruebas (12 JVM y 24 instrumentadas). La ejecución completa pasó las pruebas existentes y la integración del widget; una prueba nueva de tarifa eliminada se ajustó para avanzar el reloj más allá del bloqueo de 600 ms. Reejecución posterior: las 3 pruebas del repositorio del widget y la prueba de integración pasaron. Esta última también bloquea el emulador, envía una acción real y comprueba que no registre antes de desbloquearlo.

Cobertura nueva: migración 3→4 conserva tarifas personalizadas, servicios y combustible; consumo del token e inserción atómicos; repetición tras reabrir y doble toque sin duplicar; tarifas obsoletas rechazadas; Otro monto y Deshacer; protección de servicios editados; paginación y límite tras borrar tarifas. Integración con AppWidgetHost y PendingIntent real.

Validación manual en QA, Android 17: widget añadido al launcher; proceso terminado con `am kill` (PID desaparece), un toque registra y arranca otro proceso sin abrir Activity; Deshacer; Otro monto S/ 8.50 vuelve al launcher con importe correcto y opción Deshacer; alta de quinta tarifa S/ 12.50 actualiza el widget y aparece en página 2. QA retirado después. Capturas `app/build/mitaxi-widget-qa.png` y `app/build/widget-qa-page2.png`.

0.6.0 instalada sobre la app diaria conservando servicio S/ 5, combustible S/ 50 y tarifas originales. Widget añadido al launcher, sin introducir registros de prueba en esa app. Captura `app/build/mitaxi-v06.png`. Pendientes: teléfono físico, otros launchers, API 26 y reinicio completo del dispositivo. Forzar detención tiene restricciones distintas de cerrar el proceso; ver decisiones técnicas.

---

# Historial: validación de V0.5 — Tarifas configurables

29/09/2026: compilación correcta, Lint sin incidencias y 32 pruebas aprobadas (12 JVM, 20 instrumentadas). Nuevas pruebas verifican migración desde el esquema JSON v2 conservando servicios y combustible, orden persistente, decimales, rechazo de duplicados y montos inválidos, registro con tarifa obsoleta rechazado, histórico independiente y lista vacía conservada al reabrir. La migración previa desde v1 ahora recorre 1→2→3. UI: edición, validación, alta, cambio de orden, reflejo en Inicio, registro con decimales y cancelación/confirmación de eliminación.

0.5.0 instalada como actualización en el emulador Android 17. Inicio conserva servicio S/ 5 y combustible S/ 50 previos; aparecen tarifas iniciales 5/6/7/10. Revisión visual de Ajustes correcta, lista desplazable con controles de orden y edición. Captura `app/build/mitaxi-v05.png`. No se modificaron registros ni tarifas de la app diaria para probar; pruebas aisladas en QA. Teléfono físico y API 26 pendientes.

---

# Historial: validación de V0.4 — Resumen por período

29/09/2026: 29 pruebas aprobadas (12 JVM, 17 instrumentadas), compilación correcta y Lint sin incidencias. Nuevas pruebas cubren semana de lunes a domingo cruzando año, mes bisiesto y medianoche local, semana con cambio de horario, límites incluidos/excluidos, los cuatro filtros, totales exactos, selección conservada al navegar y actualización reactiva al añadir gastos.

0.4.0 instalada sobre la app del emulador Android 17 conservando registros. Revisión visual de Estadísticas: filtros, fechas, ganancia semaforizada y métricas visibles sin desplazamiento. Captura `app/build/mitaxi-v04.png`. No se insertaron datos de prueba en la app diaria. Continúan pendientes teléfono físico y API 26; gráficos corresponden a V0.7.

---

# Historial: validación de V0.3.1 — Ganancia y gastos rápidos

29/09/2026: compilación, Lint y 24 pruebas aprobadas (9 JVM y 15 instrumentadas). Lint sin incidencias. Prueba nueva: registro directo de combustible 50/100 y lavado 10, doble toque sin duplicar, ausencia de formulario y estados de ganancia positiva/cero/negativa al cambiar gastos. La prueba del repositorio verifica inclusión de lavado en gastos totales, desglose de combustible y exclusión de otros días.

Instalada 0.3.1 sobre la app del emulador Android 17 conservando datos. Revisión visual: resumen completo y ganancia roja visibles sin desplazar Inicio, controles rápidos de Gastos visibles. Capturas `app/build/mitaxi-v031.png` y `app/build/mitaxi-expenses-v031.png`. Los datos de prueba se mantienen en QA; no se añadieron registros a la app diaria. Se reservó espacio para que el aviso de servicio guardado no tape controles. Teléfono físico y API 26 pendientes.

---

# Historial: validación de V0.3 — Gastos

29/09/2026: `assembleDebug`, `testQaUnitTest`, `lintDebug` y `connectedQaAndroidTest` terminaron con `BUILD SUCCESSFUL`. **23 pruebas aprobadas**: 9 JVM y 14 instrumentadas; Lint sin incidencias.

Cobertura nueva: persistencia de las cinco categorías, orden del historial, edición que conserva createdAt, eliminación persistente al reabrir, rechazo de montos inválidos y registros inexistentes. UI: categoría → importe, validación de monto/fecha, cambio de categoría, actualización del combustible en Inicio, cancelación/confirmación de eliminación y edición del combustible previamente guardado desde Inicio. Las pruebas continúan aisladas en el paquete QA.

Versión 0.3.0 instalada como actualización en el emulador Android 17. Revisión visual de Inicio y Gastos: categorías y total visibles, servicio previo conservado. Captura en `app/build/mitaxi-v03.png`. No se añadieron registros de prueba a la app diaria.

Room permanece en versión 2: no cambian tablas ni se borran datos al actualizar. Filtros por período, ganancia neta y estadísticas completas corresponden a V0.4 y etapas posteriores. Validación en teléfono físico y API 26 aún pendiente.

---

# Historial: validación de V0.2.2 — Resumen diario

29/09/2026: `assembleDebug`, `testQaUnitTest`, `lintDebug` y `connectedQaAndroidTest` terminaron con `BUILD SUCCESSFUL`. **19 pruebas aprobadas**: 9 JVM y 10 instrumentadas. Lint sin incidencias.

Cobertura nueva: día local de Lima, medianoche/cambio de año, días de 23/25 horas, intervalo con inicio incluido y fin excluido, exclusión de otros días/categorías, actualización al cambiar el día observado, sumas sin desbordamiento, registro/Deshacer de combustible, migración desde el esquema JSON v1 preservando servicios y persistencia del combustible al reabrir. La prueba de UI verifica el resumen reactivo y la exclusión de un servicio de ayer.

Versión 0.2.2 instalada con actualización sobre la app existente del emulador Android 17. Inicio abrió correctamente y mostró el servicio previo conservado. Tarjeta completa visible sin desplazar la pantalla en Medium Phone: servicios, monto y combustible, con botón Añadir combustible. Captura en `app/build/mitaxi-v022.png`. No se insertaron datos de prueba en la base diaria.

Limitaciones: el módulo completo de Gastos (historial/edición y otras categorías) sigue pendiente; el combustible añadido puede deshacerse desde la tarjeta durante esa sesión. Pruebas de límites horarios deterministas; no se esperó hasta la medianoche real. Sigue pendiente validación en teléfono físico y API 26.

---

# Historial: validación de V0.2.1 — Usabilidad

29/09/2026: `assembleDebug`, `testQaUnitTest`, `lintDebug` y `connectedQaAndroidTest` completados con `BUILD SUCCESSFUL`. **12 pruebas aprobadas**: 6 JVM y 6 instrumentadas (2 de repositorio y 4 de interfaz). Se verificaron registro de un toque desde Inicio, doble toque accidental sin duplicados, Deshacer sin afectar registros anteriores, Otro monto sin campos secundarios y CRUD con opciones avanzadas. Lint sin incidencias.

APK 0.2.1 actualizado en el emulador con `adb install -r`. Inicio verificado visualmente: cuatro botones de tarifas y Otro monto. Captura en `app/build/mitaxi-v021.png`. No se añadieron servicios de prueba a la base diaria.

Gastos todavía no está implementado: se actualizó su especificación a categoría + monto, sin notas.

---

# Historial: validación de V0.2 — Servicios

Fecha: 29 de septiembre de 2026. Emulador Medium Phone, Android 17 / API 37 (`emulator-5554`).

**Resultado final:** `BUILD SUCCESSFUL`; APK debug generado, Lint sin incidencias, 6 pruebas JVM y 3 instrumentadas aprobadas. APK 0.2.0 instalado y MainActivity abierto correctamente en el emulador. Se comprobó que Inicio abre Room y que Servicios muestra su estado vacío, total y botón de registro; captura local en `app/build/mitaxi-v02.png`.

Comando de verificación:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testQaUnitTest :app:lintDebug :app:connectedQaAndroidTest
```

Cobertura:

- **6 pruebas JVM:** céntimos exactos, coma/punto decimal, importes inválidos, límite Long sin desbordamiento, zona horaria, fechas imposibles, año bisiesto y salto horario de verano.
- **2 pruebas de repositorio en Android:** inserción, orden, cierre/reapertura de una base en disco, edición, eliminación persistente, validación del repositorio y conservación de createdAt/fareId.
- **1 prueba de UI en Android:** rechazo de monto cero, registro con nota, edición de monto/nota/fecha/hora, cancelación de eliminación, confirmación y regreso al estado vacío.

Las pruebas instrumentadas utilizan el build type `qa`, con applicationId `pe.mitaxi.app.qa`, y bases exclusivas (en memoria o archivo de nombre único). Así la instalación/desinstalación automática de Gradle no afecta a la app diaria `pe.mitaxi.app` ni a sus datos.

El esquema de Room permanece en versión 1. No se usan migraciones destructivas. Se fijó Espresso 3.7.0 para evitar una dependencia transitiva anterior incompatible con la API de entrada de Android 17; las pruebas Compose utilizan API v2. Referencia: [notas oficiales de AndroidX Test](https://developer.android.com/jetpack/androidx/releases/test).

## Límites de V0.2

- Falta ejecutar en el teléfono físico y en API 26.
- La persistencia automatizada cubre cerrar/reabrir Room sobre disco, sin simular pérdida de alimentación ni terminación del proceso entre operaciones.
- El formulario usa SavedStateHandle; queda pendiente una comprobación específica tras muerte del proceso, accesibilidad con fuente grande y ambos temas.
- Filtros por período, Gastos, tarifas rápidas, widget y resumen financiero pertenecen a siguientes etapas.

---

# Historial: validación de V0.1

Fecha: 29 de septiembre de 2026.

## Entorno

- Windows, Android Studio 2026.1.4.
- JBR 25 incluido en Android Studio, bytecode objetivo Java 17.
- Android SDK 37 y Build Tools 36.0.0.

## Verificaciones

- Gradle Wrapper JAR cotejado con SHA-256 oficial; distribución fijada con checksum en wrapper properties.
- `:app:assembleDebug`: APK generado correctamente.
- KSP procesó Room y exportó `app/schemas/pe.mitaxi.app.data.local.database.TaxiDatabase/1.json`.
- Permisos del APK inspeccionados con `aapt dump permissions`: sin Internet, ubicación ni almacenamiento. Solo permiso interno de AndroidX para receptores no exportados.
- `:app:lintDebug`: aprobado, sin incidencias. La ejecución conjunta final de assembleDebug y lintDebug terminó con `BUILD SUCCESSFUL`.

Se corrigieron durante la validación el escape de la ruta Windows en local.properties y la declaración de backup para Android anteriores a 12. Gradle emitió un aviso informativo de que dos bibliotecas nativas se empaquetan con sus símbolos; no impide generar el APK debug.

No se han ejecutado pruebas de lógica de negocio: todavía no está implementada. No se declara probada la ejecución de Room en dispositivo por el solo hecho de compilar.

## Prueba manual pendiente

`adb devices` no muestra dispositivos conectados y `emulator -list-avds` no muestra emuladores configurados.

Cuando esté disponible un dispositivo:

1. Ejecutar `app` desde Android Studio o instalar el APK debug.
2. Abrir Inicio: debe pasar de «Preparando almacenamiento…» a «Base local preparada».
3. Recorrer las cinco pestañas y comprobar que volver a Inicio no duplica destinos.
4. Girar el dispositivo, cambiar tema y aumentar tamaño de fuente; comprobar legibilidad y navegación.
5. Cerrar y abrir sin conexión: debe seguir mostrando la base preparada.

El registro y la conservación de servicios se probarán en V0.2, cuando exista CRUD.
