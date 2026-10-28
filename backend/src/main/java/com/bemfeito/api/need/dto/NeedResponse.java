package com.bemfeito.api.need.dto;



public record NeedResponse(Long id, Long categoryId, String category, String name, String description, java.math.BigDecimal quantity, String unitLabel, com.bemfeito.api.common.domain.NeedPriority priority, com.bemfeito.api.common.domain.NeedStatus status) {
    public static NeedResponse from(com.bemfeito.api.need.entity.Need entity) { return new NeedResponse(entity.getId(),entity.getCategory().getId(),entity.getCategory().getName(),entity.getTitle(),entity.getDescription(),entity.getQuantity(),entity.getUnitLabel(),entity.getPriority(),entity.getStatus()); }
}
