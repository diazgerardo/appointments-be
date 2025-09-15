// ProfessionalRepo.java
package com.gerardo.appointments.repo;
import com.gerardo.appointments.domain.Professional;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface ProfessionalRepo extends MongoRepository<Professional, String> {}
