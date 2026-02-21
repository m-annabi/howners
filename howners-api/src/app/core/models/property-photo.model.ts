export interface PropertyPhoto {
  id: string;
  propertyId: string;
  fileName: string;
  fileUrl: string;
  fileSize: number;
  mimeType: string;
  caption?: string;
  displayOrder: number;
  isPrimary: boolean;
  uploaderId: string;
  uploaderName: string;
  createdAt: string;
  updatedAt: string;
}

export interface UpdatePropertyPhotoRequest {
  caption?: string;
  displayOrder?: number;
  isPrimary?: boolean;
}

export interface ReorderPhotosRequest {
  photos: PhotoOrderItem[];
}

export interface PhotoOrderItem {
  photoId: string;
  displayOrder: number;
}
