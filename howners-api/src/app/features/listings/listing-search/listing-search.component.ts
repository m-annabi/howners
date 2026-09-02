import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subject, Subscription } from 'rxjs';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';
import { ListingService } from '../../../core/services/listing.service';
import { Listing, LISTING_STATUS_LABELS } from '../../../core/models/listing.model';
import { PropertyType, PROPERTY_TYPE_LABELS } from '../../../core/models/property.model';
import { GeolocationService, CitySuggestion } from '../../../core/services/geolocation.service';
import { NotificationService } from '../../../core/services/notification.service';
import { SeoService } from '../../../core/services/seo.service';

/** Centre de la recherche par secteur : ville choisie ou position GPS. */
interface SearchCenter {
  lat: number;
  lng: number;
  label: string;
}

interface FilterChip {
  label: string;
  clear: () => void;
}

@Component({
  selector: 'app-listing-search',
  templateUrl: './listing-search.component.html',
  styleUrls: ['./listing-search.component.scss']
})
export class ListingSearchComponent implements OnInit, OnDestroy {
  listings: Listing[] = [];
  /** Distances (km) par id d'annonce, quand une recherche par secteur est active. */
  distances = new Map<string, number>();
  loading = false;
  showMoreFilters = false;

  // ── Lieu unifié : texte libre, suggestions, secteur choisi ──
  locationQuery = '';
  citySuggestions: CitySuggestion[] = [];
  showSuggestions = false;
  searchCenter: SearchCenter | null = null;
  radiusKm = 10;
  geolocating = false;
  /** true une fois qu'on sait (session en cours ou refus déjà enregistré par le navigateur) que
   *  la géolocalisation est bloquée : on n'appelle plus getCurrentPosition (comportement
   *  incohérent selon les navigateurs après un refus déjà acté), on guide directement l'utilisateur. */
  geoDenied = false;
  private locationInput$ = new Subject<string>();
  private suggestionsSub!: Subscription;

  // ── Autres filtres ──
  searchQuery = '';
  filterPriceMin: number | null = null;
  filterPriceMax: number | null = null;
  filterMinSurface: number | null = null;
  filterPropertyType = '';
  filterMinBedrooms: number | null = null;
  filterFurnished: string = '';
  filterDpeMax = '';
  filterParking = false;
  filterExtBalcony = false;
  filterExtGarden = false;
  filterCellar = false;
  filterElevator = false;
  filterPmr = false;
  sortBy = '';

  readonly dpeRatings = ['A', 'B', 'C', 'D', 'E', 'F'];

  propertyTypes = Object.values(PropertyType);
  propertyTypeLabels = PROPERTY_TYPE_LABELS;
  statusLabels = LISTING_STATUS_LABELS;

  constructor(
    private listingService: ListingService,
    private geolocationService: GeolocationService,
    private notificationService: NotificationService,
    private seoService: SeoService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    const title = 'Annonces de location — Howners';
    const description =
      'Parcourez les annonces de location disponibles sur Howners. ' +
      'Appartements, maisons, studios — trouvez votre prochain logement.';
    this.seoService.setMetaTags({ title, description, url: window.location.href });
    this.seoService.setCanonical('https://howners.fr/listings');

    this.suggestionsSub = this.locationInput$.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      switchMap(q => this.geolocationService.searchCities(q))
    ).subscribe(suggestions => {
      this.citySuggestions = suggestions;
      this.showSuggestions = suggestions.length > 0;
    });

