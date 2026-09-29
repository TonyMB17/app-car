# 🚕 App de Registro de Servicios de Taxi

Aplicación Android personal para registrar de forma rápida servicios de taxi, gastos y consultar la rentabilidad del trabajo por día, semana, mes y acumulado total.

La aplicación debe estar enfocada en **velocidad, simplicidad y funcionamiento offline**.

El objetivo principal es que registrar un servicio tome **uno o dos toques**, incluso sin necesidad de abrir la aplicación mediante un **widget de acceso rápido en la pantalla de inicio de Android**.

---

## 1. Objetivo del proyecto

Desarrollar una aplicación Android sencilla y funcional que permita:

- Registrar servicios de taxi rápidamente.
- Tener tarifas preconfiguradas para registrar servicios con un solo toque.
- Registrar servicios desde un widget de Android.
- Registrar gastos como combustible, mantenimiento y otros.
- Consultar ingresos, gastos y ganancias.
- Consultar estadísticas por:
  - Día
  - Semana
  - Mes
  - Total acumulado
- Funcionar completamente sin conexión a Internet.
- Almacenar todos los datos localmente en el teléfono.
- Mantener una interfaz sencilla y rápida de utilizar durante el trabajo.

La aplicación es inicialmente de **uso personal**, por lo que no requiere:

- Sistema de usuarios.
- Login.
- Servidor.
- API.
- Base de datos remota.
- Hosting.
- Internet para su funcionamiento normal.

---

# 2. Stack tecnológico

## Plataforma

- Android
- Lenguaje: Kotlin
- IDE: Android Studio

## Interfaz

- Jetpack Compose
- Material 3

## Arquitectura

- MVVM
- Repository Pattern
- ViewModel
- Kotlin Coroutines / Flow

## Persistencia

- Room
- SQLite

## Preferencias

- DataStore Preferences

## Widget

- Android App Widget
- Utilizar las APIs modernas recomendadas por Android.
- El widget debe permitir registrar tarifas rápidas sin necesidad de abrir la aplicación.

## Control de versiones

- Git
- GitHub

---

# 3. Principios del proyecto

La aplicación debe seguir estos principios:

### Simplicidad

No agregar funcionalidades que no sean necesarias para la primera versión.

### Rapidez

Registrar un servicio debe requerir el mínimo número de acciones posible.

### Offline First

Toda la funcionalidad principal debe funcionar sin Internet.

### Datos locales

Los registros deben almacenarse en Room/SQLite.

### Interfaz limpia

Utilizar Material 3 y Jetpack Compose.

### Fácil mantenimiento

Mantener una arquitectura clara y modular, evitando sobreingeniería.

### Preparada para crecer

Aunque la V1 sea pequeña, la arquitectura debe permitir posteriormente agregar:

- Exportación Excel/CSV.
- Copias de seguridad.
- Gráficos.
- Estadísticas avanzadas.
- Kilometraje.
- Consumo de combustible.
- Objetivos de ingresos.
- Sincronización en la nube.

Estas funciones NO deben implementarse en la V1 salvo que sean necesarias para la arquitectura.

---

# 4. Funcionalidades de la V1

La primera versión debe contener cinco áreas principales:

```text
Inicio / Resumen
Servicios
Gastos
Estadísticas
Configuración
```

---

# 5. Pantalla Inicio / Resumen

La pantalla principal debe mostrar un resumen del período seleccionado.

Por defecto debe mostrar el día actual.

**Ajuste de usabilidad:** debajo de los botones rápidos de Inicio, mostrar un resumen compacto del día en curso con servicios totales, monto total de servicios y gasto en combustible. Actualizar al registrar, editar o deshacer y al cambiar el día local. No mezclarlo con el acumulado histórico. Se permite añadir combustible desde este resumen introduciendo únicamente el importe.

**Ganancia a simple vista:** destacar ingresos por servicios menos todos los gastos del día. Verde si la ganancia es positiva, rojo si es negativa y amarillo si es cero; acompañar siempre de texto. Mantener gastos totales y combustible visibles como desglose. En Gastos, ofrecer registro de un toque para combustible S/ 50 y S/ 100, y lavado S/ 10; cada categoría tendrá Otro para introducir un importe diferente.

Ejemplo:

