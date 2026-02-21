import { Component, EventEmitter, Input, Output } from '@angular/core';
import { ListingPhotoService } from '../../../core/services/listing-photo.service';
import { ListingPhoto } from '../../../core/models/listing.model';

interface FilePreview {
  file: File;
  preview: string;
  caption: string;
  uploading: boolean;
  error?: string;
}

@Component({
  selector: 'app-listing-photo-upload',
  templateUrl: './listing-photo-upload.component.html',
  styleUrls: ['./listing-photo-upload.component.css']
})
export class ListingPhotoUploadComponent {
  @Input() listingId!: string;
  @Input() currentPhotoCount: number = 0;
  @Output() photoUploaded = new EventEmitter<ListingPhoto>();

  selectedFiles: FilePreview[] = [];
  dragOver = false;
  error: string | null = null;

  readonly MAX_PHOTOS = 5;

  constructor(private photoService: ListingPhotoService) {}

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
      input.value = '';
    }
  }

  handleFiles(files: File[]): void {
    this.error = null;

    const availableSlots = this.remainingSlots - this.selectedFiles.length;
    if (files.length > availableSlots) {
      this.error = `Vous ne pouvez ajouter que ${availableSlots} photo(s) supplementaire(s)`;
      files = files.slice(0, availableSlots);
    }

    for (const file of files) {
      const validation = this.photoService.validateImageFile(file);
      if (!validation.valid) {
        this.error = validation.error || 'Fichier invalide';
        continue;
      }

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

  async uploadAll(): Promise<void> {
    if (this.selectedFiles.length === 0) {
      return;
    }

    this.error = null;

    for (let i = 0; i < this.selectedFiles.length; i++) {
      const filePreview = this.selectedFiles[i];
      if (filePreview.uploading) continue;

      filePreview.uploading = true;
      filePreview.error = undefined;

      try {
        const photo = await this.photoService
          .uploadPhoto(this.listingId, filePreview.file, filePreview.caption)
          .toPromise();

        if (photo) {
          this.photoUploaded.emit(photo);
          this.currentPhotoCount++;
          this.selectedFiles.splice(i, 1);
          i--;
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
