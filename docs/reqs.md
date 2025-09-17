# Requisitos actualizados — Fase 2
_Última actualización: 2025-09-17 00:39:39Z_

## Alcance
Módulo de **turnos** con gestión autónoma de agenda por profesional, reserva/cancelación de turnos por pacientes y **notificación** de cancelaciones. Persistencia en **MongoDB**; front **Angular**; back **Spring Boot**.

## Actores
- **Profesional**: define y administra su disponibilidad (abre/cierra ventanas).
- **Paciente**: consulta disponibilidad y reserva/cancela su turno.
- **Sistema**: genera _slots_ a partir de ventanas abiertas, evita solapes, emite eventos y notifica.

## Supuestos y convenciones
- **Tiempo**: la API recibe/envía fechas en ISO‑8601; se almacena en **UTC** (`Instant`).
- **Tamaño de slot** configurable por request (por defecto **30 min**).
- **Modelo “OPEN”**: por defecto no hay disponibilidad; las **ventanas abiertas** (bloques con `open=true` o `reason="OPEN"`) habilitan turnos.
- **Solapes**: un turno es válido si `end > from` y `start < to` **no coincide** con otro turno programado.
- **Cancelación idempotente**: repetir una cancelación no produce error.
- **Concurrencia**: control optimista en citas (campo `version` en Mongo); códigos 409 en conflictos.
- **Seguridad**: pendiente (no bloquea Fase 2). Se asume `professionalId` y `patientId` válidos.

## Reglas de negocio
1. Sin ventanas **OPEN**, no se exponen _slots_.
2. Un _slot_ pertenece completamente a una ventana **OPEN** y **no** puede intersectar un turno en estado **SCHEDULED**.
3. Al cancelar un turno:
   - se cambia estado a **CANCELLED** (con motivo),
   - se persiste un **OutboxEvent** `AppointmentCancelled`,
   - se difunde por **SSE** al paciente afectado.
4. Eliminar una ventana **OPEN** deja de ofrecer disponibilidad futura; los turnos ya reservados **no** se borran automáticamente (el profesional debe cancelarlos si corresponde).

## Endpoints (REST + SSE)
```
GET  /api/professionals/{proId}/slots?from=&to=&slotMinutes=30
POST /api/professionals/{proId}/availability/blocks        (abre ventana)
DELETE /api/professionals/{proId}/availability/blocks/{id} (cierra ventana)
POST /api/appointments                                      (reserva turno)
POST /api/appointments/{id}/cancel                          (cancela turno)
GET  /api/notifications/stream?patientId=...                (SSE)
```

### Cuerpos request/response (resumen) + cURL
**Abrir ventana**
```bash
curl -X POST http://localhost:8080/api/professionals/1/availability/blocks   -H "Content-Type: application/json"   -d '{"startTs":"2025-09-20T12:00:00Z","endTs":"2025-09-20T15:00:00Z","reason":"OPEN"}'
```
**Consultar _slots_**
```bash
curl "http://localhost:8080/api/professionals/1/slots?from=2025-09-20T12:00:00Z&to=2025-09-20T16:00:00Z&slotMinutes=30"
```
**Reservar**
```bash
curl -X POST http://localhost:8080/api/appointments   -H "Content-Type: application/json"   -d '{"professionalId":1,"patientId":1001,"startTs":"2025-09-20T14:00:00Z","endTs":"2025-09-20T14:30:00Z"}'
```
**Cancelar**
```bash
curl -X POST http://localhost:8080/api/appointments/ABC123/cancel   -H "Content-Type: application/json"   -d '{"reason":"reprogramación del profesional"}'
```
**SSE (escucha de paciente)**
```bash
curl -N "http://localhost:8080/api/notifications/stream?patientId=1001"
```

### Códigos de estado
- `200/201` ok; `204` sin contenido.
- `400` rango inválido o fechas inconsistentes.
- `404` recurso inexistente.
- `409` conflicto (slot ocupado / carrera).
- `422` datos de dominio inválidos.

## No funcionales
- **MongoDB** con índices compuestos `{ professionalId, startTs, endTs }` y `{ professionalId, startTs, endTs, status }` (citas).
- **Logging** mínimo en cancelaciones y outbox.
- **Escalabilidad**: SSE en memoria para PoC; canal externo (mail/SMS) por Outbox en background.
- **Observabilidad**: actuator/métricas (opcional).
- **i18n**: mensajes en FE (fuera de este alcance).
