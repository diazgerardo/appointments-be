// AppointmentDto.java
package com.gerardo.appointments.dto;

import com.gerardo.appointments.domain.LocationType;
import java.time.Instant;

public record AppointmentDto(
  String id, String professionalId, String patientId,
  Instant startUtc, Instant endUtc,
  String status, String notes,
  LocationType locationType, String locationName
) {}
