package com.covoiturage.service;

import com.covoiturage.enums.StatutCompte;
import com.covoiturage.exception.BusinessException;
import com.covoiturage.model.User;
import com.covoiturage.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@Transactional
public class AuthService implements IAuthService {
    private final UserRepository userRepository;
    private final PasswordHashing passwordHashing;
    private final String dummyHash;

    public AuthService(UserRepository userRepository, PasswordHashing passwordHashing) {
        this.userRepository = userRepository;
        this.passwordHashing = passwordHashing;
        this.dummyHash = passwordHashing.hash("verification-sans-compte");
    }

    @Override
    public void creerCompte(User user) {
        if (user == null) throw new BusinessException("Utilisateur invalide.");
        user.setEmail(normalizeEmail(user.getEmail()));
        if (userRepository.existsByEmailIgnoreCase(user.getEmail())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé.");
        }
        user.setMotDePasse(passwordHashing.hash(user.getMotDePasse()));
        try {
            userRepository.saveAndFlush(user);
        } catch (DataIntegrityViolationException ex) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé.", ex);
        }
    }

    @Override
    public Optional<User> authentifier(String email, String motDePasse) {
        User user = userRepository.findByEmailIgnoreCase(normalizeEmail(email)).orElse(null);
        if (user == null || user.getStatutCompte() != StatutCompte.ACTIF) {
            passwordHashing.matches(motDePasse, dummyHash);
            return Optional.empty();
        }

        String storedPassword = user.getMotDePasse();
        if (!passwordHashing.matches(motDePasse, storedPassword)) {
            if (!passwordHashing.isHashed(storedPassword)) passwordHashing.matches(motDePasse, dummyHash);
            user.enregistrerEchecConnexion();
            return Optional.empty();
        }

        if (!passwordHashing.isHashed(storedPassword)) {
            user.setMotDePasse(passwordHashing.hash(motDePasse));
        }
        user.enregistrerConnexionReussie();
        return Optional.of(user);
    }

    public void changeAccountStatus(int id, StatutCompte status) {
        User user = userRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Utilisateur introuvable."));
        switch (status) {
            case ACTIF -> user.activerCompte();
            case SUSPENDU -> user.suspendreCompte();
            case BLOQUE -> user.bloquerCompte();
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> findUserById(int id) { return userRepository.findById(id); }

    public List<User> getUsers() { return userRepository.findAll(); }

    public User saveUser(User user) { return userRepository.save(user); }

    private String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
