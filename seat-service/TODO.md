# TicketFlow Integration TODO

## Current status

- The seat-service implementation for US-06 (`HoldSeat`) and US-07 (`WatchAvailability`) is on `feature/seat-grpc`.
- The service README and gRPC contract are available in [README.md](README.md) and [seat.proto](src/main/proto/seat.proto).
- The focused seat-service unit tests pass. A clean Maven build has been unreliable on the current Windows setup, and Docker was unavailable for local integration testing.

## Team follow-ups

- [x] **Choose one PostgreSQL Compose configuration.** (Done at merge: PostgreSQL 16, `ticketflow/ticketflow`, `docker/init.sql`.) The seat branch uses PostgreSQL 16, `postgres/change_me`, and `docker/init.sql`; the Spectateur branch uses PostgreSQL 17, `ticketflow/ticketflow`, and `docker/init-db.sql`. Both SQL scripts create `ticket_db`, `seat_db`, and `bank_db`. Align the environment variables and keep one Compose/init-script setup.
- [ ] **Locate or add the Dockerfile(s).** None were found in the repository branches or their history. For container networking, the seat service listens on gRPC `9090` and HTTP `8082`; containers should connect to PostgreSQL via `postgres:5432`, not `localhost`.
- [ ] **Integrate ticket-service with gRPC.** Generate a client from the shared [seat.proto](src/main/proto/seat.proto), call `HoldSeat` directly (not through the HTTP gateway), configure a deadline, and handle `held: false` and gRPC errors. Use `seat-service:9090` between containers and `localhost:9090` for local runs.
- [ ] **Agree on hold failure behavior.** A successful hold stays `HELD` indefinitely; there is currently no expiry, confirmation, or release RPC. Confirm whether that is sufficient for the purchase/payment stories. Add release or expiry behavior only if the team requires it.
- [ ] **Prepare event and seat data.** There is no event/seat creation API yet. Seed `events` and `seats` in `seat_db` so the gRPC methods can be exercised.
- [ ] **Run integration verification.** Start PostgreSQL and both services, verify a hold and availability stream end to end, and add a PostgreSQL-backed test for concurrent holds. Run a clean build in the agreed Java 17 environment.
- [ ] **Review and merge** `feature/seat-grpc` into `develop` after the integration configuration and tests are agreed.
