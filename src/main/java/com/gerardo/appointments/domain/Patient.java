package com.gerardo.appointments.domain;

import lombok.*;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Document("patients")
public class Patient extends Person {
  private String diagnostic;    // free text
  private String prescription;  // free text
  private List<String> requests;
}
