package com.ticketflow.ticket_service.service;

import com.ticketflow.ticket_service.entity.Spectateur;
import com.ticketflow.ticket_service.repository.SpectateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class SpectateurService {

    private final SpectateurRepository repository;

    public SpectateurService(SpectateurRepository repository) {
        this.repository = repository;
    }

    public List<Spectateur> findAll() {
        return repository.findAll();
    }

    public Spectateur findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Spectateur introuvable : " + id));
    }

    public Spectateur create(Spectateur spectateur) {
        if (repository.existsByEmail(spectateur.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé : " + spectateur.getEmail());
        }
        spectateur.setId(null);
        return repository.save(spectateur);
    }

    public Spectateur update(Long id, Spectateur data) {
        Spectateur existant = findById(id);
        if (!existant.getEmail().equals(data.getEmail()) && repository.existsByEmail(data.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé : " + data.getEmail());
        }
        existant.setNom(data.getNom());
        existant.setPrenom(data.getPrenom());
        existant.setEmail(data.getEmail());
        return repository.save(existant);
    }

    public void delete(Long id) {
        repository.delete(findById(id));
    }
}
