import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EtatDesLieuxService } from '../../../core/services/etat-des-lieux.service';
import { CreateEtatDesLieuxRequest, EtatDesLieuxType } from '../../../core/models/etat-des-lieux.model';

@Component({
  selector: 'app-edl-form',
  templateUrl: './edl-form.component.html'
})
export class EdlFormComponent implements OnInit {
  rentalId = '';
  submitting = false;

  type: EtatDesLieuxType = EtatDesLieuxType.ENTREE;
  inspectionDate = '';
  keysCount: number | null = null;
  keysDescription = '';
  generalComments = '';

  rooms: { name: string; condition: string; comments: string }[] = [
    { name: 'Entrée', condition: 'BON', comments: '' },
    { name: 'Salon', condition: 'BON', comments: '' },
    { name: 'Cuisine', condition: 'BON', comments: '' },
    { name: 'Chambre 1', condition: 'BON', comments: '' },
    { name: 'Salle de bain', condition: 'BON', comments: '' },
    { name: 'WC', condition: 'BON', comments: '' }
  ];

  meters: { type: string; value: string }[] = [
    { type: 'Électricité', value: '' },
    { type: 'Gaz', value: '' },
    { type: 'Eau', value: '' }
  ];

  conditionOptions = ['NEUF', 'BON', 'CORRECT', 'USAGE', 'MAUVAIS'];

  constructor(
    private edlService: EtatDesLieuxService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.rentalId = this.route.snapshot.paramMap.get('rentalId') || '';
  }

  addRoom(): void {
    this.rooms.push({ name: '', condition: 'BON', comments: '' });
  }

  removeRoom(index: number): void {
    this.rooms.splice(index, 1);
  }

  addMeter(): void {
    this.meters.push({ type: '', value: '' });
  }

  removeMeter(index: number): void {
    this.meters.splice(index, 1);
  }

  submit(): void {
    if (!this.inspectionDate) return;

    this.submitting = true;

    const roomConditions = JSON.stringify(this.rooms.filter(r => r.name));
    const meterReadings = JSON.stringify(this.meters.filter(m => m.type && m.value));

    const request: CreateEtatDesLieuxRequest = {
      type: this.type,
      inspectionDate: this.inspectionDate,
      roomConditions: roomConditions,
      meterReadings: meterReadings,
      keysCount: this.keysCount || undefined,
      keysDescription: this.keysDescription || undefined,
      generalComments: this.generalComments || undefined
    };

    this.edlService.create(this.rentalId, request).subscribe({
      next: (edl) => {
        this.router.navigate(['/inventory', edl.rentalId, edl.id]);
      },
      error: () => {
        this.submitting = false;
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/inventory']);
  }
}