```text
--------------------------------
          🚕 MI TAXI
--------------------------------

HOY

Ingresos              S/ 120.00
Gastos                 S/ 35.00
--------------------------------
Ganancia               S/ 85.00

Servicios                    16

--------------------------------
[ Día ] [ Semana ] [ Mes ]
--------------------------------
```

Debe existir un selector de período:

```text
Día
Semana
Mes
Total
```

El resumen debe calcular:

- Total de ingresos.
- Total de gastos.
- Ganancia neta.
- Cantidad de servicios.

Fórmula:

```text
Ganancia = Ingresos - Gastos
```

---

# 6. Registro de servicios

Cada servicio debe almacenar como mínimo:

```text
ID
Fecha
Hora
Monto
Tarifa / tipo de servicio
Nota opcional
Fecha de creación
```

La fecha y hora deben generarse automáticamente al registrar el servicio.

Ejemplo:

```text
29/09/2026
07:32
Servicio
S/ 5.00
```

---

# 7. Tarifas rápidas

El usuario debe poder configurar varias tarifas rápidas.

**Ajuste de usabilidad V0.5:** mantener como valores iniciales los ya usados en Inicio (S/ 5, 6, 7 y 10). Configuración por importe, sin pedir nombre; admite decimales. Cambiar el orden con Subir/Bajar y confirmar la eliminación. Conservar siempre los montos históricos de servicios. Si se eliminan todas, mantener Otro monto y no recrear automáticamente las tarifas iniciales.

Por defecto:

```text
S/ 5
S/ 7
S/ 10
S/ 15
S/ 20
```

Estas tarifas deben ser configurables desde:

```text
Configuración → Tarifas rápidas
```

El usuario debe poder:

- Agregar tarifa.
- Editar tarifa.
- Eliminar tarifa.
- Cambiar el orden.

La pantalla de registro puede mostrar:

```text
┌────────┐ ┌────────┐ ┌────────┐
│ S/ 5   │ │ S/ 7   │ │ S/ 10  │
└────────┘ └────────┘ └────────┘

┌────────┐ ┌────────┐ ┌────────┐
│ S/ 15  │ │ S/ 20  │ │ OTRO   │
└────────┘ └────────┘ └────────┘
```

Al tocar una tarifa:

1. Registrar inmediatamente el servicio.
2. Utilizar fecha y hora actuales.
3. Guardar en Room.
4. Actualizar inmediatamente el resumen.

No mostrar formularios innecesarios para las tarifas rápidas.

---

# 8. Registrar otro monto

Debe existir una opción:

```text
OTRO
```

Al seleccionarla, mostrar un pequeño diálogo:

```text
Monto

S/ [ ______ ]

[Cancelar] [Registrar]
```

Esto permitirá registrar cualquier monto que no esté configurado como tarifa rápida.

---

# 9. Widget de Android

El widget es una de las funcionalidades más importantes de la aplicación.

Debe permitir registrar servicios desde la pantalla de inicio del teléfono.

Ejemplo conceptual:

```text
┌─────────────────────────────┐
│ 🚕 REGISTRAR SERVICIO       │
│                             │
│ [ S/ 5 ] [ S/ 7 ] [ S/ 10 ] │
│                             │
│ [S/ 15 ] [S/ 20 ] [ OTRO ]  │
└─────────────────────────────┘
```

Al tocar:

```text
S/ 10
```

debe registrarse inmediatamente:

```text
Fecha: fecha actual
Hora: hora actual
Monto: 10.00
```

El usuario no debería tener que abrir la aplicación.

### Requisitos del widget

- Debe funcionar con la aplicación cerrada.
- Debe registrar directamente en Room.
- Debe utilizar las tarifas configuradas por el usuario.
- Debe actualizarse cuando cambien las tarifas.
- Debe ser compacto.
- Debe utilizar Material Design cuando sea aplicable.
- Debe manejar correctamente errores.

Si alguna limitación de Android impide realizar directamente alguna acción desde el widget, utilizar la alternativa recomendada por las APIs actuales de Android.

---

# 10. Registro de gastos

La aplicación debe permitir registrar gastos.

**Prioridad de usabilidad (29/09/2026):** registrar únicamente categoría y monto, sin notas. Fecha y hora automáticas. Elegir categoría mediante botones grandes; evitar formularios adicionales. Uso previsto con el vehículo detenido.

Categorías iniciales:

```text
⛽ Combustible
🔧 Mantenimiento
🛞 Reparación
🧼 Lavado
🧾 Otros
```

Cada gasto debe almacenar:

