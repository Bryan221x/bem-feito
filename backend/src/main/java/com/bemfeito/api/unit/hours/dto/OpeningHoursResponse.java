package com.bemfeito.api.unit.hours.dto;



public record OpeningHoursResponse(Long id, Byte dayOfWeek, Boolean closed, java.time.LocalTime opensAt, java.time.LocalTime closesAt, String note) {
    public static OpeningHoursResponse from(com.bemfeito.api.unit.hours.entity.UnitOpeningHours entity) { return new OpeningHoursResponse(entity.getId(),entity.getDayOfWeek(),entity.getClosed(),entity.getOpensAt(),entity.getClosesAt(),entity.getNote()); }
}
