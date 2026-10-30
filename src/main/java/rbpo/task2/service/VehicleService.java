package rbpo.task2.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import rbpo.task2.model.ParkingPass;
import rbpo.task2.model.Vehicle;
import rbpo.task2.repository.ParkingPassRepository;
import rbpo.task2.repository.ParkingSessionRepository;
import rbpo.task2.repository.VehicleRepository;

import java.util.List;

@Service
public class VehicleService {
    private final VehicleRepository vehicleRepository;
    private final UserService userService;
    private final ParkingSessionRepository sessionRepository;
    private final ParkingPassRepository passRepository;

    public VehicleService(VehicleRepository vehicleRepository, UserService userService,
                          ParkingSessionRepository sessionRepository, ParkingPassRepository passRepository) {
        this.vehicleRepository = vehicleRepository;
        this.userService = userService;
        this.sessionRepository = sessionRepository;
        this.passRepository = passRepository;
    }

    public List<Vehicle> getAll() {
        return vehicleRepository.findAll();
    }

    public Vehicle getById(Long id) {
        return vehicleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Автомобиль не найден"));
    }

    public Vehicle create(Vehicle vehicle, Long ownerId) {
        vehicle.setId(null);
        vehicle.setOwner(userService.getById(ownerId));
        return vehicleRepository.save(vehicle);
    }

    //владельца тут не меняем - для этого есть операция transfer
    public Vehicle update(Long id, Vehicle request) {
        Vehicle vehicle = getById(id);
        if (request.getNumber() != null) {
            vehicle.setNumber(request.getNumber());
        }
        return vehicleRepository.save(vehicle);
    }

    public void delete(Long id) {
        getById(id);
        vehicleRepository.deleteById(id); //сессии и абонементы машины база удалит сама
    }

    // бизнес-операция: передать автомобиль другому пользователю
    // нельзя, если машина сейчас на парковке; абонементы машины при этом отменяются
    @Transactional //все изменения сохраняются вместе, либо не сохраняется ничего
    public Vehicle transfer(Long id, Long newOwnerId) {
        Vehicle vehicle = getById(id);
        if (sessionRepository.existsByVehicleIdAndExitTimeIsNull(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Автомобиль сейчас на парковке");
        }
        if (vehicle.getOwner().getId().equals(newOwnerId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Автомобиль уже принадлежит этому пользователю");
        }
        vehicle.setOwner(userService.getById(newOwnerId));

        for (ParkingPass pass : passRepository.findByVehicleId(id)) {
            pass.setStatus("CANCELLED");
            passRepository.save(pass);
        }
        return vehicleRepository.save(vehicle);
    }
}
