import { Component, Input, OnInit } from '@angular/core';
import { CdkDragDrop, moveItemInArray } from '@angular/cdk/drag-drop';
import { PropertyPhotoService } from '../../../core/services/property-photo.service';
import { PropertyPhotoStateService } from '../../../core/services/property-photo-state.service';
import { PropertyPhoto, ReorderPhotosRequest } from '../../../core/models/property-photo.model';

@Component({
  selector: 'app-property-photo-gallery',
  templateUrl: './property-photo-gallery.component.html',
  styleUrls: ['./property-photo-gallery.component.css']
})
export class PropertyPhotoGalleryComponent implements OnInit {
  @Input() propertyId!: string;
  @Input() editable: boolean = true;

  photos: PropertyPhoto[] = [];
  loading = false;
  error: string | null = null;
  editingCaptionId: string | null = null;
  tempCaption: string = '';

  constructor(
    private photoService: PropertyPhotoService,
    private photoStateService: PropertyPhotoStateService
  ) {}

  ngOnInit(): void {
    this.loadPhotos();
  }

  loadPhotos(): void {
    this.loading = true;
    this.error = null;

    this.photoService.getPropertyPhotos(this.propertyId).subscribe({
      next: (photos) => {
        this.photos = photos;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading photos:', err);
        this.error = 'Erreur lors du chargement des photos';
        this.loading = false;
      }
    });
  }

  onPhotoUploaded(photo: PropertyPhoto): void {
    this.loadPhotos();
    // Notifier que les photos ont changé
    this.photoStateService.notifyPhotoUpdate(this.propertyId);
  }

  drop(event: CdkDragDrop<PropertyPhoto[]>): void {
    if (!this.editable) return;

    moveItemInArray(this.photos, event.previousIndex, event.currentIndex);

    // Mettre à jour les displayOrder
    const request: ReorderPhotosRequest = {
      photos: this.photos.map((photo, index) => ({
        photoId: photo.id,
        displayOrder: index
      }))
    };

    this.photoService.reorderPhotos(this.propertyId, request).subscribe({
      next: (photos) => {
        this.photos = photos;
      },
      error: (err) => {
        console.error('Error reordering photos:', err);
        this.error = 'Erreur lors de la réorganisation des photos';
        // Recharger pour restaurer l'ordre original
        this.loadPhotos();
      }
    });
  }

  startEditCaption(photo: PropertyPhoto): void {
    if (!this.editable) return;
    this.editingCaptionId = photo.id;
    this.tempCaption = photo.caption || '';
  }

  saveCaption(photo: PropertyPhoto): void {
    if (!this.editable) return;

    this.photoService
      .updatePhoto(this.propertyId, photo.id, { caption: this.tempCaption })
      .subscribe({
        next: (updatedPhoto) => {
          const index = this.photos.findIndex((p) => p.id === photo.id);
          if (index !== -1) {
            this.photos[index] = updatedPhoto;
          }
          this.editingCaptionId = null;
          this.tempCaption = '';
        },
        error: (err) => {
          console.error('Error updating caption:', err);
          this.error = 'Erreur lors de la mise à jour de la légende';
          this.editingCaptionId = null;
        }
      });
  }

  cancelEditCaption(): void {
    this.editingCaptionId = null;
    this.tempCaption = '';
  }

  setPrimary(photo: PropertyPhoto): void {
    if (!this.editable) return;

    this.photoService.setPrimaryPhoto(this.propertyId, photo.id).subscribe({
      next: () => {
        // Mettre à jour les flags primary localement
        this.photos.forEach((p) => {
          p.isPrimary = p.id === photo.id;
        });
        // Notifier que la photo de couverture a changé
        this.photoStateService.notifyPhotoUpdate(this.propertyId);
      },
      error: (err) => {
        console.error('Error setting primary photo:', err);
        this.error = 'Erreur lors de la définition de la photo de couverture';
      }
    });
  }

  deletePhoto(photo: PropertyPhoto): void {
    if (!this.editable) return;

    if (!confirm('Voulez-vous vraiment supprimer cette photo ?')) {
      return;
    }

    this.photoService.deletePhoto(this.propertyId, photo.id).subscribe({
      next: () => {
        this.photos = this.photos.filter((p) => p.id !== photo.id);
        // Notifier que les photos ont changé (la photo de couverture peut avoir changé)
        this.photoStateService.notifyPhotoUpdate(this.propertyId);
      },
      error: (err) => {
        console.error('Error deleting photo:', err);
        this.error = 'Erreur lors de la suppression de la photo';
      }
    });
  }

  get currentPhotoCount(): number {
    return this.photos.length;
  }
}
