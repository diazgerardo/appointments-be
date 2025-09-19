// src/main/java/com/gerardo/appointments/dto/BlockDTO.java
package com.gerardo.appointments.dto;

import com.gerardo.appointments.domain.LocationType;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
@Setter
@Getter
public class BlockDTO {
  private String id;
  private String professionalId;
  private OffsetDateTime startTs;
  private OffsetDateTime endTs;
  private String reason;
  private Boolean open;
  // NUEVO (opcional; si falta => HOSPITAL)
  private LocationType locationType;

}
