// src/main/java/com/gerardo/app/domain/AvailabilityBlock.java
package com.gerardo.appointments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("availability_blocks")
@CompoundIndexes({
  @CompoundIndex(name="pro_start_end", def="{ 'professionalId':1, 'startTs':1, 'endTs':1 }")
})
public class AvailabilityBlock {
  @Id private String id;
  private Long professionalId;
  private Instant startTs;  // UTC
  private Instant endTs;    // UTC
  private String reason;    // "OPEN" for availability
  private static boolean open = true;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public Long getProfessionalId() { return professionalId; }
  public void setProfessionalId(Long professionalId) { this.professionalId = professionalId; }
  public Instant getStartTs() { return startTs; }
  public void setStartTs(Instant startTs) { this.startTs = startTs; }
  public Instant getEndTs() { return endTs; }
  public void setEndTs(Instant endTs) { this.endTs = endTs; }
  public String getReason() { return reason; }
  public void setReason(String reason) { this.reason = reason; }
  public boolean isOpen() { return open; }
  public void setOpen(boolean open) { this.open = open; }
}
