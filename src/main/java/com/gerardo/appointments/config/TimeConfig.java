package com.gerardo.appointments.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
  @Value("${app.tz-availability:America/Argentina/Buenos_Aires}")
  public String availabilityZone;
}