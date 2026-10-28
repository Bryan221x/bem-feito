package com.bemfeito.api.unit.dto;

import com.bemfeito.api.common.domain.UnitStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import org.hibernate.validator.constraints.br.CNPJ;

public record UnitRequest(
    @NotNull @Positive Long cityId,
    @NotBlank @Size(max=150) String name,
    @Pattern(regexp="[0-9]{14}") @CNPJ String document,
    @com.bemfeito.api.common.validation.Utf8Text String description,
    @NotBlank @Pattern(regexp="[0-9]{8}") String postalCode,
    @NotBlank @Size(max=180) String street,
    @NotBlank @Size(max=30) String number,
    @Size(max=120) String complement,
    @NotBlank @Size(max=120) String neighborhood,
    @DecimalMin("-90") @DecimalMax("90") @Digits(integer=2,fraction=8) BigDecimal latitude,
    @DecimalMin("-180") @DecimalMax("180") @Digits(integer=3,fraction=8) BigDecimal longitude,
    @NotNull UnitStatus status
) {}
