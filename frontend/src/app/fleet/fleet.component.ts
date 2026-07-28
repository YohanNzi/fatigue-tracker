import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';

import { AircraftDetailComponent } from '../aircraft-detail/aircraft-detail.component';
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
    AircraftDetailComponent
  ],
  templateUrl: './fleet.component.html',
  styleUrl: './fleet.component.scss'
})
export class FleetComponent implements OnInit {
  private readonly api = inject(FatigueApiService);

  readonly rows = signal<FleetRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);
  readonly selectedId = signal<number | null>(null);

  readonly total = computed(() => this.rows().length);
  readonly alertCount = computed(() => this.rows().filter((row) => row.maintenanceAlert).length);
  readonly computedCount = computed(() => this.rows().filter((row) => row.computed).length);
  readonly selectedRow = computed(() => this.rows().find((row) => row.aircraftId === this.selectedId()) ?? null);

  readonly displayedColumns = ['registration', 'model', 'fatigueIndex', 'readingsCount', 'computedAt', 'status'];

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
      error: () => {
        this.error.set('Impossible de charger la flotte. Le back est-il démarré sur http://localhost:8080 ?');
        this.loading.set(false);
      }
    });
  }

  select(aircraftId: number): void {
    this.selectedId.set(aircraftId);
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
