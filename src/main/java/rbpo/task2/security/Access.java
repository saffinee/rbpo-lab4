package rbpo.task2.security;

import org.springframework.stereotype.Component;
import org.springframework.security.core.context.SecurityContextHolder;
import rbpo.task2.repository.*;

@Component("access")
public class Access {
    private final UserRepository users;
    private final VehicleRepository vehicles;
    private final ParkingSessionRepository sessions;
    private final ParkingPassRepository passes;
    public Access(UserRepository users, VehicleRepository vehicles, ParkingSessionRepository sessions, ParkingPassRepository passes) {
        this.users = users; this.vehicles = vehicles; this.sessions = sessions; this.passes = passes;
    }
    public boolean admin() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
    public Long userId() {
        return users.findByEmail(SecurityContextHolder.getContext().getAuthentication().getName()).orElseThrow().getId();
    }
    public boolean user(Long id) { return admin() || userId().equals(id); }
    public boolean vehicle(Long id) {
        return admin() || vehicles.findById(id).map(v -> user(v.getOwner().getId())).orElse(false);
    }
    public boolean session(Long id) {
        return admin() || sessions.findById(id).map(s -> vehicle(s.getVehicle().getId())).orElse(false);
    }
    public boolean pass(Long id) {
        return admin() || passes.findById(id).map(p -> vehicle(p.getVehicle().getId())).orElse(false);
    }
}
