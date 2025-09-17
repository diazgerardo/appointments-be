// src/main/java/com/gerardo/app/dto/BlockDTO.java
package com.gerardo.appointments.dto;
import java.time.OffsetDateTime;
public class BlockDTO {
  private String id;
  private String professionalId;
  private OffsetDateTime startTs;
  private OffsetDateTime endTs;
  private String reason;
  private Boolean open; // default true

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getProfessionalId() { return professionalId; }
  public void setProfessionalId(String professionalId) { this.professionalId = professionalId; }
  public OffsetDateTime getStartTs() { return startTs; }
  public void setStartTs(OffsetDateTime startTs) { this.startTs = startTs; }
  public OffsetDateTime getEndTs() { return endTs; }
  public void setEndTs(OffsetDateTime endTs) { this.endTs = endTs; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public Boolean getOpen() { return open; }
  public void setOpen(Boolean open) { this.open = open; }
}