```text
ID
Fecha
Hora
Categoría
Monto
Fecha de creación
```

Ejemplo:

```text
29/09/2026

⛽ Combustible
S/ 30.00
```

---

# 11. Pantalla de gastos

Debe mostrar una lista de gastos recientes.

Ejemplo:

```text
GASTOS

29 SEP

⛽ Combustible
S/ 30.00

🧼 Lavado
S/ 10.00

28 SEP

⛽ Combustible
S/ 25.00
```

Debe permitir:

- Agregar gasto.
- Editar gasto.
- Eliminar gasto.
- Filtrar por período.
- Ver total de gastos.

---

# 12. Pantalla de servicios

Debe mostrar los servicios registrados.

Ejemplo:

```text
SERVICIOS

29 SEP

07:32     S/ 5.00
08:15     S/ 7.00
09:02     S/ 10.00
09:45     S/ 5.00
10:21     S/ 15.00
```

Debe permitir:

- Ver servicios.
- Editar servicio.
- Eliminar servicio.
- Ver total.
- Filtrar por período.

---

# 13. Estadísticas

La pantalla de estadísticas debe mostrar información resumida.

Primera versión:

```text
ESTADÍSTICAS

Ingresos
S/ 2,850

Gastos
S/ 780

Ganancia
S/ 2,070

Servicios
342
```

Agregar selector:

```text
Día
Semana
Mes
Total
```

Opcionalmente se pueden utilizar gráficos sencillos.

No es necesario implementar gráficos complejos en la primera iteración.

---

# 14. Métricas futuras

La arquitectura debe permitir agregar posteriormente:

### Ganancia por hora

```text
Ganancia total / horas trabajadas
```

Ejemplo:

```text
Ganancia: S/ 110
Tiempo trabajado: 8h 20m

Ganancia por hora:
S/ 13.20
```

### Ganancia por kilómetro

Posteriormente se podría registrar:

```text
Kilómetros recorridos
Combustible utilizado
```

Y calcular:

```text
Ingresos/km
Gastos/km
Ganancia/km
```

Estas funciones son futuras y NO forman parte obligatoria de la V1.

---

# 15. Modelo de datos

Utilizar Room.

## Tabla: servicios

```text
Servicio
---------
id: Long
fechaHora: LocalDateTime
monto: Double
tarifaId: Long?
nota: String?
createdAt: Long
updatedAt: Long
```

## Tabla: gastos

```text
Gasto
---------
id: Long
fechaHora: LocalDateTime
categoria: String
monto: Double
createdAt: Long
updatedAt: Long
```

## Tabla: tarifas

```text
Tarifa
---------
id: Long
nombre: String
monto: Double
orden: Int
activo: Boolean
```

## Tabla/configuración

Utilizar DataStore para configuraciones simples de la aplicación.

Ejemplos:

```text
moneda
tema
primera ejecución
```

No utilizar una tabla SQL para configuraciones simples si DataStore es suficiente.

---

# 16. Arquitectura

Utilizar una arquitectura sencilla:

```text
app
│
├── data
│   ├── local
│   │   ├── database
│   │   ├── dao
│   │   └── entity
│   │
│   └── repository
│
├── domain
│   └── model
│
├── ui
│   ├── home
│   ├── services
│   ├── expenses
│   ├── statistics
│   └── settings
│
├── widget
│
└── utils
```

No crear capas innecesarias.

---

# 17. Navegación

La aplicación debe utilizar navegación moderna de Jetpack Compose.

Navegación principal:

```text
Inicio
Servicios
Gastos
Estadísticas
Configuración
```

Se recomienda una barra de navegación inferior:

```text
┌────────────────────────────────────┐
│                                    │
│            CONTENIDO               │
│                                    │
├────────────────────────────────────┤
│ 🏠     🚕      💸      📊     ⚙️   │
│Inicio Servicios Gastos Estad. Conf │
└────────────────────────────────────┘
```

---

# 18. Diseño UI/UX

La interfaz debe priorizar:

- Botones grandes.
- Texto legible.
- Pocos elementos por pantalla.
- Acciones rápidas.
- Buena visualización de montos.
- Navegación sencilla.
- Diseño Material 3.
- Soporte para modo claro y oscuro si es sencillo de implementar.

Los montos deben mostrarse claramente:

```text
S/ 120.00
```

No utilizar interfaces sobrecargadas.

---

# 19. Confirmaciones

