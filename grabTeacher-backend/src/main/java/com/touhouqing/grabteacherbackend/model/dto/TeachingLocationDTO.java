package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "授课地点请求参数")
public class TeachingLocationDTO {

    @NotBlank(message = "授课地点名称不能为空")
    @Size(max = 50, message = "授课地点名称长度不能超过50个字符")
    @Schema(description = "授课地点名称", example = "北京海淀校区", required = true)
    private String name;

    @NotBlank(message = "授课地点详细地址不能为空")
    @Size(max = 255, message = "详细地址长度不能超过255个字符")
    @Schema(description = "授课地点详细地址", example = "北京市海淀区中关村大街1号", required = true)
    private String address;

    @Schema(description = "是否激活", example = "true", defaultValue = "true")
    private Boolean isActive = true;

    @NotNull(message = "排序顺序不能为空")
    @Schema(description = "排序顺序，越小越靠前", example = "0")
    private Integer sortOrder = 0;
}

