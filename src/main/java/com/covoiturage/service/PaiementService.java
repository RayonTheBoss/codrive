package com.covoiturage.service;

import com.covoiturage.model.MoyenPaiement;
import com.covoiturage.model.Paiement;
import com.covoiturage.model.User;
import com.covoiturage.repository.PaiementRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@Transactional
public class PaiementService implements IPaiementService {
    private final PaiementRepository paiementRepository;

    public PaiementService(PaiementRepository paiementRepository) {
        this.paiementRepository = paiementRepository;
    }

    @Override
    public boolean autoriserPaiement(Paiement paiement) {
        boolean ok = paiement.payer();
        paiementRepository.save(paiement);
        return ok;
    }

    @Override
    public void capturerPaiement(Paiement paiement) {
        paiement.capturer();
        paiementRepository.save(paiement);
    }

    public Annulation reglerAnnulation(Paiement paiement, double tauxRemboursement, double tauxCompensation) {
        if (paiement == null) return new Annulation(0, 0);

        double remboursement = paiement.isCapture() ? calculerMontant(paiement.getMontant(), tauxRemboursement) : 0;
        double compensation = calculerMontant(paiement.getMontant(), tauxCompensation);
        paiement.reglerAnnulation(remboursement, compensation);
        paiementRepository.save(paiement);
        return new Annulation(remboursement, compensation);
    }

    private double calculerMontant(double montant, double taux) {
        return BigDecimal.valueOf(montant)
                .multiply(BigDecimal.valueOf(taux))
                .setScale(3, RoundingMode.HALF_UP)
                .doubleValue();
    }

    @Override
    public MoyenPaiement getMoyenPaiement(User user) {
        return user.getMoyensPaiement().isEmpty() ? null : user.getMoyensPaiement().get(0);
    }

    public record Annulation(double remboursement, double compensation) {}
}
