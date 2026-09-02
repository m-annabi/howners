import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { PublicContractService } from '../../../core/services/public-contract.service';
import { ContractPublicView } from '../../../core/models/esignature.model';

type CompletionState = 'success' | 'declined' | 'aborted' | 'processing';

/**
 * Page de retour après la cérémonie de signature externe (DocuSign renvoie le navigateur
 * ici avec `?token=…&event=…`). Publique : le signataire n'est généralement pas connecté.
 */
@Component({
  selector: 'app-public-sign-complete',
  templateUrl: './public-sign-complete.component.html',
  styleUrls: ['./public-sign-complete.component.scss']
})
export class PublicSignCompleteComponent implements OnInit {
  state: CompletionState = 'processing';
  contract: ContractPublicView | null = null;
  loading = true;

  constructor(
    private route: ActivatedRoute,
    private publicContractService: PublicContractService
  ) {}

  ngOnInit(): void {
    const params = this.route.snapshot.queryParamMap;
    const token = params.get('token');
    this.state = this.stateFromEvent(params.get('event'));

    if (!token) {
      this.loading = false;
      return;
    }

    // Les détails servent à personnaliser la page ; une erreur (token déjà consommé
    // après signature, par exemple) ne doit pas masquer la confirmation.
    this.publicContractService.getContractByToken(token).subscribe({
      next: contract => {
        this.contract = contract;
        if (this.state === 'processing' && ['SIGNED', 'ACTIVE'].includes(contract.status)) {
          this.state = 'success';
        }
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  /** Événements de fin de cérémonie DocuSign (paramètre `event` de l'URL de retour). */
  private stateFromEvent(event: string | null): CompletionState {
    switch (event) {
      case 'signing_complete': return 'success';
      case 'decline':          return 'declined';
      case 'cancel':
      case 'session_timeout':
      case 'ttl_expired':
      case 'exception':
      case 'access_code_failed':
      case 'id_check_failed':  return 'aborted';
      default:                 return 'processing';
    }
  }
}
