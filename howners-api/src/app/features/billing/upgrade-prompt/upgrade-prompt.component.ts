import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-upgrade-prompt',
  templateUrl: './upgrade-prompt.component.html'
})
export class UpgradePromptComponent {
  @Input() message = 'Vous avez atteint la limite de votre plan actuel.';
}
