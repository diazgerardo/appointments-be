package com.gerardo.appointments.repo;

import com.gerardo.appointments.domain.Appointment;
import com.gerardo.appointments.domain.AppointmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AppointmentRepo extends MongoRepository<Appointment, String> {

  // scheduled appointments overlapping a window
  List<Appointment> findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
      Long professionalId, AppointmentStatus status, Instant from, Instant to);

  // quick existence/overlap check for booking
  boolean existsByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
      Long professionalId, AppointmentStatus status, Instant start, Instant end);
}
