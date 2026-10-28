package com.bemfeito.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Capacidade em bytes do tipo TEXT no MySQL, sem limitar indevidamente texto ASCII. */
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = Utf8TextValidator.class)
public @interface Utf8Text {
    String message() default "deve ocupar no máximo 65535 bytes em UTF-8";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
