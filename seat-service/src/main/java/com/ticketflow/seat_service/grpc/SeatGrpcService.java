package com.ticketflow.seat_service.grpc;

import com.ticketflow.seat_service.exception.EventNotFoundException;
import com.ticketflow.seat_service.exception.SeatNotFoundException;
import com.ticketflow.seat_service.service.SeatAvailabilityService;
import com.ticketflow.seat_service.service.SeatReservationService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;

@GrpcService
public class SeatGrpcService extends SeatServiceGrpc.SeatServiceImplBase {

    private final SeatReservationService seatReservationService;
    private final SeatAvailabilityService seatAvailabilityService;

    public SeatGrpcService(
            SeatReservationService seatReservationService,
            SeatAvailabilityService seatAvailabilityService) {
        this.seatReservationService = seatReservationService;
        this.seatAvailabilityService = seatAvailabilityService;
    }

    @Override
    public void holdSeat(HoldSeatRequest request, StreamObserver<HoldSeatResponse> responseObserver) {
        if (request.getEventId() <= 0 || request.getSeatId() <= 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Event and seat IDs must be positive")
                    .asRuntimeException());
            return;
        }

        try {
            boolean held = seatReservationService.holdSeat(request.getEventId(), request.getSeatId());
            responseObserver.onNext(HoldSeatResponse.newBuilder().setHeld(held).build());
            responseObserver.onCompleted();
        } catch (SeatNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void watchAvailability(
            WatchAvailabilityRequest request, StreamObserver<AvailabilityUpdate> responseObserver) {
        if (request.getEventId() <= 0) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Event ID must be positive")
                    .asRuntimeException());
            return;
        }

        try {
            seatAvailabilityService.watchAvailability(request.getEventId(), responseObserver);
        } catch (EventNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }
}
