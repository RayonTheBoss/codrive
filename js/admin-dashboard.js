const state = {
  currentUserId: null,
  users: null,
  trips: null,
  refreshPromise: null
};

const els = {};

document.addEventListener("DOMContentLoaded", async () => {
  const admin = await initializeDashboard("ADMIN");
  if (!admin) return;
  state.currentUserId = admin.id;

  [
    "statUsers", "statTrips",
    "usersGrid", "emptyUsers", "loadUsersBtn", "loadNotificationsBtn", "notificationsList",
    "loadTripsBtn", "adminTripsGrid", "emptyAdminTrips"
  ].forEach((id) => els[id] = document.getElementById(id));

  els.loadUsersBtn.addEventListener("click", () => refreshAll(true));
  els.loadNotificationsBtn.addEventListener("click", () => loadDashboardNotifications(els.notificationsList, true));
  els.loadTripsBtn.addEventListener("click", () => refreshAll(true));

  const refreshVisible = () => {
    if (!document.hidden) refreshAll();
  };
  window.addEventListener("focus", refreshVisible);
  document.addEventListener("visibilitychange", refreshVisible);
  refreshAll();
  setInterval(refreshVisible, 5000);
});

async function refreshAll(announce = false) {
  if (!state.refreshPromise) {
    state.refreshPromise = Promise.allSettled([
      loadUsers(), loadTrips(), loadDashboardNotifications(els.notificationsList)
    ]).finally(() => { state.refreshPromise = null; });
  }
  const results = await state.refreshPromise;
  if (announce) {
    const failed = results.find((result) => result.status === "rejected");
    showToast(failed?.reason.message || t("Tableau de bord actualisé.", "Dashboard refreshed."));
  }
}

async function refreshAfterAction() {
  // Run a fresh load even if polling started just before the mutation.
  await state.refreshPromise;
  await refreshAll();
}

async function loadUsers() {
  const users = CONFIG.USE_MOCK_API ? [] : await apiRequest("/admin/users", {
    fallbackMessage: t("Impossible de charger les utilisateurs.", "Could not load users.")
  });
  if (JSON.stringify(users) === JSON.stringify(state.users)) return;
  state.users = users;
  renderUsers();
}

function renderUsers() {
  els.usersGrid.innerHTML = "";
  els.statUsers.textContent = String(state.users.length);

  if (!state.users.length) {
    els.emptyUsers.classList.remove("hidden");
    return;
  }
  els.emptyUsers.classList.add("hidden");

  state.users.forEach((user) => {
    const status = String(user.statut || "ACTIF").trim().toUpperCase();
    const isCurrentUser = user.id === state.currentUserId;
    const statusClass = status === "ACTIF"
      ? "status-active"
      : status === "SUSPENDU"
        ? "status-suspended"
        : "status-blocked";
    const card = document.createElement("article");
    card.className = "user-card";
    card.innerHTML = `
      <strong>${escapeHtml(user.prenom)} ${escapeHtml(user.nom)}</strong>
      <small>${escapeHtml(user.email)}</small>
      <div class="inline-actions">
        <span class="role-chip">${escapeHtml(formatRole(user.role))}</span>
        <span class="trip-chip ${statusClass}">${escapeHtml(formatStatus(status))}</span>
      </div>
      ${isCurrentUser ? "" : `<div class="action-group">
        <button class="btn btn-success reactivate-btn" ${status === "ACTIF" ? "disabled" : ""}><span class="material-icons">restart_alt</span><span>${t("Réactiver", "Reactivate")}</span></button>
        <button class="btn btn-secondary suspend-btn" ${status === "SUSPENDU" ? "disabled" : ""}><span class="material-icons">pause_circle</span><span>${t("Suspendre", "Suspend")}</span></button>
        <button class="btn btn-danger block-btn" ${status === "BLOQUE" ? "disabled" : ""}><span class="material-icons">block</span><span>${t("Bloquer", "Block")}</span></button>
      </div>`}
    `;
    const reactivateBtn = card.querySelector(".reactivate-btn");
    const suspendBtn = card.querySelector(".suspend-btn");
    const blockBtn = card.querySelector(".block-btn");
    reactivateBtn?.addEventListener("click", () => updateUserStatus(reactivateBtn, user.id, "reactivate"));
    suspendBtn?.addEventListener("click", () => updateUserStatus(suspendBtn, user.id, "suspend"));
    blockBtn?.addEventListener("click", () => updateUserStatus(blockBtn, user.id, "block"));
    els.usersGrid.appendChild(card);
  });
}

