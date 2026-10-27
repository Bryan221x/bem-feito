package com.bemfeito.api.servicearea.service;

import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.bemfeito.api.servicearea.dto.ServiceAreaResponse;
import com.bemfeito.api.servicearea.repository.ServiceAreaRepository;

@Service
public class ServiceAreaService {
    private final ServiceAreaRepository repository;

    public ServiceAreaService(ServiceAreaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ServiceAreaResponse> findAll() {
        return repository.findByActiveTrueOrderByNameAsc().stream()
            .map(ServiceAreaResponse::from).toList();
    }
}
