# Seat Service

The Seat Service is TicketFlow's gRPC service for holding seats and streaming seat availability. It owns event and seat data in the `seat_db` PostgreSQL database.

## What it does

The service provides two gRPC methods:

| Method | Type | Purpose |
|---|---|---|
| `HoldSeat` | Unary | Hold an available seat for an event |
| `WatchAvailability` | Server streaming | Send the current seat statuses, then stream changes |

The service does not currently expose REST endpoints for creating events or seats. Those records must be inserted into `seat_db` by setup scripts or another approved mechanism.

## How a seat hold works

1. A client calls `HoldSeat` with an `event_id` and `seat_id`.
2. The service validates that both IDs are positive.
3. The database conditionally changes the seat from `AVAILABLE` to `HELD`.
4. If the update changes one row, `held` is `true` and availability subscribers receive a `HELD` update.
5. If the seat exists but is already held, `held` is `false`.
6. If the seat does not belong to the requested event, the RPC returns `NOT_FOUND`.

The conditional database update ensures competing requests cannot both hold the same available seat.

**Current scope:** a hold does not expire, and there are no confirmation, release, or payment RPCs. A seat remains `HELD` until another process changes its database status. Treat `HELD` as unavailable.

## Data model

### Event

| Field | Type | Meaning |
|---|---|---|
| `id` | `Long` | Database-generated event ID |
| `name` | `String` | Event name |

### Seat

| Field | Type | Meaning |
|---|---|---|
| `id` | `Long` | Database-generated seat ID |
| `event` | `Event` | Owning event |
| `seatLabel` | `String` | Seat label, unique within its event |
| `price` | `BigDecimal` | Seat price, stored with two decimal places |
| `status` | `SeatStatus` | `AVAILABLE` or `HELD`; defaults to `AVAILABLE` |

The database enforces uniqueness of `(event_id, seat_label)`. JPA creates or updates the tables using `spring.jpa.hibernate.ddl-auto=update`.

There is no event or seat creation RPC yet. To add sample data, start the service once so JPA creates the tables, then open the seat database:

```powershell
docker exec -it ticketflow-postgres psql -U postgres -d seat_db
```

In the `psql` prompt, insert an event and seats. Use the event ID returned by the first statement:

```sql
INSERT INTO events (name) VALUES ('Demo concert') RETURNING id;
INSERT INTO seats (event_id, seat_label, price, status)
VALUES (1, 'A1', 50.00, 'AVAILABLE'),
       (1, 'A2', 50.00, 'AVAILABLE');
```

## gRPC contract

The contract is in [`src/main/proto/seat.proto`](src/main/proto/seat.proto). Maven generates the Java message and service classes during the build; generated files are under `target/` and should not be edited or committed.

### `HoldSeat`

Request:

```proto
message HoldSeatRequest {
  int64 event_id = 1;
  int64 seat_id = 2;
}
```

Response:

```proto
message HoldSeatResponse {
  bool held = 1;
}
```

| Situation | Result |
|---|---|
| Seat was available and is now held | `held: true` |
| Seat exists but is already held | `held: false` |
| Either ID is zero or negative | gRPC `INVALID_ARGUMENT` |
| Seat is not part of the specified event | gRPC `NOT_FOUND` |

### `WatchAvailability`

The client sends an event ID. The server first sends one `AvailabilityUpdate` per seat in that event, then keeps the stream open and sends an update when a seat is held.

```proto
message AvailabilityUpdate {
  int64 event_id = 1;
  int64 seat_id = 2;
  string seat_label = 3;
  SeatStatus status = 4;
}
```

Statuses are `SEAT_STATUS_AVAILABLE` and `SEAT_STATUS_HELD`. A non-positive event ID returns `INVALID_ARGUMENT`; an unknown event returns `NOT_FOUND`. Disconnecting the client removes its stream subscription.

The live subscriber registry is in memory and local to this service instance. It is not a cross-instance message broker; run one instance if clients need to receive all updates.

## Configuration

The service requires these environment variables:

| Variable | Example |
|---|---|
| `SEAT_DB_URL` | `jdbc:postgresql://localhost:5433/seat_db` |
| `SEAT_DB_USER` | `postgres` |
| `SEAT_DB_PASSWORD` | `change_me` |

Ports and application settings:

| Setting | Value |
|---|---|
| HTTP / Actuator | `8082` |
| gRPC | `9090` |
| Health endpoint | `http://localhost:8082/actuator/health` |

Only the Actuator `health` and `info` endpoints are exposed.

## Run locally (Windows PowerShell)

From the repository root, start the shared PostgreSQL container:

```powershell
docker compose up -d postgres
```

The repository's database initialization script creates `seat_db`. Set the service database settings in the same PowerShell session, then run the service:

```powershell
$env:SEAT_DB_URL = "jdbc:postgresql://localhost:5433/seat_db"
$env:SEAT_DB_USER = "postgres"
$env:SEAT_DB_PASSWORD = "change_me"

Set-Location .\seat-service
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

Java 17 is the configured target. If PostgreSQL is not running through Docker Compose, provide equivalent environment values for a local PostgreSQL database.

## Try the RPCs with `grpcurl`

Run these commands from the repository root after starting the service. They pass the proto explicitly, so server reflection is not required.

Hold seat `1` for event `1`:

```powershell
grpcurl -plaintext `
  -import-path .\seat-service\src\main\proto `
  -proto seat.proto `
  -d '{"eventId":1,"seatId":1}' `
  localhost:9090 ticketflow.seat.v1.SeatService/HoldSeat
```

Watch availability for event `1`:

```powershell
grpcurl -plaintext `
  -import-path .\seat-service\src\main\proto `
  -proto seat.proto `
  -d '{"eventId":1}' `
  localhost:9090 ticketflow.seat.v1.SeatService/WatchAvailability
```

The watch command remains connected while the stream is active. Open another terminal and hold a seat to see an update.

## Tests

The focused unit tests cover successful and repeated holds, missing-seat and invalid-ID errors, concurrent hold attempts at the service boundary, initial availability snapshots, live updates, and stream cancellation.

```powershell
.\mvnw.cmd -Dtest=SeatReservationServiceTest,SeatGrpcServiceTest,SeatAvailabilityServiceTest test
```

These are unit tests; they do not replace a PostgreSQL-backed integration test of the JPA update.
