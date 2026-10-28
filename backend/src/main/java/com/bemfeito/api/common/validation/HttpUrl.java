package com.bemfeito.api.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD,ElementType.PARAMETER,ElementType.RECORD_COMPONENT,ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy=HttpUrlValidator.class)
public @interface HttpUrl {
    String message() default "deve ser uma URL absoluta HTTP(S), sem credenciais";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
