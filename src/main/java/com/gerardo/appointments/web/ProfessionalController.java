// ProfessionalController.java (CRUD)
package com.gerardo.appointments.web;

import com.gerardo.appointments.domain.Professional;
import com.gerardo.appointments.repo.ProfessionalRepo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/professionals") @RequiredArgsConstructor
public class ProfessionalController {
  private final ProfessionalRepo repo;
  @GetMapping public List<Professional> all(){ return repo.findAll(); }
  @PostMapping public Professional create(@RequestBody Professional p){ return repo.save(p); }
  @GetMapping("/{id}") public Professional one(@PathVariable String id){ return repo.findById(id).orElseThrow(); }
  @PutMapping("/{id}") public Professional up(@PathVariable String id, @RequestBody Professional p){ p.setId(id); return repo.save(p); }
  @DeleteMapping("/{id}") public void del(@PathVariable String id){ repo.deleteById(id); }
}
