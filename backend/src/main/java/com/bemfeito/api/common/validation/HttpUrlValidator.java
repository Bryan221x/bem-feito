package com.bemfeito.api.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.net.URI;

public class HttpUrlValidator implements ConstraintValidator<HttpUrl,String> {
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value==null) return true;
        try {
            URI uri=URI.create(value);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost()!=null && uri.getUserInfo()==null;
        } catch (IllegalArgumentException error) { return false; }
    }
}
