package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "创建/更新 留学阶段 请求体")
public class StudyAbroadStageDTO {
    @NotBlank(message = "阶段名称不能为空")
    @Schema(description = "阶段名称，如高中/本科/硕士")
    private String stageName;

    @Schema(description = "排序权重")
    private Integer sortOrder;

    @Schema(description = "是否启用")
    private Boolean active;
}

