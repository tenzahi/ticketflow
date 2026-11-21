package com.ticketflow.seat_service.repository;

import com.ticketflow.seat_service.entity.Event;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EventRepository extends JpaRepository<Event, Long> {
}
