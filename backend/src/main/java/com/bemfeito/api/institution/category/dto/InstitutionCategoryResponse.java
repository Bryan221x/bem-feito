package com.bemfeito.api.institution.category.dto;

import com.bemfeito.api.institution.category.entity.InstitutionCategory;

public record InstitutionCategoryResponse(Long id, String name, String description) {
    public static InstitutionCategoryResponse from(InstitutionCategory entity) {
        return new InstitutionCategoryResponse(entity.getId(), entity.getName(), entity.getDescription());
    }
}
