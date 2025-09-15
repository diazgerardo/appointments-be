package com.gerardo.appointments.domain;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public abstract class Person {
  @Id
  private String id;

  @Field("fullName")
  private String fullName;

  private String email;
  private String phone;
}
