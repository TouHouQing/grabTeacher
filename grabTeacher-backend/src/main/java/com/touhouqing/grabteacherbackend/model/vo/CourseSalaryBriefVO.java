package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "教师一对一课程薪资简要信息")
public class CourseSalaryBriefVO {

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "课程标题")
    private String title;

    @Schema(description = "教师时薪(M豆/小时)")
    private BigDecimal teacherHourlyRate;

    @Schema(description = "本月课时(小时)")
    private BigDecimal currentHours;

    @Schema(description = "上个月课时(小时)")
    private BigDecimal lastHours;

    @Schema(description = "本月薪资(估算)")
    private BigDecimal currentAmount;

    @Schema(description = "上月薪资(估算)")
    private BigDecimal lastAmount;
}

