// URLs à adapter selon le domaine de déploiement.
// Pour substituer au build : sed -i 's|https://api.howners.com|https://your-domain.com|g' src/environments/environment.prod.ts
export const environment = {
  production: true,
  apiUrl: 'https://api.howners.com/api',
  wsUrl: 'https://api.howners.com'
};
