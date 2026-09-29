# Mi Taxi

Aplicación Android personal para registrar servicios y gastos sin conexión.

La especificación está en [README del producto](README.md%20%E2%80%94%20App%20de%20Registro%20de%20Servicios%20de%20Taxi.md). La versión actual es **V0.6.2: registro desde notificaciones**. Inicio combina registro rápido con resumen diario; Servicios y Gastos permiten registrar, consultar, editar y eliminar datos locales.

## Abrir y ejecutar

1. Abrir esta carpeta desde Android Studio: **Open**, seleccionar `app-car`.
2. Usar el JDK incluido con Android Studio en **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK**.
3. Sincronizar Gradle. La primera sincronización requiere Internet para descargar dependencias; la aplicación no requiere Internet.
4. Crear o seleccionar un dispositivo Android 8.0 (API 26) o superior y ejecutar la configuración `app`.

El SDK local se configura en `local.properties`, que no se versiona. Android Studio puede generarlo automáticamente.

En PowerShell, con el Android Studio instalado en su ubicación habitual:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest
# Con emulador o teléfono conectado:
.\gradlew.bat :app:connectedDebugAndroidTest
```

APK de depuración: `app/build/outputs/apk/debug/app-debug.apk`.
Informe Lint: `app/build/reports/lint-results-debug.html`.
En macOS/Linux usar `./gradlew` con JDK compatible y SDK local configurado.

## Base incluida

- Gradle Wrapper con checksum, catálogo de versiones y Kotlin integrado de AGP 9.
- Compose y Material 3, tema claro/oscuro según el sistema.
- Estadísticas: resumen con Hoy, Esta semana, Este mes y Todo; fechas visibles, cantidad de servicios, ingresos, gastos y ganancia semaforizada. Semana de lunes a domingo y mes calendario en la zona del teléfono.
- Navegación inferior: Inicio, Servicios, Gastos, Estadísticas y Ajustes.
- Inicio con botones grandes S/ 5, 6, 7 y 10; escritura directa en Room, sin formularios ni confirmaciones. Aviso con Deshacer y protección ante doble toque accidental.
- Resumen inferior de hoy: ganancia destacada (ingresos menos todos los gastos), servicios, ingresos, gastos y combustible. Verde para ganancia positiva, rojo para pérdida y amarillo para equilibrio; cada color acompañado de texto. Se actualiza automáticamente y utiliza el día local del teléfono.
- Añadir combustible desde el resumen: solo importe, fecha/hora automática y opción Deshacer.
- Gastos: combustible S/ 50, S/ 100 u otro y lavado S/ 10 u otro. Los importes fijos se guardan con un toque y protección ante doble toque; Otro pide solo monto. También Mantenimiento, Reparación y Otros. Historial y total, sin notas. Cualquier gasto, edición o eliminación actualiza la ganancia de Inicio.
- Room con servicios persistentes, DAO reactivo y repositorio local.
- Otro monto: solo importe, teclado numérico y fecha/hora automática. Historial del más reciente al más antiguo y total. Edición de monto y Más opciones para datos secundarios; eliminación con confirmación.
- Formulario conservado al recrear la pantalla y botones bloqueados durante operaciones de guardado.
- Ajustes: tarifas con importe y orden personalizables. Room v4 conserva servicios y gastos; inicializa S/ 5, 6, 7 y 10 una sola vez. Admite decimales y evita duplicados. DataStore disponible para futuras preferencias.
- Sin permiso de Internet; copia automática desactivada. Permiso de notificaciones solicitado al activar el acceso rápido y receptor de reinicio para restaurarlo si sigue activo.

## Estructura

```text
app/src/main/java/pe/mitaxi/app/
├── MainActivity.kt
├── TaxiApplication.kt           # dependencias manuales, ciclo de vida de aplicación
├── data/
│   ├── local/{database,dao,entity,preferences}/
│   └── repository/
├── domain/model/               # servicio y validación exacta de entradas
└── ui/
    ├── TaxiApp.kt               # navegación y pantallas de etapas pendientes
    ├── home/                   # pantalla y ViewModel
    ├── services/               # listado, formulario y ViewModel
    ├── expenses/               # categorías, historial y edición de gastos
    └── theme/
