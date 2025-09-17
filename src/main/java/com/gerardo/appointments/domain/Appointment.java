// src/main/java/com/gerardo/app/domain/Appointment.java
package com.gerardo.appointments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("appointments")
@CompoundIndexes({
  @CompoundIndex(name="pro_start_end_status", def="{ 'professionalId':1, 'startTs':1, 'endTs':1, 'status':1 }")
})
public class Appointment {
  @Id private String id;
  private Long professionalId;
  private Long patientId;
  private Instant startTs;  // UTC
  private Instant endTs;    // UTC
  private AppointmentStatus status = AppointmentStatus.SCHEDULED;
  private String cancelReason;
  @Version private Long version;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Long getProfessionalId() { return professionalId; }
  public void setProfessionalId(Long professionalId) { this.professionalId = professionalId; }
  public Long getPatientId() { return patientId; }
  public void setPatientId(Long patientId) { this.patientId = patientId; }
  public Instant getStartTs() { return startTs; }
  public void setStartTs(Instant startTs) { this.startTs = startTs; }
  public Instant getEndTs() { return endTs; }
  public void setEndTs(Instant endTs) { this.endTs = endTs; }
  public AppointmentStatus getStatus() { return status; }
  public void setStatus(AppointmentStatus status) { this.status = status; }
  public String getCancelReason() { return cancelReason; }
  public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
  public boolean isCancelled() { return status == AppointmentStatus.CANCELLED; }
  public void cancel(String reason) { this.status = AppointmentStatus.CANCELLED; this.cancelReason = reason; }
}
