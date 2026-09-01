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

export interface CitySuggestion {
  label: string;       // ex. « Nice, Alpes-Maritimes »
  city: string;
  postalCode: string;
  latitude: number;
  longitude: number;
}

/**
 * Géolocalisation browser + reverse geocoding via Nominatim (OpenStreetMap, gratuit).
 * Pas de clé API ; respecte la politique d'usage en se limitant à des requêtes
 * déclenchées explicitement par l'utilisateur.
 */
@Injectable({ providedIn: 'root' })
export class GeolocationService {
  private readonly NOMINATIM_URL = 'https://nominatim.openstreetmap.org/reverse';
  private readonly NOMINATIM_SEARCH_URL = 'https://nominatim.openstreetmap.org/search';

  constructor(private http: HttpClient) {}

  /**
   * Géocodage direct : recherche de villes par nom (autocomplete).
   * Limité aux lieux de type ville/commune pour éviter le bruit (rues, POI…).
   */
  searchCities(query: string): Observable<CitySuggestion[]> {
    if (!query || query.trim().length < 2) return of([]);
    const params = `?q=${encodeURIComponent(query.trim())}&format=json&addressdetails=1`
      + `&limit=5&featureType=city&accept-language=fr`;
    return this.http.get<any[]>(this.NOMINATIM_SEARCH_URL + params).pipe(
      map(results => (results || []).map(r => {
        const a = r.address || {};
        const city = a.city || a.town || a.village || a.municipality || r.name || '';
        const dept = a.county || a.state || '';
        return {
          label: dept && dept !== city ? `${city}, ${dept}` : city,
          city,
          postalCode: a.postcode || '',
          latitude: parseFloat(r.lat),
          longitude: parseFloat(r.lon)
        } as CitySuggestion;
      }).filter(s => !!s.city && !isNaN(s.latitude))),
      catchError(() => of([]))
    );
  }

  /**
   * État de la permission de géolocalisation (via l'API Permissions), sans jamais déclencher
   * de prompt navigateur. Permet de détecter un refus déjà enregistré (session précédente
   * incluse) avant de rappeler getCurrentPosition, dont le comportement en cas de refus déjà
   * acté varie selon les navigateurs (échec immédiat, ou blocage jusqu'au timeout).
   * 'unsupported' si l'API Permissions n'existe pas ou refuse de répondre pour 'geolocation'.
   */
  checkPermissionState(): Observable<PermissionState | 'unsupported'> {
    const permissions = (navigator as any).permissions;
    if (!permissions?.query) return of('unsupported');
    return from((permissions.query({ name: 'geolocation' }) as Promise<PermissionStatus>)).pipe(
      map(status => status.state),
      catchError(() => of('unsupported' as const))
    );
  }

  /**
   * Récupère la position via le navigateur puis la convertit en adresse (ville + CP).
   * L'erreur renvoyée porte un `code` (1 = refusé, 2 = indisponible, 3 = expiré) repris de
   * l'API native, pour permettre à l'appelant de distinguer un refus d'une simple panne.
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
        const withCode = (message: string, code: number) => {
          const e: Error & { code?: number } = new Error(message);
          e.code = code;
          return throwError(() => e);
        };
        if (err?.code === 1) return withCode('Autorisation refusée. Activez la géolocalisation pour ce site dans les réglages de votre navigateur, puis réessayez.', 1);
        if (err?.code === 2) return withCode('Position indisponible.', 2);
        if (err?.code === 3) return withCode('La géolocalisation a expiré.', 3);
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
