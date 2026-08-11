import { Component } from '@angular/core';

/**
 * Barre de navigation pour les visiteurs non connectés sur les pages publiques
 * (annonces, 404…). Elle garantit un retour vers le menu principal (accueil) et
 * l'accès aux annonces / connexion / inscription — la landing ayant sa propre
 * navigation, elle n'est pas affichée dessus (géré par AppComponent).
 */
@Component({
  selector: 'app-guest-topbar',
  templateUrl: './guest-topbar.component.html',
  styleUrls: ['./guest-topbar.component.scss']
})
export class GuestTopbarComponent {}
