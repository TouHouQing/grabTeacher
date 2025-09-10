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
@Schema(description = "\u5e74\u7ea7\u8bf7\u6c42\u53c2\u6570")
public class GradeDTO {

    @NotBlank(message = "\u5e74\u7ea7\u540d\u79f0\u4e0d\u80fd\u4e3a\u7a7a")
    @Size(max = 50, message = "\u5e74\u7ea7\u540d\u79f0\u957f\u5ea6\u4e0d\u80fd\u8d85\u8fc750\u4e2a\u5b57\u7b26")
    @Schema(description = "\u5e74\u7ea7\u540d\u79f0", example = "\u9ad8\u4e00", required = true)
    private String name;

    @Schema(description = "\u662f\u5426\u6fc0\u6d3b", example = "true", defaultValue = "true")
    private Boolean isActive = true;

    @NotNull(message = "\u6392\u5e8f\u987a\u5e8f\u4e0d\u80fd\u4e3a\u7a7a")
    @Schema(description = "\u6392\u5e8f\u987a\u5e8f\uff0c\u8d8a\u5c0f\u8d8a\u9760\u524d", example = "0")
    private Integer sortOrder = 0;
}

