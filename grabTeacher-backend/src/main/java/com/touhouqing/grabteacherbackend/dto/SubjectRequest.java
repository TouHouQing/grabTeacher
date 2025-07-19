package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "科目请求参数")
public class SubjectRequest {
    @NotBlank(message = "科目名称不能为空")
    @Size(max = 50, message = "科目名称长度不能超过50个字符")
    @Schema(description = "科目名称", example = "数学", required = true)
    private String name;

    @Size(max = 20, message = "年级范围长度不能超过20个字符")
    @Schema(description = "适用年级范围", example = "小学,初中,高中")
    private String gradeLevels;

    @Size(max = 255, message = "图标URL长度不能超过255个字符")
    @Schema(description = "科目图标URL", example = "https://example.com/math-icon.png")
    private String iconUrl;

    @Schema(description = "是否启用", example = "true", defaultValue = "true")
    private Boolean isActive = true;
} 