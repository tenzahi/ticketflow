package com.ticketflow.ticket_service.controller;

import com.ticketflow.ticket_service.entity.Spectateur;
import com.ticketflow.ticket_service.service.SpectateurService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/spectateurs")
public class SpectateurController {

    private final SpectateurService service;

    public SpectateurController(SpectateurService service) {
        this.service = service;
    }

    @GetMapping
    public List<Spectateur> getAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public Spectateur getById(@PathVariable("id") Long id) {
        return service.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Spectateur create(@Valid @RequestBody Spectateur spectateur) {
        return service.create(spectateur);
    }

    @PutMapping("/{id}")
    public Spectateur update(@PathVariable("id") Long id, @Valid @RequestBody Spectateur spectateur) {
        return service.update(id, spectateur);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable("id") Long id) {
        service.delete(id);
    }
}
