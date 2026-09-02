/**
 * Utilitaires fichiers partagés — remplacent les copies locales (téléchargement d'un
 * Blob, format de taille) qui s'étaient multipliées dans les composants.
 */

/** Déclenche le téléchargement d'un Blob sous le nom donné, puis libère l'URL objet. */
export function downloadBlob(blob: Blob, fileName: string): void {
  const url = window.URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  window.URL.revokeObjectURL(url);
}

/** Taille lisible : 0 B, 12.3 KB, 4.5 MB… */
export function formatFileSize(bytes: number | null | undefined): string {
  if (!bytes || bytes <= 0) return '0 B';
  const units = ['B', 'KB', 'MB', 'GB'];
  const i = Math.min(Math.floor(Math.log(bytes) / Math.log(1024)), units.length - 1);
  const value = bytes / Math.pow(1024, i);
  return `${i === 0 ? value : value.toFixed(1)} ${units[i]}`;
}
