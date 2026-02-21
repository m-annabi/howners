import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ListingPhoto } from '../models/listing.model';

@Injectable({
  providedIn: 'root'
})
export class ListingPhotoService {
  private apiUrl = `${environment.apiUrl}/listings`;

  private readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
  private readonly ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp'];

  constructor(private http: HttpClient) {}

  uploadPhoto(listingId: string, file: File, caption?: string): Observable<ListingPhoto> {
    const formData = new FormData();
    formData.append('file', file);

    if (caption) {
      formData.append('caption', caption);
    }

    return this.http.post<ListingPhoto>(`${this.apiUrl}/${listingId}/photos`, formData);
  }

  getListingPhotos(listingId: string): Observable<ListingPhoto[]> {
    return this.http.get<ListingPhoto[]>(`${this.apiUrl}/${listingId}/photos`);
  }

  getPrimaryPhoto(listingId: string): Observable<ListingPhoto> {
    return this.http.get<ListingPhoto>(`${this.apiUrl}/${listingId}/photos/primary`);
  }

  deletePhoto(listingId: string, photoId: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${listingId}/photos/${photoId}`);
  }

  validateImageFile(file: File): { valid: boolean; error?: string } {
    if (!this.ALLOWED_MIME_TYPES.includes(file.type)) {
      return {
        valid: false,
        error: 'Type de fichier invalide. Seuls les formats JPEG, PNG et WebP sont autorises.'
      };
    }

    if (file.size > this.MAX_FILE_SIZE) {
      return {
        valid: false,
        error: `La taille du fichier depasse la limite de ${this.formatFileSize(this.MAX_FILE_SIZE)}.`
      };
    }

    return { valid: true };
  }

  formatFileSize(bytes: number): string {
    if (bytes === 0) return '0 Bytes';

    const k = 1024;
    const sizes = ['Bytes', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));

    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  }
}
