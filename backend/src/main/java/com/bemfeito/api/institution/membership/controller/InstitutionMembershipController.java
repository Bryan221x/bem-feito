package com.bemfeito.api.institution.membership.controller;
import java.util.List;
import org.springframework.web.bind.annotation.*;
import com.bemfeito.api.institution.membership.service.InstitutionMembershipService;
import com.bemfeito.api.institution.membership.dto.*;
@RestController
public class InstitutionMembershipController {
    private final InstitutionMembershipService service;
    public InstitutionMembershipController(InstitutionMembershipService service) { this.service=service; }
    @GetMapping("/api/me/institutions") public List<MyInstitutionResponse> mine() { return service.mine(); }
    @GetMapping("/api/institutions/{institutionId}/members") public List<MembershipResponse> members(@PathVariable Long institutionId) { return service.members(institutionId); }
    @PatchMapping("/api/institutions/{institutionId}/members/{id}") public MembershipResponse patch(@PathVariable Long institutionId,@PathVariable Long id,@RequestBody MembershipPatch patch) { return service.patch(institutionId,id,patch); }
}
