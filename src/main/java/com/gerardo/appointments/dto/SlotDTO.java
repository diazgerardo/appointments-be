// src/main/java/com/gerardo/app/dto/SlotDTO.java
package com.gerardo.appointments.dto;
import java.time.OffsetDateTime;
public class SlotDTO {
  private OffsetDateTime startTs;
  private OffsetDateTime endTs;
  public SlotDTO() {}
  public SlotDTO(OffsetDateTime s, OffsetDateTime e){ this.startTs=s; this.endTs=e; }
  public OffsetDateTime getStartTs(){ return startTs; }
  public void setStartTs(OffsetDateTime startTs){ this.startTs=startTs; }
  public OffsetDateTime getEndTs(){ return endTs; }
  public void setEndTs(OffsetDateTime endTs){ this.endTs=endTs; }
}
