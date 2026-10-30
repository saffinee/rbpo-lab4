package rbpo.task2.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import rbpo.task2.model.Vehicle;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
}
