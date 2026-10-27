package com.bemfeito.api.location.city.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.bemfeito.api.location.city.dto.CityResponse;
import com.bemfeito.api.location.city.service.CityService;

@RestController
@RequestMapping("/api/cities")
public class CityController {
    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @GetMapping
    public List<CityResponse> findAll(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) Long stateId) {
        return service.findAll(state, stateId);
    }
}
