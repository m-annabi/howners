package com.howners.gestion.service.rental;

import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.exception.ForbiddenException;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.auth.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Contrôle d'accès partagé aux données d'un bail. Remplace les copies locales
 * (assertRentalAccess / checkRentalAccess / checkAccess) qui s'étaient multipliées
 * dans les services paiements, factures, quittances, états des lieux et templates.
 *
 * Deux niveaux :
 *  - participant : propriétaire du bien, locataire du bail, ou administrateur ;
 *  - propriétaire : propriétaire du bien ou administrateur.
 */
@Service
@RequiredArgsConstructor
public class RentalAccessService {

    private final UserRepository userRepository;

    /** Lève une ForbiddenException si l'utilisateur courant n'est ni propriétaire, ni locataire, ni admin. */
    public void assertParticipant(Rental rental, String message) {
        UUID currentUserId = AuthService.getCurrentUserId();
        if (!isOwner(rental, currentUserId) && !isTenant(rental, currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException(message);
        }
    }

    public void assertParticipant(Rental rental) {
        assertParticipant(rental, "Vous n'êtes pas autorisé à accéder à ce bail.");
    }

    /** Lève une ForbiddenException si l'utilisateur courant n'est ni propriétaire du bien, ni admin. */
    public void assertOwner(Rental rental, String message) {
        UUID currentUserId = AuthService.getCurrentUserId();
        if (!isOwner(rental, currentUserId) && !isAdmin(currentUserId)) {
            throw new ForbiddenException(message);
        }
    }

    public void assertOwner(Rental rental) {
        assertOwner(rental, "Cette location ne vous appartient pas.");
    }

    public boolean isAdmin(UUID userId) {
        return userRepository.findById(userId).map(User::getRole).map(r -> r == Role.ADMIN).orElse(false);
    }

    private static boolean isOwner(Rental rental, UUID userId) {
        return rental.getProperty() != null && rental.getProperty().getOwner() != null
                && userId.equals(rental.getProperty().getOwner().getId());
    }

    private static boolean isTenant(Rental rental, UUID userId) {
        return rental.getTenant() != null && userId.equals(rental.getTenant().getId());
    }
}
