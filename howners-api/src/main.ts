import { platformBrowserDynamic } from '@angular/platform-browser-dynamic';
import * as Sentry from '@sentry/angular-ivy';

import { AppModule } from './app/app.module';
import { environment } from './environments/environment';

// Sentry n'est initialisé que si un DSN est fourni (vide en dev → aucun envoi réseau).
if (environment.sentryDsn) {
  Sentry.init({
    dsn: environment.sentryDsn,
    environment: environment.production ? 'prod' : 'dev',
    // Désactivé par défaut ; relever (ex. 0.1) pour activer le tracing de perf.
    tracesSampleRate: 0,
  });
}

platformBrowserDynamic().bootstrapModule(AppModule)
  .catch(err => console.error(err));
