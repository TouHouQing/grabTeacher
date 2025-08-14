package com.touhouqing.grabteacherbackend.model.entity;

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
@TableName("study_abroad_programs")
@Schema(description = "留学项目")
public class StudyAbroadProgram {

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键ID")
    private Long id;

    @TableField("title")
    @Schema(description = "项目标题")
    private String title;

    @TableField("country_id")
    @Schema(description = "国家ID")
    private Long countryId;

    @TableField("stage_id")
    @Schema(description = "阶段ID")
    private Long stageId;

    @TableField("description")
    @Schema(description = "项目描述")
    private String description;

    @TableField("image_url")
    @Schema(description = "图片URL")
    private String imageUrl;

    @TableField("tags")
    @Schema(description = "标签，JSON字符串")
    private String tags;

    @TableField("is_hot")
    @Builder.Default
    @Schema(description = "是否热门")
    private Boolean isHot = false;


    @TableField("sort_order")
    @Builder.Default
    @Schema(description = "排序权重")
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

