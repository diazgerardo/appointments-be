// AppointmentController.java
package com.gerardo.appointments.web;

import com.gerardo.appointments.domain.Appointment;
import com.gerardo.appointments.dto.AppointmentRequest;
import com.gerardo.appointments.dto.UpdateStatusRequest;
import com.gerardo.appointments.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

  private final AppointmentService service;

  @PostMapping
  public Appointment create(@RequestBody @Valid AppointmentRequest req) {
    return service.schedule(req);
  }

  @PatchMapping("/{id}/status")
  public Appointment updateStatus(@PathVariable String id, @RequestBody @Valid UpdateStatusRequest body) {
    return service.updateStatus(id, body.status());
  }

  @GetMapping("/by-professional/{professionalId}")
  public List<Appointment> byProfessional(
      @PathVariable String professionalId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
    return service.byProfessionalRange(professionalId, from, to);
  }

  @GetMapping("/by-patient/{patientId}")
  public List<Appointment> byPatient(@PathVariable String patientId) {
    return service.byPatient(patientId);
  }
}
