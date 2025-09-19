// src/main/java/com/gerardo/appointments/repo/AppointmentRepo.java
package com.gerardo.appointments.repo;

import com.gerardo.appointments.domain.Appointment;
import com.gerardo.appointments.domain.AppointmentStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AppointmentRepo extends MongoRepository<Appointment, String> {

  // overlap en rango (profesional)
  List<Appointment> findByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
          String professionalId, AppointmentStatus status, Instant from, Instant to);

  // overlap en rango (paciente)
  List<Appointment> findByPatientIdAndStatusAndEndTsAfterAndStartTsBefore(
          String patientId, AppointmentStatus status, Instant from, Instant to);

  // capacidad por slot (profesional)
  long countByProfessionalIdAndStatusAndEndTsAfterAndStartTsBefore(
          String professionalId, AppointmentStatus status, Instant from, Instant to);

  // no solape del paciente
  boolean existsByPatientIdAndStatusAndEndTsAfterAndStartTsBefore(
          String patientId, AppointmentStatus status, Instant from, Instant to);
}
