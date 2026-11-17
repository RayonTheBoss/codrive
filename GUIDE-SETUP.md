# Lancer Codrive en local

## Prérequis

- JDK 17 et Maven 3.6.3 ou ultérieur
- Un navigateur moderne

Vérifiez la version Java utilisée par Maven avec `mvn -v`.

## Démarrage

Sous Windows, ouvrez `DEMARRER_PROJET.bat` depuis le dossier du projet. Le lanceur active les données de démonstration, attend la réponse de l’API, puis ouvre la page de connexion.

Ces commandes démarrent l’API et activent les données de démonstration dans PowerShell.

```powershell
$env:CODRIVE_DEMO_DATA = "true"
mvn spring-boot:run
```

Ouvrez ensuite `login.html` dans votre navigateur. Le frontend utilise l’API locale sur le port `8081`. Pour démarrer sans créer de comptes de démonstration, lancez `mvn spring-boot:run` avec `CODRIVE_DEMO_DATA` désactivée ou absente.

## Comptes de démonstration

| Profil | Email | Mot de passe |
| --- | --- | --- |
| Passager | `sami@mail.com` | `abcd` |
| Chauffeur | `ali@mail.com` | `1234` |
| Administrateur | `admin@mail.com` | `admin123` |

Les données de démonstration sont créées au premier lancement sur une base vide quand l’option est activée. Si les trajets d’exemple sont déjà passés, connectez-vous comme chauffeur et publiez un trajet avec une nouvelle date de départ. Consultez les [notes H2](H2-MIGRATION-NOTES.md) pour les détails sur la persistance.

## Parcours à vérifier

1. **Passager.** Réservez une place sur un trajet futur. La réservation apparaît dans **Mes réservations** avec le statut **En attente**.
2. **Chauffeur.** Connectez-vous avec le compte du chauffeur du trajet, puis acceptez la réservation dans **Réservations reçues**.
3. **Passager.** Vérifiez que la réservation est confirmée. Annulez-la si vous voulez vérifier que la place redevient disponible. Pour vérifier une évaluation, utilisez une autre réservation confirmée.
4. **Administrateur.** Vérifiez que les utilisateurs et trajets sont visibles. La suspension d’un compte bloque son accès aux routes protégées.
5. **Persistance.** Redémarrez l’API et reconnectez-vous pour retrouver les trajets et réservations enregistrés.

Les paiements et les canaux de notification sont simulés. Exécutez `mvn test` pour lancer les tests automatisés.
