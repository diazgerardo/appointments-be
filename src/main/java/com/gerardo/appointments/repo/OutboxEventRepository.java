// src/main/java/com/gerardo/app/repo/OutboxEventRepository.java
package com.gerardo.appointments.repo;

import com.gerardo.appointments.domain.OutboxEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface OutboxEventRepository extends MongoRepository<OutboxEvent, String> {
  List<OutboxEvent> findByProcessedAtIsNullAndType(String type);
}
