package rbpo.task2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rbpo.task2.model.ParkingSession;

public interface ParkingSessionRepository extends JpaRepository<ParkingSession, Long> {

    // машина на парковке, пока у сессии нет времени выезда
    boolean existsByVehicleIdAndExitTimeIsNull(Long vehicleId);

    long countByZoneIdAndExitTimeIsNull(Long zoneId);
}
