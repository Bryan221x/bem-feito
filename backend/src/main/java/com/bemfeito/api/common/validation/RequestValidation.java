package com.bemfeito.api.common.validation;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class RequestValidation {
    private final Validator validator;
    public RequestValidation(Validator validator) { this.validator=validator; }
    public <T> T check(T request) {
        var errors=validator.validate(request);
        if (!errors.isEmpty()) throw new ConstraintViolationException(errors);
        return request;
    }
    public static void require(boolean valid, String message) {
        if (!valid) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,message);
    }
    public static ResponseStatusException notFound(String resource) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND,resource+" não encontrado");
    }
}
