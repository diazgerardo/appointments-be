// src/main/java/com/gerardo/app/service/AvailabilityService.java
package com.gerardo.appointments.service;

import com.gerardo.appointments.domain.AvailabilityBlock;
import com.gerardo.appointments.dto.SlotDTO;
import com.gerardo.appointments.repo.AppointmentRepository;
import com.gerardo.appointments.repo.AvailabilityBlockRepository;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class AvailabilityService {
  private final AvailabilityBlockRepository blocks;
  private final AppointmentRepository appts;

  public AvailabilityService(AvailabilityBlockRepository b, AppointmentRepository a){
    this.blocks=b; this.appts=a;
  }

  public AvailabilityBlock addOpenBlock(String proId, OffsetDateTime start, OffsetDateTime end, String reason){
    var b = new AvailabilityBlock();
    b.setProfessionalId(proId);
    b.setStartTs(start.toInstant());
    b.setEndTs(end.toInstant());
    b.setReason(reason);
    b.setOpen(true);
    return blocks.save(b);
  }

  public void removeBlock(String blockId){ blocks.deleteById(blockId); }

  /** Available slots = OPEN windows − scheduled appointments */
  public List<SlotDTO> computeSlots(String proId, OffsetDateTime from, OffsetDateTime to, Duration slot){
    var f = from.toInstant(); var t = to.toInstant();
    var open = blocks.findByProfessionalIdAndEndTsAfterAndStartTsBefore(proId, f, t)
                     .stream().filter(AvailabilityBlock::isOpen).toList();
    var busy = appts.findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
        proId, com.gerardo.appointments.domain.AppointmentStatus.SCHEDULED, f, t);

    var out = new ArrayList<SlotDTO>();
    for (var w : open) {
      var ws = w.getStartTs().isBefore(f) ? f : w.getStartTs();
      var we = w.getEndTs().isAfter(t) ? t : w.getEndTs();
      for (var cur = ws; cur.plus(slot).compareTo(we) <= 0; cur = cur.plus(slot)) {
        var s = cur; var e = cur.plus(slot);
        boolean overlaps = busy.stream().anyMatch(a ->
            a.getEndTs().isAfter(s) && a.getStartTs().isBefore(e));
        if (!overlaps) {
          out.add(new SlotDTO(
              OffsetDateTime.ofInstant(s, ZoneOffset.UTC),
              OffsetDateTime.ofInstant(e, ZoneOffset.UTC)
          ));
        }
      }
    }
    return out;
  }
}
