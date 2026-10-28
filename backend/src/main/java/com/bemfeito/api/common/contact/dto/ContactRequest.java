package com.bemfeito.api.common.contact.dto;

import com.bemfeito.api.common.domain.ContactType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ContactRequest(
    @NotNull ContactType type,
    @NotBlank @Size(max=255) String value,
    @Size(max=100) String label,
    @NotNull Boolean isPrimary,
    @NotNull Boolean isPublic,
    @NotNull @PositiveOrZero Integer displayOrder
) {}