    this.restoreFromUrl();
    this.search();
  }

  ngOnDestroy(): void {
    this.suggestionsSub?.unsubscribe();
  }

  // ── Lieu / secteur ──────────────────────────────────────────────────────────

  onLocationInput(): void {
    // Toute frappe invalide le secteur précédemment choisi
    if (this.searchCenter) this.searchCenter = null;
    this.locationInput$.next(this.locationQuery);
  }

  selectCity(suggestion: CitySuggestion): void {
    this.searchCenter = {
      lat: suggestion.latitude,
      lng: suggestion.longitude,
      label: suggestion.label
    };
    this.locationQuery = suggestion.label;
    this.citySuggestions = [];
    this.showSuggestions = false;
    this.search();
  }

  hideSuggestionsSoon(): void {
    // Laisse le temps au (mousedown) de la suggestion de se déclencher
    setTimeout(() => this.showSuggestions = false, 150);
  }

  locateMe(): void {
    if (this.geolocating) return;

    // Refus déjà constaté cette session : inutile de rappeler l'API native (comportement
    // incohérent selon les navigateurs une fois le refus acté) — on guide directement.
    if (this.geoDenied) {
      this.notificationService.error('Autorisation refusée. Activez la géolocalisation pour ce site dans les réglages de votre navigateur, puis réessayez.');
      return;
    }

    this.geolocating = true;

    // Vérifie l'état de la permission (sans prompt) : couvre le cas d'un refus déjà enregistré
    // par le navigateur lors d'une session précédente, avant même le premier essai ici.
    this.geolocationService.checkPermissionState().subscribe(state => {
      if (state === 'denied') {
        this.geolocating = false;
        this.geoDenied = true;
        this.notificationService.error('Autorisation refusée. Activez la géolocalisation pour ce site dans les réglages de votre navigateur, puis réessayez.');
        return;
      }
      this.requestLocation();
    });
  }

  private requestLocation(): void {
    this.geolocationService.detectUserLocation().subscribe({
      next: (result) => {
        this.geolocating = false;
        const labelParts = [result.city, result.postalCode].filter(Boolean);
        this.searchCenter = {
          lat: result.latitude,
          lng: result.longitude,
          label: labelParts.length > 0 ? labelParts.join(' ') : 'Ma position'
        };
        this.locationQuery = this.searchCenter.label;
        this.showSuggestions = false;
        this.search();
      },
      error: (err) => {
        this.geolocating = false;
        if (err?.code === 1) this.geoDenied = true;
        this.notificationService.error(err?.message || 'Impossible de récupérer votre position.');
      }
    });
  }

  clearLocation(): void {
    this.searchCenter = null;
    this.locationQuery = '';
    this.citySuggestions = [];
    this.showSuggestions = false;
    this.search();
  }

  onRadiusChange(): void {
    if (this.searchCenter) this.search();
  }

  // ── Recherche ───────────────────────────────────────────────────────────────

  search(): void {
    this.loading = true;
    this.showSuggestions = false;
    const filters: any = {};
    if (this.searchQuery) filters.search = this.searchQuery;
    if (this.filterPriceMin != null) filters.priceMin = this.filterPriceMin;
    if (this.filterPriceMax != null) filters.priceMax = this.filterPriceMax;
    if (this.filterPropertyType) filters.propertyType = this.filterPropertyType;
    if (this.filterMinSurface != null) filters.minSurface = this.filterMinSurface;
    if (this.filterMinBedrooms != null) filters.minBedrooms = this.filterMinBedrooms;
    if (this.filterFurnished === 'true') filters.furnished = true;
    if (this.filterFurnished === 'false') filters.furnished = false;
    if (this.filterDpeMax) filters.dpeMax = this.filterDpeMax;
    if (this.filterParking) filters.parking = true;
    if (this.exteriorParam) filters.exterior = this.exteriorParam;
    if (this.filterCellar) filters.cellar = true;
    if (this.filterElevator) filters.elevator = true;
    if (this.filterPmr) filters.pmr = true;
    if (this.sortBy) filters.sortBy = this.sortBy;

    if (this.searchCenter && this.radiusKm > 0) {
      filters.nearLat = this.searchCenter.lat;
      filters.nearLng = this.searchCenter.lng;
      filters.radiusKm = this.radiusKm;
    } else if (this.locationQuery) {
      // Pas de secteur choisi (ou rayon 0) : le texte sert de filtre ville classique
      filters.city = this.searchCenter ? this.searchCenter.label.split(',')[0].trim() : this.locationQuery;
    }

    this.syncUrl();

    this.listingService.searchListings(Object.keys(filters).length > 0 ? filters : undefined).subscribe({
      next: (page) => {
        this.listings = page.content;
        this.computeDistances();
        this.loading = false;
      },
      error: () => this.loading = false
    });
  }

  /** Paramètre API dérivé des toggles balcon/terrasse et jardin. */
  private get exteriorParam(): string {
    if (this.filterExtBalcony && this.filterExtGarden) return 'any';
    if (this.filterExtBalcony) return 'balcony_terrace';
    if (this.filterExtGarden) return 'garden';
    return '';
  }

  toggleDpe(rating: string): void {
    this.filterDpeMax = this.filterDpeMax === rating ? '' : rating;
    this.search();
  }

  setFurnished(value: string): void {
    this.filterFurnished = value;
    this.search();
  }

  toggleAmenity(name: 'balcony' | 'garden' | 'parking' | 'cellar' | 'elevator' | 'pmr'): void {
    if (name === 'balcony') this.filterExtBalcony = !this.filterExtBalcony;
    if (name === 'garden') this.filterExtGarden = !this.filterExtGarden;
    if (name === 'parking') this.filterParking = !this.filterParking;
    if (name === 'cellar') this.filterCellar = !this.filterCellar;
    if (name === 'elevator') this.filterElevator = !this.filterElevator;
    if (name === 'pmr') this.filterPmr = !this.filterPmr;
    this.search();
  }

  /** Distance haversine entre le centre de recherche et chaque annonce. */
  private computeDistances(): void {
    this.distances.clear();
    const center = this.searchCenter;
    if (!center) return;
    for (const l of this.listings) {
      if (l.propertyLatitude == null || l.propertyLongitude == null) continue;
      const R = 6371;
      const dLat = (l.propertyLatitude - center.lat) * Math.PI / 180;
      const dLng = (l.propertyLongitude - center.lng) * Math.PI / 180;
      const a = Math.sin(dLat / 2) ** 2
        + Math.cos(center.lat * Math.PI / 180) * Math.cos(l.propertyLatitude * Math.PI / 180)
        * Math.sin(dLng / 2) ** 2;
      this.distances.set(l.id, R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a)));
    }
  }

  distanceLabel(listing: Listing): string | null {
    const d = this.distances.get(listing.id);
    if (d == null) return null;
    return d < 1 ? '< 1 km' : `à ${d.toFixed(d < 10 ? 1 : 0)} km`;
  }

  // ── URL ↔ filtres ───────────────────────────────────────────────────────────

  private syncUrl(): void {
    const q: any = {};
    if (this.searchQuery) q.q = this.searchQuery;
    if (this.searchCenter) {
      q.lat = this.searchCenter.lat.toFixed(5);
      q.lng = this.searchCenter.lng.toFixed(5);
      q.lieu = this.searchCenter.label;
      q.rayon = this.radiusKm;
    } else if (this.locationQuery) {
      q.ville = this.locationQuery;
    }
    if (this.filterPriceMin != null) q.prixMin = this.filterPriceMin;
    if (this.filterPriceMax != null) q.prixMax = this.filterPriceMax;
    if (this.filterMinSurface != null) q.surface = this.filterMinSurface;
    if (this.filterPropertyType) q.type = this.filterPropertyType;
    if (this.filterMinBedrooms != null) q.chambres = this.filterMinBedrooms;
    if (this.filterFurnished) q.meuble = this.filterFurnished;
    if (this.filterDpeMax) q.dpe = this.filterDpeMax;
    if (this.filterParking) q.parking = 1;
    if (this.filterExtBalcony) q.balcon = 1;
    if (this.filterExtGarden) q.jardin = 1;
    if (this.filterCellar) q.cave = 1;
    if (this.filterElevator) q.asc = 1;
    if (this.filterPmr) q.pmr = 1;
    if (this.sortBy) q.tri = this.sortBy;

    this.router.navigate([], { relativeTo: this.route, queryParams: q, replaceUrl: true });
  }

  private restoreFromUrl(): void {
    const p = this.route.snapshot.queryParamMap;
    this.searchQuery = p.get('q') ?? '';
    const lat = parseFloat(p.get('lat') ?? '');
    const lng = parseFloat(p.get('lng') ?? '');
    if (!isNaN(lat) && !isNaN(lng)) {
      this.searchCenter = { lat, lng, label: p.get('lieu') ?? 'Secteur' };
      this.locationQuery = this.searchCenter.label;
      this.radiusKm = Math.min(50, Math.max(1, parseInt(p.get('rayon') ?? '10', 10) || 10));
    } else {
      this.locationQuery = p.get('ville') ?? '';
    }
    this.filterPriceMin = p.get('prixMin') != null ? +p.get('prixMin')! : null;
    this.filterPriceMax = p.get('prixMax') != null ? +p.get('prixMax')! : null;
    this.filterMinSurface = p.get('surface') != null ? +p.get('surface')! : null;
    this.filterPropertyType = p.get('type') ?? '';
    this.filterMinBedrooms = p.get('chambres') != null ? +p.get('chambres')! : null;
    this.filterFurnished = p.get('meuble') ?? '';
    this.filterDpeMax = p.get('dpe') ?? '';
    this.filterParking = p.get('parking') === '1';
    this.filterExtBalcony = p.get('balcon') === '1';
    this.filterExtGarden = p.get('jardin') === '1';
    this.filterCellar = p.get('cave') === '1';
    this.filterElevator = p.get('asc') === '1';
    this.filterPmr = p.get('pmr') === '1';
    this.sortBy = p.get('tri') ?? '';
    this.showMoreFilters = this.hasSecondaryFilters;
  }

  // ── Chips de filtres actifs ─────────────────────────────────────────────────

  get activeChips(): FilterChip[] {
    const chips: FilterChip[] = [];
    if (this.searchCenter) {
      chips.push({
        label: `${this.searchCenter.label} · ${this.radiusKm} km`,
        clear: () => this.clearLocation()
      });
    } else if (this.locationQuery) {
      chips.push({ label: this.locationQuery, clear: () => this.clearLocation() });
    }
    if (this.searchQuery) {
      chips.push({ label: `« ${this.searchQuery} »`, clear: () => { this.searchQuery = ''; this.search(); } });
    }
    if (this.filterPriceMax != null) {
      chips.push({ label: `≤ ${this.filterPriceMax} €`, clear: () => { this.filterPriceMax = null; this.search(); } });
    }
    if (this.filterPriceMin != null) {
      chips.push({ label: `≥ ${this.filterPriceMin} €`, clear: () => { this.filterPriceMin = null; this.search(); } });
    }
    if (this.filterMinSurface != null) {
      chips.push({ label: `≥ ${this.filterMinSurface} m²`, clear: () => { this.filterMinSurface = null; this.search(); } });
    }
    if (this.filterPropertyType) {
      chips.push({
        label: this.propertyTypeLabels[this.filterPropertyType as PropertyType] || this.filterPropertyType,
        clear: () => { this.filterPropertyType = ''; this.search(); }
      });
    }
    if (this.filterMinBedrooms != null) {
      chips.push({ label: `${this.filterMinBedrooms}+ chambres`, clear: () => { this.filterMinBedrooms = null; this.search(); } });
    }
    if (this.filterFurnished) {
      chips.push({
        label: this.filterFurnished === 'true' ? 'Meublé' : 'Non meublé',
        clear: () => { this.filterFurnished = ''; this.search(); }
      });
    }
    if (this.filterDpeMax) {
      chips.push({ label: `DPE ≤ ${this.filterDpeMax}`, clear: () => { this.filterDpeMax = ''; this.search(); } });
    }
    if (this.filterParking) {
      chips.push({ label: 'Parking', clear: () => { this.filterParking = false; this.search(); } });
    }
    if (this.filterExtBalcony) {
      chips.push({ label: 'Balcon / terrasse', clear: () => { this.filterExtBalcony = false; this.search(); } });
    }
    if (this.filterExtGarden) {
      chips.push({ label: 'Jardin', clear: () => { this.filterExtGarden = false; this.search(); } });
    }
    if (this.filterCellar) {
      chips.push({ label: 'Cave', clear: () => { this.filterCellar = false; this.search(); } });
    }
    if (this.filterElevator) {
      chips.push({ label: 'Ascenseur', clear: () => { this.filterElevator = false; this.search(); } });
    }
    if (this.filterPmr) {
      chips.push({ label: 'Accès PMR', clear: () => { this.filterPmr = false; this.search(); } });
    }
    return chips;
  }

  // ── Divers ──────────────────────────────────────────────────────────────────

  toggleMoreFilters(): void {
    this.showMoreFilters = !this.showMoreFilters;
  }

  clearFilters(): void {
    this.searchQuery = '';
    this.locationQuery = '';
    this.searchCenter = null;
    this.citySuggestions = [];
    this.filterPriceMin = null;
    this.filterPriceMax = null;
    this.filterPropertyType = '';
    this.filterMinSurface = null;
    this.filterMinBedrooms = null;
    this.filterFurnished = '';
    this.filterDpeMax = '';
    this.filterParking = false;
    this.filterExtBalcony = false;
    this.filterExtGarden = false;
    this.filterCellar = false;
    this.filterElevator = false;
    this.filterPmr = false;
    this.sortBy = '';
    this.search();
  }

  get hasActiveFilters(): boolean {
    return this.activeChips.length > 0;
  }

  get hasSecondaryFilters(): boolean {
    return !!(this.filterMinBedrooms != null || this.filterFurnished
      || this.filterPriceMin != null
      || this.filterDpeMax || this.filterParking
      || this.filterExtBalcony || this.filterExtGarden
      || this.filterCellar || this.filterElevator || this.filterPmr);
  }
}
