package com.gerardo.appointments.service;

import com.gerardo.appointments.domain.*;
import com.gerardo.appointments.dto.AppointmentRequest;
import com.gerardo.appointments.repo.AppointmentRepo;
import com.gerardo.appointments.repo.PatientRepo;
import com.gerardo.appointments.repo.ProfessionalRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.time.*;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AppointmentService {

  private final AppointmentRepo appointments;
  private final ProfessionalRepo professionals;
  private final PatientRepo patients;

  @Value("${app.overbook-limit:3}")
  private int overbookLimit; // number of extra overlaps allowed; max concurrent = overbookLimit + 1

  @Value("${app.tz-availability:America/Argentina/Buenos_Aires}")
  private String availabilityZone;

  public Appointment schedule(AppointmentRequest req) {
    Assert.notNull(req, "request required");
    if (!req.endUtc().isAfter(req.startUtc())) {
      throw new IllegalArgumentException("endUtc must be after startUtc");
    }

    Professional pro = professionals.findById(req.professionalId())
        .orElseThrow(() -> new IllegalArgumentException("professional not found"));
    patients.findById(req.patientId())
        .orElseThrow(() -> new IllegalArgumentException("patient not found"));

    // 1) check professional availability window (in BA local time)
    ZoneId zone = ZoneId.of(availabilityZone);
    LocalDateTime startLocal = LocalDateTime.ofInstant(req.startUtc(), zone);
    LocalDateTime endLocal   = LocalDateTime.ofInstant(req.endUtc(), zone);

    AvailabilitySlot matching = pro.getAvailability() == null ? null :
        pro.getAvailability().stream().filter(slot ->
            slot.getDayOfWeek() == startLocal.getDayOfWeek() &&
            !startLocal.toLocalTime().isBefore(slot.getStartLocalTime()) &&
            !endLocal.toLocalTime().isAfter(slot.getEndLocalTime())
        ).findFirst().orElse(null);

    if (matching == null) {
      throw new IllegalStateException("outside professional availability");
    }

    // 2) compute current overlap count for this interval
    List<Appointment> overlapping = appointments
        .findByProfessionalIdAndStartUtcLessThanAndEndUtcGreaterThan(
            req.professionalId(), req.endUtc(), req.startUtc());

    long activeOverlaps = overlapping.stream()
        .filter(a -> !"CANCELLED".equalsIgnoreCase(a.getStatus()))
        .count();

    long maxConcurrent = overbookLimit + 1L; // e.g., 4
    if (activeOverlaps >= maxConcurrent) {
      throw new IllegalStateException("overbooking limit reached for this time slot");
    }

    // 3) persist
    Appointment a = Appointment.builder()
        .professionalId(req.professionalId())
        .patientId(req.patientId())
        .startUtc(req.startUtc())
        .endUtc(req.endUtc())
        .status("SCHEDULED")
        .notes(req.notes())
        .locationType(req.locationType() != null ? req.locationType() : matching.getLocationType())
        .locationName(req.locationName() != null ? req.locationName() : matching.getLocationName())
        .build();

    return appointments.save(a);
  }

  public Appointment updateStatus(String id, String status) {
    Appointment a = appointments.findById(id)
        .orElseThrow(() -> new IllegalArgumentException("appointment not found"));
    a.setStatus(status);
    return appointments.save(a);
  }

  public List<Appointment> byProfessionalRange(String professionalId, Instant from, Instant to) {
    return appointments.findByProfessionalIdAndStartUtcBetween(professionalId, from, to);
  }

  public List<Appointment> byPatient(String patientId) {
    return appointments.findByPatientIdOrderByStartUtcDesc(patientId);
  }
}
