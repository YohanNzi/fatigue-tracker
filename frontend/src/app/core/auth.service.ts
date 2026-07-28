import { HttpClient } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, tap } from 'rxjs';

import { API_BASE } from './api.config';
import { LoginRequest, LoginResponse } from '../models/fatigue.models';

interface Session {
  token: string;
  username: string;
  role: string;
  expiresAt: number;
}

const STORAGE_KEY = 'ft-auth';

/**
 * Authentification JWT (J5.3). Conserve le jeton (localStorage) et l'expose aux
 * signaux ; l'intercepteur y lit le Bearer. Le back reste la source de vérité des
 * autorisations : le front se contente de masquer/afficher les actions selon le rôle
 * (défense en profondeur, pas une sécurité côté client).
 */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly session = signal<Session | null>(this.restore());

  readonly username = computed(() => this.session()?.username ?? null);
  readonly role = computed(() => this.session()?.role ?? null);
  readonly isAuthenticated = computed(() => this.session() !== null);
  readonly isMaint = computed(() => this.session()?.role === 'MAINT');

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${API_BASE}/api/auth/login`, credentials).pipe(
      tap((response) => {
        const session: Session = {
          token: response.accessToken,
          username: credentials.username,
          role: response.role,
          expiresAt: Date.now() + response.expiresInSeconds * 1000
        };
        this.session.set(session);
        localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
      })
    );
  }

  logout(): void {
    this.session.set(null);
    localStorage.removeItem(STORAGE_KEY);
  }

  token(): string | null {
    const session = this.session();
    if (!session) {
      return null;
    }
    if (session.expiresAt <= Date.now()) {
      this.logout();
      return null;
    }
    return session.token;
  }

  private restore(): Session | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      return null;
    }
    try {
      const session = JSON.parse(raw) as Session;
      if (session.expiresAt <= Date.now()) {
        localStorage.removeItem(STORAGE_KEY);
        return null;
      }
      return session;
    } catch {
      localStorage.removeItem(STORAGE_KEY);
      return null;
    }
  }
}
