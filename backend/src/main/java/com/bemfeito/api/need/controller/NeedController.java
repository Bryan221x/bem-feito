package com.bemfeito.api.need.controller;

import com.bemfeito.api.need.dto.NeedPatch;
import com.bemfeito.api.need.dto.NeedRequest;
import com.bemfeito.api.need.dto.NeedResponse;
import com.bemfeito.api.need.service.NeedService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NeedController {
    private final NeedService service;
    public NeedController(NeedService service) { this.service=service; }
    @GetMapping("/api/management/units/{unitId}/needs")
    public List<NeedResponse> list(@PathVariable Long unitId) { return service.list(unitId); }
    @GetMapping("/api/management/units/{unitId}/needs/{id}")
    public NeedResponse find(@PathVariable Long unitId,@PathVariable Long id) { return service.find(unitId,id); }
    @PostMapping("/api/units/{unitId}/needs")
    public ResponseEntity<NeedResponse> create(@PathVariable Long unitId,@Valid @RequestBody NeedRequest request) {
        var result=service.create(unitId,request);
        return ResponseEntity.created(URI.create("/api/management/units/"+unitId+"/needs/"+result.id())).body(result);
    }
    @PutMapping("/api/units/{unitId}/needs/{id}")
    public NeedResponse replace(@PathVariable Long unitId,@PathVariable Long id,@Valid @RequestBody NeedRequest request) { return service.replace(unitId,id,request); }
    @PatchMapping("/api/units/{unitId}/needs/{id}")
    public NeedResponse patch(@PathVariable Long unitId,@PathVariable Long id,@RequestBody NeedPatch request) { return service.patch(unitId,id,request); }
    @DeleteMapping("/api/units/{unitId}/needs/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long unitId,@PathVariable Long id) { service.delete(unitId,id);return ResponseEntity.noContent().build(); }
}