Para acciones normales como registrar un servicio desde una tarifa rápida:

```text
NO pedir confirmación.
```

El registro debe ser inmediato.

Para eliminar información:

```text
¿Eliminar este registro?

[Cancelar] [Eliminar]
```

Debe solicitar confirmación.

---

# 20. Manejo de errores

La aplicación debe manejar errores de manera amigable.

Ejemplo:

```text
No se pudo registrar el servicio.
Inténtalo nuevamente.
```

No mostrar excepciones técnicas al usuario.

Los errores deben registrarse mediante logs durante desarrollo.

---

# 21. Persistencia y funcionamiento offline

La aplicación debe funcionar sin Internet.

Toda la información principal debe almacenarse localmente:

```text
Android
  ↓
Room
  ↓
SQLite
```

No realizar llamadas de red para las funcionalidades principales.

---

# 22. Seguridad y privacidad

Los datos son personales y deben mantenerse localmente.

No enviar información a servidores externos.

No solicitar permisos innecesarios.

En particular:

- No solicitar ubicación en la V1.
- No solicitar contactos.
- No solicitar llamadas.
- No solicitar SMS.
- No solicitar almacenamiento externo si no es necesario.

---

# 23. Exportación

La exportación NO es obligatoria para la primera versión.

Sin embargo, la arquitectura debe permitir posteriormente:

```text
Exportar CSV
Exportar Excel
Generar PDF
Crear copia de seguridad
```

---

# 24. Backup futuro

En una versión futura se podría agregar:

```text
💾 Copia de seguridad
```

Por ejemplo:

```text
Exportar datos
Importar datos
```

Esto permitiría recuperar los registros si se cambia de teléfono.

No implementar en la V1 salvo que sea necesario para validar la arquitectura.

---

# 25. Requisitos mínimos de la V1

La V1 se considera terminada cuando permita:

- [ ] Instalar la aplicación en Android.
- [ ] Abrir la aplicación correctamente.
- [ ] Registrar un servicio.
- [ ] Registrar un servicio mediante tarifa rápida.
- [ ] Registrar un monto personalizado.
- [ ] Editar un servicio.
- [ ] Eliminar un servicio.
- [ ] Registrar un gasto.
- [ ] Editar un gasto.
- [ ] Eliminar un gasto.
- [ ] Ver ingresos.
- [ ] Ver gastos.
- [ ] Ver ganancias.
- [ ] Ver cantidad de servicios.
- [ ] Filtrar por día.
- [ ] Filtrar por semana.
- [ ] Filtrar por mes.
- [ ] Ver total acumulado.
- [ ] Configurar tarifas rápidas.
- [ ] Utilizar el widget.
- [ ] Registrar servicios desde el widget.
- [ ] Funcionar sin Internet.
- [ ] Mantener los datos después de cerrar/reabrir la aplicación.

---

# 26. Fuera del alcance de la V1

No implementar inicialmente:

- Login.
- Registro de usuarios.
- Backend.
- API.
- Servidor.
- Base de datos remota.
- Firebase.
- Google Maps.
- GPS.
- Seguimiento de rutas.
- Sistema de clientes.
- Sistema de conductores.
- Pagos electrónicos.
- Integración con bancos.
- Publicación en Google Play.
- Publicidad.
- Suscripciones.

Estas funcionalidades podrían evaluarse posteriormente.

---

# 27. Roadmap

## V0.1 — Base del proyecto

- Crear proyecto Android.
- Configurar Kotlin.
- Configurar Jetpack Compose.
- Configurar Material 3.
- Configurar Room.
- Crear estructura de paquetes.
- Crear navegación básica.

## V0.2 — Servicios

- Entidad Servicio.
- DAO.
- Repository.
- ViewModel.
- Registro.
- Edición.
- Eliminación.
- Lista de servicios.

## V0.3 — Gastos

- Entidad Gasto.
- Categorías.
- DAO.
- Repository.
- ViewModel.
- Registro.
- Edición.
- Eliminación.
- Lista de gastos.

## V0.4 — Resumen

- Ingresos.
- Gastos.
- Ganancia.
- Cantidad de servicios.
- Filtros por período.

## V0.5 — Tarifas rápidas

- Configuración de tarifas.
- Botones rápidos.
- Registro instantáneo.

## V0.6 — Widget

- Crear widget.
- Mostrar tarifas.
- Registrar servicio directamente.
- Actualización de tarifas.
- Pruebas con aplicación cerrada.

