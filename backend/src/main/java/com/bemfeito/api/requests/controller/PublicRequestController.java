package com.bemfeito.api.requests.controller;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import com.bemfeito.api.requests.dto.RequestInput.*;
import com.bemfeito.api.requests.dto.RequestOutput.*;
import com.bemfeito.api.requests.service.*;
@RestController
@RequestMapping("/api")
public class PublicRequestController {
    private final RequestSubmissionService submissions;
    private final RequestQueryService queries;
    private final InvitationService invitations;
    public PublicRequestController(RequestSubmissionService submissions,RequestQueryService queries,InvitationService invitations) {
        this.submissions=submissions;this.queries=queries;this.invitations=invitations;
    }
    @PostMapping("/registration-requests") @ResponseStatus(HttpStatus.CREATED)
    public Submitted register(@Valid @RequestBody Registration input) { return submissions.register(input); }
    @PostMapping("/access-requests") @ResponseStatus(HttpStatus.CREATED)
    public Submitted access(@Valid @RequestBody Access input) { return submissions.access(input); }
    @GetMapping("/institutions/access-search") public List<SearchResult> search(@RequestParam(defaultValue="") String q) { return queries.search(q); }
    @PostMapping("/account-invitations/inspect") public Inspection inspect(@Valid @RequestBody Inspect input) { return invitations.inspect(input); }
    @PostMapping("/account-invitations/accept") public Map<String,String> accept(@Valid @RequestBody Accept input) { return invitations.accept(input); }
}
