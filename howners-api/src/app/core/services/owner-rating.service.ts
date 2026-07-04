import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { OwnerRating, CreateOwnerRatingRequest } from '../models/owner-rating.model';
import { User } from '../models/user.model';

@Injectable({ providedIn: 'root' })
export class OwnerRatingService {
  private api = `${environment.apiUrl}/owner-ratings`;

  constructor(private http: HttpClient) {}

  create(request: CreateOwnerRatingRequest): Observable<OwnerRating> {
    return this.http.post<OwnerRating>(this.api, request);
  }

  getRatingsForOwner(ownerId: string): Observable<OwnerRating[]> {
    return this.http.get<OwnerRating[]>(`${this.api}/owner/${ownerId}`);
  }

  getMyRatings(): Observable<OwnerRating[]> {
    return this.http.get<OwnerRating[]>(`${this.api}/my`);
  }

  getOwnerProfile(ownerId: string): Observable<User> {
    return this.http.get<User>(`${this.api}/owner/${ownerId}/profile`);
  }
}
