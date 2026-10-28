package com.bemfeito.api.common.contact.dto;



public record ContactResponse(Long id, com.bemfeito.api.common.domain.ContactType type, String value, String label, Boolean isPrimary, Boolean isPublic, Integer displayOrder) {
    public static ContactResponse from(com.bemfeito.api.institution.contact.entity.InstitutionContact entity) { return new ContactResponse(entity.getId(),entity.getType(),entity.getValue(),entity.getLabel(),entity.getIsPrimary(),entity.getIsPublic(),entity.getDisplayOrder()); }

    public static ContactResponse from(com.bemfeito.api.unit.contact.entity.UnitContact entity) { return new ContactResponse(entity.getId(),entity.getType(),entity.getValue(),entity.getLabel(),entity.getIsPrimary(),entity.getIsPublic(),entity.getDisplayOrder()); }
}
