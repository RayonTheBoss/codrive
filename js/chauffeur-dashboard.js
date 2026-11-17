const state = {
  myVehicles: [],
  myReservations: [],
  isSubmittingTrip: false,
  isSubmittingVehicle: false
};

const els = {};

document.addEventListener("DOMContentLoaded", async () => {
  if (!await initializeDashboard("CHAUFFEUR")) return;

  [
    "statMyTrips", "statDriverReservations", "statDriverRating",
    "createTripForm", "createVehicleForm", "villeDepart", "villeArrivee", "dateTrajet", "prixPlace", "nbPlacesTotal",
    "selectedVehicule", "tripVehiculeMarque", "tripVehiculeModele", "tripVehiculeImmatriculation", "vehiculeMarque", "vehiculeModele", "vehiculeImmatriculation", "vehiculeNbPlaces",
    "myTripsGrid", "emptyMyTrips", "refreshMyTripsBtn", "loadNotificationsBtn", "notificationsList",
    "loadVehiclesBtn", "vehiclesGrid", "emptyVehicles", "loadDriverReservationsBtn", "driverReservationsGrid", "emptyDriverReservations",
    "loadDriverReviewsBtn", "driverReviewsSummary", "driverReviewsList"
  ].forEach((id) => els[id] = document.getElementById(id));

  els.createTripForm.addEventListener("submit", handleCreateTrip);
  els.createVehicleForm.addEventListener("submit", handleCreateVehicle);
  els.refreshMyTripsBtn.addEventListener("click", loadMyTrips);
  els.loadNotificationsBtn.addEventListener("click", () => loadDashboardNotifications(els.notificationsList, true));
  els.loadVehiclesBtn.addEventListener("click", loadMyVehicles);
  els.loadDriverReservationsBtn.addEventListener("click", loadDriverReservations);
  els.loadDriverReviewsBtn.addEventListener("click", loadMyTrips);
  els.selectedVehicule.addEventListener("change", syncVehicleSelection);

  syncVehicleSelection();
  Promise.all([loadMyTrips(), loadMyVehicles(), loadDriverReservations(), loadDashboardNotifications(els.notificationsList)]);
});

async function loadMyTrips() {
  try {
    const trips = CONFIG.USE_MOCK_API ? [] : await apiRequest("/trajets/mine", {
      fallbackMessage: t("Impossible de charger les trajets du chauffeur.", "Could not load your trips.")
    });
    renderMyTrips(trips.filter((trip) => trip.statut !== "ANNULE"));
    renderDriverReviews(trips[0]?.chauffeur);
  } catch (error) {
    console.error(error);
    showToast(error.message || t("Erreur lors du chargement des trajets.", "Could not load trips."));
  }
}

