package rbpo.task2.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rbpo.task2.model.Vehicle;
import rbpo.task2.service.VehicleService;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostFilter;

@RestController //спринг находит этот класс при запуске и запоминает адреса
@RequestMapping("/api/vehicles")
public class VehicleController {
    private final VehicleService service;

    public VehicleController(VehicleService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @PostFilter("@access.vehicle(filterObject.id)")
    public List<Vehicle> getAll() {
        return service.getAll();
    }

    @PreAuthorize("@access.vehicle(#id)")
    @GetMapping("/{id}")
    public Vehicle getById(@PathVariable Long id) {
        return service.getById(id);
    }

    @PreAuthorize("@access.user(#ownerId)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) //201
    public Vehicle create(@RequestBody Vehicle request, @RequestParam Long ownerId) {
        return service.create(request, ownerId);
    }

    @PreAuthorize("@access.vehicle(#id)")
    @PatchMapping("/{id}")
    public Vehicle update(@PathVariable Long id, @RequestBody Vehicle request) {
        return service.update(id, request);
    }

    //бизнес-операция: передать автомобиль другому пользователю
    @PreAuthorize("@access.vehicle(#id)")
    @PostMapping("/{id}/transfer")
    public Vehicle transfer(@PathVariable Long id, @RequestParam Long newOwnerId) {
        return service.transfer(id, newOwnerId);
    }

    @PreAuthorize("@access.vehicle(#id)")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) //204
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
