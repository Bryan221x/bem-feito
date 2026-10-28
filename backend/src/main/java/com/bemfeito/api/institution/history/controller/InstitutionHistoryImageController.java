package com.bemfeito.api.institution.history.controller;

import com.bemfeito.api.institution.history.dto.HistoryImagePatch;
import com.bemfeito.api.institution.history.dto.HistoryImageRequest;
import com.bemfeito.api.institution.history.dto.HistoryImageResponse;
import com.bemfeito.api.institution.history.service.InstitutionHistoryImageService;
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
public class InstitutionHistoryImageController {
    private final InstitutionHistoryImageService service;
    public InstitutionHistoryImageController(InstitutionHistoryImageService service) { this.service=service; }
    @GetMapping("/api/management/institutions/{institutionId}/history-images")
    public List<HistoryImageResponse> list(@PathVariable Long institutionId) { return service.list(institutionId); }
    @GetMapping("/api/management/institutions/{institutionId}/history-images/{id}")
    public HistoryImageResponse find(@PathVariable Long institutionId,@PathVariable Long id) { return service.find(institutionId,id); }
    @PostMapping("/api/institutions/{institutionId}/history-images")
    public ResponseEntity<HistoryImageResponse> create(@PathVariable Long institutionId,@Valid @RequestBody HistoryImageRequest request) {
        var result=service.create(institutionId,request);
        return ResponseEntity.created(URI.create("/api/management/institutions/"+institutionId+"/history-images/"+result.id())).body(result);
    }
    @PutMapping("/api/institutions/{institutionId}/history-images/{id}")
    public HistoryImageResponse replace(@PathVariable Long institutionId,@PathVariable Long id,@Valid @RequestBody HistoryImageRequest request) { return service.replace(institutionId,id,request); }
    @PatchMapping("/api/institutions/{institutionId}/history-images/{id}")
    public HistoryImageResponse patch(@PathVariable Long institutionId,@PathVariable Long id,@RequestBody HistoryImagePatch request) { return service.patch(institutionId,id,request); }
    @DeleteMapping("/api/institutions/{institutionId}/history-images/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long institutionId,@PathVariable Long id) { service.delete(institutionId,id);return ResponseEntity.noContent().build(); }
}
