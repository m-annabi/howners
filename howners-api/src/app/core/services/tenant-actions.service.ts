import { Injectable } from '@angular/core';
import { BehaviorSubject, forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { ApplicationService } from './application.service';
import { ContractService } from './contract.service';
import { ApplicationStatus } from '../models/application.model';
import { ContractStatus } from '../models/contract.model';

export interface TenantActionBadges {
  /** Candidatures acceptées dont le contrat n'est pas encore signé (quelque chose avance / à faire). */
  applications: number;
  /** Contrats en attente de la signature du locataire. */
  contracts: number;
}

/**
 * Compteurs « action requise » du locataire, affichés en badge dans le menu
 * (Mes candidatures / Mon contrat). Rafraîchis explicitement : à la construction du
 * menu, après une signature et après un retrait de candidature.
 */
@Injectable({ providedIn: 'root' })
export class TenantActionsService {
  private subject = new BehaviorSubject<TenantActionBadges>({ applications: 0, contracts: 0 });
  readonly badges$ = this.subject.asObservable();

  constructor(
    private applicationService: ApplicationService,
    private contractService: ContractService
  ) {}

  refresh(): void {
    forkJoin({
      applications: this.applicationService.getMyApplications().pipe(catchError(() => of([]))),
      contracts: this.contractService.getMyContracts().pipe(catchError(() => of([])))
    }).subscribe(({ applications, contracts }) => {
      const contractsToSign = contracts.filter(c => c.status === ContractStatus.SENT).length;
      const acceptedPending = applications.filter(a =>
        a.status === ApplicationStatus.ACCEPTED
          && (!a.contractStatus || a.contractStatus === 'SENT')
      ).length;
      this.subject.next({ applications: acceptedPending, contracts: contractsToSign });
    });
  }

  clear(): void {
    this.subject.next({ applications: 0, contracts: 0 });
  }
}