function renderMyTrips(trips) {
  els.myTripsGrid.innerHTML = "";
  els.statMyTrips.textContent = String(trips.length);

  if (!trips.length) {
    els.emptyMyTrips.classList.remove("hidden");
    return;
  }
  els.emptyMyTrips.classList.add("hidden");

  trips.forEach((trip) => {
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
        <span class="trip-chip">${trip.nbPlacesDisponibles} ${t("places disponibles", trip.nbPlacesDisponibles === 1 ? "seat available" : "seats available")}</span>
        <span class="trip-chip">${escapeHtml(formatStatus(trip.statut || "OUVERT"))}</span>
      </div>
      <div class="trip-highlights">
        <div class="trip-highlight"><span class="trip-highlight-icon">🚘</span><span>${t("Véhicule :", "Vehicle:")} <strong>${escapeHtml((trip.vehicule?.marque || "") + " " + (trip.vehicule?.modele || ""))}</strong></span></div>
      </div>
      <div class="trip-footer">
        <div class="trip-price">${formatPrice(trip.prixPlace)}</div>
        <span class="trip-chip">ID ${trip.idTrajet}</span>
      </div>
      ${trip.canCancel ? `<div class="inline-actions">
        <button class="btn btn-danger cancel-trip-btn"><span class="material-icons">cancel</span><span>${t("Annuler le trajet", "Cancel trip")}</span></button>
      </div>` : ""}
    `;
    const cancelButton = card.querySelector(".cancel-trip-btn");
    cancelButton?.addEventListener("click", () => {
      const tripLabel = `${trip.villeDepart} → ${trip.villeArrivee}`;
      if (window.confirm(t(`Annuler le trajet ${tripLabel} ?`, `Cancel the trip ${tripLabel}?`))) {
        performDashboardAction(cancelButton, `/trajets/${trip.idTrajet}/cancel`, {
          fallbackMessage: t("Impossible d'annuler le trajet.", "Could not cancel the trip."),
          successMessage: t("Trajet annulé.", "Trip cancelled."),
          refresh: refreshBookingViews
        });
      }
    });
    els.myTripsGrid.appendChild(card);
  });
}

function refreshBookingViews() {
  return Promise.all([loadMyTrips(), loadDriverReservations(), loadDashboardNotifications(els.notificationsList)]);
}

async function loadMyVehicles() {
  try {
    const vehicles = CONFIG.USE_MOCK_API ? [] : await apiRequest("/vehicules/mine", {
      fallbackMessage: t("Impossible de charger les véhicules.", "Could not load vehicles.")
    });
    state.myVehicles = vehicles;
    renderVehicleSelect();

    els.vehiclesGrid.innerHTML = "";
    if (!vehicles.length) {
      els.emptyVehicles.classList.remove("hidden");
      return;
    }
    els.emptyVehicles.classList.add("hidden");

    vehicles.forEach((vehicle) => {
      const card = document.createElement("article");
      card.className = "user-card vehicle-card";
      card.innerHTML = `
        <div class="vehicle-top">
          <div>
            <strong>${escapeHtml(vehicle.marque)} ${escapeHtml(vehicle.modele)}</strong>
            <small>${escapeHtml(vehicle.immatriculation)}</small>
          </div>
          <span class="vehicle-badge">🚘</span>
        </div>
        <div class="action-group">
          <span class="role-chip">${vehicle.nbPlaces} ${t("place(s)", vehicle.nbPlaces === 1 ? "seat" : "seats")}</span>
        </div>
      `;
      els.vehiclesGrid.appendChild(card);
    });
  } catch (error) {
    console.error(error);
    showToast(error.message || t("Erreur lors du chargement des véhicules.", "Could not load vehicles."));
  }
}

function renderVehicleSelect() {
  const currentValue = els.selectedVehicule.value;
  els.selectedVehicule.innerHTML = `<option value="">${t("Choisir un véhicule déjà enregistré", "Select a saved vehicle")}</option>`;

  state.myVehicles.forEach((vehicle) => {
    const option = document.createElement("option");
    option.value = String(vehicle.idVehicule);
    const seatLabel = t("place(s)", vehicle.nbPlaces === 1 ? "seat" : "seats");
    option.textContent = `${vehicle.marque} ${vehicle.modele} • ${vehicle.immatriculation} • ${vehicle.nbPlaces} ${seatLabel}`;
    option.dataset.marque = vehicle.marque;
    option.dataset.modele = vehicle.modele;
    option.dataset.immatriculation = vehicle.immatriculation;
    option.dataset.nbPlaces = String(vehicle.nbPlaces);
    if (String(vehicle.idVehicule) === currentValue) option.selected = true;
    els.selectedVehicule.appendChild(option);
  });

  syncVehicleSelection();
}

function syncVehicleSelection() {
  const option = els.selectedVehicule.selectedOptions[0];
  const hasVehicle = option && option.value;

  if (hasVehicle) {
    els.tripVehiculeMarque.value = option.dataset.marque || "";
    els.tripVehiculeModele.value = option.dataset.modele || "";
    els.tripVehiculeImmatriculation.value = option.dataset.immatriculation || "";
    els.nbPlacesTotal.max = option.dataset.nbPlaces;
    if (!els.nbPlacesTotal.value) {
      els.nbPlacesTotal.value = option.dataset.nbPlaces || "";
    }
  } else {
    els.nbPlacesTotal.removeAttribute("max");
    if (els.tripVehiculeMarque.readOnly) {
      els.tripVehiculeMarque.value = "";
      els.tripVehiculeModele.value = "";
      els.tripVehiculeImmatriculation.value = "";
    }
  }

  [els.tripVehiculeMarque, els.tripVehiculeModele, els.tripVehiculeImmatriculation].forEach((input) => {
    input.readOnly = Boolean(hasVehicle);
    input.required = !hasVehicle;
  });
}

async function handleCreateVehicle(event) {
  event.preventDefault();
  if (state.isSubmittingVehicle) return;

  const payload = {
    marque: els.vehiculeMarque.value.trim(),
    modele: els.vehiculeModele.value.trim(),
    immatriculation: els.vehiculeImmatriculation.value.trim(),
    nbPlaces: Number(els.vehiculeNbPlaces.value)
  };

  state.isSubmittingVehicle = true;
  const submitBtn = els.createVehicleForm.querySelector('button[type="submit"]');
  if (submitBtn) submitBtn.disabled = true;

  try {
    if (!CONFIG.USE_MOCK_API) {
      await apiRequest("/vehicules", {
        method: "POST",
        body: payload,
        fallbackMessage: t("Impossible d'ajouter le véhicule.", "Could not add the vehicle.")
      });
    }
    showToast(CONFIG.USE_MOCK_API
      ? t("Ajout de véhicule simulé.", "Vehicle added in demo mode.")
      : t("Véhicule ajouté avec succès.", "Vehicle added."));
    els.createVehicleForm.reset();
    els.selectedVehicule.value = "";
    await loadMyVehicles();
  } catch (error) {
    console.error(error);
    showToast(error.message || t("Erreur lors de l'ajout du véhicule.", "Could not add the vehicle."));
  } finally {
    state.isSubmittingVehicle = false;
    if (submitBtn) submitBtn.disabled = false;
  }
}

function renderDriverReviews(chauffeur) {
  const reviews = chauffeur?.evaluations ?? [];
  const rating = renderStars(chauffeur?.noteMoyenne);
  els.statDriverRating.innerHTML = rating;
  const reviewLabel = t("avis", reviews.length === 1 ? "review" : "reviews");
  els.driverReviewsSummary.innerHTML = reviews.length ? `${reviews.length} ${reviewLabel} · ${rating}` : "";
  els.driverReviewsList.replaceChildren();
  if (!reviews.length) {
    els.driverReviewsList.innerHTML = `<li class="notification-item muted">${t("Aucun avis pour le moment.", "No reviews yet.")}</li>`;
  }
  for (const review of reviews) {
    const item = document.createElement("li");
    item.className = "notification-item review-card";
    item.innerHTML = `
      <div>${renderStars(review.note)}</div>
      <div>${escapeHtml(review.commentaire || t("Sans commentaire", "No comment"))}</div>
    `;
    els.driverReviewsList.appendChild(item);
  }
}

async function loadDriverReservations() {
  try {
    state.myReservations = CONFIG.USE_MOCK_API ? [] : await apiRequest("/reservations/chauffeur", {
      fallbackMessage: t("Impossible de charger les réservations reçues.", "Could not load incoming bookings.")
    });
    renderDriverReservations();
  } catch (error) {
    console.error(error);
    showToast(error.message || t("Erreur lors du chargement des réservations reçues.", "Could not load incoming bookings."));
  }
}

function renderDriverReservations() {
  els.driverReservationsGrid.innerHTML = "";
  els.statDriverReservations.textContent = String(state.myReservations.length);

  if (!state.myReservations.length) {
    els.emptyDriverReservations.classList.remove("hidden");
    return;
  }
  els.emptyDriverReservations.classList.add("hidden");

  state.myReservations.forEach((item) => {
    const card = document.createElement("article");
    card.className = "trip-card";
    const canAccept = Boolean(item.canAccept);

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
        ${t("Passager :", "Passenger:")} <strong>${escapeHtml(item.passagerNom)}</strong><br>
        ${t("Date :", "Date:")} <strong>${formatDate(item.dateDepart)}</strong><br>
        ${t("Paiement :", "Payment:")} <strong>${escapeHtml(formatPayment(item.typePaiement || "N/A"))}</strong>
      </p>
      <div class="trip-footer">
        <div class="trip-price">${formatPrice(item.montantTotal)}</div>
        <span class="trip-chip">${t("Réservation", "Booking")} #${item.idReservation}</span>
      </div>
      ${canAccept ? `
        <div class="inline-actions">
          <button type="button" class="btn btn-success accept-btn"><span class="material-icons">check</span><span>${t("Accepter la réservation", "Accept booking")}</span></button>
        </div>
      ` : ""}
    `;

    const acceptButton = card.querySelector(".accept-btn");
    acceptButton?.addEventListener("click", () => {
      performDashboardAction(acceptButton, `/reservations/${item.idReservation}/accept`, {
        fallbackMessage: t("Impossible d'accepter la réservation.", "Could not accept the booking."),
        successMessage: t("Réservation acceptée.", "Booking accepted."),
        refresh: refreshBookingViews
      });
    });

    els.driverReservationsGrid.appendChild(card);
  });
}

