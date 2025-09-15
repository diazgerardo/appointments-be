// ProfessionalDto.java
package com.gerardo.appointments.dto;

import com.gerardo.appointments.domain.AvailabilitySlot;
import java.util.List;

public record ProfessionalDto(
  String id, String fullName, String email, String phone,
  String specialty, List<AvailabilitySlot> availability
) {}
