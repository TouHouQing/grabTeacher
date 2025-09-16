package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("courses")
@Schema(description = "课程实体")
public class Course {
    @TableId(type = IdType.AUTO)
    @Schema(description = "课程ID", example = "1")
    private Long id;

    @NotNull(message = "教师ID不能为空")
    @TableField("teacher_id")
    @Schema(description = "授课教师ID", example = "1")
    private Long teacherId;

    @NotNull(message = "科目ID不能为空")
    @TableField("subject_id")
    @Schema(description = "课程科目ID", example = "1")
    private Long subjectId;

    @NotBlank(message = "课程标题不能为空")
    @Size(max = 200, message = "课程标题长度不能超过200个字符")
    @Schema(description = "课程标题", example = "高中数学 - 函数与导数专题")
    private String title;

    @Schema(description = "课程详细描述", example = "本课程深入讲解高中数学中的函数与导数知识点，适合高二、高三学生")
    private String description;


    @Schema(description = "课程评分等级", example = "0")
    @TableField("rating")
    private BigDecimal rating;

    @NotNull(message = "课程类型不能为空")
    @TableField("course_type")
    @Schema(description = "课程类型", example = "one_on_one", allowableValues = {"one_on_one", "large_class"})
    private String courseType;

    @TableField(value = "duration_minutes", fill = FieldFill.DEFAULT)
    @Schema(description = "单次课程时长（分钟）。仅大班课需要，90或120；一对一为空，由学生预约时选择", example = "120")
    private Integer durationMinutes;

    @Builder.Default
    @Schema(description = "课程状态", example = "pending", allowableValues = {"active", "inactive", "full", "pending"})
    private String status = "pending";

    @TableField("is_featured")
    @Builder.Default
    @Schema(description = "是否为精选课程，在最新课程页面展示", example = "false")
    private Boolean featured = false;

    @TableField("price")
    @Schema(description = "课程单价，单位：M豆，1M豆=1元，课程价格，1对1代表每小时价格，大班课代表总课程价格", example = "299.00")
    private BigDecimal price;

    @TableField("teacher_hourly_rate")
    @Schema(description = "教师时薪（仅一对一使用），单位：M豆/小时。用于计算教师收入，与学生价格（price）可不一致", example = "60.00")
    private BigDecimal teacherHourlyRate;

    @TableField("start_date")
    @Schema(description = "开始日期（大班课专用）", example = "2026-01-15")
    private LocalDate startDate;

    @TableField("end_date")
    @Schema(description = "结束日期（大班课专用）", example = "2026-03-15")
    private LocalDate endDate;

    @TableField("person_limit")
    @Schema(description = "人数限制（大班课专用，为空表示不限制）", example = "30")
    private Integer personLimit;



    @TableField("enrollment_count")
    @Schema(description = "当前报名人数", example = "0")
    @Builder.Default
    private Integer enrollmentCount = 0;

    @TableField("course_time_slots")
    @Schema(description = "上课时间安排（仅大班课需要），JSON字符串，示例：[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"17:00-19:00\"]},{\"weekday\":6,\"timeSlots\":[\"13:00-15:00\",\"15:00-17:00\"]}]，weekday: 1=周一...7=周日")
    private String courseTimeSlots;

    @TableField("image_url")
    @Schema(description = "课程封面图URL")
    private String imageUrl;

    @TableField("course_location")
    @Schema(description = "课程地点", example = "线上", allowableValues = {"线上", "线下"})
    private String courseLocation;


    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "课程创建时间")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @Builder.Default
    @Schema(description = "是否删除", example = "false", hidden = true)
    private Boolean deleted = false;

    @TableField("deleted_at")
    @Schema(description = "删除时间", hidden = true)
    private LocalDateTime deletedAt;

    // 保留必要的构造函数
    public Course(Long teacherId, Long subjectId, String title, String courseType, Integer durationMinutes) {
        this.teacherId = teacherId;
        this.subjectId = subjectId;
        this.title = title;
        this.courseType = courseType;
        this.durationMinutes = durationMinutes;
        this.status = "pending";
        this.featured = false;
        this.deleted = false;
    }
}
