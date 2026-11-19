package com.ticketflow.ticket_service.repository;

import com.ticketflow.ticket_service.entity.Spectateur;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpectateurRepository extends JpaRepository<Spectateur, Long> {
    boolean existsByEmail(String email);
}
