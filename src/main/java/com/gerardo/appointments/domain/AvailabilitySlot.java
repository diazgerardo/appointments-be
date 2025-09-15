package com.gerardo.appointments.domain;

import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AvailabilitySlot {
  private DayOfWeek dayOfWeek;        // e.g., MONDAY
  private LocalTime startLocalTime;   // e.g., 14:00
  private LocalTime endLocalTime;     // e.g., 18:00
  private LocationType locationType;  // PRIVATE or HOSPITAL
  private String locationName;        // optional: hospital name or "Consultorio Privado"
}
