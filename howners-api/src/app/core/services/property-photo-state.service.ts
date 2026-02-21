import { Injectable } from '@angular/core';
import { Subject } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class PropertyPhotoStateService {
  private photoUpdatedSource = new Subject<string>();

  // Observable que les composants peuvent écouter
  photoUpdated$ = this.photoUpdatedSource.asObservable();

  // Notifier qu'une photo a été mise à jour pour une propriété
  notifyPhotoUpdate(propertyId: string): void {
    this.photoUpdatedSource.next(propertyId);
  }
}
