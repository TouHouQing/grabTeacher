package com.touhouqing.grabteacherbackend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("study_abroad_countries")
@Schema(description = "留学国家")
public class StudyAbroadCountry {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("country_name")
    @Schema(description = "国家名称")
    private String countryName;

    @TableField("sort_order")
    @Builder.Default
    @Schema(description = "排序权重，越小越靠前")
    private Integer sortOrder = 0;

    @TableField("is_active")
    @Builder.Default
    @Schema(description = "是否启用")
    private Boolean isActive = true;

    @TableField("created_at")
    @Schema(description = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @Builder.Default
    @Schema(description = "是否删除", hidden = true)
    private Boolean isDeleted = false;

    @TableField("deleted_at")
    @Schema(description = "删除时间", hidden = true)
    private LocalDateTime deletedAt;
}

