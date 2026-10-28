package com.bemfeito.api.institution.history.dto;



public record HistoryImageResponse(Long id, String url, String alt, String caption, Integer order, java.time.LocalDateTime createdAt) {
    public static HistoryImageResponse from(com.bemfeito.api.institution.history.entity.InstitutionHistoryImage entity) { return new HistoryImageResponse(entity.getId(),entity.getImageUrl(),entity.getAltText(),entity.getCaption(),entity.getDisplayOrder(),entity.getCreatedAt()); }
}
