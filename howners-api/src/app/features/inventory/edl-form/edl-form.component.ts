import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { EtatDesLieuxService } from '../../../core/services/etat-des-lieux.service';
import { CreateEtatDesLieuxRequest, EtatDesLieuxType } from '../../../core/models/etat-des-lieux.model';
import { RentalService } from '../../rentals/rental.service';
import { Rental } from '../../../core/models/rental.model';

@Component({
  selector: 'app-edl-form',
  templateUrl: './edl-form.component.html'
})
export class EdlFormComponent implements OnInit {
  rentalId = '';
  // Bail concerné, affiché en lecture seule : l'utilisateur voit d'emblée pour quel
  // bien/locataire il rédige l'état des lieux (pré-sélection depuis la notification ou le contrat).
  rental: Rental | null = null;
  submitting = false;
  error: string | null = null;

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

  // Inventaire du mobilier (logement meublé). Section affichée pour les baux meublés
  // ou activable manuellement. Pré-remplie avec les 11 catégories minimales du décret 2015-981.
  showFurniture = false;
  furniture: { item: string; quantity: string; condition: string; comments: string }[] = [];
  private readonly DECRET_2015_981_ITEMS = [
    'Literie avec couette ou couverture',
    "Dispositif d'occultation des fenêtres des chambres",
    'Plaques de cuisson',
    'Four ou four à micro-ondes',
    'Réfrigérateur et congélateur (ou compartiment ≤ -6 °C)',
    'Vaisselle nécessaire aux repas',
    'Ustensiles de cuisine',
    'Table et sièges',
    'Étagères de rangement',
    'Luminaires',
    "Matériel d'entretien ménager adapté"
  ];

  constructor(
    private edlService: EtatDesLieuxService,
    private rentalService: RentalService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.rentalId = this.route.snapshot.paramMap.get('rentalId') || '';
    if (!this.rentalId) {
      // Pas de bail dans l'URL : retour à la liste, qui propose le sélecteur de bail.
      this.router.navigate(['/inventory']);
      return;
    }
    // Le type peut être imposé par le lien d'origine (?type=SORTIE) ; défaut : ENTREE.
    const type = this.route.snapshot.queryParamMap.get('type');
    if (type === 'SORTIE') {
      this.type = EtatDesLieuxType.SORTIE;
    }
    this.rentalService.getRental(this.rentalId).subscribe({
      next: (rental) => {
        this.rental = rental;
        // Logement meublé : afficher et pré-remplir l'inventaire du mobilier (décret 2015-981).
        if (rental.furnished) {
          this.enableFurniture();
        }
      },
      error: () => { this.rental = null; }
    });
  }

  /** Initialise l'inventaire avec la liste minimale du décret (idempotent). */
  enableFurniture(): void {
    this.showFurniture = true;
    if (this.furniture.length === 0) {
      this.furniture = this.DECRET_2015_981_ITEMS.map(item => ({
        item, quantity: '1', condition: 'BON', comments: ''
      }));
    }
  }

  addFurniture(): void {
    this.furniture.push({ item: '', quantity: '1', condition: 'BON', comments: '' });
  }

  removeFurniture(index: number): void {
    this.furniture.splice(index, 1);
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
    const furnitureInventory = this.showFurniture
      ? JSON.stringify(this.furniture.filter(f => f.item.trim()))
      : undefined;

    const request: CreateEtatDesLieuxRequest = {
      type: this.type,
      inspectionDate: this.inspectionDate,
      roomConditions: roomConditions,
      meterReadings: meterReadings,
      furnitureInventory: furnitureInventory,
      keysCount: this.keysCount || undefined,
      keysDescription: this.keysDescription || undefined,
      generalComments: this.generalComments || undefined
    };

    this.error = null;
    this.edlService.create(this.rentalId, request).subscribe({
      next: (edl) => {
        this.router.navigate(['/inventory', edl.rentalId, edl.id]);
      },
      error: (err) => {
        this.submitting = false;
        this.error = err.error?.message || 'Erreur lors de la creation de l\'etat des lieux';
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/inventory']);
  }
}
