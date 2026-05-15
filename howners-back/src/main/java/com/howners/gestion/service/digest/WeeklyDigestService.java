package com.howners.gestion.service.digest;

import com.howners.gestion.domain.contract.ContractStatus;
import com.howners.gestion.domain.contract.SignatureRequestStatus;
import com.howners.gestion.domain.payment.PaymentStatus;
import com.howners.gestion.domain.application.ApplicationStatus;
import com.howners.gestion.domain.user.Role;
import com.howners.gestion.domain.user.User;
import com.howners.gestion.dto.email.WeeklyDigestEmailData;
import com.howners.gestion.repository.ApplicationRepository;
import com.howners.gestion.repository.ContractRepository;
import com.howners.gestion.repository.ContractSignatureRequestRepository;
import com.howners.gestion.repository.PaymentRepository;
import com.howners.gestion.repository.UserRepository;
import com.howners.gestion.service.email.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeeklyDigestService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;
    private final ContractRepository contractRepository;
    private final ContractSignatureRequestRepository signatureRequestRepository;
    private final ApplicationRepository applicationRepository;
    private final EmailService emailService;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Sends a weekly "À traiter" digest to every owner with at least one
     * actionable item. Fires every Monday at 09:00 server time.
     */
    @Scheduled(cron = "0 0 9 ? * MON")
    @Transactional(readOnly = true)
    public void runWeeklyDigest() {
        log.info("Running weekly digest job");
        LocalDate today = LocalDate.now();
        LocalDate in30 = today.plusDays(30);

        int sent = 0, skipped = 0;
        for (User owner : userRepository.findByRole(Role.OWNER)) {
            try {
                long late = paymentRepository.findByOwnerId(owner.getId()).stream()
                        .filter(p -> p.getStatus() == PaymentStatus.LATE
                                || (p.getStatus() == PaymentStatus.PENDING
                                    && p.getDueDate() != null
                                    && p.getDueDate().isBefore(today)))
                        .count();

                long expiring = contractRepository.findAll().stream()
                        .filter(c -> c.getRental() != null
                                && c.getRental().getProperty() != null
                                && c.getRental().getProperty().getOwner() != null
                                && owner.getId().equals(c.getRental().getProperty().getOwner().getId()))
                        .filter(c -> c.getStatus() == ContractStatus.ACTIVE
                                || c.getStatus() == ContractStatus.SIGNED)
                        .filter(c -> {
                            LocalDate end = c.getRental().getEndDate();
                            return end != null && !end.isBefore(today) && !end.isAfter(in30);
                        })
                        .count();

                long awaitingSig = contractRepository.findAll().stream()
                        .filter(c -> c.getRental() != null
                                && c.getRental().getProperty() != null
                                && c.getRental().getProperty().getOwner() != null
                                && owner.getId().equals(c.getRental().getProperty().getOwner().getId()))
                        .filter(c -> c.getStatus() == ContractStatus.SENT)
                        .count();

                long pendingApps = applicationRepository.findAll().stream()
                        .filter(a -> a.getListing() != null
                                && a.getListing().getProperty() != null
                                && a.getListing().getProperty().getOwner() != null
                                && owner.getId().equals(a.getListing().getProperty().getOwner().getId()))
                        .filter(a -> a.getStatus() == ApplicationStatus.SUBMITTED
                                || a.getStatus() == ApplicationStatus.UNDER_REVIEW)
                        .count();

                WeeklyDigestEmailData data = WeeklyDigestEmailData.builder()
                        .recipientEmail(owner.getEmail())
                        .ownerName(owner.getFirstName() != null ? owner.getFirstName() : owner.getFullName())
                        .latePayments(late)
                        .expiringContracts(expiring)
                        .awaitingSignatures(awaitingSig)
                        .pendingApplications(pendingApps)
                        .dashboardUrl(frontendUrl + "/dashboard")
                        .build();

                if (data.hasContent()) {
                    emailService.sendWeeklyDigestEmail(data);
                    sent++;
                } else {
                    skipped++;
                }
            } catch (Exception e) {
                log.error("Weekly digest failed for owner {}: {}", owner.getEmail(), e.getMessage());
            }
        }
        log.info("Weekly digest completed — {} sent, {} skipped (nothing to flag)", sent, skipped);
    }
}
