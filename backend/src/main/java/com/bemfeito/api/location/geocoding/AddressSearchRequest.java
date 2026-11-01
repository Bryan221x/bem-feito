package com.bemfeito.api.location.geocoding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressSearchRequest(
    @NotBlank @Size(max = 160) String street,
    @Size(max = 32) String number,
    @Size(max = 100) String neighborhood,
    @Pattern(regexp = "|[0-9]{8}|[0-9]{5}-[0-9]{3}") String postalCode,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String state) { }
