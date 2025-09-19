// src/main/java/com/gerardo/appointments/dto/BookingItemDTO.java
package com.gerardo.appointments.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingItemDTO {
  private String appointmentId;
  private String patientId;
  private String patientName;

  public BookingItemDTO() {}
  public BookingItemDTO(String appointmentId, String patientId, String patientName){
    this.appointmentId = appointmentId;
    this.patientId = patientId;
    this.patientName = patientName;
  }

}
