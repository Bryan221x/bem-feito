package com.bemfeito.api.servicearea.controller;

import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bemfeito.api.servicearea.dto.ServiceAreaResponse;
import com.bemfeito.api.servicearea.service.ServiceAreaService;

@RestController
@RequestMapping("/api/service-areas")
public class ServiceAreaController {
    private final ServiceAreaService service;

    public ServiceAreaController(ServiceAreaService service) {
        this.service = service;
    }

    @GetMapping
    public List<ServiceAreaResponse> findAll() {
        return service.findAll();
    }
}
