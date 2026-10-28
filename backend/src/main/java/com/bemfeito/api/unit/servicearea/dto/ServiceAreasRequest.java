package com.bemfeito.api.unit.servicearea.dto;



public record ServiceAreasRequest(@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Size(max=100) java.util.Set<@jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Positive Long> serviceAreaIds) {  }
