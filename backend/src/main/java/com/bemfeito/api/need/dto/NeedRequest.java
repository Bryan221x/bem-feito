package com.bemfeito.api.need.dto;

import com.bemfeito.api.common.domain.NeedPriority;
import com.bemfeito.api.common.domain.NeedStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record NeedRequest(
    @NotNull @Positive Long categoryId,
    @NotBlank @Size(max=150) String title,
    @com.bemfeito.api.common.validation.Utf8Text String description,
    @DecimalMin("0") @Digits(integer=8,fraction=2) BigDecimal quantity,
    @Size(max=50) String unitLabel,
    @NotNull NeedPriority priority,
    @NotNull NeedStatus status
) {}
