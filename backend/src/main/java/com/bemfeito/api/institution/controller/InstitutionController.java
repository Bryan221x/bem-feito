package com.bemfeito.api.institution.controller;

import com.bemfeito.api.common.dto.PageResponse;
import com.bemfeito.api.institution.dto.InstitutionManagementResponse;
import com.bemfeito.api.institution.dto.InstitutionManagementSummary;
import com.bemfeito.api.institution.dto.InstitutionPatch;
import com.bemfeito.api.institution.dto.InstitutionPublicResponse;
import com.bemfeito.api.institution.dto.InstitutionRequest;
import com.bemfeito.api.institution.dto.InstitutionSummary;
import com.bemfeito.api.institution.service.InstitutionQueryService;
import com.bemfeito.api.institution.service.InstitutionService;
import com.bemfeito.api.unit.dto.UnitPublicResponse;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstitutionController {
    private final InstitutionService service;
    private final InstitutionQueryService queries;
    public InstitutionController(InstitutionService service,InstitutionQueryService queries) { this.service=service;this.queries=queries; }
    @GetMapping("/api/institutions")
    public PageResponse<InstitutionSummary> list(@RequestParam(required=false) Long cityId,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.list(cityId,page,size); }
    @GetMapping("/api/institutions/{id}")
    public InstitutionPublicResponse detail(@PathVariable Long id) { return queries.publicDetail(id); }
    @GetMapping("/api/institutions/{id}/units")
    public List<UnitPublicResponse> units(@PathVariable Long id) { return queries.publicUnits(id); }
    @GetMapping("/api/management/institutions")
    public PageResponse<InstitutionManagementSummary> managementList(@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="20") int size) { return queries.managementList(page,size); }
    @GetMapping("/api/management/institutions/{id}")
    public InstitutionManagementResponse management(@PathVariable Long id) { return queries.management(id); }
    @PostMapping("/api/institutions")
    public ResponseEntity<InstitutionManagementResponse> create(@Valid @RequestBody InstitutionRequest request) {
        var result=service.create(request);
        return ResponseEntity.created(URI.create("/api/management/institutions/"+result.id())).body(result);
    }
    @PutMapping("/api/institutions/{id}")
    public InstitutionManagementResponse replace(@PathVariable Long id,@Valid @RequestBody InstitutionRequest request) { return service.replace(id,request); }
    @PatchMapping("/api/institutions/{id}")
    public InstitutionManagementResponse patch(@PathVariable Long id,@RequestBody InstitutionPatch request) { return service.patch(id,request); }
}
