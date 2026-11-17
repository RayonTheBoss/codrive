package com.covoiturage.service;

import com.covoiturage.model.Trajet;
import com.covoiturage.model.User;
import java.util.List;

public interface ITrajetService {
    void proposerTrajet(Trajet trajet);
    String annulerTrajet(int id, User acteur);
    List<Trajet> getTrajets();
}
