import { Component, Input, Output, EventEmitter, ViewChild } from '@angular/core';
import { SignaturePadComponent } from '../../../shared/components/signature-pad/signature-pad.component';

@Component({
  selector: 'app-contract-sign-dialog',
  templateUrl: './contract-sign-dialog.component.html',
  styleUrls: ['./contract-sign-dialog.component.scss']
})
export class ContractSignDialogComponent {
  @Input() contractTitle: string = '';
  @Input() contractContent: string = '';
  @Input() show: boolean = false;
  @Output() close = new EventEmitter<void>();
  @Output() sign = new EventEmitter<string>();

  @ViewChild(SignaturePadComponent) signaturePad!: SignaturePadComponent;

  signatureData: string = '';
  signerName: string = '';
  signerEmail: string = '';
  acceptTerms: boolean = false;

  onSignatureChange(signature: string): void {
    this.signatureData = signature;
  }

  canSign(): boolean {
    return this.acceptTerms && 
           this.signerName.trim().length > 0 && 
           this.signerEmail.trim().length > 0 &&
           this.signatureData.length > 0;
  }

  onClose(): void {
    this.close.emit();
  }

  onSign(): void {
    if (this.canSign()) {
      const signaturePayload = {
        signature: this.signatureData,
        signerName: this.signerName,
        signerEmail: this.signerEmail,
        signedAt: new Date().toISOString()
      };
      this.sign.emit(JSON.stringify(signaturePayload));
    }
  }
}
