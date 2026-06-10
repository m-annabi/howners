package com.howners.gestion.service.contract;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.contract.ContractStatus;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.ContractExpiryEmailData;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Service planifie pour l'envoi d'emails d'alerte d'echeance de contrat (J-30).
 * Previent les bailleurs lorsqu'un contrat de location arrive a echeance dans 30 jours.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractExpiryReminderService {

    private final RentalRepository rentalRepository;
    private final ContractRepository contractRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    /**
     * Execute chaque jour a 10h30. Recherche les locations actives dont la date de fin
     * est dans exactement 30 jours et envoie un email d'alerte au bailleur.
     */
    @Scheduled(cron = "0 30 10 * * ?")
    @Transactional(readOnly = true)
    public void sendContractExpiryWarnings() {
        log.info("Demarrage du job d'alerte d'echeance de contrat (J-30)");

        LocalDate targetDate = LocalDate.now().plusDays(30);
        List<Rental> expiringRentals = rentalRepository.findActiveRentalsEndingOn(targetDate);
        log.info("{} location(s) active(s) arrivant a echeance le {}", expiringRentals.size(), targetDate);

        int sent = 0;

        for (Rental rental : expiringRentals) {
            try {
                Property property = rental.getProperty();
                User owner = property.getOwner();
                User tenant = rental.getTenant();

                // Rechercher les contrats actifs/signes lies a cette location
                List<Contract> contracts = contractRepository.findByRentalId(rental.getId()).stream()
                        .filter(c -> c.getStatus() == ContractStatus.ACTIVE
                                || c.getStatus() == ContractStatus.SIGNED)
                        .toList();

                if (contracts.isEmpty()) {
                    log.debug("Aucun contrat actif/signe pour la location {} — ignore", rental.getId());
                    continue;
                }

                String tenantName = tenant != null ? tenant.getFullName() : "Locataire non renseigne";
                String ownerName = owner.getFirstName() != null ? owner.getFirstName() : owner.getFullName();

                for (Contract contract : contracts) {
                    ContractExpiryEmailData data = ContractExpiryEmailData.builder()
                            .recipientEmail(owner.getEmail())
                            .recipientName(ownerName)
                            .tenantName(tenantName)
                            .propertyName(property.getName())
                            .contractNumber(contract.getContractNumber())
                            .expiryDate(targetDate.format(DATE_FORMATTER))
                            .contractViewUrl(frontendUrl + "/contracts/" + contract.getId())
                            .build();

                    emailService.sendContractExpiryWarningEmail(data);
                    sent++;
                    log.info("Email d'alerte d'echeance envoye a {} pour le contrat {}",
                            owner.getEmail(), contract.getContractNumber());
                }
            } catch (Exception e) {
                log.error("Echec de l'envoi de l'alerte d'echeance pour la location {} : {}",
                        rental.getId(), e.getMessage());
            }
        }

        log.info("Job d'alerte d'echeance termine — {} email(s) envoye(s)", sent);
    }
}
