package com.bemfeito.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.nio.charset.StandardCharsets;

public class Utf8TextValidator implements ConstraintValidator<Utf8Text, String> {
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        return value == null || value.getBytes(StandardCharsets.UTF_8).length <= 65535;
    }
}
