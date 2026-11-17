package com.covoiturage.service;

import com.covoiturage.enums.StatutReservation;
import com.covoiturage.enums.StatutTrajet;
import com.covoiturage.enums.TypePaiement;
import com.covoiturage.model.Admin;
import com.covoiturage.model.Chauffeur;
import com.covoiturage.model.Passager;
import com.covoiturage.model.Reservation;
import com.covoiturage.model.Trajet;
import com.covoiturage.model.Vehicule;
import com.covoiturage.repository.PaiementRepository;
import com.covoiturage.repository.ReservationRepository;
import com.covoiturage.repository.TrajetRepository;
import com.covoiturage.repository.UserRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:booking-regression;DB_CLOSE_DELAY=-1",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.h2.console.enabled=false"
})
class BookingIntegrationTest {
    @Autowired private ReservationService reservationService;
    @Autowired private TrajetService trajetService;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private TrajetRepository trajetRepository;
    @Autowired private PaiementRepository paiementRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private TransactionTemplate transactions;
    @Autowired private JdbcTemplate jdbc;

    private Chauffeur chauffeur;
    private Passager passager;
    private Passager autrePassager;
    private Admin admin;

    @BeforeEach
    void createAccounts() {
        String prefix = UUID.randomUUID().toString();
        chauffeur = new Chauffeur("Test", "Chauffeur", prefix + "-driver@codrive.test", "inutilise", "12345678");
        chauffeur.ajouterVehicule(new Vehicule("Test", "Car", prefix, 4));
        chauffeur = userRepository.save(chauffeur);
        passager = userRepository.save(new Passager("Test", "Passager", prefix + "-one@codrive.test", "inutilise", "12345678"));
        autrePassager = userRepository.save(new Passager("Test", "Passager", prefix + "-two@codrive.test", "inutilise", "12345678"));
        admin = userRepository.save(new Admin("Test", "Admin", prefix + "-admin@codrive.test", "inutilise", "12345678"));
    }

