package com.bemfeito.api.servicearea.dto;

import com.bemfeito.api.servicearea.entity.ServiceArea;

public record ServiceAreaResponse(Long id, String name, String description) {
    public static ServiceAreaResponse from(ServiceArea entity) {
        return new ServiceAreaResponse(entity.getId(), entity.getName(), entity.getDescription());
    }
}
