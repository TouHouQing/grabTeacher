package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "管理员批量更新一对一课程的时薪与本月课时 DTO")
public class AdminCourseMetricsUpdateDTO {

    @Schema(description = "课程ID", required = true)
    private Long courseId;

    @Schema(description = "教师时薪（M豆/小时，仅一对一课程）")
    private BigDecimal teacherHourlyRate;

    @Schema(description = "本月课时数（小时，仅一对一课程）")
    private BigDecimal currentHours;
}

