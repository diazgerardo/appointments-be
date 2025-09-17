// src/main/java/com/gerardo/app/repo/AppointmentRepository.java
package com.gerardo.appointments.repo;

import com.gerardo.appointments.domain.Appointment;
import com.gerardo.appointments.domain.AppointmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AppointmentRepository extends MongoRepository<Appointment, String> {
  List<Appointment> findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
      String proId, AppointmentStatus status, Instant from, Instant to);

  boolean existsByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
      String proId, AppointmentStatus status, Instant start, Instant end);
}
