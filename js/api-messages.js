// The API supplies French prose. Translate only its known messages at the display boundary.
const apiMessagesInEnglish = new Map([
  ["Connexion réussie.", "Signed in successfully."],
  ["Compte créé avec succès.", "Account created successfully."],
  ["Email ou mot de passe incorrect.", "Incorrect email address or password."],
  ["Email déjà utilisé.", "This email address is already in use."],
  ["Rôle invalide. Utilisez PASSAGER ou CHAUFFEUR.", "Choose Passenger or Driver."],
  ["Session absente, expirée ou invalide.", "Your session is missing, expired, or invalid. Please sign in."],
  ["Seul un passager connecté peut effectuer une réservation.", "Only signed-in passengers can book a trip."],
  ["Seul un chauffeur connecté peut gérer les trajets.", "Only signed-in drivers can manage trips."],
  ["Seul un administrateur connecté peut accéder à cette ressource.", "Only signed-in administrators can access this resource."],
  ["Utilisateur invalide.", "Invalid user account."],
  ["Utilisateur introuvable.", "User not found."],
  ["Utilisateur suspendu avec succès.", "User suspended."],
  ["Utilisateur bloqué avec succès.", "User blocked."],
  ["Utilisateur réactivé avec succès.", "User reactivated."],
  ["Vous ne pouvez pas suspendre ou bloquer votre propre compte.", "You cannot suspend or block your own account."],
  ["Réservation créée. Paiement autorisé, en attente d'acceptation par le chauffeur.",
    "Booking created. Payment authorized. Waiting for the driver to accept."],
  ["Réservation acceptée. Paiement capturé.", "Booking accepted. Payment captured."],
  ["Réservation annulée.", "Booking cancelled."],
  ["Réservation annulée. Autorisation de paiement libérée.", "Booking cancelled. Payment authorization released."],
  ["Évaluation enregistrée avec succès.", "Review saved successfully."],
  ["Données de réservation invalides.", "Invalid booking details."],
  ["Vous avez déjà une réservation active pour ce trajet.", "You already have an active booking for this trip."],
  ["Pas assez de places disponibles.", "There aren't enough seats available."],
  ["Compte passager introuvable.", "Passenger account not found."],
  ["Montant de réservation invalide.", "Invalid booking amount."],
  ["Le paiement n'a pas été autorisé.", "Payment authorization failed."],
  ["Vous ne pouvez accepter que les réservations de vos trajets.", "You can only accept bookings for your own trips."],
  ["Seules les réservations en attente peuvent être acceptées.", "Only pending bookings can be accepted."],
  ["Aucun paiement autorisé pour cette réservation.", "There is no authorized payment for this booking."],
  ["Cette réservation n'est plus active.", "This booking is no longer active."],
  ["Seule une réservation confirmée et sans évaluation peut être évaluée.", "Only confirmed bookings that haven't been reviewed can be reviewed."],
  ["Réservation introuvable.", "Booking not found."],
  ["Cette réservation appartient à un autre passager.", "This booking belongs to another passenger."],
  ["L'email de réservation doit correspondre à l'utilisateur connecté.", "The booking email must match the signed-in account."],
  ["Trajet invalide.", "Invalid trip details."],
  ["Trajet introuvable.", "Trip not found."],
  ["Le trajet est annulé ou a déjà débuté.", "This trip was cancelled or has already started."],
  ["Le trajet a déjà débuté.", "This trip has already started."],
  ["La date de départ doit être dans le futur.", "The departure date must be in the future."],
  ["La date de départ doit être au format ISO local.", "Enter the departure date and time in local ISO format."],
  ["Le prix par place doit être un montant positif.", "The price per seat must be positive."],
  ["Le nombre de places du trajet dépasse la capacité du véhicule sélectionné.",
    "The number of seats exceeds the selected vehicle's capacity."],
  ["Le véhicule sélectionné n'appartient pas au chauffeur connecté.", "The selected vehicle does not belong to your account."],
  ["Veuillez sélectionner un véhicule existant ou saisir un nouveau véhicule complet.",
    "Select an existing vehicle or enter all the details for a new one."],
  ["Un trajet identique existe déjà pour ce chauffeur et ce véhicule.", "This driver already has an identical trip for this vehicle."],
  ["Vous ne pouvez annuler que vos propres trajets.", "You can only cancel your own trips."],
  ["Ce trajet est déjà annulé.", "This trip has already been cancelled."],
  ["Requête invalide.", "Invalid request."],
  ["Corps de requête invalide.", "Invalid request data."],
  ["Erreur interne.", "An internal error occurred."]
]);

const apiMessageTemplatesInEnglish = [
  [/^Réservation annulée\. Remboursement (\d+)% soit (\S+) TND\.$/,
    "Booking cancelled. Refund: $1% ($2 TND)."],
  [/^Trajet annulé\. Compensation passager : (\d+)%\.$/,
    "Trip cancelled. Passenger compensation: $1%."],
  [/^Trajet annulé par l'administrateur\. Compensation passager : (\d+)%\.$/,
    "Trip cancelled by the administrator. Passenger compensation: $1%."]
];

const notificationTemplatesInEnglish = [
  [/^Votre réservation est en attente d'acceptation pour le trajet (.+)$/s,
    "Your booking for $1 is waiting for the driver to accept."],
  [/^Nouvelle demande de réservation en attente sur votre trajet (.+)$/s,
    "You have a new booking request for your trip $1."],
  [/^Votre réservation a été acceptée par le chauffeur pour le trajet (.+)$/s,
    "The driver accepted your booking for $1."],
  [/^Le passager a annulé une réservation sur votre trajet (.+)$/s,
    "A passenger cancelled their booking for your trip $1."],
  [/^Vous avez reçu une nouvelle évaluation\.$/,
    "You received a new review."],
  [/^Votre trajet (.+) a été publié\.$/s,
    "Your trip $1 was published."],
  [/^Le chauffeur a annulé le trajet (.+)\. Compensation : (\S+) TND\.$/s,
    "The driver cancelled the trip $1. Compensation: $2 TND."],
  [/^L'administrateur a annulé le trajet (.+)\. Compensation : (\S+) TND\.$/s,
    "The administrator cancelled the trip $1. Compensation: $2 TND."],
  [/^L'administrateur a annulé votre trajet (.+)\.$/s,
    "The administrator cancelled your trip $1."],
  [/^Vous avez annulé le trajet (.+)\.$/s,
    "You cancelled the trip $1."]
];

function translateMessageTemplate(message, templates) {
  for (const [pattern, translation] of templates) {
    if (pattern.test(message)) return message.replace(pattern, translation);
  }
}

function translateApiMessage(message, fallbackMessage) {
  if (getLanguage() !== "en" || typeof message !== "string" || !message) return message || fallbackMessage;
  return apiMessagesInEnglish.get(message)
    || translateMessageTemplate(message, apiMessageTemplatesInEnglish)
    || fallbackMessage
    || message;
}

function translateNotification(message) {
  if (getLanguage() !== "en" || typeof message !== "string") return message;
  return translateMessageTemplate(message, notificationTemplatesInEnglish) || message;
}
