package com.bemfeito.api.unit.servicearea.controller;

import com.bemfeito.api.servicearea.dto.ServiceAreaResponse;
import com.bemfeito.api.unit.servicearea.dto.ServiceAreasRequest;
import com.bemfeito.api.unit.servicearea.service.UnitServiceAreaService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnitServiceAreaController {
    private final UnitServiceAreaService service;
    public UnitServiceAreaController(UnitServiceAreaService service) { this.service=service; }
    @GetMapping("/api/management/units/{unitId}/service-areas")
    public List<ServiceAreaResponse> list(@PathVariable Long unitId) { return service.list(unitId); }
    @PutMapping("/api/units/{unitId}/service-areas")
    public List<ServiceAreaResponse> replace(@PathVariable Long unitId,@Valid @RequestBody ServiceAreasRequest request) { return service.replace(unitId,request); }
}
