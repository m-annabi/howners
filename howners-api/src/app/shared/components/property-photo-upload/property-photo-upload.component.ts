import { Component, EventEmitter, Input, Output } from '@angular/core';
import { PropertyPhotoService } from '../../../core/services/property-photo.service';
import { PropertyPhoto } from '../../../core/models/property-photo.model';

interface FilePreview {
  file: File;
  preview: string;
  caption: string;
  uploading: boolean;
  error?: string;
}

@Component({
  selector: 'app-property-photo-upload',
  templateUrl: './property-photo-upload.component.html',
  styleUrls: ['./property-photo-upload.component.css']
})
export class PropertyPhotoUploadComponent {
  @Input() propertyId!: string;
  @Input() currentPhotoCount: number = 0;
  @Output() photoUploaded = new EventEmitter<PropertyPhoto>();

  selectedFiles: FilePreview[] = [];
  dragOver = false;
  error: string | null = null;

  readonly MAX_PHOTOS = 5;

  constructor(private photoService: PropertyPhotoService) {}

  get remainingSlots(): number {
    return this.MAX_PHOTOS - this.currentPhotoCount;
  }

  get canUpload(): boolean {
    return this.remainingSlots > 0;
  }

  get hasUploadingFiles(): boolean {
    return this.selectedFiles.some(f => f.uploading);
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    if (this.canUpload) {
      this.dragOver = true;
    }
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.dragOver = false;

    if (!this.canUpload) {
      this.error = 'Limite de 5 photos atteinte';
      return;
    }

    const files = event.dataTransfer?.files;
    if (files) {
      this.handleFiles(Array.from(files));
    }
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files) {
      this.handleFiles(Array.from(input.files));
      input.value = ''; // Reset input
    }
  }

  handleFiles(files: File[]): void {
    this.error = null;

    // Vérifier le nombre de photos
    const availableSlots = this.remainingSlots - this.selectedFiles.length;
    if (files.length > availableSlots) {
      this.error = `Vous ne pouvez ajouter que ${availableSlots} photo(s) supplémentaire(s)`;
      files = files.slice(0, availableSlots);
    }

    // Valider et ajouter chaque fichier
    for (const file of files) {
      const validation = this.photoService.validateImageFile(file);
      if (!validation.valid) {
        this.error = validation.error || 'Fichier invalide';
        continue;
      }

      // Créer une preview
      const reader = new FileReader();
      reader.onload = (e) => {
        this.selectedFiles.push({
          file,
          preview: e.target?.result as string,
          caption: '',
          uploading: false
        });
      };
      reader.readAsDataURL(file);
    }
  }

  removeFile(index: number): void {
    this.selectedFiles.splice(index, 1);
    this.error = null;
  }

  updateCaption(index: number, caption: string): void {
    this.selectedFiles[index].caption = caption;
  }

  async uploadAll(): Promise<void> {
    if (this.selectedFiles.length === 0) {
      return;
    }

    this.error = null;

    // Upload séquentiel
    for (let i = 0; i < this.selectedFiles.length; i++) {
      const filePreview = this.selectedFiles[i];
      if (filePreview.uploading) continue;

      filePreview.uploading = true;
      filePreview.error = undefined;

      try {
        const photo = await this.photoService
          .uploadPhoto(this.propertyId, filePreview.file, filePreview.caption)
          .toPromise();

        if (photo) {
          this.photoUploaded.emit(photo);
          this.currentPhotoCount++;
          this.selectedFiles.splice(i, 1);
          i--; // Ajuster l'index après suppression
        }
      } catch (error: any) {
        console.error('Error uploading photo:', error);
        filePreview.error = error.error?.message || 'Erreur lors de l\'upload';
        filePreview.uploading = false;
      }
    }
  }

  getFileSize(file: File): string {
    return this.photoService.formatFileSize(file.size);
  }

  clearAll(): void {
    this.selectedFiles = [];
    this.error = null;
  }
}
