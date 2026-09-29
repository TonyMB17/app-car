# Decisiones técnicas iniciales

Fecha de revisión: 29 de septiembre de 2026.

## Widget — V0.6

AppWidgetProvider + RemoteViews nativos, sin nuevas dependencias. Cuatro tarifas por página, paginación, Otro monto en Activity dedicada y Deshacer del último servicio del widget. PendingIntents explícitos e inmutables; receiver y Activity no exportados. Operaciones asíncronas con goAsync, tiempo acotado y serialización. Se comprueba que el ID corresponda a un widget activo y se rechazan acciones con el teléfono bloqueado. Solo pantalla principal, sin eludir el bloqueo.

Room v4 añade widget_state por instancia: token, página y último registro. Consumo del token, inserción del servicio y renovación del token ocurren en una misma transacción. Repetir el PendingIntent o reiniciar el proceso no duplica la operación. Bloqueo adicional de 600 ms; tarifas eliminadas o con otro importe son rechazadas. Deshacer comprueba que el registro no haya sido editado. Eliminar un widget borra solo su estado, nunca servicios.

Observación de tarifas actualiza widgets sin sondeo periódico. Recepción de actualización y acciones arranca el proceso cuando Android lo permite. Recursos específicos API 31 para previsualización y dimensiones por celdas; minWidth/minHeight para API 26–30. Referencia: [documentación Android de widgets](https://developer.android.com/develop/ui/views/appwidgets/advanced).

## Tarifas configurables — V0.5

Room v3 añade `fares` con ID estable, importe en céntimos y posición. El importe es también la etiqueta: no se pide un nombre para conservar el flujo simple. Migración 2→3 y callback de creación inicializan 5/6/7/10 una sola vez. Borrar todas no restablece los valores al reiniciar. Orden, altas, cambios y bajas se serializan con transacciones. La validación del repositorio impide importes no positivos o repetidos.

Al registrar desde Inicio se comprueba dentro de la transacción que la tarifa siga existiendo y que su monto coincida con el mostrado. El servicio almacena su propio importe y `fareId`; no hay borrado en cascada ni recálculo del historial. ID de origen sin clave foránea para preservar esa referencia tras eliminar la tarifa. Cambios en tarifas se observan con Flow; se mantienen Deshacer y protección contra doble toque. DataStore no se usa para duplicar estas tarifas.

## Resumen por período — V0.4

Estadísticas aloja el resumen por período para mantener Inicio centrado en registrar. Hoy, semana calendario de lunes a domingo, mes calendario y todo el historial. Los límites se calculan con java.time en la zona del dispositivo, inicio incluido y siguiente inicio excluido; nunca se asumen días de 24 horas. Consultas reactivas, sumas exactas BigDecimal, actualización del rango al vencer y comprobación del reloj/zona como máximo cada minuto. Se reevalúa al reanudar la pantalla. Selección en SavedStateHandle; al cambiar período se ocultan valores anteriores durante la carga.

Se comparte ProfitCard con Inicio para mantener colores y etiquetas coherentes. La base Room sigue en v2. No se incluyen gráficos ni comparaciones históricas de V0.7.

## Ganancia diaria y gastos rápidos — V0.3.1

La ganancia usa todos los gastos del mismo día local, con sumas y resta BigDecimal. Combustible es un desglose de esos gastos, no una segunda deducción. Una consulta diaria de gastos alimenta ambas cifras para evitar desajustes entre emisiones. Color y etiqueta dependen del signo de la ganancia; no hay porcentajes ni umbrales arbitrarios. Los colores usan pares de fondo/texto con contraste y conservan su significado en ambos temas.

Importes rápidos visibles en Gastos: combustible 5000/10000 céntimos y lavado 1000. Comparten validación y persistencia con Otro; no abren teclado al guardar. Bloqueo durante escritura y 600 ms adicionales evita doble toque accidental. Room permanece en v2.

## Versiones fijadas

| Componente | Versión |
| --- | --- |
| Android Gradle Plugin | 9.4.1 |
| Gradle Wrapper | 9.6.0 |
| Kotlin / compilador Compose | 2.4.20 |
| Compose BOM | 2026.09.00 |
| Material 3 | administrado por Compose BOM |
| Room | 2.8.5 |
| KSP | 2.3.12 |
| Lifecycle / ViewModel | 2.11.0 |
| Activity Compose | 1.13.0 |
| Navigation Compose | 2.10.2 |
| DataStore Preferences | 1.2.1 |
| compileSdk / targetSdk | 37 (Android 17) |
| minSdk | 26 (Android 8.0) |
| Bytecode Java / Kotlin | 17 |

AGP 9 integra Kotlin: no se aplica `org.jetbrains.kotlin.android`. El plugin del compilador Compose se fija en el catálogo. Gradle se ejecuta con el JBR de Android Studio (JDK 25 en esta máquina); el bytecode de la app apunta a Java 17.

Referencias oficiales consultadas y metadatos de artefactos:

- [Compatibilidad AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes): Gradle 9.6.0, SDK máximo 37, Build Tools 36.0.0.
- [Versiones publicadas de AGP](https://dl.google.com/dl/android/maven2/com/android/tools/build/gradle/maven-metadata.xml).
- [Kotlin publicado](https://repo.maven.apache.org/maven2/org/jetbrains/kotlin/kotlin-gradle-plugin/maven-metadata.xml).
- [Compose BOM](https://developer.android.com/develop/ui/compose/bom) y [versiones publicadas](https://dl.google.com/dl/android/maven2/androidx/compose/compose-bom/maven-metadata.xml).
- [Room estable](https://developer.android.com/jetpack/androidx/releases/room).

## Alcance y arquitectura

Un único módulo app. Dependencias creadas manualmente en Application, Room compartido por proceso. Compose → ViewModel → Repository → Room. No se agrega framework de inyección ni capas sin responsabilidad concreta.

La etapa inicial solo comprueba apertura de almacenamiento; no calcula ingresos ni genera datos. La entidad de servicios permite verificar KSP y exportación de esquema desde el primer build. Gastos y tarifas se incorporarán mediante migraciones explícitas.

En V0.2 se añadió el repositorio de servicios con Flow, validación de entradas y ViewModel con SavedStateHandle para el formulario. El esquema de Room sigue en versión 1: las operaciones CRUD no cambian tablas ni columnas. Las ediciones actualizan solo monto, nota, fecha del servicio y updatedAt; preservan createdAt y fareId. Los totales usan BigDecimal para no desbordar al sumar importes. Las inserciones toman un único instante del reloj para fecha del servicio y fechas de auditoría.

Las pruebas utilizan el build type por defecto `debug` (`pe.mitaxi.app`) con las tareas estándar `testDebugUnitTest` y `connectedDebugAndroidTest`. Se mantiene la variante `qa` (`pe.mitaxi.app.qa`) en el build script por si se requiere aislar instalaciones en dispositivo.

V0.2.1 adelanta el registro rápido a Inicio con importes iniciales 500/600/700/1000 céntimos. El ViewModel guarda mediante el mismo repositorio, bloquea pulsaciones rápidas durante 600 ms y ofrece Deshacer sobre el ID exacto recién creado. Los botones no son todavía configurables. Otro monto solo pide importe, con foco y teclado numérico; las notas existentes se preservan en edición avanzada. La configuración de tarifas sigue en V0.5. No cambió el esquema ni se borraron notas históricas. Gastos se diseñará sin nota por instrucción expresa del usuario.

## Ajustes al modelo conceptual del README

V0.3 completa Gastos con ExpenseCategory (FUEL, MAINTENANCE, REPAIR, WASH, OTHER), ExpenseRepository y ExpensesViewModel. Reutiliza expenses v2 sin nueva migración. El alta de combustible de Inicio delega al mismo repositorio de gastos. Actualizar conserva createdAt y cambia updatedAt; la consulta de combustible del día reacciona a importe, categoría, fecha y borrado. Deshacer de Inicio se retira al salir de esa pantalla, evitando borrar posteriormente un gasto editado desde Gastos. Las correcciones posteriores se realizan desde el historial con confirmación de eliminación.

V0.2.2 incorpora `DailySummaryRepository` y un ViewModel del resumen. Consulta importes del intervalo `[inicioDelDía, inicioDelSiguienteDía)` con índices de Room; suma en BigDecimal y filtra combustible por categoría estable `FUEL`. Los límites usan ZoneId.systemDefault y días calendario, incluidos días de 23/25 horas. La observación se detiene al salir de primer plano, se reinicia al volver y despierta a medianoche; revisa cambios de reloj/zona al menos cada minuto mientras está visible.

Room versión 2 añade `expenses` (categoría, importe, fecha y auditoría, sin notas), índice por categoría/fecha y migración explícita 1→2. El combustible se guarda como gasto normal reutilizable en V0.3; Deshacer elimina únicamente el ID recién creado. La migración se prueba construyendo una base con el esquema JSON histórico versión 1 y conservando un servicio existente.

- **Dinero:** `amountCents: Long` en lugar de `Double`, para evitar errores de punto flotante. S/ 5.00 se almacenará como 500. Los formularios deberán validar importes positivos y como máximo dos decimales sin pasar por Double.
- **Tiempo:** `occurredAt`, `createdAt` y `updatedAt` como epoch milliseconds. Presentación y filtros en la zona horaria del dispositivo. La implementación futura usará límites `[inicio, siguienteInicio)`; semana de lunes a domingo y mes calendario. Se recalcularán al cambiar día o zona.
- **Tarifas:** desde V0.5 se guardan en Room v3; `fareId` sigue siendo opcional para servicios con Otro monto y registros históricos.
- **Compatibilidad:** API 26 como mínimo simplifica java.time. Es una decisión inicial revisable antes de ampliar la V1 si el teléfono objetivo requiere una versión anterior.
- **Identidad:** nombre Mi Taxi, applicationId provisional `pe.mitaxi.app`. Cambiarlo antes de instalar una versión con datos reales si se desea otro identificador.
- **Privacidad:** no permisos de red, no servicios externos, sin backup automático en nube ni transferencia automática. La copia manual permanece fuera de V0.1.

## Widget futuro

V0.6 usa RemoteViews nativo, suficiente para cuatro botones por página y sin dependencias adicionales. Comparte Room y abre una Activity solo para Otro monto. En Android 15 y superiores, Forzar detención cancela PendingIntents y deshabilita temporalmente widgets; se recuperan al volver a abrir la app. No se intenta eludir esa decisión del sistema. [Restricciones oficiales del estado detenido](https://developer.android.com/about/versions/15/behavior-changes-all#stopped-state).
