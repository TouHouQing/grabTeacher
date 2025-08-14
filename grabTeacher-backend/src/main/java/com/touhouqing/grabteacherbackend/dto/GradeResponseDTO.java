package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 年级响应DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "年级响应")
public class GradeResponseDTO {

    @Schema(description = "年级ID", example = "1")
    private Long id;

    @Schema(description = "年级名称", example = "小学一年级")
    private String gradeName;

    @Schema(description = "年级描述", example = "适合6-7岁儿童")
    private String description;

    @Schema(description = "使用该年级的课程数量", example = "5")
    private Long courseCount;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
