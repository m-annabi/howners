FROM node:18-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --ignore-scripts
COPY . .
# Prérend les routes publiques (landing) en HTML statique : meilleur SEO + paint.
# La sortie est dans dist/howners-api/browser/ (le serveur SSR dans server/ n'est pas servi).
RUN npm run prerender

FROM nginx:1.25-alpine
COPY --from=build /app/dist/howners-api/browser /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
RUN chown -R nginx:nginx /usr/share/nginx/html
EXPOSE 80
HEALTHCHECK --interval=30s --timeout=3s CMD wget --quiet --tries=1 --spider http://localhost:80 || exit 1
