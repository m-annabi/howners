import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject } from 'rxjs';
import { takeUntil } from 'rxjs/operators';
import { PaymentService } from '../../../core/services/payment.service';
import { NotificationService } from '../../../core/services/notification.service';
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
  creatingIntent = false;

  PaymentStatus = PaymentStatus;
  statusLabels = PAYMENT_STATUS_LABELS;
  statusColors = PAYMENT_STATUS_COLORS;
  typeLabels = PAYMENT_TYPE_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) {
      this.loadPayment(id);
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
      error: (err) => {
        console.error('Error loading payment:', err);
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
        error: (err) => {
          console.error('Error confirming payment:', err);
          this.confirming = false;
          this.notificationService.error('Erreur lors de la confirmation du paiement');
        }
      });
    }
  }

  createStripeIntent(): void {
    if (!this.payment) return;

    this.creatingIntent = true;
    this.paymentService.createStripeIntent(this.payment.id).pipe(takeUntil(this.destroy$)).subscribe({
      next: (response) => {
        this.creatingIntent = false;
        this.notificationService.success('Intent Stripe créé. ID: ' + response.paymentIntentId);
      },
      error: (err) => {
        console.error('Error creating Stripe intent:', err);
        this.creatingIntent = false;
        this.notificationService.error('Erreur lors de la création du paiement Stripe');
      }
    });
  }

  canConfirm(): boolean {
    return this.payment?.status === PaymentStatus.PENDING || this.payment?.status === PaymentStatus.LATE;
  }

  goBack(): void {
    this.router.navigate(['/payments']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
