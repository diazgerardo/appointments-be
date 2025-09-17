// src/main/java/com/gerardo/app/service/OutboxPump.java
package com.gerardo.appointments.service;

import com.gerardo.appointments.repo.OutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class OutboxPump {
  private final OutboxEventRepository repo;
  public OutboxPump(OutboxEventRepository r){ this.repo=r; }

  @Scheduled(fixedDelay = 30000)
  public void pumpCancelled(){
    var events = repo.findByProcessedAtIsNullAndType("AppointmentCancelled");
    events.forEach(e -> {
      // TODO: send real email/SMS/WhatsApp here with e.getPayloadJson()
      e.setProcessedAt(Instant.now());
      repo.save(e);
    });
  }
}
