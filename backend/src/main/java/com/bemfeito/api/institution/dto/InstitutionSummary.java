package com.bemfeito.api.institution.dto;



public record InstitutionSummary(Long id, String displayName, Long categoryId, String category, String description, String location,
        com.bemfeito.api.institution.history.dto.HistoryImageResponse image,
        com.bemfeito.api.common.contact.dto.ContactResponse publicContact) {
    public InstitutionSummary(Long id, String displayName, Long categoryId, String category, String description, String location) {
        this(id,displayName,categoryId,category,description,location,null,null);
    }
}
