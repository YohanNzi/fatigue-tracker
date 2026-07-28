import { ApplicationConfig, provideZoneChangeDetection } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';

import { routes } from './app.routes';
import { provideAnimationsAsync } from '@angular/platform-browser/animations/async';

export const appConfig: ApplicationConfig = {
  providers: [
    provideZoneChangeDetection({ eventCoalescing: true }),
    // withComponentInputBinding : le paramètre de route :id est injecté dans l'@Input id
    // du composant détail (utilisé aussi embarqué sous la Flotte).
    provideRouter(routes, withComponentInputBinding()),
    provideAnimationsAsync(),
    // withInterceptors : l'intercepteur d'auth (Bearer JWT) sera branché ici en J5.3.
    provideHttpClient(withInterceptors([]))
  ]
};
