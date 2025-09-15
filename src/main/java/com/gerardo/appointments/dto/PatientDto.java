// PatientDto.java
package com.gerardo.appointments.dto;

import java.util.List;

public record PatientDto(
  String id, String fullName, String email, String phone,
  String diagnostic, String prescription, List<String> requests
) {}
