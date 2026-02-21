export interface Country {
  code: string;
  label: string;
}

export interface Department {
  code: string;
  name: string;
  country: string;
}

export const COUNTRIES: Country[] = [
  { code: 'FR', label: 'France' },
  { code: 'CH', label: 'Suisse' },
];

export const DEPARTMENTS: Department[] = [
  // France metropolitaine
  { code: '01', name: 'Ain', country: 'FR' },
  { code: '02', name: 'Aisne', country: 'FR' },
  { code: '03', name: 'Allier', country: 'FR' },
  { code: '04', name: 'Alpes-de-Haute-Provence', country: 'FR' },
  { code: '05', name: 'Hautes-Alpes', country: 'FR' },
  { code: '06', name: 'Alpes-Maritimes', country: 'FR' },
  { code: '07', name: 'Ardeche', country: 'FR' },
  { code: '08', name: 'Ardennes', country: 'FR' },
  { code: '09', name: 'Ariege', country: 'FR' },
  { code: '10', name: 'Aube', country: 'FR' },
  { code: '11', name: 'Aude', country: 'FR' },
  { code: '12', name: 'Aveyron', country: 'FR' },
  { code: '13', name: 'Bouches-du-Rhone', country: 'FR' },
  { code: '14', name: 'Calvados', country: 'FR' },
  { code: '15', name: 'Cantal', country: 'FR' },
  { code: '16', name: 'Charente', country: 'FR' },
  { code: '17', name: 'Charente-Maritime', country: 'FR' },
  { code: '18', name: 'Cher', country: 'FR' },
  { code: '19', name: 'Correze', country: 'FR' },
  { code: '2A', name: 'Corse-du-Sud', country: 'FR' },
  { code: '2B', name: 'Haute-Corse', country: 'FR' },
  { code: '21', name: "Cote-d'Or", country: 'FR' },
  { code: '22', name: "Cotes-d'Armor", country: 'FR' },
  { code: '23', name: 'Creuse', country: 'FR' },
  { code: '24', name: 'Dordogne', country: 'FR' },
  { code: '25', name: 'Doubs', country: 'FR' },
  { code: '26', name: 'Drome', country: 'FR' },
  { code: '27', name: 'Eure', country: 'FR' },
  { code: '28', name: 'Eure-et-Loir', country: 'FR' },
  { code: '29', name: 'Finistere', country: 'FR' },
  { code: '30', name: 'Gard', country: 'FR' },
  { code: '31', name: 'Haute-Garonne', country: 'FR' },
  { code: '32', name: 'Gers', country: 'FR' },
  { code: '33', name: 'Gironde', country: 'FR' },
  { code: '34', name: 'Herault', country: 'FR' },
  { code: '35', name: 'Ille-et-Vilaine', country: 'FR' },
  { code: '36', name: 'Indre', country: 'FR' },
  { code: '37', name: 'Indre-et-Loire', country: 'FR' },
  { code: '38', name: 'Isere', country: 'FR' },
  { code: '39', name: 'Jura', country: 'FR' },
  { code: '40', name: 'Landes', country: 'FR' },
  { code: '41', name: 'Loir-et-Cher', country: 'FR' },
  { code: '42', name: 'Loire', country: 'FR' },
  { code: '43', name: 'Haute-Loire', country: 'FR' },
  { code: '44', name: 'Loire-Atlantique', country: 'FR' },
  { code: '45', name: 'Loiret', country: 'FR' },
  { code: '46', name: 'Lot', country: 'FR' },
  { code: '47', name: 'Lot-et-Garonne', country: 'FR' },
  { code: '48', name: 'Lozere', country: 'FR' },
  { code: '49', name: 'Maine-et-Loire', country: 'FR' },
  { code: '50', name: 'Manche', country: 'FR' },
  { code: '51', name: 'Marne', country: 'FR' },
  { code: '52', name: 'Haute-Marne', country: 'FR' },
  { code: '53', name: 'Mayenne', country: 'FR' },
  { code: '54', name: 'Meurthe-et-Moselle', country: 'FR' },
  { code: '55', name: 'Meuse', country: 'FR' },
  { code: '56', name: 'Morbihan', country: 'FR' },
  { code: '57', name: 'Moselle', country: 'FR' },
  { code: '58', name: 'Nievre', country: 'FR' },
  { code: '59', name: 'Nord', country: 'FR' },
  { code: '60', name: 'Oise', country: 'FR' },
  { code: '61', name: 'Orne', country: 'FR' },
  { code: '62', name: 'Pas-de-Calais', country: 'FR' },
  { code: '63', name: 'Puy-de-Dome', country: 'FR' },
  { code: '64', name: 'Pyrenees-Atlantiques', country: 'FR' },
  { code: '65', name: 'Hautes-Pyrenees', country: 'FR' },
  { code: '66', name: 'Pyrenees-Orientales', country: 'FR' },
  { code: '67', name: 'Bas-Rhin', country: 'FR' },
  { code: '68', name: 'Haut-Rhin', country: 'FR' },
  { code: '69', name: 'Rhone', country: 'FR' },
  { code: '70', name: 'Haute-Saone', country: 'FR' },
  { code: '71', name: 'Saone-et-Loire', country: 'FR' },
  { code: '72', name: 'Sarthe', country: 'FR' },
  { code: '73', name: 'Savoie', country: 'FR' },
  { code: '74', name: 'Haute-Savoie', country: 'FR' },
  { code: '75', name: 'Paris', country: 'FR' },
  { code: '76', name: 'Seine-Maritime', country: 'FR' },
  { code: '77', name: 'Seine-et-Marne', country: 'FR' },
  { code: '78', name: 'Yvelines', country: 'FR' },
  { code: '79', name: 'Deux-Sevres', country: 'FR' },
  { code: '80', name: 'Somme', country: 'FR' },
  { code: '81', name: 'Tarn', country: 'FR' },
  { code: '82', name: 'Tarn-et-Garonne', country: 'FR' },
  { code: '83', name: 'Var', country: 'FR' },
  { code: '84', name: 'Vaucluse', country: 'FR' },
  { code: '85', name: 'Vendee', country: 'FR' },
  { code: '86', name: 'Vienne', country: 'FR' },
  { code: '87', name: 'Haute-Vienne', country: 'FR' },
  { code: '88', name: 'Vosges', country: 'FR' },
  { code: '89', name: 'Yonne', country: 'FR' },
  { code: '90', name: 'Territoire de Belfort', country: 'FR' },
  { code: '91', name: 'Essonne', country: 'FR' },
  { code: '92', name: 'Hauts-de-Seine', country: 'FR' },
  { code: '93', name: 'Seine-Saint-Denis', country: 'FR' },
  { code: '94', name: 'Val-de-Marne', country: 'FR' },
  { code: '95', name: "Val-d'Oise", country: 'FR' },
  // DOM-TOM
  { code: '971', name: 'Guadeloupe', country: 'FR' },
  { code: '972', name: 'Martinique', country: 'FR' },
  { code: '973', name: 'Guyane', country: 'FR' },
  { code: '974', name: 'La Reunion', country: 'FR' },
  { code: '976', name: 'Mayotte', country: 'FR' },
  // Suisse - Cantons
  { code: 'AG', name: 'Argovie', country: 'CH' },
  { code: 'AI', name: 'Appenzell Rhodes-Interieures', country: 'CH' },
  { code: 'AR', name: 'Appenzell Rhodes-Exterieures', country: 'CH' },
  { code: 'BE', name: 'Berne', country: 'CH' },
  { code: 'BL', name: 'Bale-Campagne', country: 'CH' },
  { code: 'BS', name: 'Bale-Ville', country: 'CH' },
  { code: 'FR', name: 'Fribourg', country: 'CH' },
  { code: 'GE', name: 'Geneve', country: 'CH' },
  { code: 'GL', name: 'Glaris', country: 'CH' },
  { code: 'GR', name: 'Grisons', country: 'CH' },
  { code: 'JU', name: 'Jura', country: 'CH' },
  { code: 'LU', name: 'Lucerne', country: 'CH' },
  { code: 'NE', name: 'Neuchatel', country: 'CH' },
  { code: 'NW', name: 'Nidwald', country: 'CH' },
  { code: 'OW', name: 'Obwald', country: 'CH' },
  { code: 'SG', name: 'Saint-Gall', country: 'CH' },
  { code: 'SH', name: 'Schaffhouse', country: 'CH' },
  { code: 'SO', name: 'Soleure', country: 'CH' },
  { code: 'SZ', name: 'Schwyz', country: 'CH' },
  { code: 'TG', name: 'Thurgovie', country: 'CH' },
  { code: 'TI', name: 'Tessin', country: 'CH' },
  { code: 'UR', name: 'Uri', country: 'CH' },
  { code: 'VD', name: 'Vaud', country: 'CH' },
  { code: 'VS', name: 'Valais', country: 'CH' },
  { code: 'ZG', name: 'Zoug', country: 'CH' },
  { code: 'ZH', name: 'Zurich', country: 'CH' },
];

/**
 * Derive the French department code from a postal code.
 * e.g. "92150" -> "92", "20000" -> "2A"/"2B", "97100" -> "971"
 */
export function getDepartmentFromPostalCode(postalCode: string, country: string): string | null {
  if (country !== 'FR' || !postalCode || postalCode.length < 2) return null;

  // DOM-TOM: 3-digit codes
  if (postalCode.startsWith('97')) {
    const code = postalCode.substring(0, 3);
    return DEPARTMENTS.find(d => d.code === code && d.country === 'FR') ? code : null;
  }

  // Corse
  if (postalCode.startsWith('20')) {
    const num = parseInt(postalCode, 10);
    return num >= 20000 && num < 20200 ? '2A' : '2B';
  }

  const code = postalCode.substring(0, 2);
  return DEPARTMENTS.find(d => d.code === code && d.country === 'FR') ? code : null;
}

export function getDepartmentsByCountry(country: string): Department[] {
  return DEPARTMENTS.filter(d => d.country === country);
}

export function getDepartmentLabel(dept: Department): string {
  return `${dept.code} - ${dept.name}`;
}
