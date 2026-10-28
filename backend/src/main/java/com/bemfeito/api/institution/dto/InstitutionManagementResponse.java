package com.bemfeito.api.institution.dto;



public record InstitutionManagementResponse(Long id, String displayName, Long categoryId, String category, String objective, String description, String history, String website, java.util.List<com.bemfeito.api.common.contact.dto.ContactResponse> contacts, java.util.List<com.bemfeito.api.institution.history.dto.HistoryImageResponse> historyImages, java.util.List<com.bemfeito.api.unit.dto.UnitManagementResponse> units, String legalName, String cnpj, com.bemfeito.api.common.domain.InstitutionStatus status, java.time.LocalDateTime createdAt, java.time.LocalDateTime updatedAt) {  }
