// src/main/java/com/gerardo/appointments/service/SlotsService.java
package com.gerardo.appointments.service;

import com.gerardo.appointments.domain.*;
import com.gerardo.appointments.dto.*;
import com.gerardo.appointments.repo.*;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class SlotsService {
  private final AvailabilityBlockRepo blocks;
  private final AppointmentRepo appts;
  private final ProfessionalRepo pros;
  private final PatientRepo patients;

  public SlotsService(AvailabilityBlockRepo blocks, AppointmentRepo appts, ProfessionalRepo pros, PatientRepo patients){
    this.blocks = blocks; this.appts = appts; this.pros = pros; this.patients = patients;
  }

  public List<SlotViewDTO> professionalSlots(String professionalId, OffsetDateTime from, OffsetDateTime to, int slotMinutes, String include){
    var f = from.toInstant(); var t = to.toInstant();
    var open = blocks.findByProfessionalIdAndEndTsAfterAndStartTsBefore(professionalId, f, t)
                     .stream().filter(AvailabilityBlock::isOpen).toList();

    // cache nombres
    var proName = pros.findById(professionalId).map(Professional::getFullName).orElse(null);

    // traer todas las appts del rango para el pro
    var busy = appts.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
        professionalId, AppointmentStatus.SCHEDULED, f, t);

    // cache de pacientes
    Map<String,String> patientNames = patients.findAllById(
        busy.stream().map(Appointment::getPatientId).collect(Collectors.toSet())
    ).stream().collect(Collectors.toMap(Patient::getId, Patient::getFullName));

    var out = new ArrayList<SlotViewDTO>();
    var slot = Duration.ofMinutes(slotMinutes);

    for (var w : open) {
      var ws = w.getStartTs().isBefore(f) ? f : w.getStartTs();
      var we = w.getEndTs().isAfter(t) ? t : w.getEndTs();
      for (var cur = ws; cur.plus(slot).compareTo(we) <= 0; cur = cur.plus(slot)) {
        var s = cur; var e = cur.plus(slot);
        // bookings que se solapan con el slot (mismo pro)
        var bookings = busy.stream().filter(a -> a.getEndTs().isAfter(s) && a.getStartTs().isBefore(e))
          .map(a -> new BookingItemDTO(
              a.getId(),
              a.getPatientId(),
              patientNames.getOrDefault(a.getPatientId(), null)
          ))
          .toList();

        int overlapCount = bookings.size();

        // filtro include
        boolean want;
        if ("available".equalsIgnoreCase(include))      want = overlapCount == 0;
        else if ("booked".equalsIgnoreCase(include))    want = overlapCount > 0;
        else                                            want = true;

        if (want) {
          var dto = new SlotViewDTO();
          dto.setProfessionalId(professionalId);
          dto.setProfessionalName(proName);
          dto.setStartTs(OffsetDateTime.ofInstant(s, ZoneOffset.UTC));
          dto.setEndTs(OffsetDateTime.ofInstant(e, ZoneOffset.UTC));
          dto.setLocationType(w.getLocationType() == null ? LocationType.HOSPITAL : w.getLocationType());
          dto.setOverlapCount(overlapCount);
          dto.setBookings(bookings);
          dto.setStatus(overlapCount > 0 ? "BOOKED" : "AVAILABLE");
          out.add(dto);
        }
      }
    }
    return out;
  }

  public List<SlotViewDTO> patientSlots(String patientId, OffsetDateTime from, OffsetDateTime to, int slotMinutes){
    var f = from.toInstant(); var t = to.toInstant();
    var slot = Duration.ofMinutes(slotMinutes);

    // citas del paciente en rango
    var mine = appts.findByPatientIdAndStatusAndEndTsAfterAndStartTsBefore(
        patientId, AppointmentStatus.SCHEDULED, f, t);

    // profesionales + nombres cache
    Map<String,String> proNames = pros.findAllById(
        mine.stream().map(Appointment::getProfessionalId).collect(Collectors.toSet())
    ).stream().collect(Collectors.toMap(Professional::getId, Professional::getFullName));

    var out = new ArrayList<SlotViewDTO>();

    for (var a : mine) {
      // localizar bloque OPEN que contenga el slot para obtener locationType; si no, default HOSPITAL
      var blks = blocks.findByProfessionalIdAndEndTsAfterAndStartTsBefore(a.getProfessionalId(), a.getStartTs(), a.getEndTs());
      var lt = blks.stream().filter(AvailabilityBlock::isOpen).findFirst().map(AvailabilityBlock::getLocationType)
                   .orElse(LocationType.HOSPITAL);

      // normalizar a múltiplos del slot si querés; por ahora usamos el tramo exacto de la cita
      var dto = new SlotViewDTO();
      dto.setProfessionalId(a.getProfessionalId());
      dto.setProfessionalName(proNames.getOrDefault(a.getProfessionalId(), null));
      dto.setStartTs(OffsetDateTime.ofInstant(a.getStartTs(), ZoneOffset.UTC));
      dto.setEndTs(OffsetDateTime.ofInstant(a.getEndTs(), ZoneOffset.UTC));
      dto.setLocationType(lt);
      dto.setOverlapCount(1);
      dto.setBookings(List.of(new BookingItemDTO(a.getId(), patientId, null))); // patientName opcional aquí
      dto.setStatus("BOOKED");
      out.add(dto);
    }
    return out;
  }
}
