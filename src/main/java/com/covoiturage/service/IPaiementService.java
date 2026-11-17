package com.covoiturage.service;

import com.covoiturage.model.MoyenPaiement;
import com.covoiturage.model.Paiement;
import com.covoiturage.model.User;

public interface IPaiementService {
    boolean autoriserPaiement(Paiement paiement);
    void capturerPaiement(Paiement paiement);
    MoyenPaiement getMoyenPaiement(User user);
}
