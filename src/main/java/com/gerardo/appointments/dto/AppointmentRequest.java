// AppointmentRequest.java
package com.gerardo.appointments.dto;

import com.gerardo.appointments.domain.LocationType;
import jakarta.validation.constraints.*;
import java.time.Instant;

public record AppointmentRequest(
  @NotBlank String professionalId,
  @NotBlank String patientId,
  @NotNull Instant startUtc,
  @NotNull Instant endUtc,
  String notes,
  LocationType locationType,   // optional: if null, resolved from availability that matches
  String locationName          // optional
) {}
