package com.bemfeito.api.unit.contact.controller;

import com.bemfeito.api.common.contact.dto.ContactPatch;
import com.bemfeito.api.common.contact.dto.ContactRequest;
import com.bemfeito.api.common.contact.dto.ContactResponse;
import com.bemfeito.api.unit.contact.service.UnitContactService;
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
public class UnitContactController {
    private final UnitContactService service;
    public UnitContactController(UnitContactService service) { this.service=service; }
    @GetMapping("/api/management/units/{unitId}/contacts")
    public List<ContactResponse> list(@PathVariable Long unitId) { return service.list(unitId); }
    @GetMapping("/api/management/units/{unitId}/contacts/{id}")
    public ContactResponse find(@PathVariable Long unitId,@PathVariable Long id) { return service.find(unitId,id); }
    @PostMapping("/api/units/{unitId}/contacts")
    public ResponseEntity<ContactResponse> create(@PathVariable Long unitId,@Valid @RequestBody ContactRequest request) {
        var result=service.create(unitId,request);
        return ResponseEntity.created(URI.create("/api/management/units/"+unitId+"/contacts/"+result.id())).body(result);
    }
    @PutMapping("/api/units/{unitId}/contacts/{id}")
    public ContactResponse replace(@PathVariable Long unitId,@PathVariable Long id,@Valid @RequestBody ContactRequest request) { return service.replace(unitId,id,request); }
    @PatchMapping("/api/units/{unitId}/contacts/{id}")
    public ContactResponse patch(@PathVariable Long unitId,@PathVariable Long id,@RequestBody ContactPatch request) { return service.patch(unitId,id,request); }
    @DeleteMapping("/api/units/{unitId}/contacts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long unitId,@PathVariable Long id) { service.delete(unitId,id);return ResponseEntity.noContent().build(); }
}
