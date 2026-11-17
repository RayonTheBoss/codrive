const state = {
  trips: [],
  filteredTrips: [],
  selectedTrip: null,
  myReservations: [],
  isSubmittingReservation: false
};

const mockTrips = [
  {
    idTrajet: 1,
    villeDepart: "Sfax",
    villeArrivee: "Tunis",
    dateDepart: new Date(Date.now() + 24 * 60 * 60 * 1000).toISOString(),
    prixPlace: 25,
    nbPlacesDisponibles: 3,
    statut: "OUVERT",
    chauffeur: { nom: "Ben Salem", prenom: "Ali", noteMoyenne: 0 },
    vehicule: { marque: "Peugeot", modele: "208" }
  }
];

const els = {};

document.addEventListener("DOMContentLoaded", async () => {
  const user = await initializeDashboard("PASSAGER");
  if (!user) return;

  [
    "statTrips", "statReservations", "tripsGrid", "emptyState",
    "resultsInfo", "searchForm", "depart", "arrivee", "dateDepart", "placesMin", "resetSearchBtn",
    "refreshTripsBtn", "selectedTrip", "passagerNom", "passagerEmail", "nbPlaces", "paymentType",
    "reservationForm", "reservationSubmitBtn", "reservationSummary",
    "loadNotificationsBtn", "notificationsList", "loadReservationsBtn",
    "reservationsGrid", "emptyReservations"
  ].forEach((id) => els[id] = document.getElementById(id));

  els.searchForm.addEventListener("submit", handleSearch);
  els.resetSearchBtn.addEventListener("click", resetSearch);
  els.refreshTripsBtn.addEventListener("click", loadTrips);
  els.reservationForm.addEventListener("submit", handleReservation);
  els.nbPlaces.addEventListener("input", updateReservationSummary);
  els.loadNotificationsBtn.addEventListener("click", () => loadDashboardNotifications(els.notificationsList, true));
  els.loadReservationsBtn.addEventListener("click", loadMyReservations);

  els.passagerNom.value = `${user.prenom} ${user.nom}`.trim();
  els.passagerEmail.value = user.email;
  refreshReservations();
});

function refreshReservations() {
  return Promise.all([loadTrips(), loadMyReservations(), loadDashboardNotifications(els.notificationsList)]);
}

async function loadTrips() {
  try {
    state.trips = CONFIG.USE_MOCK_API
      ? mockTrips
      : await apiRequest("/trajets", { fallbackMessage: t("Impossible de charger les trajets.", "Could not load trips.") });
    if (state.selectedTrip) {
      selectTrip(state.trips.find((trip) => trip.idTrajet === state.selectedTrip.idTrajet) || null);
    }
    applySearch();
  } catch (error) {
    showToast(error.message);
  }
}

function renderTrips() {
  els.tripsGrid.innerHTML = "";
  const trips = state.filteredTrips;
  els.statTrips.textContent = String(trips.length);
  const resultLabel = t(trips.length > 1 ? "résultats" : "résultat", trips.length === 1 ? "result" : "results");
  els.resultsInfo.textContent = `${trips.length} ${resultLabel}`;

  if (!trips.length) {
    els.emptyState.classList.remove("hidden");
    return;
  }
  els.emptyState.classList.add("hidden");

  for (const trip of trips) {
    const card = document.createElement("article");
    card.className = "trip-card";
    card.innerHTML = `
      <div class="trip-route">
        <span>${escapeHtml(trip.villeDepart)}</span>
        <span>→</span>
        <span>${escapeHtml(trip.villeArrivee)}</span>
      </div>
      <div class="trip-meta">
        <span class="trip-chip">${formatDate(trip.dateDepart)}</span>
        <span class="trip-chip">${trip.nbPlacesDisponibles} ${t("place(s)", trip.nbPlacesDisponibles === 1 ? "seat" : "seats")}</span>
      </div>
      <div class="trip-highlights">
        <div class="trip-highlight"><span class="trip-highlight-icon">👤</span><span>${t("Chauffeur :", "Driver:")} <strong>${escapeHtml((trip.chauffeur?.prenom || "") + " " + (trip.chauffeur?.nom || ""))}</strong></span></div>
        <div class="trip-highlight"><span class="trip-highlight-icon">⭐</span><span>${t("Note chauffeur :", "Driver rating:")} ${renderStars(trip.chauffeur?.noteMoyenne || 0)}</span></div>
        <div class="trip-highlight"><span class="trip-highlight-icon">🚘</span><span>${t("Véhicule :", "Vehicle:")} <strong>${escapeHtml((trip.vehicule?.marque || "") + " " + (trip.vehicule?.modele || ""))}</strong></span></div>
        <div class="trip-highlight"><span class="trip-highlight-icon">💬</span><span>${Array.isArray(trip.chauffeur?.evaluations) && trip.chauffeur.evaluations.length
          ? `${t("Dernier avis :", "Latest review:")} <strong>${escapeHtml(trip.chauffeur.evaluations[trip.chauffeur.evaluations.length - 1].commentaire || t("Sans commentaire", "No comment"))}</strong>`
          : t("Aucun avis pour le moment", "No reviews yet")}</span></div>
      </div>
      <div class="trip-footer">
        <div class="trip-price">${formatPrice(trip.prixPlace)}</div>
        <button class="btn btn-primary choose-btn"><span class="material-icons">check_circle</span><span>${t("Choisir", "Select")}</span></button>
      </div>
    `;
    card.querySelector(".choose-btn").addEventListener("click", () => {
      if (state.isSubmittingReservation) return;
      els.nbPlaces.value = 1;
      selectTrip(trip);
      els.nbPlaces.focus({ preventScroll: true });
      document.getElementById("reservation").scrollIntoView();
    });
    els.tripsGrid.appendChild(card);
  }
}