    @Test
    void twoRequestsForTheLastSeatProduceOneReservation() throws Exception {
        int trajetId = createTrip(1, LocalDateTime.now().plusDays(2));
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(attemptBooking(passager, trajetId, ready, start));
            var second = executor.submit(attemptBooking(autrePassager, trajetId, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            assertThat(List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(HttpStatus.CREATED, HttpStatus.CONFLICT);
        } finally {
            executor.shutdownNow();
        }

        assertThat(reservationService.getReservationsChauffeur(chauffeur.getId())).hasSize(1);
        assertTrip(trajetId, 0, StatutTrajet.COMPLET);
    }

    @Test
    void pendingCancellationReleasesSeatsOnceAndAllowsRebooking() {
        int trajetId = createTrip(2, LocalDateTime.now().plusHours(12));
        Reservation reservation = book(passager, trajetId, 2);
        assertConflict(() -> book(passager, trajetId, 1));
        assertThatThrownBy(() -> reservationService.annulerReservation(reservation.getIdReservation(), autrePassager.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(reservationService.annulerReservation(reservation.getIdReservation(), passager.getId()))
                .contains("Autorisation de paiement libérée");
        assertConflict(() -> reservationService.annulerReservation(reservation.getIdReservation(), passager.getId()));

        Reservation cancelled = reservationRepository.findById(reservation.getIdReservation()).orElseThrow();
        assertThat(cancelled.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(cancelled.getPaiement().isAutorise()).isFalse();
        assertThat(cancelled.getPaiement().isCapture()).isFalse();
        assertThat(cancelled.getPaiement().getMontantRembourse()).isZero();
        assertTrip(trajetId, 2, StatutTrajet.OUVERT);
        assertThat(book(passager, trajetId, 1).getIdReservation()).isNotEqualTo(reservation.getIdReservation());
    }

    @Test
    void capturedCancellationUsesTheFullTimeInterval() {
        int trajetId = createTrip(2, LocalDateTime.now().plusHours(24).plusMinutes(30));
        Reservation reservation = book(passager, trajetId, 1);
        reservationService.confirmerReservation(reservation.getIdReservation(), chauffeur.getId());
        assertConflict(() -> reservationService.confirmerReservation(reservation.getIdReservation(), chauffeur.getId()));

        assertThat(reservationService.annulerReservation(reservation.getIdReservation(), passager.getId()))
                .contains("100%");
        Reservation cancelled = reservationRepository.findById(reservation.getIdReservation()).orElseThrow();
        assertThat(cancelled.getPaiement().isCapture()).isTrue();
        assertThat(cancelled.getPaiement().getMontantRembourse()).isEqualTo(25);
    }

    @Test
    void tripCancellationLeavesPreviouslyCancelledReservationsUntouched() {
        int trajetId = createTrip(3, LocalDateTime.now().plusHours(12));
        Reservation former = book(passager, trajetId, 1);
        reservationService.confirmerReservation(former.getIdReservation(), chauffeur.getId());
        reservationService.annulerReservation(former.getIdReservation(), passager.getId());
        Reservation current = book(autrePassager, trajetId, 2);
        reservationService.confirmerReservation(current.getIdReservation(), chauffeur.getId());
        long notificationCount = notifications(autrePassager.getId());

        assertThat(trajetService.annulerTrajet(trajetId, admin)).contains("20%");
        assertThat(trajetService.annulerTrajet(trajetId, chauffeur)).contains("déjà annulé");
        assertConflict(() -> book(passager, trajetId, 1));

        Reservation cancelled = reservationRepository.findById(current.getIdReservation()).orElseThrow();
        assertThat(cancelled.getStatut()).isEqualTo(StatutReservation.ANNULEE);
        assertThat(cancelled.getPaiement().getMontantCompensation()).isEqualTo(10);
        assertThat(cancelled.getPaiement().getMontantRembourse()).isEqualTo(50);
        Reservation alreadyCancelled = reservationRepository.findById(former.getIdReservation()).orElseThrow();
        assertThat(alreadyCancelled.getPaiement().getMontantRembourse()).isEqualTo(12.5);
        assertThat(alreadyCancelled.getPaiement().getMontantCompensation()).isZero();
        assertThat(notifications(autrePassager.getId())).isEqualTo(notificationCount + 1);
        assertTrip(trajetId, 3, StatutTrajet.ANNULE);
    }

    @Test
    void expiredTripsCannotBeBookedOrCancelled() {
        int trajetId = createTrip(4, LocalDateTime.now().minusHours(1));
        assertConflict(() -> book(passager, trajetId, 1));
        assertConflict(() -> trajetService.annulerTrajet(trajetId, admin));
        assertThat(trajetService.getTrajetsDisponibles())
                .noneMatch(trajet -> trajet.getIdTrajet() == trajetId);
    }

    @Test
    void legacyInventoryCannotSellSeatsAlreadyHeldByAReservation() {
        int trajetId = createTrip(1, LocalDateTime.now().plusDays(2));
        book(passager, trajetId, 1);
        jdbc.update("update trajet set nb_places_disponibles = 3, statut = 'OUVERT' where id_trajet = ?", trajetId);

        assertConflict(() -> book(autrePassager, trajetId, 1));
        assertThat(trajetService.getTrajetsDisponibles()).noneMatch(trajet -> trajet.getIdTrajet() == trajetId);
        assertTrip(trajetId, 0, StatutTrajet.COMPLET);
    }

    @Test
    void bookingRollbackAlsoRollsBackPaymentInventoryAndNotifications() {
        int trajetId = createTrip(4, LocalDateTime.now().plusDays(2));
        long paymentsBefore = paiementRepository.count();
        long notificationsBefore = notifications(chauffeur.getId());

        transactions.executeWithoutResult(transaction -> {
            book(passager, trajetId, 4);
            transaction.setRollbackOnly();
        });

        assertThat(reservationService.getReservationsPassager(passager.getId())).isEmpty();
        assertThat(paiementRepository.count()).isEqualTo(paymentsBefore);
        assertThat(notifications(chauffeur.getId())).isEqualTo(notificationsBefore);
        assertTrip(trajetId, 4, StatutTrajet.OUVERT);
    }

    @Test
    void aConfirmedReservationCanEvaluateItsDriverOnlyOnce() {
        int trajetId = createTrip(4, LocalDateTime.now().plusDays(2));
        int reservationId = book(passager, trajetId, 1).getIdReservation();
        assertConflict(() -> reservationService.evaluerChauffeur(reservationId, passager.getId(), 5, "Bravo"));
        reservationService.confirmerReservation(reservationId, chauffeur.getId());
        reservationService.evaluerChauffeur(reservationId, passager.getId(), 5, "Bravo");
        assertConflict(() -> reservationService.evaluerChauffeur(reservationId, passager.getId(), 1, "Double"));

        assertThat(reservationRepository.findById(reservationId).orElseThrow().peutEvaluerChauffeur()).isFalse();
        transactions.executeWithoutResult(transaction -> {
            Chauffeur conducteur = (Chauffeur) userRepository.findById(chauffeur.getId()).orElseThrow();
            assertThat(conducteur.getEvaluations()).singleElement().satisfies(evaluation -> {
                assertThat(evaluation.getNote()).isEqualTo(5);
                assertThat(evaluation.getCommentaire()).isEqualTo("Bravo");
            });
        });
    }

    private Callable<HttpStatus> attemptBooking(Passager client, int trajetId, CountDownLatch ready, CountDownLatch start) {
        return () -> {
            ready.countDown();
            if (!start.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("Booking requests did not start");
            try {
                book(client, trajetId, 1);
                return HttpStatus.CREATED;
            } catch (ResponseStatusException exception) {
                return HttpStatus.valueOf(exception.getStatusCode().value());
            }
        };
    }

    private Reservation book(Passager client, int trajetId, int places) {
        return reservationService.creerReservation(client.getId(), trajetId, places, TypePaiement.CARTE);
    }

    private int createTrip(int seats, LocalDateTime date) {
        return transactions.execute(transaction -> {
            Chauffeur conducteur = (Chauffeur) userRepository.findById(chauffeur.getId()).orElseThrow();
            Vehicule vehicule = conducteur.getVehicules().get(0);
            return trajetRepository.save(new Trajet("Sfax", "Tunis", date, 25, seats, conducteur, vehicule)).getIdTrajet();
        });
    }

    private void assertTrip(int id, int places, StatutTrajet statut) {
        transactions.executeWithoutResult(transaction -> {
            Trajet trajet = trajetRepository.findById(id).orElseThrow();
            assertThat(trajet.getNbPlacesDisponibles()).isEqualTo(places);
            assertThat(trajet.getStatut()).isEqualTo(statut);
        });
    }

    private int notifications(int userId) {
        return transactions.execute(transaction ->
                userRepository.findById(userId).orElseThrow().consulterNotifications().size());
    }

    private void assertConflict(ThrowingCallable action) {
        assertThatThrownBy(action).isInstanceOfSatisfying(ResponseStatusException.class,
                ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }
}
