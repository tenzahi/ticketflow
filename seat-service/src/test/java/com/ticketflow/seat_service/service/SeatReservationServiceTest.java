package com.ticketflow.seat_service.service;

import com.ticketflow.seat_service.entity.SeatStatus;
import com.ticketflow.seat_service.exception.SeatNotFoundException;
import com.ticketflow.seat_service.repository.SeatRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeatReservationServiceTest {

    private static final Long EVENT_ID = 10L;
    private static final Long SEAT_ID = 20L;

    @Mock
    private SeatRepository seatRepository;

    private SeatReservationService service;

    @BeforeEach
    void setUp() {
        service = new SeatReservationService(seatRepository);
    }

    @Test
    void holdsAvailableSeat() {
        when(seatRepository.holdIfAvailable(
                EVENT_ID, SEAT_ID, SeatStatus.AVAILABLE, SeatStatus.HELD)).thenReturn(1);

        assertTrue(service.holdSeat(EVENT_ID, SEAT_ID));
        verify(seatRepository).holdIfAvailable(
                EVENT_ID, SEAT_ID, SeatStatus.AVAILABLE, SeatStatus.HELD);
    }

    @Test
    void returnsFalseWhenSeatIsAlreadyHeld() {
        when(seatRepository.holdIfAvailable(
                EVENT_ID, SEAT_ID, SeatStatus.AVAILABLE, SeatStatus.HELD)).thenReturn(0);
        when(seatRepository.existsByIdAndEvent_Id(SEAT_ID, EVENT_ID)).thenReturn(true);

        assertFalse(service.holdSeat(EVENT_ID, SEAT_ID));
    }

    @Test
    void throwsWhenSeatDoesNotBelongToEvent() {
        when(seatRepository.holdIfAvailable(
                EVENT_ID, SEAT_ID, SeatStatus.AVAILABLE, SeatStatus.HELD)).thenReturn(0);
        when(seatRepository.existsByIdAndEvent_Id(SEAT_ID, EVENT_ID)).thenReturn(false);

        assertThrows(SeatNotFoundException.class, () -> service.holdSeat(EVENT_ID, SEAT_ID));
    }

    @Test
    void concurrentRequestsAllowOnlyOneHold() throws Exception {
        AtomicBoolean claimed = new AtomicBoolean();
        when(seatRepository.holdIfAvailable(
                EVENT_ID, SEAT_ID, SeatStatus.AVAILABLE, SeatStatus.HELD))
                .thenAnswer(invocation -> claimed.compareAndSet(false, true) ? 1 : 0);
        when(seatRepository.existsByIdAndEvent_Id(SEAT_ID, EVENT_ID)).thenReturn(true);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            var first = executor.submit(() -> attemptHold(ready, start));
            var second = executor.submit(() -> attemptHold(ready, start));

            assertTrue(ready.await(5, TimeUnit.SECONDS));
            start.countDown();
            assertEquals(1, (first.get(5, TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(5, TimeUnit.SECONDS) ? 1 : 0));
        } finally {
            executor.shutdownNow();
        }
    }

    private boolean attemptHold(CountDownLatch ready, CountDownLatch start) throws InterruptedException {
        ready.countDown();
        assertTrue(start.await(5, TimeUnit.SECONDS));
        return service.holdSeat(EVENT_ID, SEAT_ID);
    }
}
