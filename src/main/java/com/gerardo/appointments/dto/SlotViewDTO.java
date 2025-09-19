// src/main/java/com/gerardo/appointments/dto/SlotViewDTO.java
package com.gerardo.appointments.dto;

import com.gerardo.appointments.domain.LocationType;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;
@Data
public class SlotViewDTO {
  private String professionalId;
  private String professionalName;
  private OffsetDateTime startTs;
  private OffsetDateTime endTs;
  private LocationType locationType;               // PRIVATE | HOSPITAL
  private String status;                           // "AVAILABLE" | "BOOKED"
  private int overlapCount;                        // 0..3
  private List<BookingItemDTO> bookings;           // cada item: { appointmentId, patientId, patientName }

}
