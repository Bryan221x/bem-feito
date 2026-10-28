package com.bemfeito.api.unit.controller;

import com.bemfeito.api.location.city.dto.CityResponse;
import com.bemfeito.api.unit.dto.UnitManagementResponse;
import com.bemfeito.api.unit.dto.UnitPatch;
import com.bemfeito.api.unit.dto.UnitPublicResponse;
import com.bemfeito.api.unit.dto.UnitRequest;
import com.bemfeito.api.unit.service.UnitQueryService;
import com.bemfeito.api.unit.service.UnitService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UnitController {
    private final UnitService service;
    private final UnitQueryService queries;
    public UnitController(UnitService service,UnitQueryService queries) { this.service=service;this.queries=queries; }
    @GetMapping("/api/cities/available")
    public List<CityResponse> availableCities() { return queries.availableCities(); }
    @GetMapping("/api/units/{id}")
    public UnitPublicResponse detail(@PathVariable Long id) { return queries.publicDetail(id); }
    @GetMapping("/api/management/units/{id}")
    public UnitManagementResponse management(@PathVariable Long id) { return queries.management(id); }
    @PostMapping("/api/institutions/{institutionId}/units")
    public ResponseEntity<UnitManagementResponse> create(@PathVariable Long institutionId,@Valid @RequestBody UnitRequest request) {
        var result=service.create(institutionId,request);
        return ResponseEntity.created(URI.create("/api/management/units/"+result.id())).body(result);
    }
    @PutMapping("/api/units/{id}")
    public UnitManagementResponse replace(@PathVariable Long id,@Valid @RequestBody UnitRequest request) { return service.replace(id,request); }
    @PatchMapping("/api/units/{id}")
    public UnitManagementResponse patch(@PathVariable Long id,@RequestBody UnitPatch request) { return service.patch(id,request); }
}
