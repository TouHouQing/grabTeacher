package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 年级请求DTO
 */
@Data
@Schema(description = "年级请求")
public class GradeDTO {

    @NotBlank(message = "年级名称不能为空")
    @Size(max = 50, message = "年级名称长度不能超过50个字符")
    @Schema(description = "年级名称", example = "小学一年级", required = true)
    private String gradeName;

    @Schema(description = "年级描述", example = "适合6-7岁儿童")
    private String description;
}
