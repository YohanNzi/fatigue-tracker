import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';

import { API_BASE } from './api.config';
import { AuthService } from './auth.service';

/**
 * Ajoute l'en-tête {@code Authorization: Bearer <token>} aux appels de l'API quand un
 * jeton valide est présent. Ne touche qu'aux requêtes vers le back (API_BASE) et jamais
 * au login lui-même.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();

  const targetsApi = req.url.startsWith(API_BASE) || req.url.startsWith('/api');
  const isLogin = req.url.includes('/api/auth/login');

  if (token && targetsApi && !isLogin) {
    return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
  }
  return next(req);
};
