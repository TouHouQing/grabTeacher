package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("subjects")
@Schema(description = "科目实体")
public class Subject {
    @TableId(type = IdType.AUTO)
    @Schema(description = "科目ID", example = "1")
    private Long id;

    @NotBlank(message = "科目名称不能为空")
    @Size(max = 50, message = "科目名称长度不能超过50个字符")
    @Schema(description = "科目名称", example = "数学")
    private String name;



    @TableField("icon_url")
    @Size(max = 255, message = "图标URL长度不能超过255个字符")
    @Schema(description = "科目图标URL", example = "https://example.com/math-icon.png")
    private String iconUrl;

    @TableField("is_active")
    @Builder.Default
    @Schema(description = "是否启用", example = "true")
    private Boolean isActive = true;

    @TableField("is_deleted")
    @Builder.Default
    @Schema(description = "是否删除", example = "false", hidden = true)
    private Boolean isDeleted = false;
    
    @TableField("deleted_at")
    @Schema(description = "删除时间", hidden = true)
    private LocalDateTime deletedAt;
} 