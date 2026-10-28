package com.bemfeito.api.unit.hours.controller;

import com.bemfeito.api.unit.hours.dto.OpeningHoursRequest;
import com.bemfeito.api.unit.hours.dto.OpeningHoursResponse;
import com.bemfeito.api.unit.hours.service.UnitOpeningHoursService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnitOpeningHoursController {
    private final UnitOpeningHoursService service;
    public UnitOpeningHoursController(UnitOpeningHoursService service) { this.service=service; }
    @GetMapping("/api/management/units/{unitId}/opening-hours")
    public List<OpeningHoursResponse> list(@PathVariable Long unitId) { return service.list(unitId); }
    @PutMapping("/api/units/{unitId}/opening-hours/{day}")
    public OpeningHoursResponse put(@PathVariable Long unitId,@PathVariable int day,@Valid @RequestBody OpeningHoursRequest request) {
        return service.put(unitId,day,request);
    }
}
