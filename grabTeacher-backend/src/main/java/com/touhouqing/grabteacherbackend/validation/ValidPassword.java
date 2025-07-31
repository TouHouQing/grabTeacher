package com.touhouqing.grabteacherbackend.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "密码必须至少6位，且包含字母和数字";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
