// src/main/java/com/gerardo/app/web/NotificationsController.java
package com.gerardo.appointments.web;

import com.gerardo.appointments.service.NotificationHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/notifications")
public class NotificationsController {
  @GetMapping(path="/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
  public SseEmitter stream(@RequestParam Long patientId){
    return NotificationHub.subscribe(patientId);
  }
}
