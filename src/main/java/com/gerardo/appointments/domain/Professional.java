package com.gerardo.appointments.domain;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document("professionals")
public class Professional extends Person {
  private String specialty;
  private List<AvailabilitySlot> availability; // weekly recurring
}
