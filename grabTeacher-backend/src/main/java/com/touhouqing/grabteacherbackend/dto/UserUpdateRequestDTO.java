package com.touhouqing.grabteacherbackend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequestDTO {
    
    @NotBlank(message = "出生年月不能为空")
    private String birthDate;
}