```

Ajustes permite configurar las tarifas. El widget permite registrar desde la pantalla principal del teléfono.

## Usar Servicios

1. Con el vehículo detenido, abrir Inicio y tocar una **tarifa configurada** (inicialmente S/ 5, 6, 7 o 10): queda registrado inmediatamente.
2. Para otro importe: **Otro monto → escribir importe → Registrar** (también desde la tecla Listo del teclado). No se piden notas; fecha/hora automáticas.
3. Usar **Deshacer** en el aviso si se registró por error. Solo revierte ese registro.
4. En **Servicios → Editar**, cambiar el monto. **Más opciones** contiene nota y fecha/hora para correcciones ocasionales.
5. **Eliminar** en el historial pide confirmación.

El total de **Servicios** corresponde a todo el historial; el resumen de **Inicio** incluye exclusivamente el día en curso. La medianoche cambia el período, sin borrar datos. La fila Combustible suma todos los registros de esa categoría del día, sin restarlos del monto de servicios.

Para guardar una carga: **Inicio → Añadir combustible → importe → Registrar**. Usa **Deshacer** si te equivocas. Al salir de Inicio, las correcciones se realizan desde Gastos.

## Usar Gastos

1. Combustible: S/ 50, S/ 100 u Otro; lavado: S/ 10 u Otro. Los importes fijos se guardan con un toque. Otras categorías piden solo el importe. No hay notas.
2. El historial muestra los más recientes primero y el total de todas las categorías.
3. Editar cambia el monto; Más opciones permite cambiar categoría, fecha y hora.
4. Eliminar pide confirmación. Cancelar conserva el gasto.
5. Las cargas guardadas desde Inicio aparecen en el mismo historial; cambiar su importe/categoría/fecha o eliminarlas actualiza el combustible de hoy.

Estadísticas permite consultar Hoy/Semana/Mes/Todo. Ajustes permite añadir, editar, ordenar y eliminar tarifas rápidas. Los cambios se reflejan en Inicio sin alterar servicios anteriores.

## Decisiones y siguientes pasos

### Widget del teléfono

En Ajustes, pulsa **Añadir widget al teléfono** y confirma en el launcher. También está disponible manteniendo pulsada la pantalla principal → Widgets → Mi Taxi. Muestra cuatro tarifas por página, respeta el orden configurado y se actualiza cuando cambian. Tocar un importe registra directamente; Otro monto abre solo el teclado y Deshacer revierte el último servicio de esa instancia si no fue editado.

El widget muestra el importe y hora del último registro y, desde V0.6.1, una franja verde temporal «Guardado: S/ …» por cada guardado efectivo, incluso al repetir importe. También solicita un toast cuando Android lo permite. Funciona al cerrar normalmente la app; Forzar detención es una restricción de Android que requiere volver a abrirla. No registra con el teléfono bloqueado. Pruebas y límites por dispositivo en `docs/VALIDATION.md`.

### Registro desde notificaciones

En Ajustes, activa **Registro rápido en notificaciones** y permite las notificaciones cuando Android lo solicite. Despliega la notificación: muestra cuatro tarifas por página, Otro monto y Deshacer del último servicio registrado desde ella. Los guardados muestran importe, hora con segundos y una franja verde temporal. Usa las mismas tarifas que Inicio y el widget, pero conserva su propio último registro para Deshacer.

Puede desactivarse en Ajustes. Si Android permite retirarla deslizando, se desactiva también la preferencia; vuelve a activar el interruptor para recuperarla. **Volver a mostrar** restaura una notificación activa que no sea visible. Se restaura tras reiniciar o actualizar si sigue activada y tiene permiso. No usa un servicio permanente ni registra con el teléfono bloqueado. Forzar detención requiere abrir la app de nuevo. El diseño y la expansión dependen de Android y del fabricante.

Ver [plan de implementación](docs/PLAN.md) y [versiones y decisiones técnicas](docs/TECHNICAL_DECISIONS.md).

No hay repositorio remoto configurado. El desarrollo puede mantenerse local y conectarse a GitHub cuando se defina el destino.
