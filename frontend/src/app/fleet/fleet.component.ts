import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialog } from '@angular/material/dialog';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';

import { AircraftDetailComponent } from '../aircraft-detail/aircraft-detail.component';
import { AuthService } from '../core/auth.service';
import { LoginDialogComponent } from '../auth/login-dialog.component';
import { FatigueApiService } from '../services/fatigue-api.service';
import { FleetRow } from '../models/fatigue.models';

/**
 * Tableau de bord Flotte (dashboard master-détail sur une seule page) : indicateurs
 * (KPI) + tableau des appareils, puis le détail de l'appareil sélectionné en dessous
 * (carte + relevés paginés, composant {@link AircraftDetailComponent} embarqué).
 * Par défaut on met en avant l'appareil en alerte de maintenance ; cliquer une ligne
 * change la sélection. Données publiques (aucun jeton requis en lecture).
 */
@Component({
  selector: 'app-fleet',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule,
    MatChipsModule,
    MatProgressBarModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    AircraftDetailComponent
  ],
  templateUrl: './fleet.component.html',
  styleUrl: './fleet.component.scss'
})
export class FleetComponent implements OnInit {
  private readonly api = inject(FatigueApiService);
  private readonly dialog = inject(MatDialog);
  private readonly snackBar = inject(MatSnackBar);
  protected readonly auth = inject(AuthService);

  readonly rows = signal<FleetRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly selectedId = signal<number | null>(null);
  readonly recomputing = signal(false);
  readonly resetting = signal(false);

  /** Accord du compteur d'alertes (pipe i18nPlural, pas de « appareil(s) »). */
  readonly alertLabels: Record<string, string> = {
    '=1': '1 appareil en alerte de maintenance',
    other: '# appareils en alerte de maintenance'
  };

  readonly total = computed(() => this.rows().length);
  readonly alertCount = computed(() => this.rows().filter((row) => row.maintenanceAlert).length);
  readonly computedCount = computed(() => this.rows().filter((row) => row.computed).length);
  readonly selectedRow = computed(() => this.rows().find((row) => row.aircraftId === this.selectedId()) ?? null);

  /** Statut juste après l'immatriculation : visible sans défiler sur petit écran. */
  readonly displayedColumns = ['registration', 'status', 'fatigueIndex', 'model', 'readingsCount', 'computedAt'];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.api.getFleetView().subscribe({
      next: (rows) => {
        this.rows.set(rows);
        this.ensureSelection(rows);
        this.loading.set(false);
      },
      error: (err) => {
        console.error('Chargement de la flotte échoué', err);
        this.error.set(
          'Impossible de charger la flotte : serveur injoignable. Réessayez avec « Rafraîchir » ; au premier appel, la démo peut mettre près d\'une minute à démarrer.'
        );
        this.loading.set(false);
      }
    });
  }

  select(aircraftId: number): void {
    this.selectedId.set(aircraftId);
  }

  /**
   * Recalcule la fatigue de la flotte (action protégée MAINT). Si l'utilisateur n'est
   * pas connecté en MAINT, ouvre d'abord la connexion puis enchaîne si le rôle convient.
   */
  recompute(): void {
    this.withMaint(() => this.runRecompute());
  }

  /** Restaure la flotte de démo (action protégée MAINT), proposée depuis l'état vide. */
  resetDemo(): void {
    this.withMaint(() => this.runResetDemo());
  }

  /** Enchaîne l'action si l'utilisateur est MAINT, sinon ouvre d'abord la connexion. */
  private withMaint(action: () => void): void {
    if (this.auth.isMaint()) {
      action();
      return;
    }
    this.dialog
      .open(LoginDialogComponent, { autoFocus: 'dialog' })
      .afterClosed()
      .subscribe(() => {
        if (this.auth.isMaint()) {
          action();
        }
      });
  }

  private runRecompute(): void {
    this.recomputing.set(true);
    this.api.recompute().subscribe({
      next: (result) => {
        this.recomputing.set(false);
        this.snackBar.open(`Fatigue recalculée pour ${FleetComponent.aircraftCount(result.aircraftProcessed)}.`, 'OK', {
          duration: 4000
        });
        this.load();
      },
      error: (err) => {
        this.recomputing.set(false);
        this.showError(err.status, 'recalculer la fatigue');
      }
    });
  }

  private runResetDemo(): void {
    this.resetting.set(true);
    this.api.resetDemo().subscribe({
      next: (result) => {
        this.resetting.set(false);
        this.snackBar.open(`Données de démo restaurées : ${FleetComponent.aircraftCount(result.aircraftSeeded)}.`, 'OK', {
          duration: 4000
        });
        this.load();
      },
      error: (err) => {
        this.resetting.set(false);
        this.showError(err.status, 'restaurer les données');
      }
    });
  }

  /** Erreur persistante (pas de durée) : elle reste jusqu'à ce que l'utilisateur la ferme. */
  private showError(status: number, action: string): void {
    const message =
      status === 401 || status === 403
        ? `Connectez-vous avec un compte Maintenance pour ${action}.`
        : `Impossible de ${action} : serveur injoignable. Réessayez dans quelques secondes.`;
    this.snackBar.open(message, 'Fermer', { politeness: 'assertive' });
  }

  private static aircraftCount(n: number): string {
    return n === 1 ? '1 appareil' : `${n} appareils`;
  }

  /** Met en avant l'appareil en alerte par défaut ; conserve la sélection si toujours présente. */
  private ensureSelection(rows: FleetRow[]): void {
    const current = this.selectedId();
    if (current !== null && rows.some((row) => row.aircraftId === current)) {
      return;
    }
    const featured = rows.find((row) => row.maintenanceAlert) ?? rows[0];
    this.selectedId.set(featured ? featured.aircraftId : null);
  }
}
