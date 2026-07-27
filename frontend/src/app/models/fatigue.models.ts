/** Miroirs TypeScript des DTO exposés par l'API (voir dev.ynzi.fatiguetracker.*.dto). */

export interface AircraftResponse {
  id: number;
  registration: string;
  model: string;
  flightHours: number;
}

export interface FatigueStatusResponse {
  aircraftId: number;
  fatigueIndex: number;
  readingsCount: number;
  computedAt: string | null;
  maintenanceAlert: boolean;
  /** false tant que le job de recalcul n'a jamais produit de résultat pour cet appareil. */
  computed: boolean;
}

export interface FleetFatigueResponse {
  aircraft: FatigueStatusResponse[];
  maintenanceAlerts: FatigueStatusResponse[];
}

/** Ligne d'affichage : fusion appareil (registration/model) + son état de fatigue. */
export interface FleetRow {
  aircraftId: number;
  registration: string;
  model: string;
  fatigueIndex: number;
  readingsCount: number;
  computedAt: string | null;
  maintenanceAlert: boolean;
  computed: boolean;
}
