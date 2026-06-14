package com.howners.gestion.service.rental;

import com.howners.gestion.domain.notification.NotificationType;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.PropertyRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.service.email.EmailService;
import com.howners.gestion.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RappelsReglementairesServiceTest {

    @Mock RentalRepository rentalRepository;
    @Mock PropertyRepository propertyRepository;
    @Mock NotificationService notificationService;
    @Mock EmailService emailService;

    @InjectMocks RappelsReglementairesService rappelsService;

    UUID ownerId;
    User owner;
    Property property;

    @BeforeEach
    void setup() {
        ownerId = UUID.randomUUID();
        owner = User.builder().id(ownerId).email("o@t.fr").firstName("O").lastName("W").build();
        property = new Property();
        property.setId(UUID.randomUUID());
        property.setOwner(owner);
        property.setName("Bien Test");

        lenient().when(rentalRepository.findActiveRentalsEndingOn(any())).thenReturn(List.of());
        lenient().when(rentalRepository.findActiveWithAssuranceExpiringBefore(any())).thenReturn(List.of());
        lenient().when(propertyRepository.findByDpeDateNotNullAndDpeDateBefore(any())).thenReturn(List.of());
    }

    @Test
    void notifieLaFinDeBailAJMoins30() {
        Rental rental = Rental.builder()
                .id(UUID.randomUUID())
                .property(property)
                .endDate(LocalDate.now().plusDays(30))
                .build();
        when(rentalRepository.findActiveRentalsEndingOn(LocalDate.now().plusDays(30)))
                .thenReturn(List.of(rental));

        rappelsService.verifierEcheances();

        verify(notificationService).create(eq(ownerId), eq(NotificationType.LEASE_END),
                anyString(), anyString(), anyString());
    }

    @Test
    void notifieLeDpeExactementAJMoins90() {
        // DPE réalisé il y a 10 ans moins 90 jours → expire dans exactement 90 jours
        property.setDpeDate(LocalDate.now().plusDays(90).minusYears(10));
        when(propertyRepository.findByDpeDateNotNullAndDpeDateBefore(any()))
                .thenReturn(List.of(property));

        rappelsService.verifierEcheances();

        verify(notificationService).create(eq(ownerId), eq(NotificationType.DPE_EXPIRY),
                anyString(), anyString(), anyString());
    }

    @Test
    void ignoreLeDpeHorsFenetre() {
        // Expire dans 45 jours → hors fenêtres 90/30/0
        property.setDpeDate(LocalDate.now().plusDays(45).minusYears(10));
        when(propertyRepository.findByDpeDateNotNullAndDpeDateBefore(any()))
                .thenReturn(List.of(property));

        rappelsService.verifierEcheances();

        verify(notificationService, never()).create(any(), eq(NotificationType.DPE_EXPIRY),
                anyString(), anyString(), anyString());
    }

    @Test
    void notifieLAssuranceAJMoins30AvecEmailLocataire() {
        User tenant = User.builder().id(UUID.randomUUID()).email("t@t.fr").firstName("T").lastName("E").build();
        Rental rental = Rental.builder()
                .id(UUID.randomUUID())
                .property(property)
                .tenant(tenant)
                .assuranceExpiration(LocalDate.now().plusDays(30))
                .build();
        when(rentalRepository.findActiveWithAssuranceExpiringBefore(any()))
                .thenReturn(List.of(rental));

        rappelsService.verifierEcheances();

        verify(notificationService).create(eq(ownerId), eq(NotificationType.INSURANCE_RENEWAL),
                anyString(), anyString(), anyString());
        verify(emailService).sendNotificationEmail(any());
    }

    @Test
    void ignoreLAssuranceHorsFenetre() {
        Rental rental = Rental.builder()
                .id(UUID.randomUUID())
                .property(property)
                .assuranceExpiration(LocalDate.now().plusDays(12))
                .build();
        when(rentalRepository.findActiveWithAssuranceExpiringBefore(any()))
                .thenReturn(List.of(rental));

        rappelsService.verifierEcheances();

        verify(notificationService, never()).create(any(), eq(NotificationType.INSURANCE_RENEWAL),
                anyString(), anyString(), anyString());
    }
}
