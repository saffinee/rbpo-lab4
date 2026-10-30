package rbpo.task2.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import rbpo.task2.model.ParkingPass;
import rbpo.task2.service.ParkingPassService;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.access.prepost.PostFilter;

@RestController //спринг находит этот класс при запуске и запоминает адреса
@RequestMapping("/api/parking-passes")
public class ParkingPassController {
    private final ParkingPassService service;

    public ParkingPassController(ParkingPassService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @PostFilter("@access.pass(filterObject.id)")
    public List<ParkingPass> getAll() {
        return service.getAll();
    }

    @PreAuthorize("@access.pass(#id)")
    @GetMapping("/{id}")
    public ParkingPass getById(@PathVariable Long id) {
        return service.getById(id);
    }

    //бизнес-операция: оформить абонемент на days дней
    @PreAuthorize("@access.vehicle(#vehicleId)")
    @PostMapping("/purchase")
    @ResponseStatus(HttpStatus.CREATED) //201
    public ParkingPass purchase(@RequestParam Long vehicleId, @RequestParam Long zoneId, @RequestParam int days) {
        return service.purchase(vehicleId, zoneId, days);
    }

    //бизнес-операция: продлить абонемент на days дней
    @PreAuthorize("@access.pass(#id)")
    @PostMapping("/{id}/extend")
    public ParkingPass extend(@PathVariable Long id, @RequestParam int days) {
        return service.extend(id, days);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT) //204
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
