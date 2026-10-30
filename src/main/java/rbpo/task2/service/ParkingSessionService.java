package rbpo.task2.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rbpo.task2.model.ParkingSession;
import rbpo.task2.model.ParkingZone;
import rbpo.task2.repository.ParkingPassRepository;
import rbpo.task2.repository.ParkingSessionRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ParkingSessionService {
    private final ParkingSessionRepository sessionRepository;
    private final ParkingPassRepository passRepository;
    private final VehicleService vehicleService;
    private final ParkingZoneService zoneService;

    public ParkingSessionService(ParkingSessionRepository sessionRepository, ParkingPassRepository passRepository,
                                 VehicleService vehicleService, ParkingZoneService zoneService) {
        this.sessionRepository = sessionRepository;
        this.passRepository = passRepository;
        this.vehicleService = vehicleService;
        this.zoneService = zoneService;
    }

    public List<ParkingSession> getAll() {
        return sessionRepository.findAll();
    }

    public ParkingSession getById(Long id) {
        return sessionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Парковочная сессия не найдена"));
    }

    // бизнес-операция: въезд. Нельзя, если машина уже на парковке или в зоне нет свободных мест
    @Transactional
    public ParkingSession enter(Long vehicleId, Long zoneId) {
        ParkingSession session = new ParkingSession();
        session.setVehicle(vehicleService.getById(vehicleId));
        session.setZone(zoneService.getById(zoneId));

        if (sessionRepository.existsByVehicleIdAndExitTimeIsNull(vehicleId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Автомобиль уже на парковке");
        }
        if (sessionRepository.countByZoneIdAndExitTimeIsNull(zoneId) >= session.getZone().getCapacity()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "В зоне нет свободных мест");
        }
        session.setEntryTime(LocalDateTime.now());
        return sessionRepository.save(session);
    }

    // бизнес-операция: выезд. Сумму считает сервер, а по действующему абонементу этой зоны стоянка бесплатная
    @Transactional
    public ParkingSession exit(Long id) {
        ParkingSession session = getById(id);
        if (session.getExitTime() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Сессия уже завершена");
        }
        ParkingZone zone = session.getZone();
        LocalDateTime now = LocalDateTime.now();

        boolean hasPass = passRepository.existsByVehicleIdAndZoneIdAndStatusAndValidToAfter(
                session.getVehicle().getId(), zone.getId(), "ACTIVE", now);
        if (hasPass) {
            session.setAmount(0.0);
        } else {
            long hours = Duration.between(session.getEntryTime(), now).toHours() + 1; //начатый час считается как целый
            session.setAmount(hours * zone.getHourlyPrice());
        }
        session.setExitTime(now);
        return sessionRepository.save(session);
    }

    public void delete(Long id) {
        getById(id);
        sessionRepository.deleteById(id);
    }
}
