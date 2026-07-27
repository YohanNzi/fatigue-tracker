import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, forkJoin, map } from 'rxjs';

import { API_BASE } from '../core/api.config';
import {
  AircraftResponse,
  FatigueStatusResponse,
  FleetFatigueResponse,
  FleetRow,
  FlightReadingResponse,
  PagedResponse
} from '../models/fatigue.models';

/**
 * Accès à l'API fatigue. La vue Flotte croise deux endpoints publics
 * ({@code GET /api/aircraft} pour l'immatriculation/modèle, {@code GET /api/fatigue}
 * pour l'indice) car {@code /api/fatigue} ne porte que l'{@code aircraftId} — on fusionne
 * en mémoire pour afficher des lignes lisibles (immat + indice + alerte).
 */
@Injectable({ providedIn: 'root' })
export class FatigueApiService {
  private readonly http = inject(HttpClient);

  getFleetView(): Observable<FleetRow[]> {
    return forkJoin({
      aircraft: this.http.get<AircraftResponse[]>(`${API_BASE}/api/aircraft`),
      fleet: this.http.get<FleetFatigueResponse>(`${API_BASE}/api/fatigue`)
    }).pipe(
      map(({ aircraft, fleet }) => {
        const statusByAircraftId = new Map(fleet.aircraft.map((s) => [s.aircraftId, s]));
        return aircraft
          .map((ac): FleetRow => {
            const status = statusByAircraftId.get(ac.id);
            return {
              aircraftId: ac.id,
              registration: ac.registration,
              model: ac.model,
              fatigueIndex: status?.fatigueIndex ?? 0,
              readingsCount: status?.readingsCount ?? 0,
              computedAt: status?.computedAt ?? null,
              maintenanceAlert: status?.maintenanceAlert ?? false,
              computed: status?.computed ?? false
            };
          })
          // Appareils en alerte de maintenance en tête, puis indice décroissant.
          .sort((a, b) => Number(b.maintenanceAlert) - Number(a.maintenanceAlert) || b.fatigueIndex - a.fatigueIndex);
      })
    );
  }

  getAircraft(id: number): Observable<AircraftResponse> {
    return this.http.get<AircraftResponse>(`${API_BASE}/api/aircraft/${id}`);
  }

  getAircraftFatigue(id: number): Observable<FatigueStatusResponse> {
    return this.http.get<FatigueStatusResponse>(`${API_BASE}/api/aircraft/${id}/fatigue`);
  }

  /** Relevés d'un appareil, paginés (endpoint public paginé côté back). */
  getReadings(id: number, pageIndex: number, pageSize: number): Observable<PagedResponse<FlightReadingResponse>> {
    const params = new HttpParams()
      .set('page', pageIndex)
      .set('size', pageSize)
      .set('sort', 'recordedAt,desc');
    return this.http.get<PagedResponse<FlightReadingResponse>>(`${API_BASE}/api/aircraft/${id}/readings`, { params });
  }
}
