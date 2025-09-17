// src/main/java/com/gerardo/app/service/AppointmentService.java
package com.gerardo.appointments.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gerardo.appointments.domain.Appointment;
import com.gerardo.appointments.domain.AppointmentStatus;
import com.gerardo.appointments.domain.OutboxEvent;
import com.gerardo.appointments.repo.AppointmentRepository;
import com.gerardo.appointments.repo.OutboxEventRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.Map;

@Service
public class AppointmentService {
  private final AppointmentRepository repo;
  private final OutboxEventRepository outbox;
  private final ObjectMapper om = new ObjectMapper();

  public AppointmentService(AppointmentRepository r, OutboxEventRepository o){ this.repo=r; this.outbox=o; }

  public Appointment book(Long proId, Long patientId, OffsetDateTime start, OffsetDateTime end){
    var s = start.toInstant(); var e = end.toInstant();
    if (repo.existsByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(proId, AppointmentStatus.SCHEDULED, s, e))
      throw new IllegalStateException("slot not available");

    var a = new Appointment();
    a.setProfessionalId(proId);
    a.setPatientId(patientId);
    a.setStartTs(s);
    a.setEndTs(e);
    a.setStatus(AppointmentStatus.SCHEDULED);
    return repo.save(a);
  }

  public void cancel(String id, String reason){
    var a = repo.findById(id).orElseThrow();
    if (a.isCancelled()) return;
    a.cancel(reason);
    repo.save(a);

    var payload = Map.of(
      "appointmentId", a.getId(),
      "professionalId", a.getProfessionalId(),
      "patientId", a.getPatientId(),
      "startTs", a.getStartTs().toString(),
      "endTs", a.getEndTs().toString(),
      "reason", reason
    );
    var e = new OutboxEvent();
    e.setType("AppointmentCancelled");
    e.setAggregateId(Long.valueOf(a.getId().hashCode())); // optional; or keep null
    e.setPayloadJson(writeJson(payload));
    outbox.save(e);

    NotificationHub.broadcastToPatient(a.getPatientId(), "AppointmentCancelled", e.getPayloadJson());
  }

  private String writeJson(Object x){
    try { return om.writeValueAsString(x); } catch(Exception e){ throw new RuntimeException(e); }
  }
}
