package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程请求参数")
public class CourseDTO {

    @Schema(description = "授课教师ID（管理员创建课程时需要，教师创建时自动使用当前用户）", example = "1")
    private Long teacherId;

    @NotNull(message = "科目ID不能为空")
    @Schema(description = "课程科目ID", example = "1")
    private Long subjectId;

    @NotBlank(message = "课程标题不能为空")
    @Size(max = 200, message = "课程标题长度不能超过200个字符")
    @Schema(description = "课程标题", example = "高中数学 - 函数与导数专题")
    private String title;

    @Schema(description = "课程详细描述", example = "本课程深入讲解高中数学中的函数与导数知识点，适合高二、高三学生")
    private String description;

    @NotNull(message = "课程类型不能为空")
    @Schema(description = "课程类型", example = "one_on_one", allowableValues = {"one_on_one", "large_class"})
    private String courseType;

    @Schema(description = "单次课程时长（分钟）。仅大班课需要（90或120）；一对一可为空，学生在预约时选择", example = "120", allowableValues = {"90", "120"})
    private Integer durationMinutes;

    @Schema(description = "课程状态", example = "active", allowableValues = {"active", "inactive", "full"})
    private String status = "active";

    @Schema(description = "课程地点", example = "线上", allowableValues = {"线上", "线下"})
    private String courseLocation;

    @Schema(description = "是否支持线上（仅大班课），当选择线上时无需选择线下地点")
    private Boolean supportsOnline;

    @Schema(description = "线下地点ID（仅大班课，必须来自授课地点表，且当前仅允许选择一个）", example = "184")
    private Long offlineLocationId;

    @Schema(description = "课程价格（大班课专用，为空表示可定制价格）", example = "299.00")
    private BigDecimal price;

    @Schema(description = "开始日期（大班课专用）", example = "2024-01-15")
    private LocalDate startDate;

    @Schema(description = "结束日期（大班课专用）", example = "2024-03-15")
    private LocalDate endDate;

    @Schema(description = "人数限制（大班课专用，为空表示不限制）", example = "30")
    private Integer personLimit;


    @Schema(description = "课程每周的上课时间周期（仅大班课需要），JSON数组：[{weekday:1,timeSlots:[\"08:00-10:00\"]}]，weekday:1=周一...7=周日")
    private java.util.List<com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO> courseTimeSlots;

    @Schema(description = "课程封面图URL（保存课程时确定，预览阶段不上传）")
    private String imageUrl;
}
