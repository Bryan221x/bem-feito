package com.bemfeito.api.institution.history.dto;

import com.bemfeito.api.common.validation.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record HistoryImageRequest(
    @NotBlank @Size(max=500) @HttpUrl String imageUrl,
    @NotBlank @Size(max=255) String altText,
    @Size(max=255) String caption,
    @NotNull @PositiveOrZero Integer displayOrder
) {}
