# TicketFlow

Plateforme de billetterie d'événements en architecture microservices.

## Description
Des spectateurs achètent des billets de concerts en ligne. Une place ne doit
être vendue qu'une seule fois. Chaque achat est payé via une passerelle SOAP
d'une banque partenaire (simulée).

## Services
| Service | Style | Rôle |
|---|---|---|
| api-gateway | Spring Cloud Gateway | Point d'entrée unique |
| ticket-service | REST | Spectateurs, billets, achat |
| seat-service | gRPC | Événements, places, blocage |
| bank-service | SOAP | Paiement et remboursement |

## Équipe
- Tenzahi
- Rim
- Ghofrane
- jassem