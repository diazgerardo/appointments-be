// src/main/java/com/gerardo/app/domain/Appointment.java
package com.gerardo.appointments.domain;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("appointments")
@CompoundIndexes({
  @CompoundIndex(name="pro_start_end_status", def="{ 'professionalId':1, 'startTs':1, 'endTs':1, 'status':1 }")
})
public class Appointment {
  @Setter
  @Getter
  @Id private String id;
  @Setter
  @Getter
  private String professionalId;
  @Setter
  @Getter
  private String patientId;
  @Setter
  @Getter
  private Instant startTs;  // UTC
  @Setter
  @Getter
  private Instant endTs;    // UTC
  @Setter
  @Getter
  private AppointmentStatus status = AppointmentStatus.SCHEDULED;
  @Setter
  @Getter
  private String cancelReason;

    public boolean isCancelled() { return status == AppointmentStatus.CANCELLED; }
  public void cancel(String reason) { this.status = AppointmentStatus.CANCELLED; this.cancelReason = reason; }
}
