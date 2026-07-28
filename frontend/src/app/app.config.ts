import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';
import { authInterceptor } from './core/auth.interceptor';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    // withComponentInputBinding : le paramètre de route :id est injecté dans l'@Input id
    // du composant détail (utilisé aussi embarqué sous la Flotte).
    provideRouter(routes, withComponentInputBinding()),
    provideAnimationsAsync(),
    // authInterceptor : ajoute le Bearer JWT aux appels de l'API (J5.3).
    provideHttpClient(withInterceptors([authInterceptor]))
  ]
};
