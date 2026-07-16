package dev.ynzi.fatiguetracker.aircraft;

import dev.ynzi.fatiguetracker.aircraft.dto.AircraftRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AircraftService {

    private final AircraftRepository aircraftRepository;

    public AircraftService(AircraftRepository aircraftRepository) {
        this.aircraftRepository = aircraftRepository;
    }

    public List<Aircraft> findAll() {
        return aircraftRepository.findAll();
    }

    public Aircraft findById(Long id) {
        return aircraftRepository.findById(id)
                .orElseThrow(() -> new AircraftNotFoundException(id));
    }

    @Transactional
    public Aircraft create(AircraftRequest request) {
        Aircraft aircraft = new Aircraft(request.registration(), request.model(), request.flightHours());
        return aircraftRepository.save(aircraft);
    }

    @Transactional
    public Aircraft update(Long id, AircraftRequest request) {
        Aircraft existing = findById(id);
        existing.setRegistration(request.registration());
        existing.setModel(request.model());
        existing.setFlightHours(request.flightHours());
        return aircraftRepository.save(existing);
    }

    @Transactional
    public void delete(Long id) {
        Aircraft existing = findById(id);
        aircraftRepository.delete(existing);
    }
}
