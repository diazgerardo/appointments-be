// src/main/java/com/gerardo/app/domain/OutboxEvent.java
package com.gerardo.appointments.domain;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document("outbox_events")
public class OutboxEvent {
  @Id private String id;
  private String type;
  private Long aggregateId;
  private String payloadJson;
  private Instant createdAt = Instant.now();
  private Instant processedAt;

  public String getId() { return id; }
  public void setId(String id) { this.id = id; }
  public String getType() { return type; }
  public void setType(String type) { this.type = type; }
  public Long getAggregateId() { return aggregateId; }
  public void setAggregateId(Long aggregateId) { this.aggregateId = aggregateId; }
  public String getPayloadJson() { return payloadJson; }
  public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
  public Instant getProcessedAt() { return processedAt; }
  public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}
