package com.touhouqing.grabteacherbackend.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateDTO {
    
    @NotBlank(message = "出生年月不能为空")
    private String birthDate;
}
