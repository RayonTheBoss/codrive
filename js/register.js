document.addEventListener("DOMContentLoaded", () => {
  const session = loadSession();
  if (session) {
    redirectToDashboard(session);
    return;
  }

  document.getElementById("registerForm").addEventListener("submit", (event) => {
    event.preventDefault();
    const payload = {
      prenom: document.getElementById("prenom").value.trim(),
      nom: document.getElementById("nom").value.trim(),
      email: document.getElementById("email").value.trim(),
      telephone: document.getElementById("telephone").value.trim(),
      motDePasse: document.getElementById("password").value,
      role: document.getElementById("role").value
    };
    const { nom, prenom, email, role } = payload;
    submitAuthForm(
      event.currentTarget,
      "/auth/register",
      payload,
      { nom, prenom, email, role },
      t("Inscription impossible.", "Could not create your account.")
    );
  });
});
