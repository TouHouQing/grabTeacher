package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "创建/更新 留学项目 请求体")
public class StudyAbroadProgramRequestDTO {
    @NotBlank(message = "项目标题不能为空")
    @Schema(description = "项目标题")
    private String title;

    @NotNull(message = "国家ID不能为空")
    @Schema(description = "国家ID")
    private Long countryId;

    @NotNull(message = "阶段ID不能为空")
    @Schema(description = "阶段ID")
    private Long stageId;

    @Schema(description = "项目描述")
    private String description;

    @Schema(description = "图片URL")
    private String imageUrl;

    @Schema(description = "标签，JSON字符串")
    private String tags;

    @Schema(description = "是否热门")
    private Boolean isHot;


    @Schema(description = "排序权重")
    private Integer sortOrder;

    @Schema(description = "是否启用")
    private Boolean isActive;
}