function handleSearch(event) {
  event.preventDefault();
  applySearch();
}

function applySearch() {
  const depart = els.depart.value.trim().toLowerCase();
  const arrivee = els.arrivee.value.trim().toLowerCase();
  const dateValue = els.dateDepart.value;
  const minPlaces = Number(els.placesMin.value || 1);

  state.filteredTrips = state.trips.filter((trip) =>
    (!depart || trip.villeDepart.toLowerCase().includes(depart)) &&
    (!arrivee || trip.villeArrivee.toLowerCase().includes(arrivee)) &&
    (!dateValue || String(trip.dateDepart).startsWith(dateValue)) &&
    trip.nbPlacesDisponibles >= minPlaces
  );
  renderTrips();
}

function resetSearch() {
  els.searchForm.reset();
  applySearch();
}

function selectTrip(trip) {
  state.selectedTrip = trip;
  els.selectedTrip.value = trip
    ? `${trip.villeDepart} → ${trip.villeArrivee} · ${formatDate(trip.dateDepart)}`
    : "";
  els.nbPlaces.disabled = !trip;
  els.paymentType.disabled = !trip;
  if (trip) els.nbPlaces.max = trip.nbPlacesDisponibles;
  else els.nbPlaces.removeAttribute("max");
  updateReservationSummary();
}

function updateReservationSummary() {
  const trip = state.selectedTrip;
  els.reservationSubmitBtn.disabled = !trip || state.isSubmittingReservation;
  if (!trip) {
    els.reservationSummary.textContent = t("Choisis un trajet pour connaître le total.", "Select a trip to see the total.");
    return;
  }
  const placesLabel = trip.nbPlacesDisponibles === 1
    ? t("place disponible", "seat available")
    : t("places disponibles", "seats available");
  const availability = `${trip.nbPlacesDisponibles} ${placesLabel}`;
  const total = Number(els.nbPlaces.value) * trip.prixPlace;
  els.reservationSummary.textContent = els.nbPlaces.validity.valid
    ? `${availability} · ${t("Total :", "Total:")} ${formatPrice(total)}`
    : availability;
}

async function handleReservation(event) {
  event.preventDefault();
  if (state.isSubmittingReservation || !state.selectedTrip || !els.nbPlaces.reportValidity()) return;

  const payload = {
    nbPlaces: Number(els.nbPlaces.value),
    moyenPaiement: els.paymentType.value,
    idTrajet: state.selectedTrip.idTrajet
  };

  state.isSubmittingReservation = true;
  const label = els.reservationSubmitBtn.lastElementChild;
  const initialLabel = label.textContent;
  label.textContent = t("Envoi de la demande…", "Sending request…");
  updateReservationSummary();

  try {
    const result = CONFIG.USE_MOCK_API
      ? { message: t("Réservation simulée.", "Booking simulated.") }
      : await apiRequest("/reservations", {
        method: "POST",
        body: payload,
        fallbackMessage: t("Réservation impossible.", "Could not make the booking.")
      });
    els.nbPlaces.value = 1;
    els.paymentType.selectedIndex = 0;
    selectTrip(null);
    await refreshReservations();
    showToast(result?.message || t("Réservation créée.", "Booking created."));
  } catch (error) {
    if (error.status === 409 || error.status === 404) await refreshReservations();
    showToast(error.message);
  } finally {
    state.isSubmittingReservation = false;
    label.textContent = initialLabel;
    updateReservationSummary();
  }
}

