package com.howners.gestion.service.payment;

import com.howners.gestion.domain.payment.Payment;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.payment.PaymentType;
import com.howners.gestion.domain.property.Property;
import com.howners.gestion.domain.rental.Rental;
import com.howners.gestion.domain.rental.RentalStatus;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.RentalRepository;
import com.howners.gestion.service.notification.NotificationDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentScheduleServiceTest {

    @Mock PaymentRepository paymentRepository;
    @Mock RentalRepository rentalRepository;
    @Mock NotificationDispatcher notificationDispatcher;

    @InjectMocks RentScheduleService rentScheduleService;

    Rental rental;
    YearMonth month;

    @BeforeEach
    void setup() {
        month = YearMonth.now();
        User tenant = User.builder().id(UUID.randomUUID()).email("t@t.fr").role(Role.TENANT).build();
        User owner = User.builder().id(UUID.randomUUID()).email("o@t.fr").role(Role.OWNER).build();
        Property property = Property.builder().id(UUID.randomUUID()).name("T2 Centre").owner(owner).build();
        rental = Rental.builder()
                .id(UUID.randomUUID())
                .property(property)
                .tenant(tenant)
                .status(RentalStatus.ACTIVE)
                .startDate(month.minusMonths(2).atDay(1))
                .monthlyRent(new BigDecimal("800.00"))
                .charges(new BigDecimal("50.00"))
                .paymentDay(5)
                .currency("EUR")
                .build();
        lenient().when(paymentRepository.save(any(Payment.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void genere_le_loyer_du_mois_complet_avec_charges() {
        when(paymentRepository.existsAnyRentPaymentInMonth(any(), any(), any())).thenReturn(false);

        boolean created = rentScheduleService.ensureRentPayment(rental, month);

        assertThat(created).isTrue();
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        Payment p = saved.getValue();
        assertThat(p.getPaymentType()).isEqualTo(PaymentType.RENT);
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(p.getAmount()).isEqualByComparingTo("850.00");
        // Échéance : jour de paiement du bail, jamais dans le passé.
        assertThat(p.getDueDate()).isAfterOrEqualTo(LocalDate.now());
        assertThat(YearMonth.from(p.getDueDate())).isEqualTo(month);
    }

    @Test
    void ne_cree_pas_de_doublon_si_une_echeance_existe_deja_sur_le_mois() {
        // Une échéance créée MANUELLEMENT par le bailleur (ou déjà générée) bloque la génération.
        when(paymentRepository.existsAnyRentPaymentInMonth(any(), any(), any())).thenReturn(true);

        boolean created = rentScheduleService.ensureRentPayment(rental, month);

        assertThat(created).isFalse();
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void proratise_le_premier_mois_en_cas_d_entree_en_cours_de_mois() {
        // Entrée le 16 d'un mois de 30 jours : 15/30 jours couverts → moitié du loyer CC.
        YearMonth m = YearMonth.of(2026, 9);
        rental.setStartDate(LocalDate.of(2026, 9, 16));
        when(paymentRepository.existsAnyRentPaymentInMonth(any(), any(), any())).thenReturn(false);

        boolean created = rentScheduleService.ensureRentPayment(rental, m);

        assertThat(created).isTrue();
        ArgumentCaptor<Payment> saved = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(saved.capture());
        assertThat(saved.getValue().getAmount()).isEqualByComparingTo("425.00");
        // L'échéance ne précède jamais l'entrée dans les lieux.
        assertThat(saved.getValue().getDueDate()).isAfterOrEqualTo(LocalDate.of(2026, 9, 16));
    }

    @Test
    void ignore_les_baux_sans_locataire_ou_hors_periode() {
        rental.setTenant(null);
        assertThat(rentScheduleService.ensureRentPayment(rental, month)).isFalse();

        rental.setTenant(User.builder().id(UUID.randomUUID()).role(Role.TENANT).build());
        rental.setEndDate(month.minusMonths(1).atEndOfMonth());
        assertThat(rentScheduleService.ensureRentPayment(rental, month)).isFalse();

        verify(paymentRepository, never()).save(any());
    }

    @Test
    void le_rattrapage_quotidien_couvre_tous_les_baux_actifs() {
        when(rentalRepository.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of(rental));
        when(paymentRepository.existsAnyRentPaymentInMonth(any(), any(), any())).thenReturn(false);

        rentScheduleService.generateMonthlyRents();

        verify(paymentRepository).save(any(Payment.class));
    }
}
