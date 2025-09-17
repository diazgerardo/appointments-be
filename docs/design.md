# Diseño — Clases y responsabilidades
_Última actualización: 2025-09-17 00:39:39Z_

## Dominio (Mongo Documents)
### Appointment
- **Campos**: `id:String`, `professionalId:Long`, `patientId:Long`, `startTs:Instant`, `endTs:Instant`, `status:AppointmentStatus`, `cancelReason:String`, `version:Long`.
- **Responsabilidad**: representar una cita; `isCancelled()`, `cancel(reason)`.
- **Índices**: `{ professionalId:1, startTs:1, endTs:1, status:1 }`.

### AvailabilityBlock
- **Campos**: `id:String`, `professionalId:Long`, `startTs:Instant`, `endTs:Instant`, `reason:String`, `open:boolean=true`.
- **Responsabilidad**: definir **ventanas OPEN** (disponibilidad).
- **Índices**: `{ professionalId:1, startTs:1, endTs:1 }`.

### OutboxEvent
- **Campos**: `id:String`, `type:String`, `aggregateId:Long?`, `payloadJson:String`, `createdAt:Instant`, `processedAt:Instant?`.
- **Responsabilidad**: garantizar entrega eventual de notificaciones externas.

### AppointmentStatus (enum)
- Valores: `SCHEDULED`, `CANCELLED`.

## DTOs (`com.gerardo.appointments.dto`)
- **BookRequest**: `professionalId, patientId, startTs:OffsetDateTime, endTs:OffsetDateTime`.
- **CancelRequest**: `reason:String`.
- **SlotDTO**: `startTs, endTs` (OffsetDateTime).
- **BlockDTO**: `id, professionalId, startTs, endTs, reason, open`.

## Repositorios (Spring Data Mongo)
- **AppointmentRepo**
  - `findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(...)`
  - `existsByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(...)`
- **AvailabilityBlockRepo**
  - `findByProfessionalId(...)`
  - `findByProfessionalIdAndEndTsAfterAndStartTsBefore(...)`
- **OutboxEventRepo**
  - `findByProcessedAtIsNullAndType(String type)`

## Servicios
### AvailabilityService
- `addOpenBlock(proId, start, end, reason)` → crea ventana OPEN.
- `removeBlock(blockId)` → cierra ventana.
- `computeSlots(proId, from, to, slot)` → *slots* = **OPEN − citas** (discretiza por `slot`).

### AppointmentService
- `book(proId, patientId, start, end)` → valida solapes; guarda *SCHEDULED*.
- `cancel(id, reason)` → marca *CANCELLED*, persiste **OutboxEvent**, emite **SSE**.

### NotificationHub (SSE en memoria)
- `subscribe(patientId)` → `SseEmitter`.
- `broadcastToPatient(patientId, eventName, json)`.

### OutboxPump (scheduler)
- Cada 30s procesa `AppointmentCancelled`: envía por canal externo (pendiente), marca `processedAt`.

## Controladores
- **AvailabilityController**
  - `POST /professionals/{proId}/availability/blocks`
  - `DELETE /professionals/{proId}/availability/blocks/{blockId}`
  - `GET /professionals/{proId}/slots?from&to&slotMinutes`
- **AppointmentController**
  - `POST /appointments` (book)
  - `POST /appointments/{id}/cancel`
- **NotificationsController**
  - `GET /notifications/stream?patientId=…` (SSE)

## Validaciones e invariantes
- `startTs < endTs` en bloques y citas.
- Reserva inválida si interseca otra *SCHEDULED* del mismo profesional.
- Cancelación **idempotente** (segura de repetir).
- Conversión de tiempos: `OffsetDateTime` ⇄ `Instant (UTC)`.

## Secuencia — Cancelación y notificación (texto)
1. **FE** → `POST /api/appointments/{id}/cancel` con `reason`.
2. **AppointmentService**: cambia estado a `CANCELLED`, persiste `OutboxEvent("AppointmentCancelled")`.
3. **AppointmentService**: emite **SSE** inmediato a `patientId` (canal en memoria).
4. **OutboxPump** (cada 30s): toma eventos no procesados, intenta entrega externa (email/SMS), marca `processedAt`.
