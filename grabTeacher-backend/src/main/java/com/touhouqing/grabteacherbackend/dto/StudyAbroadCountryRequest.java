package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建/更新 留学国家 请求体")
public class StudyAbroadCountryRequest {
    @NotBlank(message = "国家名称不能为空")
    @Schema(description = "国家名称")
    private String countryName;

    @Schema(description = "排序权重")
    private Integer sortOrder;

    @Schema(description = "是否启用")
    private Boolean isActive;
}

