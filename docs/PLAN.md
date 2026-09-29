# Plan por etapas

## Resumen de hoy — V0.2.2

Tarjeta compacta debajo de los botones rápidos: servicios totales, monto de servicios y combustible del día local. Datos reactivos; límites desde medianoche incluida hasta la siguiente excluida. Actualización al cambiar el día y al volver a Inicio. Registro mínimo de combustible (solo importe) y Deshacer disponibles desde la tarjeta. Migración Room 1→2 preserva servicios. Esto adelanta parte de Gastos y Resumen, sin implementar todavía todo V0.3/V0.4.

## Prioridad de usabilidad — V0.2.1

Inicio pasa a ser registro rápido: S/ 5, 6, 7 y 10 con un toque, sin formulario ni confirmación. Otro monto pide únicamente importe, abre teclado numérico y admite guardar desde el teclado. Confirmación breve con Deshacer y bloqueo temporal de doble toque. Historial y edición quedan en Servicios. Notas de servicios solo en Más opciones de edición; no se solicitan al registrar.

Esta prioridad adelanta los botones de tarifas de V0.5. Su configuración y ordenación siguen pendientes. Interacciones pensadas para cuando el vehículo está detenido.

## V0.1 — Preparación

Crear proyecto, versiones fijadas, Compose/Material 3, navegación, ViewModel, base Room y documentación. Esta etapa no incluye registro real, tarifas ni widget.

Criterios: generar APK debug, pasar Android Lint, abrir Inicio, comprobar Room y recorrer las cinco secciones en dispositivo/emulador. El estado de validación se registra en VALIDATION.md.

## V0.2 — Servicios (implementado)

- Modelo de dominio, validación monetaria y repositorio de servicios.
- Registrar monto, nota y fecha/hora; listado reactivo con Flow.
- Editar y eliminar con confirmación de eliminación.
- Pruebas de persistencia al cerrar/reabrir, validación de montos y operaciones DAO.

## V0.3 — Gastos (implementado)

Entidad, migración de Room, categorías iniciales, repositorio, ViewModel y CRUD. **Sin notas.** Flujo: tocar categoría → introducir monto → guardar; fecha/hora automáticas. Botones grandes para Combustible, Mantenimiento, Reparación, Lavado y Otros. No solicitar fecha, hora ni texto libre al crear. Edición avanzada fuera del flujo habitual. Probar migración sin pérdida de servicios.

Se reutiliza expenses de V0.2.2 para las cinco categorías, listado reactivo, total y CRUD. Sin cambios de esquema ni migración adicional. El combustible creado desde Inicio se edita y elimina en este mismo historial.

## V0.3.1 — Ganancia y gastos rápidos (implementado)

Inicio destaca ganancia del día = servicios menos todos los gastos del mismo día local. Semáforo: positivo verde, negativo rojo, cero amarillo, acompañado de texto. Combustible mantiene su desglose. Gastos ofrece combustible 50/100/Otro y lavado 10/Otro directamente en pantalla. Sin cambios de esquema Room.

## V0.4 — Resumen (implementado)

Ingresos, gastos, ganancia y cantidad; filtros día/semana/mes/total. Probar medianoche, cambio de mes/año y zona horaria.

Disponible en Estadísticas con Hoy, Esta semana, Este mes y Todo. Rangos calendario explícitos, semana de lunes a domingo. Conserva selección al navegar y reanuda consultas al volver. Inicio mantiene exclusivamente el día actual para no añadir pasos al registro.

## V0.5 — Tarifas rápidas (implementado)

Configuración en Ajustes: añadir, editar importe, eliminar con confirmación y ordenar con Subir/Bajar. Admite céntimos, valida importes positivos y evita duplicados. Room v3 añade tarifas con ID, importe y posición. Migraciones 1→2→3 y 2→3 conservan datos. Los valores iniciales 5/6/7/10 se crean una sola vez; eliminar todas deja Otro monto disponible. Un servicio conserva el importe cobrado y el ID de origen aunque cambie o desaparezca la tarifa. DataStore queda disponible para preferencias futuras que no sean tarifas.

## V0.6 — Widget (implementado)

Tarifas del usuario, persistencia mediante repositorio, actualización al cambiar tarifas, estado de error. Validar con proceso cerrado, dispositivo bloqueado y restricciones del sistema.

Widget nativo con cuatro tarifas por página, Anterior/Siguiente, Otro monto directo y Deshacer. Se añade desde Ajustes mediante la confirmación del launcher. Room v4 conserva el estado y token de cada instancia; la migración 3→4 no modifica tarifas, servicios ni gastos. Revisar VALIDATION.md para el alcance real de las pruebas.

## V0.6.2 — Acceso desde notificaciones (implementado)

Activación voluntaria en Ajustes y permiso de notificaciones desde Android 13. Notificación con tarifas paginadas, Otro monto, confirmación de guardado y Deshacer. Comparte transacciones y protección contra duplicados con el widget; mantiene estado propio. Restauración tras reinicio/actualización cuando está activada, sin servicio continuo. Respeta desactivación, permisos y bloqueo del dispositivo.

## V0.7 — Estadísticas (siguiente)

Reutilizar consultas y períodos de Resumen. Gráficos solo si aportan valor.

## V1.0 — Validación final

Completar checklist de la especificación; probar modo avión, conservación de datos, confirmaciones, tamaños de texto, claro/oscuro, rotación y errores. Generar APK instalable. No publicar en Google Play.

Exportación, backups manuales, kilómetros, objetivos, sincronización y cualquier backend quedan fuera de estas etapas.
