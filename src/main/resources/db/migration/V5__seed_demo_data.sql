-- V5 : données de DÉMONSTRATION (avions + relevés de vol) pour que le tableau de bord
-- ne soit pas vide au premier lancement. Purement illustratif — aucune donnée réelle
-- d'un employeur. L'indice de fatigue se calcule à la demande (POST /api/fatigue/recompute,
-- rôle MAINT) selon : index = Σ(cycles × maxLoadFactor^exponent) / normalizationFactor
-- (params par défaut : exponent=3, referenceLoadFactor=1, normalizationFactor=1000, seuil=80).
--
-- Spread visé après recompute : 2 avions en ALERTE (>80), 1 borderline (~72), 1 modéré
-- (~53), 1 sain (~20). Les avions sont référencés par immatriculation (sous-requête) pour
-- ne pas figer d'id explicite (colonnes IDENTITY BY DEFAULT → éviter la désync de séquence).

INSERT INTO aircraft (registration, model, flight_hours) VALUES
    ('F-GKXA', 'Airbus A320-214', 41250.0),   -- fatigue élevée → alerte
    ('F-GKXB', 'Airbus A320-214', 30870.0),   -- modéré
    ('F-GKXC', 'Airbus A319-111', 22540.0),   -- sain
    ('F-GKXD', 'Airbus A321-211', 37600.0),   -- borderline (juste sous le seuil)
    ('F-GKXE', 'Airbus A320-251N', 39980.0);  -- fatigue élevée → alerte

-- F-GKXA — Σ(cycles×load^3)/1000 ≈ 125 → ALERTE
INSERT INTO flight_reading (aircraft_id, recorded_at, cycles, max_load_factor, flight_hours)
SELECT id, ts, cyc, load, fh FROM aircraft, (VALUES
    (TIMESTAMP WITH TIME ZONE '2026-03-12 08:30:00+00', 5000, 2.5, 39800.0),
    (TIMESTAMP WITH TIME ZONE '2026-05-04 14:10:00+00', 3000, 2.0, 40600.0),
    (TIMESTAMP WITH TIME ZONE '2026-07-19 06:45:00+00', 4000, 1.8, 41250.0)
) AS r(ts, cyc, load, fh) WHERE registration = 'F-GKXA';

-- F-GKXB — index ≈ 53 → modéré (pas d'alerte)
INSERT INTO flight_reading (aircraft_id, recorded_at, cycles, max_load_factor, flight_hours)
SELECT id, ts, cyc, load, fh FROM aircraft, (VALUES
    (TIMESTAMP WITH TIME ZONE '2026-04-02 09:00:00+00', 4000, 1.8, 29900.0),
    (TIMESTAMP WITH TIME ZONE '2026-06-15 11:20:00+00', 3500, 1.7, 30500.0),
    (TIMESTAMP WITH TIME ZONE '2026-07-28 16:05:00+00', 3000, 1.6, 30870.0)
) AS r(ts, cyc, load, fh) WHERE registration = 'F-GKXB';

-- F-GKXC — index ≈ 20 → sain
INSERT INTO flight_reading (aircraft_id, recorded_at, cycles, max_load_factor, flight_hours)
SELECT id, ts, cyc, load, fh FROM aircraft, (VALUES
    (TIMESTAMP WITH TIME ZONE '2026-04-20 07:15:00+00', 3000, 1.4, 21800.0),
    (TIMESTAMP WITH TIME ZONE '2026-06-08 13:40:00+00', 2500, 1.3, 22200.0),
    (TIMESTAMP WITH TIME ZONE '2026-07-30 10:25:00+00', 2000, 1.5, 22540.0)
) AS r(ts, cyc, load, fh) WHERE registration = 'F-GKXC';

-- F-GKXD — index ≈ 72 → borderline (juste sous 80)
INSERT INTO flight_reading (aircraft_id, recorded_at, cycles, max_load_factor, flight_hours)
SELECT id, ts, cyc, load, fh FROM aircraft, (VALUES
    (TIMESTAMP WITH TIME ZONE '2026-03-28 05:50:00+00', 5000, 2.0, 36200.0),
    (TIMESTAMP WITH TIME ZONE '2026-05-22 15:30:00+00', 4000, 1.9, 37100.0),
    (TIMESTAMP WITH TIME ZONE '2026-07-11 12:00:00+00', 1500, 1.5, 37600.0)
) AS r(ts, cyc, load, fh) WHERE registration = 'F-GKXD';

-- F-GKXE — index ≈ 98 → ALERTE
INSERT INTO flight_reading (aircraft_id, recorded_at, cycles, max_load_factor, flight_hours)
SELECT id, ts, cyc, load, fh FROM aircraft, (VALUES
    (TIMESTAMP WITH TIME ZONE '2026-04-09 08:05:00+00', 6000, 2.2, 38600.0),
    (TIMESTAMP WITH TIME ZONE '2026-06-01 09:45:00+00', 3000, 2.0, 39400.0),
    (TIMESTAMP WITH TIME ZONE '2026-07-25 17:20:00+00', 2000, 1.7, 39980.0)
) AS r(ts, cyc, load, fh) WHERE registration = 'F-GKXE';
