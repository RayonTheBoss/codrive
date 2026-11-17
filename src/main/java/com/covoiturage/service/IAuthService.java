package com.covoiturage.service;

import com.covoiturage.model.User;

import java.util.Optional;

public interface IAuthService {
    void creerCompte(User user);
    Optional<User> authentifier(String email, String motDePasse);
}
