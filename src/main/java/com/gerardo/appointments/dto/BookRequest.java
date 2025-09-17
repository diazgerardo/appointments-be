// BookRequest.java
package com.gerardo.appointments.dto;

import java.time.OffsetDateTime;

public class BookRequest {
  private Long professionalId;
  private Long patientId;
  private OffsetDateTime startTs;
  private OffsetDateTime endTs;

  public Long getProfessionalId() { return professionalId; }
  public void setProfessionalId(Long professionalId) { this.professionalId = professionalId; }

  public Long getPatientId() { return patientId; }
  public void setPatientId(Long patientId) { this.patientId = patientId; }

  public OffsetDateTime getStartTs() { return startTs; }
  public void setStartTs(OffsetDateTime startTs) { this.startTs = startTs; }

  public OffsetDateTime getEndTs() { return endTs; }
  public void setEndTs(OffsetDateTime endTs) { this.endTs = endTs; }
}
