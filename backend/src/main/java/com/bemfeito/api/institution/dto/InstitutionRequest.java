package com.bemfeito.api.institution.dto;

import com.bemfeito.api.common.domain.InstitutionStatus;
import com.bemfeito.api.common.validation.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.br.CNPJ;

public record InstitutionRequest(
    @NotNull @Positive Long categoryId,
    @NotBlank @Size(max=150) String displayName,
    @NotBlank @Size(max=200) String legalName,
    @NotBlank @Pattern(regexp="[0-9]{14}") @CNPJ String cnpj,
    @com.bemfeito.api.common.validation.Utf8Text String objective,
    @com.bemfeito.api.common.validation.Utf8Text String description,
    @com.bemfeito.api.common.validation.Utf8Text String history,
    @Size(max=255) @HttpUrl String website,
    @NotNull InstitutionStatus status
) {}
