package com.bemfeito.api.unit.dto;



public record UnitManagementResponse(Long id, Long institutionId, String name, String description, AddressResponse address, java.math.BigDecimal latitude, java.math.BigDecimal longitude, java.util.List<com.bemfeito.api.common.contact.dto.ContactResponse> contacts, java.util.List<com.bemfeito.api.unit.hours.dto.OpeningHoursResponse> openingHours, java.util.List<com.bemfeito.api.servicearea.dto.ServiceAreaResponse> serviceAreas, java.util.List<com.bemfeito.api.need.dto.NeedResponse> needs, String document, com.bemfeito.api.common.domain.UnitStatus status, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {  }
