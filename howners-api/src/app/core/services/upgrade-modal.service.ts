import { Injectable } from '@angular/core';
import { BehaviorSubject, Observable } from 'rxjs';

export interface UpgradeModalState {
  visible: boolean;
  message: string;
}

@Injectable({
  providedIn: 'root'
})
export class UpgradeModalService {
  private stateSubject = new BehaviorSubject<UpgradeModalState>({
    visible: false,
    message: ''
  });

  state$: Observable<UpgradeModalState> = this.stateSubject.asObservable();

  show(message: string): void {
    this.stateSubject.next({ visible: true, message });
  }

  hide(): void {
    this.stateSubject.next({ visible: false, message: '' });
  }
}
