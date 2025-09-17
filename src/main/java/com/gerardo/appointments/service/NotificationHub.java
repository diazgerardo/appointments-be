// src/main/java/com/gerardo/app/service/NotificationHub.java
package com.gerardo.appointments.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class NotificationHub {
  private static final Map<Long, List<SseEmitter>> byPatient = new ConcurrentHashMap<>();

  public static SseEmitter subscribe(Long patientId){
    var emitter = new SseEmitter(0L);
    byPatient.computeIfAbsent(patientId, k -> new ArrayList<>()).add(emitter);
    emitter.onTimeout(emitter::complete);
    emitter.onCompletion(() -> byPatient.getOrDefault(patientId, List.of()).remove(emitter));
    return emitter;
  }

  public static void broadcastToPatient(String patientId, String event, String json){
    var list = byPatient.getOrDefault(patientId, List.of());
    var dead = new ArrayList<SseEmitter>();
    for (var e : list){
      try { e.send(SseEmitter.event().name(event).data(json)); }
      catch (IOException ex){ dead.add(e); }
    }
    list.removeAll(dead);
  }
}
