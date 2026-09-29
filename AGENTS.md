# Mi Taxi: reglas de trabajo

El documento `README.md — App de Registro de Servicios de Taxi.md` es la especificación del producto. `README.md` explica cómo trabajar con el proyecto y `docs/PLAN.md` delimita cada etapa.

- Android nativo: Kotlin, Compose, Material 3, MVVM, Room, Coroutines/Flow y DataStore.
- Mantener un único módulo app y dependencias mínimas. Inyección manual mientras resulte suficiente.
- Trabajo por etapas: no implementar toda la V1 de una sola vez.
- No añadir backend, autenticación, red, telemetría, GPS, permisos externos ni funciones futuras sin autorización.
- UI sin lógica de negocio. ViewModel expone estado; repositorios acceden a persistencia.
- Dinero en céntimos (`Long`); no usar `Double` para importes. Fechas persistidas como epoch milliseconds, presentadas en la zona del dispositivo.
- No borrar registros automáticamente ni usar migraciones destructivas de Room. Versionar los esquemas en app/schemas.
- Registro rápido sin confirmación; eliminación con confirmación cuando se implemente.
- Recursos de texto en español. No mostrar detalles técnicos de errores al usuario.
- Antes de cambiar arquitectura, explicar la propuesta.
- Después de cada etapa ejecutar `gradlew.bat :app:assembleDebug :app:lintDebug` y pruebas pertinentes. Distinguir compilación de ejecución en dispositivo.
- No versionar local.properties, firmas, cachés, APK ni archivos de Android Studio.

Prioridad explícita del usuario: mínimo tiempo y pulsaciones; uso al estar detenido. Inicio con tarifas de un toque, Otro monto solo importe, fecha/hora automáticas y Deshacer. No solicitar notas en Gastos ni al registrar Servicios. Notas de servicios existentes se conservan y se editan en Más opciones. Gastos: categoría + monto, sin columna de notas.

La fase actual es V0.6: widget nativo con tarifas, paginación, Otro monto y Deshacer; tarifas configurables en Ajustes; resumen por período en Estadísticas (Hoy/Semana/Mes/Todo), conservando Inicio diario. Gastos con categorías, listado, total, edición y eliminación. Combustible ofrece 50/100/Otro y lavado 10/Otro con guardado directo. Inicio destaca ganancia del día (servicios menos todos los gastos): verde positiva, rojo negativa, amarillo cero; siempre con etiqueta textual. Room está en versión 4, con migraciones 1→2→3→4 que conservan servicios y gastos. Tarifas con ID, importe y posición; eliminar o editar no cambia el historial. Iniciales 5/6/7/10 se crean una sola vez. widget_state conserva tokens y últimos registros por instancia. El siguiente hito es V0.7: estadísticas; después validación final.
