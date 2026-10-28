package com.bemfeito.api.admin;
import org.springframework.web.bind.annotation.*;
import com.bemfeito.api.common.dto.PageResponse;
import com.bemfeito.api.common.domain.InstitutionStatus;
@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminQueryService service;
    public AdminController(AdminQueryService service) { this.service=service; }
    @GetMapping("/summary") public AdminOutput.Summary summary() { return service.summary(); }
    @GetMapping("/institutions") public PageResponse<AdminOutput.InstitutionRow> institutions(@RequestParam(defaultValue="") String q,@RequestParam(required=false) InstitutionStatus status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.institutions(q,status,page,size); }
}
