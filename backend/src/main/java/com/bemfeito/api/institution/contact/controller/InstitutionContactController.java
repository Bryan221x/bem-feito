package com.bemfeito.api.institution.contact.controller;

import com.bemfeito.api.common.contact.dto.ContactPatch;
import com.bemfeito.api.common.contact.dto.ContactRequest;
import com.bemfeito.api.common.contact.dto.ContactResponse;
import com.bemfeito.api.institution.contact.service.InstitutionContactService;
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
public class InstitutionContactController {
    private final InstitutionContactService service;
    public InstitutionContactController(InstitutionContactService service) { this.service=service; }
    @GetMapping("/api/management/institutions/{institutionId}/contacts")
    public List<ContactResponse> list(@PathVariable Long institutionId) { return service.list(institutionId); }
    @GetMapping("/api/management/institutions/{institutionId}/contacts/{id}")
    public ContactResponse find(@PathVariable Long institutionId,@PathVariable Long id) { return service.find(institutionId,id); }
    @PostMapping("/api/institutions/{institutionId}/contacts")
    public ResponseEntity<ContactResponse> create(@PathVariable Long institutionId,@Valid @RequestBody ContactRequest request) {
        var result=service.create(institutionId,request);
        return ResponseEntity.created(URI.create("/api/management/institutions/"+institutionId+"/contacts/"+result.id())).body(result);
    }
    @PutMapping("/api/institutions/{institutionId}/contacts/{id}")
    public ContactResponse replace(@PathVariable Long institutionId,@PathVariable Long id,@Valid @RequestBody ContactRequest request) { return service.replace(institutionId,id,request); }
    @PatchMapping("/api/institutions/{institutionId}/contacts/{id}")
    public ContactResponse patch(@PathVariable Long institutionId,@PathVariable Long id,@RequestBody ContactPatch request) { return service.patch(institutionId,id,request); }
    @DeleteMapping("/api/institutions/{institutionId}/contacts/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long institutionId,@PathVariable Long id) { service.delete(institutionId,id);return ResponseEntity.noContent().build(); }
}
