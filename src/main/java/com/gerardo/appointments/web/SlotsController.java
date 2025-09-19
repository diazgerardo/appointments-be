// src/main/java/com/gerardo/appointments/web/SlotsController.java
package com.gerardo.appointments.web;

import com.gerardo.appointments.dto.SlotViewDTO;
import com.gerardo.appointments.service.SlotsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/slots")
public class SlotsController {
  private final SlotsService svc;
  public SlotsController(SlotsService svc){ this.svc = svc; }

  @GetMapping("/professionals/{professionalId}")
  public List<SlotViewDTO> byProfessional(
      @PathVariable String professionalId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @RequestParam(defaultValue = "30") int slotMinutes,
      @RequestParam(required = false) String include // "available" | "booked" | otro => all
  ){
    return svc.professionalSlots(professionalId, from, to, slotMinutes, include);
  }

  @GetMapping("/patients/{patientId}")
  public List<SlotViewDTO> byPatient(
      @PathVariable String patientId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @RequestParam(defaultValue = "30") int slotMinutes
  ){
    return svc.patientSlots(patientId, from, to, slotMinutes);
  }
}
