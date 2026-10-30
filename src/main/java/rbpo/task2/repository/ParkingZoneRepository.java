package rbpo.task2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rbpo.task2.model.ParkingZone;

public interface ParkingZoneRepository extends JpaRepository<ParkingZone, Long> {
}
