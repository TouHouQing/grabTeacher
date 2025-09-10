package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("grades")
@Schema(description = "年级实体")
public class Grade {

    @TableId(type = IdType.AUTO)
    @Schema(description = "年级ID", example = "1")
    private Long id;

    @NotBlank(message = "年级名称不能为空")
    @Size(max = 50, message = "年级名称长度不能超过50个字符")
    @Schema(description = "年级名称", example = "高一")
    private String name;

    @TableField("is_active")
    @Builder.Default
    @Schema(description = "是否激活", example = "true")
    private Boolean isActive = true;

    @NotNull(message = "排序顺序不能为空")
    @TableField("sort_order")
    @Builder.Default
    @Schema(description = "排序顺序，越小越靠前", example = "0")
    private Integer sortOrder = 0;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    @Schema(description = "最后更新时间")
    private LocalDateTime updatedAt;
}

