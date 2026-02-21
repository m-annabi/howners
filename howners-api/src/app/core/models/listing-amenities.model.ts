export interface AmenityItem {
  key: string;
  label: string;
  icon: string;
}

export const PREDEFINED_AMENITIES: AmenityItem[] = [
  { key: 'wifi', label: 'WiFi', icon: 'bi-wifi' },
  { key: 'parking', label: 'Parking', icon: 'bi-p-circle' },
  { key: 'ascenseur', label: 'Ascenseur', icon: 'bi-arrow-up-square' },
  { key: 'balcon', label: 'Balcon', icon: 'bi-border-outer' },
  { key: 'terrasse', label: 'Terrasse', icon: 'bi-sun' },
  { key: 'cave', label: 'Cave', icon: 'bi-box-seam' },
  { key: 'gardien', label: 'Gardien', icon: 'bi-shield-check' },
  { key: 'interphone', label: 'Interphone', icon: 'bi-telephone' },
  { key: 'digicode', label: 'Digicode', icon: 'bi-lock' },
  { key: 'lave_linge', label: 'Lave-linge', icon: 'bi-droplet' },
  { key: 'seche_linge', label: 'Seche-linge', icon: 'bi-wind' },
  { key: 'lave_vaisselle', label: 'Lave-vaisselle', icon: 'bi-cup-straw' },
  { key: 'climatisation', label: 'Climatisation', icon: 'bi-snow' },
  { key: 'chauffage_collectif', label: 'Chauffage collectif', icon: 'bi-thermometer-half' },
  { key: 'fibre_optique', label: 'Fibre optique', icon: 'bi-lightning' },
  { key: 'jardin', label: 'Jardin', icon: 'bi-tree' },
  { key: 'piscine', label: 'Piscine', icon: 'bi-water' },
  { key: 'local_velo', label: 'Local velo', icon: 'bi-bicycle' }
];

export const PREDEFINED_REQUIREMENTS: AmenityItem[] = [
  { key: 'non_fumeur', label: 'Non-fumeur', icon: 'bi-slash-circle' },
  { key: 'pas_animaux', label: 'Pas d\'animaux', icon: 'bi-x-circle' },
  { key: 'garant_obligatoire', label: 'Garant obligatoire', icon: 'bi-person-check' },
  { key: 'cdi_requis', label: 'CDI requis', icon: 'bi-briefcase' },
  { key: 'revenus_3x_loyer', label: 'Revenus 3x loyer', icon: 'bi-cash-stack' },
  { key: 'assurance_habitation', label: 'Assurance habitation', icon: 'bi-house-check' },
  { key: 'preavis_1_mois', label: 'Preavis 1 mois', icon: 'bi-calendar' },
  { key: 'preavis_3_mois', label: 'Preavis 3 mois', icon: 'bi-calendar3' },
  { key: 'etat_des_lieux_entree', label: 'Etat des lieux entree', icon: 'bi-clipboard-check' },
  { key: 'depot_garantie_1_mois', label: 'Depot garantie 1 mois', icon: 'bi-piggy-bank' },
  { key: 'depot_garantie_2_mois', label: 'Depot garantie 2 mois', icon: 'bi-safe' }
];

export const AMENITIES_MAP: Record<string, AmenityItem> = Object.fromEntries(
  PREDEFINED_AMENITIES.map(a => [a.key, a])
);

export const REQUIREMENTS_MAP: Record<string, AmenityItem> = Object.fromEntries(
  PREDEFINED_REQUIREMENTS.map(r => [r.key, r])
);
