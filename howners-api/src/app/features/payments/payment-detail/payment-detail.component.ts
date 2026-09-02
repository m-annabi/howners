import { NavigationService } from '../../../core/services/navigation.service';
import { ConfirmDialogService } from '../../../shared/components/confirm-dialog/confirm-dialog.service';
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
  PAYMENT_TYPE_LABELS,
  DECLARED_METHOD_LABELS
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
  declaring = false;
  declaredMethodLabels = DECLARED_METHOD_LABELS;

  /** Frais Stripe indicatifs (cartes européennes) : 1,5 % + 0,25 € par transaction. */
  readonly stripeFeePercent = 1.5;
  readonly stripeFeeFixed = 0.25;

  PaymentStatus = PaymentStatus;
  statusLabels = PAYMENT_STATUS_LABELS;
  statusColors = PAYMENT_STATUS_COLORS;
  typeLabels = PAYMENT_TYPE_LABELS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private paymentService: PaymentService,
    private notificationService: NotificationService,
    private authService: AuthService,
    private confirmDialog: ConfirmDialogService,
    private nav: NavigationService
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

    this.confirmDialog.confirm('Confirmation', 'Confirmer ce paiement comme reçu ?', 'warning').subscribe(ok => {
      if (!ok) return;
      this.confirming = true;
      this.paymentService.confirmPayment(this.payment!.id).pipe(takeUntil(this.destroy$)).subscribe({
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
    });
  }

  // ── Relance des impayés (propriétaire) ────────────────────────────────────
  relancing = false;

  /** Une relance (niveau 1) puis une mise en demeure (niveau 2) sont possibles tant que l'impayé court. */
  canRelancer(): boolean {
    if (!this.isOwner || !this.payment) return false;
    const unpaid = this.payment.status === PaymentStatus.LATE || this.payment.status === PaymentStatus.PENDING;
    return unpaid && (this.payment.relanceNiveau ?? 0) < 2;
  }

  get relanceLabel(): string {
    return (this.payment?.relanceNiveau ?? 0) === 0 ? 'Envoyer une relance' : 'Envoyer la mise en demeure';
  }

  relancer(): void {
    if (!this.payment || this.relancing) return;
    const niveau = this.payment.relanceNiveau ?? 0;
    const question = niveau === 0
      ? 'Envoyer une relance amiable au locataire par e-mail ?'
      : 'Envoyer la mise en demeure (courrier PDF horodaté, joint au bail) ?';
    this.confirmDialog.confirm('Confirmation', question, 'warning').subscribe(ok => {
      if (!ok) return;
      this.relancing = true;
      this.paymentService.relancer(this.payment!.id).pipe(takeUntil(this.destroy$)).subscribe({
        next: (payment) => {
          this.payment = payment;
          this.relancing = false;
          this.notificationService.success(niveau === 0
            ? 'Relance envoyée au locataire.'
            : 'Mise en demeure envoyée. Le courrier est disponible dans les documents du bail.');
        },
        error: (err) => {
          this.relancing = false;
          this.notificationService.error(err.error?.message || 'Impossible d\'envoyer la relance');
        }
      });
    });
  }

  canConfirm(): boolean {
    // Confirmer un paiement comme « reçu » (et générer la quittance) est une
    // action réservée au propriétaire/admin. Le locataire ne doit pas y accéder.
    if (!this.isOwner) return false;
    return this.payment?.status === PaymentStatus.PENDING || this.payment?.status === PaymentStatus.LATE;
  }

  /** Le locataire (payeur) peut régler en ligne un loyer non encore payé, si le propriétaire a activé le paiement carte. */
  canPay(): boolean {
    if (this.isOwner || !this.payment?.onlinePaymentAvailable) return false;
    return this.payment?.status === PaymentStatus.PENDING || this.payment?.status === PaymentStatus.LATE;
  }

  /** Le locataire doit régler hors plateforme (déclaratif) : on lui affiche les coordonnées du propriétaire. */
  get showPaymentInstructions(): boolean {
    if (this.isOwner || !this.payment) return false;
    const pending = this.payment.status === PaymentStatus.PENDING || this.payment.status === PaymentStatus.LATE;
    return pending && !this.payment.onlinePaymentAvailable;
  }

  /** Le locataire peut déclarer un règlement hors plateforme tant que le paiement est en attente et non déjà déclaré. */
  canDeclare(): boolean {
    if (this.isOwner || !this.payment || this.payment.declaredAt) return false;
    return this.payment.status === PaymentStatus.PENDING || this.payment.status === PaymentStatus.LATE;
  }

  declare(method: 'BANK_TRANSFER' | 'CHECK' | 'CASH' = 'BANK_TRANSFER'): void {
    if (!this.payment || this.declaring) return;
    const label = this.declaredMethodLabels[method];
    this.confirmDialog.confirm('Déclarer le règlement',
      `Confirmez-vous avoir réglé ${this.payment.amount} ${this.payment.currency} par ${label} ? Votre propriétaire sera prévenu et confirmera la réception.`,
      'warning').subscribe(ok => {
      if (!ok) return;
      this.declaring = true;
      this.paymentService.declare(this.payment!.id, method).pipe(takeUntil(this.destroy$)).subscribe({
        next: (payment) => {
          this.payment = payment;
          this.declaring = false;
          this.notificationService.success('Règlement déclaré. Votre propriétaire confirmera la réception.');
        },
        error: (err) => {
          this.declaring = false;
          this.notificationService.error(err.error?.message || 'Impossible de déclarer ce règlement');
        }
      });
    });
  }

  /** Frais estimés d'un paiement carte (commission plateforme + Stripe), à la charge du bailleur. */
  get onlineFees(): { platform: number; stripe: number; total: number; net: number } | null {
    if (!this.payment || this.payment.platformFeePercent == null) return null;
    const amount = this.payment.amount;
    const platform = Math.round(amount * this.payment.platformFeePercent) / 100;
    const stripe = Math.round((amount * this.stripeFeePercent / 100 + this.stripeFeeFixed) * 100) / 100;
    const total = Math.round((platform + stripe) * 100) / 100;
    return { platform, stripe, total, net: Math.round((amount - total) * 100) / 100 };
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
    this.nav.back(['/payments']);
  }

  ngOnDestroy(): void {
    this.destroy$.next();
    this.destroy$.complete();
  }
}
