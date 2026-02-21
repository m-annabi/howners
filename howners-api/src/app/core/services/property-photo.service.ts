import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import {
  PropertyPhoto,
  UpdatePropertyPhotoRequest,
  ReorderPhotosRequest
} from '../models/property-photo.model';

@Injectable({
  providedIn: 'root'
})
export class PropertyPhotoService {
  private apiUrl = `${environment.apiUrl}/properties`;

  private readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  private readonly ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  constructor(private http: HttpClient) {}

  /**
   * Upload une photo pour un bien
   */
  uploadPhoto(propertyId: string, file: File, caption?: string): Observable<PropertyPhoto> {
    const formData = new FormData();
    formData.append('file', file);

    if (caption) {
      formData.append('caption', caption);
    }

    return this.http.post<PropertyPhoto>(`${this.apiUrl}/${propertyId}/photos`, formData);
  }

  /**
   * Récupère toutes les photos d'un bien
   */
  getPropertyPhotos(propertyId: string): Observable<PropertyPhoto[]> {
    return this.http.get<PropertyPhoto[]>(`${this.apiUrl}/${propertyId}/photos`);
  }

  /**
   * Récupère la photo de couverture d'un bien
   */
  getPrimaryPhoto(propertyId: string): Observable<PropertyPhoto> {
    return this.http.get<PropertyPhoto>(`${this.apiUrl}/${propertyId}/photos/primary`);
  }

  /**
   * Met à jour une photo
   */
  updatePhoto(
    propertyId: string,
    photoId: string,
    request: UpdatePropertyPhotoRequest
  ): Observable<PropertyPhoto> {
    return this.http.put<PropertyPhoto>(
      `${this.apiUrl}/${propertyId}/photos/${photoId}`,
      request
    );
  }

  /**
   * Réorganise les photos
   */
  reorderPhotos(propertyId: string, request: ReorderPhotosRequest): Observable<PropertyPhoto[]> {
    return this.http.post<PropertyPhoto[]>(
      `${this.apiUrl}/${propertyId}/photos/reorder`,
      request
    );
  }

  /**
   * Supprime une photo
   */
  deletePhoto(propertyId: string, photoId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${propertyId}/photos/${photoId}`);
  }

  /**
   * Définit une photo comme photo de couverture
   */
  setPrimaryPhoto(propertyId: string, photoId: string): Observable<PropertyPhoto> {
    return this.updatePhoto(propertyId, photoId, { isPrimary: true });
  }

  /**
   * Valide un fichier image
   */
  validateImageFile(file: File): { valid: boolean; error?: string } {
    // Vérifier le type MIME
    if (!this.ALLOWED_MIME_TYPES.includes(file.type)) {
      return {
        valid: false,
        error: 'Type de fichier invalide. Seuls les formats JPEG, PNG et WebP sont autorisés.'
      };
    }

    // Vérifier la taille
    if (file.size > this.MAX_FILE_SIZE) {
      return {
        valid: false,
        error: `La taille du fichier dépasse la limite de ${this.formatFileSize(this.MAX_FILE_SIZE)}.`
      };
    }

    return { valid: true };
  }

  /**
   * Formate la taille d'un fichier en chaîne lisible
   */
  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }
}
