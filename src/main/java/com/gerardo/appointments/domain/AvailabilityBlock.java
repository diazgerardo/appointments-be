// src/main/java/com/gerardo/appointments/domain/AvailabilityBlock.java
package com.gerardo.appointments.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Setter
@Getter
@Document("availability_blocks")
@CompoundIndexes({
        @CompoundIndex(name="pro_start_end", def="{ 'professionalId':1, 'startTs':1, 'endTs':1 }")
})
public class AvailabilityBlock {
    // getters/setters...
    @Id private String id;
  private String professionalId;
  private Instant startTs;
  private Instant endTs;
  private String reason;
  private boolean open = true;

  // NUEVO: pertenece al slot (default HOSPITAL)
  private LocationType locationType = LocationType.HOSPITAL;

}
