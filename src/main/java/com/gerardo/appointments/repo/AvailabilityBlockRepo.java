package com.gerardo.appointments.repo;

import com.gerardo.appointments.domain.AvailabilityBlock;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface AvailabilityBlockRepo extends MongoRepository<AvailabilityBlock, String> {

  List<AvailabilityBlock> findByProfessionalId(Long professionalId);

  // windows overlapping a range
  List<AvailabilityBlock> findByProfessionalIdAndEndTsAfterAndStartTsBefore(
      Long professionalId, Instant from, Instant to);
}
