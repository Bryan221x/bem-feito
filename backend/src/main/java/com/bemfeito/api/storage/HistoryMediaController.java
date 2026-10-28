package com.bemfeito.api.storage;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import com.bemfeito.api.institution.history.service.InstitutionHistoryImageService;
import com.bemfeito.api.institution.history.dto.HistoryImageResponse;
@RestController
public class HistoryMediaController {
    private final InstitutionHistoryImageService service;
    public HistoryMediaController(InstitutionHistoryImageService service) { this.service=service; }
    @PostMapping(value="/api/institutions/{institutionId}/history-images/upload",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public HistoryImageResponse upload(@PathVariable Long institutionId,@RequestParam MultipartFile file,@RequestParam String altText,@RequestParam(defaultValue="") String caption,@RequestParam(defaultValue="0") int displayOrder) { return service.upload(institutionId,file,altText,caption,displayOrder); }
    @GetMapping("/api/history-media/{institutionId}/{key}")
    public ResponseEntity<Resource> read(@PathVariable Long institutionId,@PathVariable String key) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore()).header("X-Content-Type-Options","nosniff").body(service.media(institutionId,key));
    }
}
