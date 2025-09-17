// BookRequest.java
package com.gerardo.appointments.dto;

import java.time.OffsetDateTime;

public class BookRequest {
  private String professionalId;
  private String patientId;
  private OffsetDateTime startTs;
  private OffsetDateTime endTs;

  public String getProfessionalId() { return professionalId; }
  public void setProfessionalId(String professionalId) { this.professionalId = professionalId; }

  public String getPatientId() { return patientId; }
  public void setPatientId(String patientId) { this.patientId = patientId; }

  public OffsetDateTime getStartTs() { return startTs; }
  public void setStartTs(OffsetDateTime startTs) { this.startTs = startTs; }

  public OffsetDateTime getEndTs() { return endTs; }
  public void setEndTs(OffsetDateTime endTs) { this.endTs = endTs; }
}
