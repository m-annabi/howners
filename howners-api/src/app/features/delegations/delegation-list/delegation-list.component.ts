import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { DelegationService } from '../../../core/services/delegation.service';
import { Delegation } from '../../../core/models/delegation.model';

@Component({
  selector: 'app-delegation-list',
  templateUrl: './delegation-list.component.html',
  styleUrls: ['./delegation-list.component.scss']
})
export class DelegationListComponent implements OnInit {
  mesDelegations: Delegation[] = [];
  delegationsRecues: Delegation[] = [];
  loading = false;
  inviting = false;
  error: string | null = null;
  successMessage: string | null = null;

  inviteForm: FormGroup;

  constructor(
    private delegationService: DelegationService,
    private fb: FormBuilder
  ) {
    this.inviteForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.delegationService.getMesDelegations().subscribe({
      next: (delegations) => {
        this.mesDelegations = delegations;
        this.loading = false;
      },
      error: () => this.loading = false
    });
    this.delegationService.getDelegationsRecues().subscribe({
      next: (delegations) => this.delegationsRecues = delegations
    });
  }

  inviter(): void {
    if (this.inviteForm.invalid) return;
    this.inviting = true;
    this.error = null;
    this.successMessage = null;
    this.delegationService.inviter(this.inviteForm.value.email).subscribe({
      next: () => {
        this.inviting = false;
        this.successMessage = 'Accès délégué accordé.';
        this.inviteForm.reset();
        this.load();
      },
      error: (err) => {
        this.inviting = false;
        this.error = err.error?.message || 'Impossible de créer la délégation.';
      }
    });
  }

  revoquer(delegation: Delegation): void {
    this.delegationService.revoquer(delegation.id).subscribe({
      next: () => this.load()
    });
  }
}
