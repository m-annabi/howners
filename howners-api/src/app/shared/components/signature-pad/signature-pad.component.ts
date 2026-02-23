import { Component, OnInit, OnDestroy, ViewChild, ElementRef, Output, EventEmitter, Input, AfterViewInit } from '@angular/core';
import SignaturePad from 'signature_pad';

@Component({
  selector: 'app-signature-pad',
  templateUrl: './signature-pad.component.html',
  styleUrls: ['./signature-pad.component.scss']
})
export class SignaturePadComponent implements OnInit, AfterViewInit, OnDestroy {
  @ViewChild('signatureCanvas', { static: false }) signatureCanvas!: ElementRef<HTMLCanvasElement>;
  @Output() signatureChange = new EventEmitter<string>();
  @Input() disabled = false;

  signaturePad!: SignaturePad;
  canvasWidth = 600;
  canvasHeight = 200;

  ngOnInit(): void {
    if (window.innerWidth < 768) {
      this.canvasWidth = window.innerWidth - 40;
      this.canvasHeight = 150;
    }
  }

  ngAfterViewInit(): void {
    this.initializeSignaturePad();
  }

  ngOnDestroy(): void {
    if (this.signaturePad) {
      this.signaturePad.off();
    }
  }

  initializeSignaturePad(): void {
    const canvas = this.signatureCanvas.nativeElement;
    canvas.width = this.canvasWidth;
    canvas.height = this.canvasHeight;

    this.signaturePad = new SignaturePad(canvas, {
      backgroundColor: 'rgb(255, 255, 255)',
      penColor: 'rgb(0, 0, 0)',
      minWidth: 1,
      maxWidth: 2.5,
      throttle: 0,
      velocityFilterWeight: 0.7
    });

    this.signaturePad.addEventListener('endStroke', () => {
      this.onSignatureChange();
    });

    if (this.disabled) {
      this.signaturePad.off();
    }
  }

  onSignatureChange(): void {
    if (!this.signaturePad.isEmpty()) {
      const dataURL = this.signaturePad.toDataURL('image/png');
      this.signatureChange.emit(dataURL);
    }
  }

  clear(): void {
    this.signaturePad.clear();
    this.signatureChange.emit('');
  }

  isEmpty(): boolean {
    return this.signaturePad ? this.signaturePad.isEmpty() : true;
  }

  getSignatureData(): string | null {
    if (this.signaturePad && !this.signaturePad.isEmpty()) {
      return this.signaturePad.toDataURL('image/png');
    }
    return null;
  }

  setSignatureData(dataURL: string): void {
    if (this.signaturePad && dataURL) {
      this.signaturePad.fromDataURL(dataURL);
    }
  }

  undo(): void {
    const data = this.signaturePad.toData();
    if (data && data.length > 0) {
      data.pop();
      this.signaturePad.fromData(data);
      this.onSignatureChange();
    }
  }
}
