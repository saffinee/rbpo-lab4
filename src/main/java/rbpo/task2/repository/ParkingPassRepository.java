package rbpo.task2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rbpo.task2.model.ParkingPass;

import java.time.LocalDateTime;
import java.util.List;

public interface ParkingPassRepository extends JpaRepository<ParkingPass, Long> {

    List<ParkingPass> findByVehicleId(Long vehicleId);

    // есть ли абонемент с таким статусом, который ещё не закончился на момент time
    boolean existsByVehicleIdAndZoneIdAndStatusAndValidToAfter(Long vehicleId, Long zoneId, String status,
                                                               LocalDateTime time);
}
