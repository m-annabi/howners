import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EtatDesLieuxService } from '../../../core/services/etat-des-lieux.service';
import { EtatDesLieux, EDL_TYPE_LABELS, EDL_TYPE_COLORS } from '../../../core/models/etat-des-lieux.model';

@Component({
  selector: 'app-edl-detail',
  templateUrl: './edl-detail.component.html'
})
export class EdlDetailComponent implements OnInit {
  edl: EtatDesLieux | null = null;
  rentalId = '';
  loading = true;
  typeLabels = EDL_TYPE_LABELS;
  typeColors = EDL_TYPE_COLORS;

  rooms: { name: string; condition: string; comments: string }[] = [];
  meters: { type: string; value: string }[] = [];

  constructor(
    private edlService: EtatDesLieuxService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.rentalId = this.route.snapshot.paramMap.get('rentalId') || '';
    const id = this.route.snapshot.paramMap.get('id') || '';
    this.loadEdl(id);
  }

  loadEdl(id: string): void {
    this.loading = true;
    this.edlService.getById(this.rentalId, id).subscribe({
      next: (edl) => {
        this.edl = edl;
        this.parseJsonFields();
        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
    });
  }

  parseJsonFields(): void {
    if (this.edl?.roomConditions) {
      try {
        this.rooms = JSON.parse(this.edl.roomConditions);
      } catch {
        this.rooms = [];
      }
    }
    if (this.edl?.meterReadings) {
      try {
        this.meters = JSON.parse(this.edl.meterReadings);
      } catch {
        this.meters = [];
      }
    }
  }

  signAs(role: string): void {
    if (!this.edl) return;
    const roleLabel = role === 'OWNER' ? 'bailleur' : 'locataire';
    if (confirm(`Êtes-vous sûr de vouloir signer en tant que ${roleLabel} ?`)) {
      this.edlService.sign(this.rentalId, this.edl.id, role).subscribe({
        next: (updated) => {
          this.edl = updated;
        }
      });
    }
  }

  downloadPdf(): void {
    if (!this.edl) return;
    this.edlService.downloadPdf(this.rentalId, this.edl.id).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `etat-des-lieux-${this.edl!.id}.pdf`;
        a.click();
        window.URL.revokeObjectURL(url);
      }
    });
  }

  getConditionBadge(condition: string): string {
    switch (condition) {
      case 'NEUF': return 'success';
      case 'BON': return 'primary';
      case 'CORRECT': return 'info';
      case 'USAGE': return 'warning';
      case 'MAUVAIS': return 'danger';
      default: return 'secondary';
    }
  }

  goBack(): void {
    this.router.navigate(['/inventory']);
  }
}
