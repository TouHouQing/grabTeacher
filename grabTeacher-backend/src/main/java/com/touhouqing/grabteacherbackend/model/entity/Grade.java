package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 年级实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("grades")
@Schema(description = "年级")
public class Grade {

    @TableId(value = "id", type = IdType.AUTO)
    @Schema(description = "年级ID", hidden = true)
    private Long id;

    @TableField("grade_name")
    @Schema(description = "年级名称", example = "小学一年级")
    private String gradeName;

    @TableField("description")
    @Schema(description = "年级描述", example = "适合6-7岁儿童")
    private String description;

    @TableField("created_at")
    @Schema(description = "创建时间", hidden = true)
    private LocalDateTime createdAt;

    @TableField("updated_at")
    @Schema(description = "更新时间", hidden = true)
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @Schema(description = "是否删除", hidden = true)
    private Boolean deleted;

    @TableField("deleted_at")
    @Schema(description = "删除时间", hidden = true)
    private LocalDateTime deletedAt;
}
