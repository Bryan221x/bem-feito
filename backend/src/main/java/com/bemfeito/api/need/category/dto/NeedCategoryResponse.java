package com.bemfeito.api.need.category.dto;

import com.bemfeito.api.need.category.entity.NeedCategory;

public record NeedCategoryResponse(Long id, String name, String description) {
    public static NeedCategoryResponse from(NeedCategory entity) {
        return new NeedCategoryResponse(entity.getId(), entity.getName(), entity.getDescription());
    }
}
