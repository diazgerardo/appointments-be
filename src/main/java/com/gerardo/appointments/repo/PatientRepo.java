// PatientRepo.java
package com.gerardo.appointments.repo;
import com.gerardo.appointments.domain.Patient;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface PatientRepo extends MongoRepository<Patient, String> {}
