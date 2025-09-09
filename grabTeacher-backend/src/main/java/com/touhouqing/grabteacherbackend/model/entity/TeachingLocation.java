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
@TableName("teaching_locations")
@Schema(description = "授课地点实体")
public class TeachingLocation {

    @TableId(type = IdType.AUTO)
    @Schema(description = "授课地点ID", example = "1")
    private Long id;

    @NotBlank(message = "授课地点名称不能为空")
    @Size(max = 50, message = "授课地点名称长度不能超过50个字符")
    @Schema(description = "授课地点名称", example = "北京海淀校区")
    private String name;

    @NotBlank(message = "授课地点详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过255个字符")
    @TableField("address")
    @Schema(description = "授课地点详细地址", example = "北京市海淀区中关村大街1号")
    private String address;

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

