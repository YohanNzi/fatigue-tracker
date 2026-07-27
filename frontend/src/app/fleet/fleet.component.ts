import { CommonModule } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';

import { FatigueApiService } from '../services/fatigue-api.service';
import { FleetRow } from '../models/fatigue.models';

/**
 * Vue Flotte (J5.1) : tableau des appareils avec indice de fatigue et alerte de
 * maintenance, appareils en alerte mis en tête et surlignés. Données publiques
 * (aucun jeton requis en lecture) — le flow JWT arrive en J5.3 pour le recalcul.
 */
@Component({
  selector: 'app-fleet',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatChipsModule,
    MatProgressBarModule,
    MatIconModule,
    MatButtonModule
  ],
  templateUrl: './fleet.component.html',
  styleUrl: './fleet.component.scss'
})
export class FleetComponent implements OnInit {
  private readonly api = inject(FatigueApiService);

  readonly rows = signal<FleetRow[]>([]);
  readonly loading = signal(true);
  readonly error = signal<string | null>(null);

  readonly alertCount = computed(() => this.rows().filter((row) => row.maintenanceAlert).length);

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
        this.loading.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger la flotte. Le back est-il démarré sur http://localhost:8080 ?');
        this.loading.set(false);
      }
    });
  }
}
