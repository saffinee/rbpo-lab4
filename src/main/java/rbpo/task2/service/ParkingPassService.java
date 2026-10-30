package rbpo.task2.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rbpo.task2.model.ParkingPass;
import rbpo.task2.repository.ParkingPassRepository;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParkingPassService {
    private final ParkingPassRepository passRepository;
    private final VehicleService vehicleService;
    private final ParkingZoneService zoneService;

    public ParkingPassService(ParkingPassRepository passRepository, VehicleService vehicleService,
                              ParkingZoneService zoneService) {
        this.passRepository = passRepository;
        this.vehicleService = vehicleService;
        this.zoneService = zoneService;
    }

    public List<ParkingPass> getAll() {
        return passRepository.findAll();
    }

    public ParkingPass getById(Long id) {
        return passRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Абонемент не найден"));
    }

    // бизнес-операция: оформить абонемент на days дней. Нельзя, если в этой зоне у машины уже есть действующий
    @Transactional
    public ParkingPass purchase(Long vehicleId, Long zoneId, int days) {
        ParkingPass pass = new ParkingPass();
        pass.setVehicle(vehicleService.getById(vehicleId));
        pass.setZone(zoneService.getById(zoneId));

        LocalDateTime now = LocalDateTime.now();
        if (days <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Количество дней должно быть больше нуля");
        }
        if (passRepository.existsByVehicleIdAndZoneIdAndStatusAndValidToAfter(vehicleId, zoneId, "ACTIVE", now)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "У автомобиля уже есть абонемент в этой зоне");
        }
        pass.setValidFrom(now);
        pass.setValidTo(now.plusDays(days));
        pass.setStatus("ACTIVE");
        return passRepository.save(pass);
    }

    // бизнес-операция: продлить абонемент на days дней. Продлить можно только действующий
    @Transactional
    public ParkingPass extend(Long id, int days) {
        ParkingPass pass = getById(id);
        if (days <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Количество дней должно быть больше нуля");
        }
        if (!pass.getStatus().equals("ACTIVE") || pass.getValidTo().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Абонемент отменён или уже истёк");
        }
        pass.setValidTo(pass.getValidTo().plusDays(days));
        return passRepository.save(pass);
    }

    public void delete(Long id) {
        getById(id);
        passRepository.deleteById(id);
    }
}
