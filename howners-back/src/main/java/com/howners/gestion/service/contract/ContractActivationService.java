package com.howners.gestion.service.contract;

import com.howners.gestion.domain.contract.Contract;
import com.howners.gestion.domain.contract.ContractStatus;
import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.service.notification.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Activation automatique des contrats : un contrat signé devient ACTIF à la date de début du bail
 * (immédiatement si cette date est déjà passée ou inconnue), et la location passe de PENDING à
 * ACTIVE. Les deux parties sont prévenues. Plus aucune action manuelle du bailleur.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContractActivationService {

    private static final DateTimeFormatter FR_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final ContractRepository contractRepository;
    private final RentalRepository rentalRepository;
    private final NotificationDispatcher notificationDispatcher;

    /** À appeler dès qu'un contrat passe SIGNED : active tout de suite si la date de début est atteinte. */
    @Transactional
    public void activateIfDue(Contract contract) {
        if (contract.getStatus() != ContractStatus.SIGNED) return;
        LocalDate start = contract.getRental() != null ? contract.getRental().getStartDate() : null;
        if (start == null || !start.isAfter(LocalDate.now())) {
            activate(contract);
        } else {
            log.info("Contract {} signed, activation scheduled on {}", contract.getContractNumber(), start);
        }
    }

    /** Chaque nuit : active les contrats signés dont la date de début est arrivée. */
    @Scheduled(cron = "0 15 0 * * ?")
    @Transactional
    public void activateDueContracts() {
        List<Contract> signed = contractRepository.findByStatus(ContractStatus.SIGNED);
        int activated = 0;
        for (Contract contract : signed) {
            LocalDate start = contract.getRental() != null ? contract.getRental().getStartDate() : null;
            if (start == null || !start.isAfter(LocalDate.now())) {
                activate(contract);
                activated++;
            }
        }
        if (activated > 0) log.info("{} contrat(s) activé(s) automatiquement", activated);
    }

    private void activate(Contract contract) {
        contract.setStatus(ContractStatus.ACTIVE);
        contractRepository.save(contract);

        Rental rental = contract.getRental();
        if (rental != null && (rental.getStatus() == RentalStatus.PENDING || rental.getStatus() == RentalStatus.LISTED
                || rental.getStatus() == RentalStatus.VACANT)) {
            rental.setStatus(RentalStatus.ACTIVE);
            rentalRepository.save(rental);
        }
        log.info("Contract {} activated", contract.getContractNumber());

        if (rental == null) return;
        String property = rental.getProperty() != null ? rental.getProperty().getName() : "";
        String startLabel = rental.getStartDate() != null ? " à compter du " + rental.getStartDate().format(FR_DATE) : "";
        User owner = rental.getProperty() != null ? rental.getProperty().getOwner() : null;
        User tenant = rental.getTenant();
        String link = "/contracts/" + contract.getId();

        notificationDispatcher.notifyAndEmail(tenant, NotificationType.SIGNATURE_COMPLETED,
                "Votre bail est actif",
                "Le contrat " + contract.getContractNumber() + " pour " + property + " est actif" + startLabel + ".",
                link,
                new NotificationDispatcher.Email(
                        "Votre bail est actif — " + property,
                        "Bail actif",
                        "Toutes les signatures sont réunies : votre bail pour <strong>" + property + "</strong> est actif"
                                + startLabel + ". Vos échéances de loyer et vos quittances sont disponibles dans votre espace.",
                        null, "Voir mon contrat", null, false));
        notificationDispatcher.notify(owner, NotificationType.SIGNATURE_COMPLETED,
                "Contrat activé",
                "Le contrat " + contract.getContractNumber() + " (" + property + ") est actif" + startLabel + ".",
                link);
    }
}
