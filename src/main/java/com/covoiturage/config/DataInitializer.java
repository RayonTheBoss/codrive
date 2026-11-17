package com.covoiturage.config;

import com.covoiturage.enums.TypePaiement;
import com.covoiturage.model.*;
import com.covoiturage.repository.UserRepository;
import com.covoiturage.service.PasswordHashing;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "codrive.demo-data", havingValue = "true")
public class DataInitializer {

    @Bean
    CommandLineRunner initData(UserRepository userRepository, PasswordHashing passwordHashing) {
        return args -> {
            if (userRepository.count() > 0) return;

            Chauffeur chauffeur1 = new Chauffeur("Ali", "Ben Salem", "ali@mail.com", "1234", "22111111");
            Chauffeur chauffeur2 = new Chauffeur("Meriem", "Jlassi", "meriem@mail.com", "1234", "22333333");
            Passager passager1 = new Passager("Sami", "Trabelsi", "sami@mail.com", "abcd", "22999999");
            Passager passager2 = new Passager("Youssef", "Mnif", "youssef@mail.com", "abcd", "22777777");
            Admin admin1 = new Admin("Admin", "System", "admin@mail.com", "admin123", "22000000");

            passager1.ajouterMoyenPaiement(new MoyenPaiement(TypePaiement.CARTE, "Visa finissant par 4242"));
            passager2.ajouterMoyenPaiement(new MoyenPaiement(TypePaiement.PAYPAL, "paypal@youssef.com"));

            Vehicule vehicule1 = new Vehicule("Peugeot", "208", "123-TUN-456", 4);
            Vehicule vehicule2 = new Vehicule("Hyundai", "i20", "555-TUN-999", 4);

            chauffeur1.ajouterVehicule(vehicule1);
            chauffeur2.ajouterVehicule(vehicule2);

            Trajet trajet1 = new Trajet("Sfax", "Tunis", LocalDateTime.now().plusDays(2).withHour(8).withMinute(30), 25.0, 3, chauffeur1, vehicule1);
            Trajet trajet2 = new Trajet("Sousse", "Monastir", LocalDateTime.now().plusDays(3).withHour(14).withMinute(0), 12.0, 2, chauffeur2, vehicule2);
            Trajet trajet3 = new Trajet("Gabès", "Sfax", LocalDateTime.now().plusDays(4).withHour(17).withMinute(15), 18.0, 4, chauffeur1, vehicule1);

            chauffeur1.ajouterTrajet(trajet1);
            chauffeur2.ajouterTrajet(trajet2);
            chauffeur1.ajouterTrajet(trajet3);

            List<User> demoUsers = List.of(chauffeur1, chauffeur2, passager1, passager2, admin1);
            demoUsers.forEach(user -> user.setMotDePasse(passwordHashing.hash(user.getMotDePasse())));
            userRepository.saveAll(demoUsers);
        };
    }
}
