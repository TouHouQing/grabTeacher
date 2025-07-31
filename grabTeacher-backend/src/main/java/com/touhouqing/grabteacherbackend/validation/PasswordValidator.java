package com.touhouqing.grabteacherbackend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public void initialize(ValidPassword constraintAnnotation) {
        // 初始化方法，可以为空
    }

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.length() < 6) {
            return false;
        }

        // 检查是否包含字母
        boolean hasLetter = password.matches(".*[a-zA-Z].*");
        
        // 检查是否包含数字
        boolean hasNumber = password.matches(".*[0-9].*");

        return hasLetter && hasNumber;
    }
}