function updateUserStatus(button, userId, action) {
  performDashboardAction(button, `/admin/users/${userId}/${action}`, {
    fallbackMessage: t("Action admin impossible.", "Could not perform the admin action."),
    successMessage: t("Statut mis à jour.", "Status updated."),
    refresh: refreshAfterAction
  });
}
async function loadTrips() {
  const trips = CONFIG.USE_MOCK_API ? [] : await apiRequest("/admin/trajets", {
    fallbackMessage: t("Impossible de charger les trajets.", "Could not load trips.")
  });
  if (JSON.stringify(trips) === JSON.stringify(state.trips)) return;
  state.trips = trips;
  renderTrips();
}

function renderTrips() {
  els.adminTripsGrid.innerHTML = "";
  els.statTrips.textContent = String(state.trips.length);

  if (!state.trips.length) {
    els.emptyAdminTrips.classList.remove("hidden");
    return;
  }
  els.emptyAdminTrips.classList.add("hidden");

  state.trips.forEach((trip) => {
    const evaluations = Array.isArray(trip.chauffeur?.evaluations) ? trip.chauffeur.evaluations : [];
    const comments = evaluations.length
      ? evaluations.map(e => `<li class="notification-item">${renderStars(e.note || 0)}<small>${escapeHtml(e.commentaire || t("Sans commentaire", "No comment"))}</small></li>`).join("")
      : `<li>${t("Aucun commentaire", "No comments")}</li>`;

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
        <span class="trip-chip">${escapeHtml(formatStatus(trip.statut || "OUVERT"))}</span>
      </div>
      <div class="trip-highlights">
        <div class="trip-highlight"><span class="trip-highlight-icon">👤</span><span>${t("Chauffeur :", "Driver:")} <strong>${escapeHtml((trip.chauffeur?.prenom || "") + " " + (trip.chauffeur?.nom || ""))}</strong></span></div>
        <div class="trip-highlight"><span class="trip-highlight-icon">⭐</span><span>${t("Note moyenne :", "Average rating:")} ${renderStars(trip.chauffeur?.noteMoyenne || 0)}</span></div>
        <div class="trip-highlight"><span class="trip-highlight-icon">🚘</span><span>${t("Véhicule :", "Vehicle:")} <strong>${escapeHtml((trip.vehicule?.marque || "") + " " + (trip.vehicule?.modele || ""))}</strong></span></div>
      </div>
      <div class="dashboard-card dashboard-card-soft">
        <strong>${t("Avis chauffeur", "Driver reviews")}</strong>
        <ul class="notification-list list-spaced">${comments}</ul>
      </div>
      <div class="trip-footer">
        <div class="trip-price">${formatPrice(trip.prixPlace)}</div>
        <span class="trip-chip">ID ${trip.idTrajet}</span>
      </div>
      <div class="action-group">
        <button class="btn btn-danger admin-cancel-trip-btn" ${trip.canCancel ? "" : "disabled"}>
          <span class="material-icons">cancel</span><span>${t("Annuler le trajet", "Cancel trip")}</span>
        </button>
      </div>
    `;
    const cancelBtn = card.querySelector('.admin-cancel-trip-btn');
    cancelBtn.addEventListener("click", () => cancelTripAsAdmin(trip, cancelBtn));
    els.adminTripsGrid.appendChild(card);
  });
}

function cancelTripAsAdmin(trip, button) {
  const tripLabel = `${trip.villeDepart} → ${trip.villeArrivee}`;
  if (!window.confirm(t(`Annuler le trajet ${tripLabel} et ses réservations ?`, `Cancel the trip ${tripLabel} and its bookings?`))) return;
  performDashboardAction(button, `/admin/trajets/${trip.idTrajet}/cancel`, {
    fallbackMessage: t("Impossible d'annuler ce trajet.", "Could not cancel this trip."),
    successMessage: t("Trajet annulé avec succès.", "Trip cancelled."),
    refresh: refreshAfterAction
  });
}

