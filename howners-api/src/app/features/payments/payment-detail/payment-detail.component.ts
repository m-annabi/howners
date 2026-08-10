import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PaymentService } from '../../../core/services/payment.service';
import { NotificationService } from '../../../core/services/notification.service';
import { AuthService } from '../../../core/auth/auth.service';
import {
  Payment,
  PaymentStatus,
  PAYMENT_STATUS_LABELS,
  PAYMENT_STATUS_COLORS,
  PAYMENT_TYPE_LABELS
} from '../../../core/models/payment.model';

@Component({
  selector: 'app-payment-detail',
  templateUrl: './payment-detail.component.html',
  styles: []
})
export class PaymentDetailComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();

  payment: Payment | null = null;
  loading = false;
  error: string | null = null;
  confirming = false;
  paying = false;
  finalizing = false;

  PaymentStatus = PaymentStatus;
  statusLabels = PAYMENT_STATUS_LABELS;
  statusColors = PAYMENT_STATUS_COLORS;
  typeLabels = PAYMENT_TYPE_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private notificationService: NotificationService,
    private authService: AuthService
  ) {}

  get isOwner(): boolean {
    return this.authService.hasRole('OWNER') || this.authService.hasRole('ADMIN');
  }

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      const sessionId = this.route.snapshot.queryParamMap.get('session_id');
      if (sessionId) {
        this.finalizeCheckout(id, sessionId);
      } else {
        this.loadPayment(id);
      }
    }
  }

  loadPayment(id: string): void {
    this.loading = true;
    this.error = null;

    this.paymentService.getById(id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (payment) => {
        this.payment = payment;
        this.loading = false;
      },
      error: () => {
        this.error = 'Erreur lors du chargement du paiement';
        this.loading = false;
      }
    });
  }

  confirmPayment(): void {
    if (!this.payment) return;

    if (confirm('Confirmer ce paiement comme reçu ?')) {
      this.confirming = true;
      this.paymentService.confirmPayment(this.payment.id).pipe(takeUntil(this.destroy$)).subscribe({
        next: (payment) => {
          this.payment = payment;
          this.confirming = false;
          this.notificationService.success('Paiement confirmé avec succès. Une quittance a été générée.');
        },
        error: () => {
          this.confirming = false;
          this.notificationService.error('Erreur lors de la confirmation du paiement');
        }
      });
    }
  }

  canConfirm(): boolean {
    // Confirmer un paiement comme « reçu » (et générer la quittance) est une
    // action réservée au propriétaire/admin. Le locataire ne doit pas y accéder.
    if (!this.isOwner) return false;
    return this.payment?.status === PaymentStatus.PENDING || this.payment?.status === PaymentStatus.LATE;
  }

  /** Le locataire (payeur) peut régler en ligne un loyer non encore payé. */
  canPay(): boolean {
    if (this.isOwner) return false;
    return this.payment?.status === PaymentStatus.PENDING || this.payment?.status === PaymentStatus.LATE;
  }

  payNow(): void {
    if (!this.payment) return;
    this.paying = true;
    this.paymentService.createCheckout(this.payment.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (res) => {
        window.location.href = res.checkoutUrl;
      },
      error: (err) => {
        this.paying = false;
        this.notificationService.error(err.error?.message || 'Impossible de démarrer le paiement');
      }
    });
  }

  private finalizeCheckout(paymentId: string, sessionId: string): void {
    this.loading = true;
    this.finalizing = true;
    this.paymentService.finalizeCheckout(paymentId, sessionId).pipe(takeUntil(this.destroy$)).subscribe({
      next: (payment) => {
        this.payment = payment;
        this.loading = false;
        this.finalizing = false;
        this.notificationService.success('Paiement effectué. Une quittance a été générée.');
        // Nettoie l'URL (retire session_id)
        this.router.navigate([], { relativeTo: this.route, queryParams: {} });
      },
      error: () => {
        this.finalizing = false;
        // La session n'est pas (encore) payée : on affiche simplement le paiement.
        this.loadPayment(paymentId);
      }
    });
  }

  goBack(): void {
    this.router.navigate(['/payments']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
