package com.bemfeito.api.unit.hours.dto;



public record OpeningHoursRequest(@jakarta.validation.constraints.NotNull Boolean closed, java.time.LocalTime opensAt, java.time.LocalTime closesAt, @jakarta.validation.constraints.Size(max=150) String note) {  }
