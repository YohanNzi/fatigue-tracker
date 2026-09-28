import { ApplicationConfig, LOCALE_ID, provideZoneChangeDetection } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { MatPaginatorIntl } from '@angular/material/paginator';

import { routes } from './app.routes';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { authInterceptor } from './core/auth.interceptor';
import { PaginatorIntlFr } from './core/paginator-intl-fr';

// Locale française : nombres (48 210,5) et dates cohérents avec une UI en français.
registerLocaleData(localeFr);

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    { provide: LOCALE_ID, useValue: 'fr-FR' },
    { provide: MatPaginatorIntl, useClass: PaginatorIntlFr },
    // withComponentInputBinding : le paramètre de route :id est injecté dans l'@Input id
    // du composant détail (utilisé aussi embarqué sous la Flotte).
    provideRouter(routes, withComponentInputBinding()),
    provideAnimationsAsync(),
    // authInterceptor : ajoute le Bearer JWT aux appels de l'API (J5.3).
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