async function loadMyReservations() {
  try {
    state.myReservations = CONFIG.USE_MOCK_API ? [] : await apiRequest("/reservations/mine", {
      fallbackMessage: t("Impossible de charger les réservations.", "Could not load your bookings.")
    });
    renderReservations();
  } catch (error) {
    showToast(error.message);
  }
}

function renderReservations() {
  els.reservationsGrid.innerHTML = "";
  els.statReservations.textContent = String(state.myReservations.length);

  if (!state.myReservations.length) {
    els.emptyReservations.classList.remove("hidden");
    return;
  }
  els.emptyReservations.classList.add("hidden");

  state.myReservations.forEach((item) => {
    const card = document.createElement("article");
    card.className = "trip-card";
    const canCancel = Boolean(item.canCancel);
    const canEvaluate = Boolean(item.canEvaluate);

    card.innerHTML = `
      <div class="trip-route">
        <span>${escapeHtml(item.villeDepart)}</span>
        <span>→</span>
        <span>${escapeHtml(item.villeArrivee)}</span>
      </div>
      <div class="trip-meta">
        <span class="trip-chip">${escapeHtml(formatStatus(item.statut))}</span>
        <span class="trip-chip">${item.nbPlacesReservees} ${t("place(s)", item.nbPlacesReservees === 1 ? "seat" : "seats")}</span>
      </div>
      <p class="trip-desc">
        ${t("Date :", "Date:")} <strong>${formatDate(item.dateDepart)}</strong><br>
        ${t("Paiement :", "Payment:")} <strong>${escapeHtml(formatPayment(item.typePaiement || "N/A"))}</strong>
      </p>
      <div class="trip-footer">
        <div class="trip-price">${formatPrice(item.montantTotal)}</div>
        <span class="trip-chip">${t("Réservation", "Booking")} #${item.idReservation}</span>
      </div>
      <div class="inline-actions">
        ${canCancel ? `<button type="button" class="btn btn-danger cancel-btn"><span class="material-icons">close</span><span>${t("Annuler", "Cancel")}</span></button>` : ""}
        ${canEvaluate ? `<button type="button" class="btn btn-primary eval-btn"><span class="material-icons">star</span><span>${t("Évaluer chauffeur", "Review driver")}</span></button>` : ""}
      </div>
    `;

    const cancelBtn = card.querySelector(".cancel-btn");
    if (cancelBtn) cancelBtn.addEventListener("click", () => {
      if (window.confirm(t("Annuler cette réservation ?", "Cancel this booking?"))) {
        performDashboardAction(cancelBtn, `/reservations/${item.idReservation}/cancel-by-passenger`, {
          fallbackMessage: t("Impossible d'annuler la réservation.", "Could not cancel the booking."),
          successMessage: t("Réservation annulée.", "Booking cancelled."),
          refresh: refreshReservations
        });
      }
    });

    const evalBtn = card.querySelector(".eval-btn");
    if (evalBtn) evalBtn.addEventListener("click", () => evaluateDriver(item.idReservation, evalBtn));

    els.reservationsGrid.appendChild(card);
  });
}

function evaluateDriver(idReservation, button) {
  const noteValue = window.prompt(t("Note du chauffeur (1 à 5)", "Driver rating (1 to 5)"));
  if (noteValue === null) return;
  const note = Number(noteValue);
  if (!Number.isInteger(note) || note < 1 || note > 5) {
    showToast(t("La note doit être un nombre entier entre 1 et 5.", "Enter a whole number between 1 and 5."));
    return;
  }
  const commentaire = window.prompt(t("Commentaire", "Comment"))?.trim();
  if (!commentaire) return;
  performDashboardAction(button, `/reservations/${idReservation}/evaluate`, {
    body: { note, commentaire },
    fallbackMessage: t("Impossible d'enregistrer l'évaluation.", "Could not save the review."),
    successMessage: t("Évaluation enregistrée.", "Review saved."),
    refresh: refreshReservations
  });
}
