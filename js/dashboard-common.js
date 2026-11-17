async function initializeDashboard(role) {
  const status = document.getElementById("apiStatus");
  document.getElementById("logoutBtn").addEventListener("click", handleLogout);

  try {
    const user = await requireRole(role);
    if (!user) return null;

    document.getElementById("currentUserBadge").innerHTML = `
      <strong>${escapeHtml(user.prenom)} ${escapeHtml(user.nom)}</strong>
      <small>${escapeHtml(user.email)}</small>
      <span class="role-chip">${escapeHtml(formatRole(user.role))}</span>
    `;
    status.textContent = CONFIG.USE_MOCK_API ? t("Mode démo", "Demo mode") : t("API connectée", "API connected");
    status.dataset.state = CONFIG.USE_MOCK_API ? "demo" : "connected";
    setupSidebarActiveLinks();
    return user;
  } catch (error) {
    if (error.status !== 401) {
      status.textContent = t("API indisponible", "API unavailable");
      status.dataset.state = "error";
      showToast(error.message);
    }
    return null;
  }
}

async function loadDashboardNotifications(list, announce = false) {
  try {
    const notifications = CONFIG.USE_MOCK_API
      ? []
      : await apiRequest("/notifications", { fallbackMessage: t("Impossible de charger les notifications.", "Could not load notifications.") });
    list.replaceChildren();
    if (!notifications.length) {
      list.innerHTML = `<li class="notification-item muted">${t("Aucune notification disponible.", "No notifications yet.")}</li>`;
    }
    for (const notification of notifications) {
      const item = document.createElement("li");
      item.className = "notification-item";
      item.innerHTML = `
        <strong>${escapeHtml(translateNotification(notification.message))}</strong>
        <small>${escapeHtml(t(notification.canal, notification.canal === "EMAIL" ? "Email" : notification.canal))} • ${formatDate(notification.dateEnvoi)}</small>
      `;
      list.appendChild(item);
    }
    if (announce) showToast(t("Notifications actualisées.", "Notifications refreshed."));
  } catch (error) {
    if (announce) showToast(error.message);
  }
}

async function performDashboardAction(button, path, { body, fallbackMessage, successMessage, refresh }) {
  if (button.disabled) return;
  button.disabled = true;
  try {
    const data = await apiRequest(path, { method: "POST", body, fallbackMessage });
    await refresh();
    showToast(data?.message || successMessage);
  } catch (error) {
    if (error.status === 409 || error.status === 404) await refresh();
    showToast(error.message);
  } finally {
    button.disabled = false;
  }
}

function showToast(message) {
  const toast = document.getElementById("toast");
  toast.textContent = message;
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => { toast.textContent = ""; }, 3600);
}

const dashboardLocale = t("fr-FR", "en-GB");
const dateFormatter = new Intl.DateTimeFormat(dashboardLocale, { dateStyle: "medium", timeStyle: "short" });
const priceFormatter = new Intl.NumberFormat(dashboardLocale, {
  style: "currency",
  currency: "TND",
  currencyDisplay: "code"
});

function formatDate(value) {
  const timestamp = Date.parse(value);
  return Number.isNaN(timestamp) ? t("Date inconnue", "Unknown date") : dateFormatter.format(timestamp);
}

function formatPrice(value) {
  return priceFormatter.format(value);
}

const statusLabelsInEnglish = new Map([
  ["ACTIF", "Active"],
  ["SUSPENDU", "Suspended"],
  ["BLOQUE", "Blocked"],
  ["EN_ATTENTE", "Pending"],
  ["CONFIRMEE", "Confirmed"],
  ["ANNULEE", "Cancelled"],
  ["REMBOURSEE", "Refunded"],
  ["OUVERT", "Open"],
  ["COMPLET", "Full"],
  ["ANNULE", "Cancelled"]
]);
const roleLabelsInEnglish = new Map([
  ["PASSAGER", "Passenger"],
  ["CHAUFFEUR", "Driver"],
  ["ADMIN", "Admin"]
]);
const paymentLabelsInEnglish = new Map([
  ["CARTE", "Card"],
  ["PAYPAL", "PayPal"],
  ["ESPECES", "Cash"]
]);

function formatStatus(value) {
  return t(value, statusLabelsInEnglish.get(value) ?? value);
}

function formatRole(value) {
  return t(value, roleLabelsInEnglish.get(value) ?? value);
}

function formatPayment(value) {
  return t(value, paymentLabelsInEnglish.get(value) ?? value);
}

function escapeHtml(value = "") {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function renderStars(value) {
  const rating = Math.max(0, Math.min(5, Number(value) || 0));
  const full = Math.floor(rating);
  const half = rating - full >= 0.5 ? 1 : 0;
  const empty = 5 - full - half;
  const icons = [
    ...Array(full).fill('<span class="material-icon-star">star</span>'),
    ...Array(half).fill('<span class="material-icon-star-half">star_half</span>'),
    ...Array(empty).fill('<span class="material-icon-star-empty">star</span>')
  ].join("");
  return `<span class="rating-line"><span class="rating-stars-material" aria-hidden="true">${icons}</span><span class="rating-number">${rating.toFixed(1)} / 5</span></span>`;
}

function setupSidebarActiveLinks() {
  const links = [...document.querySelectorAll('.sidebar-nav a[href^="#"]')];
  const sections = links.map((link) => document.querySelector(link.hash));

  function setCurrentLink(activeLink) {
    for (const link of links) {
      const isActive = link === activeLink;
      link.classList.toggle("is-active", isActive);
      if (isActive) link.setAttribute("aria-current", "location");
      else link.removeAttribute("aria-current");
    }
  }

  function update() {
    let currentLink = links[0];
    sections.forEach((section, index) => {
      if (section && section.getBoundingClientRect().top <= 180) currentLink = links[index];
    });
    setCurrentLink(currentLink);
  }

  for (const link of links) link.addEventListener("click", () => setCurrentLink(link));
  update();
  window.addEventListener("scroll", update, { passive: true });
}
