package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeacherLevelVO {
    private Long id;

    @Schema(description = "教师级别名称")
    private String name;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @Schema(description = "使用该级别的教师数量")
    private Long usageCount;
}


