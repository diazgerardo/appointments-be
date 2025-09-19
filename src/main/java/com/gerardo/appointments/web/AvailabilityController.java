// src/main/java/com/gerardo/app/web/AvailabilityController.java
package com.gerardo.appointments.web;

import com.gerardo.appointments.domain.AvailabilityBlock;
import com.gerardo.appointments.dto.BlockDTO;
import com.gerardo.appointments.dto.SlotDTO;
import com.gerardo.appointments.service.AvailabilityService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/professionals/{proId}")
public class AvailabilityController {
  private final AvailabilityService svc;
  public AvailabilityController(AvailabilityService s){ this.svc=s; }

  @PostMapping("/availability/blocks")
  public BlockDTO addOpenBlock(@PathVariable String proId, @RequestBody BlockDTO req){
    AvailabilityBlock b = svc.addOpenBlock(proId, req.getStartTs(), req.getEndTs(), req.getReason());
    var dto = new BlockDTO();
    dto.setId(b.getId()); dto.setProfessionalId(b.getProfessionalId());
    dto.setStartTs(OffsetDateTime.ofInstant(b.getStartTs(), java.time.ZoneOffset.UTC));
    dto.setEndTs(OffsetDateTime.ofInstant(b.getEndTs(), java.time.ZoneOffset.UTC));
    dto.setReason(b.getReason()); dto.setOpen(true);
    // dentro de addOpenBlock(...)
    var lt = req.getLocationType() == null ? com.gerardo.appointments.domain.LocationType.HOSPITAL : req.getLocationType();
    b.setLocationType(lt);
    return dto;
  }

  @DeleteMapping("/availability/blocks/{blockId}")
  public void removeBlock(@PathVariable String blockId){ svc.removeBlock(blockId); }

  @GetMapping("/slots")
  public List<SlotDTO> slots(@PathVariable String proId,
      @RequestParam @DateTimeFormat(iso= DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
      @RequestParam @DateTimeFormat(iso= DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to,
      @RequestParam(defaultValue="30") int slotMinutes){
    return svc.computeSlots(proId, from, to, Duration.ofMinutes(slotMinutes));
  }
}
