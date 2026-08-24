import { Component, Input } from '@angular/core';

export type StepState = 'done' | 'current' | 'todo';

export interface StepItem {
  /** Libellé de l'étape. */
  label: string;
  /** État de l'étape (calculé par le parent). */
  state: StepState;
  /** Indication courte optionnelle (affichée sous le libellé). */
  hint?: string;
  /** Icône bootstrap optionnelle (sinon numéro / coche). */
  icon?: string;
}

/**
 * Barre d'étapes horizontale (stepper) réutilisable : cercles numérotés reliés,
 * l'étape « done » cochée, l'étape « current » mise en avant. Responsive (défile
 * horizontalement sur mobile plutôt que de casser la mise en page).
 */
@Component({
  selector: 'app-stepper',
  templateUrl: './stepper.component.html',
  styleUrls: ['./stepper.component.scss']
})
export class StepperComponent {
  @Input() steps: StepItem[] = [];

  /** Pourcentage de progression (étapes terminées / total) pour la barre de fond. */
  get progressPercent(): number {
    if (!this.steps.length) return 0;
    const done = this.steps.filter(s => s.state === 'done').length;
    return Math.round((done / this.steps.length) * 100);
  }
}
