FROM node:18-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
COPY . .
# Domaine de déploiement : l'API est attendue sur api.<APP_DOMAIN> (cf. Caddyfile).
# Vide (défaut) → environment.prod.ts inchangé (howners.com). Pour un staging :
#   docker build --build-arg APP_DOMAIN=staging.mondomaine.fr …
# (ou APP_DOMAIN dans le .env passé à docker compose, cf. docker-compose.prod.yml)
ARG APP_DOMAIN=""
RUN if [ -n "$APP_DOMAIN" ]; then \
      sed -i "s|api\.howners\.com|api.${APP_DOMAIN}|g" src/environments/environment.prod.ts && \
      grep "apiUrl" src/environments/environment.prod.ts; \
    fi
# Prérend les routes publiques (landing) en HTML statique : meilleur SEO + paint.
# La sortie est dans dist/howners-api/browser/ (le serveur SSR dans server/ n'est pas servi).
RUN npm run prerender

FROM nginx:1.25-alpine
COPY --from=build /app/dist/howners-api/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN chown -R nginx:nginx /usr/share/nginx/html
EXPOSE 80
# 127.0.0.1 explicite : dans l'image alpine, localhost résout d'abord en ::1
# (IPv6) où nginx n'écoute pas — le healthcheck serait toujours "unhealthy".
HEALTHCHECK --interval=30s --timeout=3s CMD wget --quiet --tries=1 --spider http://127.0.0.1:80 || exit 1
