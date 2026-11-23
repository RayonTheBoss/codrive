# Codrive

Codrive is a carpooling demo for passengers, drivers, and administrators. Each role has its own dashboard in a plain HTML, CSS, and JavaScript frontend. A Java 17 Spring Boot REST API uses Spring Data JPA and a local H2 database.

## Run the demo

From the project folder on Windows, run `DEMARRER_PROJET.bat`. It enables sample data, starts the API on port `8081`, and opens the login page. For a manual launch and the sample accounts, see the [setup guide](GUIDE-SETUP.md).

Use the menu in the top-right corner to switch between French and English. To open the English sign-in page directly, use `login.html?lang=en`.

1. Sign in as a passenger and reserve a seat on a sample trip.
2. In a separate tab, sign in as the driver and accept the reservation.
3. Return to the passenger dashboard and check the updated status. Use the admin dashboard to manage accounts and trips.

## Architecture

- **Authentication.** `AuthService` verifies credentials at sign-in. Protected endpoints use `RequestAuthHelper` to validate the browser's bearer token, check the account's current status, and enforce the required role. Passwords are hashed with salted PBKDF2. Signing out revokes the token.
- **Booking.** A database lock on each trip serializes competing booking requests. When two passengers request the final seat, the second request checks availability after the first transaction commits. Reservation, simulated payment, and notification changes are grouped in one transaction.
- **Frontend.** The shared JavaScript handles authenticated requests and common dashboard behavior. Each role still has its own HTML page and actions. The dashboard uses eligibility flags from the API to decide which reservation buttons to show.

Sessions expire after eight hours by default and live in memory on one API instance, so an API restart signs users out. H2 persists accounts, trips, reservations, and other domain data across restarts. Payment actions and notification channels are simulated in the app.

## Verify

```text
mvn test
```

See the [API reference](README-API.md) for request examples and the [H2 notes](H2-MIGRATION-NOTES.md) for existing database files.
