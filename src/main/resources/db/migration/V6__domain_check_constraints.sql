-- V6 : défense en profondeur des invariants du modèle (déjà imposés par les entités
-- FlightReading / Aircraft et par la validation d'entrée). Un facteur de charge ≤ 0 est
-- physiquement impossible ET dangereux ici : il est élevé au cube dans le calcul de
-- fatigue, une valeur négative produirait une contribution négative capable de masquer
-- une alerte de maintenance. Le seed V5 respecte déjà toutes ces contraintes.

ALTER TABLE flight_reading
    ADD CONSTRAINT ck_flight_reading_cycles CHECK (cycles >= 0),
    ADD CONSTRAINT ck_flight_reading_max_load_factor CHECK (max_load_factor > 0),
    ADD CONSTRAINT ck_flight_reading_flight_hours CHECK (flight_hours >= 0);

ALTER TABLE aircraft
    ADD CONSTRAINT ck_aircraft_flight_hours CHECK (flight_hours >= 0);