## V0.7 — Estadísticas

- Resumen mensual.
- Resumen semanal.
- Resumen diario.
- Total acumulado.
- Gráficos sencillos si aportan valor.

## V1.0 — Primera versión funcional

- Corrección de errores.
- Mejoras visuales.
- Pruebas.
- Optimización.
- Instalación como APK.
- Documentación.

---

# 28. Reglas para Codex

Al desarrollar este proyecto:

1. No implementar funcionalidades fuera del alcance sin solicitar autorización.
2. Priorizar una solución sencilla sobre una solución excesivamente compleja.
3. Utilizar las APIs modernas y recomendadas de Android.
4. Evitar dependencias innecesarias.
5. Mantener el proyecto compilable después de cada etapa.
6. No introducir backend ni servicios externos.
7. No agregar autenticación.
8. No agregar funcionalidades que requieran Internet.
9. Mantener separación clara entre UI, lógica y persistencia.
10. Utilizar nombres claros y consistentes.
11. Escribir código Kotlin idiomático.
12. Utilizar Coroutines/Flow cuando corresponda.
13. Evitar lógica de negocio directamente dentro de los Composables.
14. Utilizar ViewModel para manejar el estado de las pantallas.
15. Utilizar Room para los datos persistentes.
16. Utilizar DataStore para preferencias simples.
17. Mantener la aplicación preparada para agregar funcionalidades futuras.
18. Antes de realizar cambios importantes en la arquitectura, explicar la propuesta.
19. No reemplazar tecnologías principales sin justificarlo.
20. Después de cada etapa, verificar que el proyecto compile.

---

# 29. Estrategia de desarrollo con Codex

El proyecto debe desarrollarse por etapas.

No intentar construir toda la aplicación en una sola operación.

Orden recomendado:

```text
1. Crear proyecto
       ↓
2. Configurar arquitectura
       ↓
3. Room
       ↓
4. Servicios
       ↓
5. Gastos
       ↓
6. Resumen
       ↓
7. Tarifas rápidas
       ↓
8. Widget
       ↓
9. Estadísticas
       ↓
10. Pruebas y mejoras
```

Después de cada etapa:

```text
Compilar
↓
Ejecutar
↓
Probar
↓
Corregir
↓
Continuar
```

---

# 30. Primera tarea para Codex

Antes de escribir una cantidad significativa de código:

1. Analizar este README.
2. Confirmar que el alcance de la V1 está claro.
3. Proponer la estructura inicial del proyecto.
4. Indicar las versiones actuales recomendadas de:
   - Android Gradle Plugin.
   - Kotlin.
   - Compose.
   - Room.
   - Android SDK.
5. Crear únicamente la base inicial del proyecto.
6. Configurar:
   - Kotlin.
   - Jetpack Compose.
   - Material 3.
   - Navegación.
   - Room.
   - ViewModel.
7. Crear una pantalla inicial funcional.
8. Verificar que el proyecto compile.
9. No implementar todavía el widget ni toda la lógica de negocio.

Una vez validada esta primera etapa, continuar con el módulo de **Servicios**.

---

# 31. Resultado esperado

El resultado final de la V1 debe ser una aplicación Android personal que permita algo tan sencillo como:

```text
Termina un servicio
       ↓
Toca "S/ 10"
       ↓
Servicio registrado
       ↓
Continúa trabajando
```

Y posteriormente, al terminar el día:

```text
Abre la aplicación
       ↓
Resumen
       ↓
Ingresos      S/ 150
Gastos        S/ 40
Ganancia      S/ 110
Servicios     18
```

La aplicación debe sentirse como una **herramienta rápida de trabajo**, no como un sistema administrativo complejo.

---

# 32. Visión futura

La aplicación puede evolucionar posteriormente hacia un pequeño sistema personal de control de rentabilidad:

```text
                    🚕 MI TAXI
                        │
        ┌───────────────┼───────────────┐
        │               │               │
    Servicios        Gastos         Estadísticas
        │               │               │
     Ingresos       Combustible      Ganancia
        │           Mantenimiento    Por hora
        │           Reparaciones     Por km
        │               │               │
        └───────────────┼───────────────┘
                        │
                    Reportes
                        │
               Excel / CSV / PDF
```

La prioridad siempre debe ser:

**Registrar rápido → almacenar correctamente → mostrar información útil.**
