package com.bemfeito.api.requests.controller;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import com.bemfeito.api.requests.dto.RequestInput.Decision;
import com.bemfeito.api.requests.dto.RequestOutput.*;
import com.bemfeito.api.requests.entity.RequestStatus;
import com.bemfeito.api.requests.service.*;
@RestController
@RequestMapping("/api/admin")
public class AdminRequestController {
    private final RequestQueryService queries;
    private final RequestReviewService reviews;
    private final InvitationService invitations;
    public AdminRequestController(RequestQueryService queries,RequestReviewService reviews,InvitationService invitations) {
        this.queries=queries;this.reviews=reviews;this.invitations=invitations;
    }
    @GetMapping("/registration-requests") public Page<Summary> registrations(@RequestParam(required=false) RequestStatus status,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.registrations(status,page,size); }
    @GetMapping("/access-requests") public Page<Summary> accesses(@RequestParam(required=false) RequestStatus status,
            @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return queries.accesses(status,page,size); }
    @GetMapping("/registration-requests/{id}") public Detail registration(@PathVariable Long id) { return queries.registration(id); }
    @GetMapping("/access-requests/{id}") public Detail access(@PathVariable Long id) { return queries.access(id); }
    @PostMapping("/registration-requests/{id}/{action}") public Reviewed reviewRegistration(@PathVariable Long id,@PathVariable String action,
            @Valid @RequestBody Decision input) { return reviews.registration(id,action,input); }
    @PostMapping("/access-requests/{id}/{action}") public Reviewed reviewAccess(@PathVariable Long id,@PathVariable String action,
            @Valid @RequestBody Decision input) { return reviews.access(id,action,input); }
    @PostMapping("/users/{id}/invitation") public Invitation reissue(@PathVariable Long id) { return invitations.reissue(id); }
    @GetMapping("/requests/summary") public Dashboard dashboard() { return queries.dashboard(); }
}
