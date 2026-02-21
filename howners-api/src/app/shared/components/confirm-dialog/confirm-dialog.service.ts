import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';

export interface ConfirmDialogConfig {
  title: string;
  message: string;
  type?: 'warning' | 'danger';
  confirmLabel?: string;
  cancelLabel?: string;
}

@Injectable({ providedIn: 'root' })
export class ConfirmDialogService {
  private dialogSubject = new Subject<ConfirmDialogConfig & { resolve: (result: boolean) => void }>();
  dialog$ = this.dialogSubject.asObservable();

  confirm(title: string, message: string, type: 'warning' | 'danger' = 'warning'): Observable<boolean> {
    return new Observable(observer => {
      this.dialogSubject.next({
        title,
        message,
        type,
        resolve: (result: boolean) => {
          observer.next(result);
          observer.complete();
        }
      });
    });
  }
}
