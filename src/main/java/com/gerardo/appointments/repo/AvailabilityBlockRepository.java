// src/main/java/com/gerardo/app/repo/AvailabilityBlockRepository.java
package com.gerardo.appointments.repo;

import com.gerardo.appointments.domain.AvailabilityBlock;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AvailabilityBlockRepository extends MongoRepository<AvailabilityBlock, String> {
  List<AvailabilityBlock> findByProfessionalId(String proId);

  // overlap: end > from AND start < to
  List<AvailabilityBlock> findByProfessionalIdAndEndTsAfterAndStartTsBefore(
      String proId, Instant from, Instant to);
}
