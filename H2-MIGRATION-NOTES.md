# Persistance H2

Codrive utilise Spring Data JPA et H2 pour conserver les comptes, véhicules, trajets, réservations, paiements simulés, évaluations et notifications. En configuration standard, H2 stocke ces données dans `data/covoiturage_db.mv.db` sous le dossier de lancement.

Les bases existantes peuvent être réutilisées. L’application met à jour le schéma avec Hibernate. Lorsqu’un utilisateur se connecte avec succès à un ancien compte dont le mot de passe était stocké en clair, le service remplace cette valeur par un hachage salé. Les sessions sont conservées en mémoire et nécessitent une nouvelle connexion après le redémarrage de l’API.

La disponibilité d’un trajet est calculée à partir de ses réservations actives, y compris pour les trajets déjà enregistrés. Les avis existants restent visibles. Ils ont été enregistrés sans référence à une réservation. La limite d’un avis par réservation s’applique aux évaluations enregistrées à partir de cette version.

## Inspecter la base locale

La console est désactivée par défaut. Ces commandes l’activent pour une session de développement dans PowerShell.

```powershell
$env:SPRING_H2_CONSOLE_ENABLED = "true"
mvn spring-boot:run
```

Ouvrez [la console H2 locale](http://127.0.0.1:8081/h2-console) avec l’URL JDBC `jdbc:h2:file:./data/covoiturage_db`, l’utilisateur `sa` et un mot de passe vide. Pour inspecter une base configurée via `SPRING_DATASOURCE_URL`, renseignez la même URL dans la console.
