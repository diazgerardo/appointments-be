// UpdateStatusRequest.java
package com.gerardo.appointments.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateStatusRequest(@NotBlank String status) {}
