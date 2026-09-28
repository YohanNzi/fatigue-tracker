import { Component, ElementRef, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';

import { AuthService } from '../core/auth.service';

/**
 * Boîte de dialogue de connexion (J5.3). Émet un JWT via {@code POST /api/auth/login}.
 * Les identifiants de démo sont rappelés dans le formulaire (projet portfolio public).
 * <p>
 * Le bouton d'envoi reste actif : la validation se fait à l'envoi, les champs en
 * erreur s'annoncent via {@code mat-error} et le focus va au premier champ invalide.
 */
@Component({
  selector: 'app-login-dialog',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatIconModule
  ],
  template: `
    <h2 mat-dialog-title>Connexion</h2>
    <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
      <mat-dialog-content>
        <p class="hint">
          Comptes de démo :
          <button type="button" class="hint__link" (click)="fill('demo.maint', 'maint123')">
            demo.maint / maint123 (écriture)
          </button>
          ·
          <button type="button" class="hint__link" (click)="fill('demo.viewer', 'viewer123')">
            demo.viewer / viewer123 (lecture)
          </button>
        </p>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Identifiant</mat-label>
          <input matInput formControlName="username" autocomplete="username" />
          <mat-error>Saisissez votre identifiant.</mat-error>
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Mot de passe</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="current-password" />
          <mat-error>Saisissez votre mot de passe.</mat-error>
        </mat-form-field>

        @if (error()) {
          <p class="error" role="alert"><mat-icon aria-hidden="true">error_outline</mat-icon> {{ error() }}</p>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="loading()">Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="loading()">
          {{ loading() ? 'Connexion…' : 'Se connecter' }}
        </button>
      </mat-dialog-actions>
    </form>
  `,
  styles: [
    `
      .full { width: 100%; }
      .hint { font-size: 0.82rem; color: var(--af-muted); margin: 0 0 12px; }
      .hint__link {
        background: none; border: none; padding: 0; font: inherit;
        color: var(--af-navy-600); cursor: pointer; text-decoration: underline;
      }
      :host-context([data-theme='dark']) .hint__link { color: #8fb2ff; }
      .error { display: flex; align-items: center; gap: 6px; color: var(--af-danger-text); font-size: 0.85rem; margin: 4px 0 0; }
      /* 340px sur desktop, mais jamais plus large que le panneau (max 80vw) moins le padding : tient à 320px. */
      mat-dialog-content { min-width: min(340px, calc(80vw - 48px)); }
    `
  ]
})
export class LoginDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly dialogRef = inject(MatDialogRef<LoginDialogComponent>);
  private readonly host = inject<ElementRef<HTMLElement>>(ElementRef);

  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly form = this.fb.nonNullable.group({
    username: ['', Validators.required],
    password: ['', Validators.required]
  });

  fill(username: string, password: string): void {
    this.form.setValue({ username, password });
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      this.host.nativeElement.querySelector<HTMLInputElement>('input.ng-invalid')?.focus();
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => {
        this.loading.set(false);
        this.error.set(LoginDialogComponent.messageFor(err.status));
      }
    });
  }

  private static messageFor(status: number): string {
    switch (status) {
      case 401:
        return 'Identifiant ou mot de passe incorrect. Vérifiez la saisie ou choisissez un compte de démo ci-dessus.';
      case 429:
        return 'Trop de tentatives de connexion. Réessayez dans 15 minutes au plus.';
      default:
        return 'Serveur injoignable. Réessayez dans quelques secondes : au premier appel, la démo peut mettre près d\'une minute à démarrer.';
    }
  }
}
