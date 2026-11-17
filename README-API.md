# API REST Codrive

L’API locale est accessible à l’adresse `http://127.0.0.1:8081/api`. Les requêtes avec un corps utilisent le JSON.

## Authentification

| Méthode et route | Usage |
| --- | --- |
| `POST /auth/login` | Connexion avec `email` et `motDePasse`. |
| `POST /auth/register` | Inscription avec `prenom`, `nom`, `email`, `telephone`, `motDePasse` (8 à 255 caractères) et `role` (`PASSAGER` ou `CHAUFFEUR`). |
| `GET /auth/me` | Profil associé à la session actuelle. |
| `POST /auth/logout` | Révocation de la session actuelle. |

Envoyez les identifiants dans le corps JSON.

```json
{
  "email": "sami@mail.com",
  "motDePasse": "abcd"
}
```

La connexion et l’inscription renvoient le profil, un champ `token` et une date d’expiration `expiresAt`. La durée par défaut est de huit heures. Ajoutez cet en-tête aux appels protégés.

```http
Authorization: Bearer <token>
```

## Trajets, réservations et notifications

| Méthode et route | Accès et effet |
| --- | --- |
| `GET /trajets` | Recherche publique des trajets à venir ouverts à la réservation. Filtres facultatifs `depart` et `arrivee`. |
| `GET /trajets/mine` | Trajets du chauffeur connecté. |
| `POST /trajets` | Publication d’un trajet par le chauffeur. |
| `POST /trajets/{id}/cancel` | Annulation d’un trajet par son chauffeur. |
| `GET /vehicules/mine`, `POST /vehicules` | Liste et ajout de véhicules du chauffeur connecté. |
| `POST /reservations` | Demande du passager connecté. Elle immobilise les places et autorise le paiement simulé. |
| `GET /reservations/mine` | Réservations du passager connecté. |
| `GET /reservations/chauffeur` | Réservations reçues sur les trajets du chauffeur connecté. |
| `POST /reservations/{id}/accept` | Acceptation par le chauffeur du trajet et capture du paiement simulé. |
| `POST /reservations/{id}/cancel-by-passenger` | Annulation par le passager qui a réservé. |
| `POST /reservations/{id}/evaluate` | Une évaluation par réservation confirmée, avec une `note` de 1 à 5 et un `commentaire`. |
| `GET /notifications` | Notifications du compte connecté. |

Le corps JSON d’une réservation indique le trajet, le nombre de places et le moyen de paiement.

```json
{
  "idTrajet": 1,
  "nbPlaces": 1,
  "moyenPaiement": "CARTE"
}
```

Remplacez `idTrajet` par un identifiant renvoyé par la recherche. Les moyens de paiement simulés sont `CARTE`, `PAYPAL` et `ESPECES`. Le compte de la session détermine l’identité du passager.

Une réservation commence en `EN_ATTENTE`, devient `CONFIRMEE` après acceptation et passe à `ANNULEE` après annulation. Tant que le départ est dans le futur, une annulation par le passager libère la place. Elle libère l’autorisation en attente ou rembourse un paiement capturé à hauteur de 100 % à plus de 24 h du départ et de 50 % à 24 h ou moins. Une annulation de trajet libère les autorisations et rembourse les paiements capturés. À moins de 24 h du départ, elle applique également une compensation simulée de 20 % aux réservations actives.

Les réponses de liste donnent les champs `canAccept`, `canCancel` et `canEvaluate` utilisés par les tableaux de bord.

## Administration

| Méthode et route | Usage |
| --- | --- |
| `GET /admin/users` | Profils et statuts des comptes. |
| `POST /admin/users/{id}/suspend` | Suspension d’un compte. |
| `POST /admin/users/{id}/block` | Blocage d’un compte. |
| `POST /admin/users/{id}/reactivate` | Réactivation d’un compte. |
| `GET /admin/trajets` | Consultation des trajets. |
| `POST /admin/trajets/{id}/cancel` | Annulation administrative d’un trajet. |

## Réponses d’erreur

Les erreurs de l’API contiennent un champ `message`. Une entrée invalide renvoie `400`. Un jeton absent, expiré ou révoqué renvoie `401`. Une action interdite pour le compte renvoie `403`. Une ressource de réservation introuvable renvoie `404`. Un conflit métier, par exemple une demande en double ou des places épuisées, renvoie `409`.
