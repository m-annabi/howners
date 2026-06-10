import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, from, of, throwError } from 'rxjs';
import { catchError, switchMap, map } from 'rxjs/operators';

export interface ReverseGeocodeResult {
  latitude: number;
  longitude: number;
  city: string;
  postalCode: string;
  country: string;
  state?: string;
  countyCode?: string;
  raw?: any;
}

/**
 * Géolocalisation browser + reverse geocoding via Nominatim (OpenStreetMap, gratuit).
 * Pas de clé API ; respecte la politique d'usage en se limitant à des requêtes
 * déclenchées explicitement par l'utilisateur.
 */
@Injectable({ providedIn: 'root' })
export class GeolocationService {
  private readonly NOMINATIM_URL = 'https://nominatim.openstreetmap.org/reverse';

  constructor(private http: HttpClient) {}

  /**
   * Récupère la position via le navigateur puis la convertit en adresse (ville + CP).
   */
  detectUserLocation(): Observable<ReverseGeocodeResult> {
    if (!navigator.geolocation) {
      return throwError(() => new Error('Géolocalisation non supportée par ce navigateur.'));
    }

    return from(new Promise<GeolocationPosition>((resolve, reject) => {
      navigator.geolocation.getCurrentPosition(resolve, reject, {
        enableHighAccuracy: false,
        timeout: 8000,
        maximumAge: 60_000
      });
    })).pipe(
      switchMap(pos => this.reverseGeocode(pos.coords.latitude, pos.coords.longitude).pipe(
        map(r => ({ ...r, latitude: pos.coords.latitude, longitude: pos.coords.longitude }))
      )),
      catchError(err => {
        if (err?.code === 1) return throwError(() => new Error('Autorisation refusée. Activez la géolocalisation pour ce site.'));
        if (err?.code === 2) return throwError(() => new Error('Position indisponible.'));
        if (err?.code === 3) return throwError(() => new Error('La géolocalisation a expiré.'));
        return throwError(() => err);
      })
    );
  }

  private reverseGeocode(lat: number, lon: number): Observable<ReverseGeocodeResult> {
    const params = `?lat=${lat}&lon=${lon}&format=json&addressdetails=1&accept-language=fr`;
    return this.http.get<any>(this.NOMINATIM_URL + params).pipe(
      map(resp => {
        const a = resp?.address || {};
        const city = a.city || a.town || a.village || a.municipality || a.hamlet || '';
        const postalCode = a.postcode || '';
        const country = (a.country_code || '').toUpperCase();
        const state = a.state || a.region;
        const countyCode = a.county || a['ISO3166-2-lvl4'];
        return { latitude: lat, longitude: lon, city, postalCode, country, state, countyCode, raw: resp };
      }),
      catchError(() => of({ latitude: lat, longitude: lon, city: '', postalCode: '', country: '' } as ReverseGeocodeResult))
    );
  }
}
