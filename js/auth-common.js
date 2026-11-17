const SESSION_KEY = "codrive.session";
const DASHBOARDS = new Map([
  ["PASSAGER", "passager-dashboard.html"],
  ["CHAUFFEUR", "chauffeur-dashboard.html"],
  ["ADMIN", "admin-dashboard.html"]
]);

function saveSession(session) {
  try {
    const { token, expiresAt, role } = session;
    const stored = CONFIG.USE_MOCK_API ? session : { token, expiresAt, role };
    sessionStorage.setItem(SESSION_KEY, JSON.stringify(stored));
  } catch {
    throw new Error(t("Le navigateur bloque le stockage de session.", "The browser is blocking session storage."));
  }
}

function clearSession() {
  sessionStorage.removeItem(SESSION_KEY);
}

function hasActiveToken(session) {
  return typeof session?.token === "string" && session.token.length > 0 && Date.parse(session.expiresAt) > Date.now();
}

function loadSession() {
  try {
    const session = JSON.parse(sessionStorage.getItem(SESSION_KEY));
    if (!session || !DASHBOARDS.has(session.role)) return null;
    if (CONFIG.USE_MOCK_API && session.demo === true) return session;
    if (!CONFIG.USE_MOCK_API && hasActiveToken(session)) {
      return session;
    }
    clearSession();
  } catch {
    return null;
  }
  return null;
}

function redirectToDashboard(user = loadSession()) {
  window.location.replace(localizedUrl(DASHBOARDS.get(user?.role) || "login.html"));
}

class ApiError extends Error {
  constructor(message, status) {
    super(message);
    this.status = status;
  }
}

async function apiRequest(path, {
  method = "GET", body, authenticated = true, fallbackMessage = t("La requête a échoué.", "The request failed."), signal
} = {}) {
  const session = authenticated ? loadSession() : null;
  if (authenticated && !session) {
    redirectToDashboard(null);
    throw new ApiError(t("Session expirée. Reconnecte-toi.", "Your session expired. Please sign in again."), 401);
  }
  const headers = { Accept: "application/json" };
  if (session?.token) headers.Authorization = `Bearer ${session.token}`;
  if (body !== undefined) headers["Content-Type"] = "application/json";

  let response;
  try {
    response = await fetch(`${CONFIG.API_BASE_URL}${path}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      cache: "no-store",
      signal
    });
  } catch {
    throw new Error(t("Impossible de joindre l’API Codrive.", "Could not reach the Codrive API."));
  }

  const invalidSession = response.status === 401 || (path === "/auth/me" && response.status === 403);
  if (authenticated && invalidSession) {
    const currentSession = loadSession();
    // A delayed response from an old session must not clear a newer one.
    if (!currentSession || currentSession.token === session.token) {
      clearSession();
      redirectToDashboard(null);
    }
  }

  const data = response.status === 204 ? null : await response.json().catch(() => null);
  if (!response.ok) throw new ApiError(translateApiMessage(data?.message, fallbackMessage), response.status);
  if (typeof data?.message === "string") data.message = translateApiMessage(data.message);
  return data;
}

async function requireRole(expectedRole) {
  const session = loadSession();
  if (!session) {
    redirectToDashboard(null);
    return null;
  }

  const user = CONFIG.USE_MOCK_API ? session : await apiRequest("/auth/me");
  if (!DASHBOARDS.has(user?.role)) throw new Error(t("Profil reçu invalide.", "The API returned an invalid profile."));
  if (user.role !== expectedRole) {
    redirectToDashboard(user);
    return null;
  }
  return user;
}

async function handleLogout(event) {
  const button = event.currentTarget;
  button.disabled = true;

  try {
    if (!CONFIG.USE_MOCK_API && loadSession()?.token) {
      await apiRequest("/auth/logout", { method: "POST", signal: AbortSignal.timeout(5000) });
    }
  } catch {
    // Local sign-out still works when the API is unreachable.
  } finally {
    clearSession();
    redirectToDashboard(null);
  }
}

async function submitAuthForm(form, path, payload, demoUser, fallbackMessage) {
  const button = form.querySelector('[type="submit"]');
  if (button.disabled) return;
  button.disabled = true;
  const label = button.lastElementChild;
  const initialLabel = label.textContent;
  label.textContent = t("En cours…", "Please wait…");
  form.setAttribute("aria-busy", "true");
  const feedback = document.getElementById("authFeedback");
  feedback.textContent = "";

  try {
    const session = CONFIG.USE_MOCK_API
      ? { ...demoUser, demo: true }
      : await apiRequest(path, { method: "POST", body: payload, authenticated: false, fallbackMessage });
    if (!DASHBOARDS.has(session?.role) || (!CONFIG.USE_MOCK_API && !hasActiveToken(session))) {
      throw new Error(t("Session reçue invalide.", "The API returned an invalid session."));
    }
    saveSession(session);
    redirectToDashboard(session);
  } catch (error) {
    feedback.textContent = error.message;
    button.disabled = false;
    label.textContent = initialLabel;
    form.removeAttribute("aria-busy");
  }
}
