package com.gerardo.appointments.web;

import com.gerardo.appointments.domain.Appointment;
import com.gerardo.appointments.dto.BookRequest;
import com.gerardo.appointments.dto.CancelRequest;
import com.gerardo.appointments.service.AppointmentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentController {

  private final AppointmentService svc;

  public AppointmentController(AppointmentService svc) {
    this.svc = svc;
  }

  @PostMapping
  public Appointment book(@RequestBody BookRequest r) {
    return svc.book(r.getProfessionalId(), r.getPatientId(), r.getStartTs(), r.getEndTs());
  }

  @PostMapping("/{id}/cancel")
  public void cancel(@PathVariable String id, @RequestBody CancelRequest r) {
    svc.cancel(id, r.getReason());
  }
}
