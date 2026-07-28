import { Component, inject, signal } from '@angular/core';
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
    <form [formGroup]="form" (ngSubmit)="submit()">
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
        </mat-form-field>

        <mat-form-field appearance="outline" class="full">
          <mat-label>Mot de passe</mat-label>
          <input matInput type="password" formControlName="password" autocomplete="current-password" />
        </mat-form-field>

        @if (error()) {
          <p class="error" role="alert"><mat-icon>error_outline</mat-icon> {{ error() }}</p>
        }
      </mat-dialog-content>

      <mat-dialog-actions align="end">
        <button mat-button type="button" mat-dialog-close [disabled]="loading()">Annuler</button>
        <button mat-flat-button color="primary" type="submit" [disabled]="loading() || form.invalid">
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
      .error { display: flex; align-items: center; gap: 6px; color: #c62828; font-size: 0.85rem; margin: 4px 0 0; }
      mat-dialog-content { min-width: 340px; }
    `
  ]
})
export class LoginDialogComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly dialogRef = inject(MatDialogRef<LoginDialogComponent>);

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
      return;
    }
    this.loading.set(true);
    this.error.set(null);
    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => {
        this.loading.set(false);
        this.error.set(err.status === 401 ? 'Identifiants invalides.' : 'Connexion impossible (back démarré ?).');
      }
    });
  }
}
