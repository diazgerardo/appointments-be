package com.gerardo.appointments.repo;

import com.gerardo.appointments.domain.Appointment;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AppointmentRepo extends MongoRepository<Appointment, String> {
  List<Appointment> findByProfessionalIdAndStartUtcLessThanAndEndUtcGreaterThan(
          String professionalId, Instant endExclusive, Instant startExclusive);
  List<Appointment> findByProfessionalIdAndStartUtcBetween(String professionalId, Instant from, Instant to);
  List<Appointment> findByPatientIdOrderByStartUtcDesc(String patientId);
}
