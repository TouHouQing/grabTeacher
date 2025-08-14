package com.touhouqing.grabteacherbackend.entity.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserUpdateRequest {
    
    @NotBlank(message = "出生年月不能为空")
    private String birthDate;
}
