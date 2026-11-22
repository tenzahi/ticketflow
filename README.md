# TicketFlow

Plateforme de billetterie d'événements en architecture microservices (mini-projet SOA et Services Web, 2026).

## Description
Des spectateurs achètent des billets de concerts en ligne. Une place ne doit
être vendue qu'une seule fois. Chaque achat est payé via une passerelle SOAP
d'une banque partenaire (simulée).

## Architecture

```
Client web / mobile
        │ HTTP / JSON
        ▼
  api-gateway :8080  ── /api/v1/** ──►  ticket-service :8081 (REST)
                                           │            │
                                     gRPC  │            │  SOAP
                                           ▼            ▼
                         seat-service :9090 (gRPC)   bank-service :8083 (SOAP)
                                   │                         │
                                seat_db                   bank_db
                                                (ticket-service → ticket_db)
```

| Service | Style | Rôle | Ports | Base |
|---|---|---|---|---|
| api-gateway | Spring Cloud Gateway | Point d'entrée unique, routage `/api/v1`, santé des services | 8080 | – |
| ticket-service | REST `/api/v1` + OpenAPI | Spectateurs, billets, orchestration de l'achat | 8081 | `ticket_db` |
| seat-service | gRPC (unaire + streaming) | Événements, places, blocage (`HoldSeat`), disponibilité en direct (`WatchAvailability`) | 8082 (HTTP/actuator), 9090 (gRPC) | `seat_db` |
| bank-service | SOAP contract-first (XSD → WSDL) | `AuthorizePayment`, `RefundPayment`, SOAP Fault | 8083 | `bank_db` |

## Contrats d'API
- **REST / OpenAPI** : [`docs/openapi-spectateurs.yaml`](docs/openapi-spectateurs.yaml) ; Swagger UI du ticket-service : `http://localhost:8081/swagger-ui.html` (JSON : `/v3/api-docs`).
- **gRPC** : [`seat-service/src/main/proto/seat.proto`](seat-service/src/main/proto/seat.proto) (voir aussi [`seat-service/README.md`](seat-service/README.md)).
- **SOAP** : [`bank-service/src/main/resources/xsd/bank.xsd`](bank-service/src/main/resources/xsd/bank.xsd) ; WSDL généré : `http://localhost:8083/ws/bank.wsdl`.

## Configuration
La configuration est externalisée par variables d'environnement (voir [`.env.example`](.env.example)) :

| Variable | Utilisée par | Valeur locale par défaut |
|---|---|---|
| `TICKET_DB_URL` / `_USER` / `_PASSWORD` | ticket-service | `jdbc:postgresql://localhost:5433/ticket_db`, `ticketflow` / `ticketflow` |
| `SEAT_DB_URL` / `_USER` / `_PASSWORD` | seat-service | `jdbc:postgresql://localhost:5433/seat_db`, `ticketflow` / `ticketflow` |
| `BANK_DB_URL` / `_USER` / `_PASSWORD` | bank-service | `jdbc:postgresql://localhost:5433/bank_db` (datasource désactivée pour l'instant) |
| `TICKET_SERVICE_URL`, `SEAT_SERVICE_URL`, `BANK_SERVICE_URL` | api-gateway | `http://localhost:8081`, `:8082`, `:8083` |

Routes du gateway (`api-gateway/src/main/resources/application.properties`) :
- `/api/v1/**` → ticket-service
- `/health/ticket`, `/health/seat`, `/health/bank` → `/actuator/health` de chaque service

## Lancer le projet en local
Prérequis : Java 17+, Docker.

```bash
# 1. PostgreSQL (crée ticket_db, seat_db, bank_db)
docker compose up -d

# 2. Variables d'environnement
cp .env.example .env   # puis les exporter dans le terminal / l'IDE

# 3. Chaque service, dans un terminal séparé
cd ticket-service && ./mvnw spring-boot:run
cd seat-service   && ./mvnw spring-boot:run
cd bank-service   && ./mvnw spring-boot:run
cd api-gateway    && ./mvnw spring-boot:run
```

Test rapide : `curl http://localhost:8080/api/v1/spectateurs` puis `curl http://localhost:8080/health/seat`.

## Documentation
- Plan des sprints et user stories : [`docs/TicketFlow_Sprints_UserStories.docx`](docs/TicketFlow_Sprints_UserStories.docx)
- Suivi de l'intégration du seat-service : [`seat-service/TODO.md`](seat-service/TODO.md)

## Équipe
- Tenzahi
- Rim
- Ghoufrane
- jassem
