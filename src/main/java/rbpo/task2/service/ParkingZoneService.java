package rbpo.task2.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import rbpo.task2.model.ParkingZone;
import rbpo.task2.repository.ParkingZoneRepository;

import java.util.List;

@Service
public class ParkingZoneService {
    private final ParkingZoneRepository zoneRepository;

    public ParkingZoneService(ParkingZoneRepository zoneRepository) {
        this.zoneRepository = zoneRepository;
    }

    public List<ParkingZone> getAll() {
        return zoneRepository.findAll();
    }

    public ParkingZone getById(Long id) {
        return zoneRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Парковочная зона не найдена"));
    }

    public ParkingZone create(ParkingZone zone) {
        zone.setId(null);
        return zoneRepository.save(zone);
    }

    public ParkingZone update(Long id, ParkingZone request) {
        ParkingZone zone = getById(id);
        if (request.getCapacity() != null) {
            zone.setCapacity(request.getCapacity());
        }
        if (request.getHourlyPrice() != null) {
            zone.setHourlyPrice(request.getHourlyPrice());
        }
        return zoneRepository.save(zone);
    }

    public void delete(Long id) {
        getById(id);
        zoneRepository.deleteById(id); //сессии и абонементы зоны база удалит сама
    }
}
