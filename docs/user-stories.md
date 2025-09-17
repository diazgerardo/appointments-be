# Historias de Usuario — API turnos
_Última actualización: 2025-09-17 00:39:39Z_

## HU‑01 — Abrir ventana de disponibilidad
**Como** profesional, **quiero** habilitar una franja horaria, **para** que los pacientes puedan reservar.
- **Dado** un `professionalId` válido  
  **Cuando** envío `POST /api/professionals/{id}/availability/blocks` con `startTs < endTs` y `reason="OPEN"`  
  **Entonces** se crea la ventana.
```bash
curl -X POST http://localhost:8080/api/professionals/1/availability/blocks   -H "Content-Type: application/json"   -d '{"startTs":"2025-09-20T12:00:00Z","endTs":"2025-09-20T15:00:00Z","reason":"OPEN"}'
```

## HU‑02 — Cerrar ventana
**Como** profesional, **quiero** eliminar una ventana abierta, **para** dejar de ofrecer _slots_.
```bash
curl -X DELETE http://localhost:8080/api/professionals/1/availability/blocks/{BLOCK_ID}
```

## HU‑03 — Consultar _slots_
**Como** paciente, **quiero** ver horarios disponibles en un rango, **para** elegir un turno.
```bash
curl "http://localhost:8080/api/professionals/1/slots?from=2025-09-20T12:00:00Z&to=2025-09-20T16:00:00Z&slotMinutes=30"
```

## HU‑04 — Reservar un turno
**Como** paciente, **quiero** reservar un horario, **para** confirmar mi atención.
- Si el slot está ocupado → **409**.
```bash
curl -X POST http://localhost:8080/api/appointments   -H "Content-Type: application/json"   -d '{"professionalId":1,"patientId":1001,"startTs":"2025-09-20T14:00:00Z","endTs":"2025-09-20T14:30:00Z"}'
```

## HU‑05 — Idempotencia frente a carreras
**Como** sistema, **quiero** evitar duplicidad por clics múltiples, **para** mantener consistencia.
- Dos reservas simultáneas → 1 éxito, 1 **409** (existencia por solape).

## HU‑06 — Cancelar y notificar
**Como** profesional, **quiero** cancelar una cita, **para** liberar agenda y avisar al paciente.
```bash
curl -X POST http://localhost:8080/api/appointments/{APPT_ID}/cancel   -H "Content-Type: application/json"   -d '{"reason":"indisponibilidad del profesional"}'
# Escucha SSE del paciente
curl -N "http://localhost:8080/api/notifications/stream?patientId=1001"
```

## HU‑07 — Validación de rango inválido
**Como** sistema, **quiero** rechazar `startTs >= endTs`, **para** mantener integridad. → **400**.

## HU‑08 — Disponibilidad vacía
**Como** paciente, **quiero** recibir lista vacía cuando no hay ventanas OPEN, **para** decidir alternativas. → `200 []`.

## HU‑09 — Entrega diferida por Outbox
**Como** sistema, **quiero** asegurar notificaciones aunque fallen servicios externos, **para** no perder avisos.
- _Pump_ cada 30s procesa `outbox_events` (`processedAt=null`, `type='AppointmentCancelled'`).