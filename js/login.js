document.addEventListener("DOMContentLoaded", () => {
  const session = loadSession();
  if (session) {
    redirectToDashboard(session);
    return;
  }

  document.getElementById("loginForm").addEventListener("submit", (event) => {
    event.preventDefault();
    const email = document.getElementById("loginEmail").value.trim();
    const motDePasse = document.getElementById("loginPassword").value;
    submitAuthForm(
      event.currentTarget,
      "/auth/login",
      { email, motDePasse },
      { nom: t("Démo", "Demo"), prenom: t("Passager", "Passenger"), email, role: "PASSAGER" },
      t("Connexion échouée.", "Sign-in failed.")
    );
  });
});