async function handleCreateTrip(event) {
  event.preventDefault();
  if (state.isSubmittingTrip) return;

  const payload = {
    villeDepart: els.villeDepart.value.trim(),
    villeArrivee: els.villeArrivee.value.trim(),
    dateDepart: els.dateTrajet.value,
    prixPlace: Number(els.prixPlace.value),
    nbPlacesTotal: Number(els.nbPlacesTotal.value)
  };

  if (els.selectedVehicule.value) {
    payload.idVehicule = Number(els.selectedVehicule.value);
  } else {
    payload.vehiculeMarque = els.tripVehiculeMarque.value.trim();
    payload.vehiculeModele = els.tripVehiculeModele.value.trim();
    payload.vehiculeImmatriculation = els.tripVehiculeImmatriculation.value.trim();
  }

  state.isSubmittingTrip = true;
  const submitBtn = els.createTripForm.querySelector('button[type="submit"]');
  if (submitBtn) submitBtn.disabled = true;

  try {
    if (!CONFIG.USE_MOCK_API) {
      await apiRequest("/trajets", {
        method: "POST",
        body: payload,
        fallbackMessage: t("Impossible de publier le trajet.", "Could not publish the trip.")
      });
    }
    showToast(CONFIG.USE_MOCK_API
      ? t("Publication simulée.", "Trip published in demo mode.")
      : t("Trajet publié avec succès.", "Trip published."));
    els.createTripForm.reset();
    els.selectedVehicule.value = "";
    syncVehicleSelection();
    await Promise.all([loadMyTrips(), loadMyVehicles(), loadDashboardNotifications(els.notificationsList)]);
  } catch (error) {
    console.error(error);
    showToast(error.message || t("Erreur lors de la publication du trajet.", "Could not publish the trip."));
  } finally {
    state.isSubmittingTrip = false;
    if (submitBtn) submitBtn.disabled = false;
  }
}
