package com.gerardo.appointments.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document("appointments")
@CompoundIndexes({
  @CompoundIndex(name="byProfessionalAndStart", def = "{'professionalId':1,'startUtc':1}"),
  @CompoundIndex(name="byPatientAndStart", def = "{'patientId':1,'startUtc':1}"),
  @CompoundIndex(name="byStatus", def = "{'status':1}")
})
public class Appointment {
  @Id
  private String id;

  private String professionalId;
  private String patientId;

  private Instant startUtc;
  private Instant endUtc;

  private String status; // SCHEDULED | CONFIRMED | CANCELLED | COMPLETED
  private String notes;

  private LocationType locationType;
  private String locationName; // resolved from the slot used
}
