import { CommonModule } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatChipsModule } from '@angular/material/chips';
import { MatIconModule } from '@angular/material/icon';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';

import { FatigueApiService } from '../services/fatigue-api.service';
import { AircraftResponse, FatigueStatusResponse, FlightReadingResponse } from '../models/fatigue.models';

/**
 * Vue Détail appareil (J5.2) : caractéristiques + état de fatigue + ses relevés de vol
 * paginés côté serveur (le back renvoie une page à la fois via {@code PagedModel}).
 * Le {@code MatPaginator} pilote directement les appels API (pagination serveur, pas
 * client) — cohérent avec la borne posée côté back.
 */
@Component({
  selector: 'app-aircraft-detail',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatCardModule,
    MatChipsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressBarModule
  ],
  templateUrl: './aircraft-detail.component.html',
  styleUrl: './aircraft-detail.component.scss'
})
export class AircraftDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly api = inject(FatigueApiService);

  aircraftId!: number;

  readonly aircraft = signal<AircraftResponse | null>(null);
  readonly fatigue = signal<FatigueStatusResponse | null>(null);
  readonly readings = signal<FlightReadingResponse[]>([]);
  readonly totalReadings = signal(0);
  readonly pageSize = signal(10);
  readonly pageIndex = signal(0);
  readonly loadingReadings = signal(true);
  readonly error = signal<string | null>(null);

  readonly displayedColumns = ['recordedAt', 'cycles', 'maxLoadFactor', 'flightHours'];

  ngOnInit(): void {
    this.aircraftId = Number(this.route.snapshot.paramMap.get('id'));
    this.api.getAircraft(this.aircraftId).subscribe({
      next: (aircraft) => this.aircraft.set(aircraft),
      error: () => this.error.set(`Appareil ${this.aircraftId} introuvable.`)
    });
    this.api.getAircraftFatigue(this.aircraftId).subscribe({
      next: (fatigue) => this.fatigue.set(fatigue),
      error: () => this.fatigue.set(null)
    });
    this.loadReadings();
  }

  loadReadings(): void {
    this.loadingReadings.set(true);
    this.api.getReadings(this.aircraftId, this.pageIndex(), this.pageSize()).subscribe({
      next: (page) => {
        this.readings.set(page.content);
        this.totalReadings.set(page.page.totalElements);
        this.loadingReadings.set(false);
      },
      error: () => {
        this.error.set('Impossible de charger les relevés. Le back est-il démarré sur http://localhost:8080 ?');
        this.loadingReadings.set(false);
      }
    });
  }

  onPage(event: PageEvent): void {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadReadings();
  }
}
