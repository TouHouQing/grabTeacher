package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 预约申请响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "预约申请响应数据")
public class BookingVO {
    
    @Schema(description = "预约申请ID", example = "1")
    private Long id;

    @Schema(description = "学生ID", example = "1")
    private Long studentId;

    @Schema(description = "学生姓名", example = "张三")
    private String studentName;

    @Schema(description = "教师ID", example = "1")
    private Long teacherId;

    @Schema(description = "教师姓名", example = "李老师")
    private String teacherName;

    @Schema(description = "课程ID", example = "1")
    private Long courseId;

    @Schema(description = "课程标题", example = "高中数学 - 函数与导数专题")
    private String courseTitle;

    @Schema(description = "科目名称", example = "数学")
    private String subjectName;

    @Schema(description = "课程时长（分钟）", example = "120")
    private Integer courseDurationMinutes;

    @Schema(description = "预约类型", example = "single", allowableValues = {"single", "recurring"})
    private String bookingType;

    @Schema(description = "请求的上课日期(单次预约)", example = "2024-01-15")
    private LocalDate requestedDate;

    @Schema(description = "请求的开始时间(单次预约)", example = "14:00")
    private LocalTime requestedStartTime;

    @Schema(description = "请求的结束时间(单次预约)", example = "16:00")
    private LocalTime requestedEndTime;

    @Schema(description = "周期性预约的星期几列表", example = "[1, 3, 5]")
    private List<Integer> recurringWeekdays;

    @Schema(description = "周期性预约的时间段列表", example = "[\"14:00-16:00\", \"18:00-20:00\"]")
    private List<String> recurringTimeSlots;

    @Schema(description = "周期性预约开始日期", example = "2024-01-15")
    private LocalDate startDate;

    @Schema(description = "周期性预约结束日期", example = "2024-06-15")
    private LocalDate endDate;

    @Schema(description = "总课程次数", example = "12")
    private Integer totalTimes;

    @Schema(description = "学生需求说明", example = "希望重点提高数学成绩")
    private String studentRequirements;

    @Schema(description = "申请状态", example = "pending", allowableValues = {"pending", "approved", "rejected", "cancelled"})
    private String status;

    @Schema(description = "教师回复内容", example = "同意预约，期待与您的合作")
    private String teacherReply;

    @Schema(description = "管理员备注", example = "特殊情况处理")
    private String adminNotes;

    @Schema(description = "申请时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "批准时间")
    private LocalDateTime approvedAt;

    @Schema(description = "是否为免费试听课", example = "false")
    private Boolean trial;

    @Schema(description = "试听课时长（分钟）", example = "30")
    private Integer trialDurationMinutes;
}
