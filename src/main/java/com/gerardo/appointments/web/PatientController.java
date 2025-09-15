// PatientController.java (basic CRUD)
package com.gerardo.appointments.web;

import com.gerardo.appointments.domain.Patient;
import com.gerardo.appointments.repo.PatientRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/patients") @RequiredArgsConstructor
public class PatientController {
  private final PatientRepo repo;
  @GetMapping public List<Patient> all(){ return repo.findAll(); }
  @PostMapping public Patient create(@RequestBody Patient p){ return repo.save(p); }
  @GetMapping("/{id}") public Patient one(@PathVariable String id){ return repo.findById(id).orElseThrow(); }
  @PutMapping("/{id}") public Patient up(@PathVariable String id, @RequestBody Patient p){ p.setId(id); return repo.save(p); }
  @DeleteMapping("/{id}") public void del(@PathVariable String id){ repo.deleteById(id); }
}
