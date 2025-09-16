package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * 调课申请响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "调课申请响应数据")
public class RescheduleVO {

    @Schema(description = "调课申请ID", example = "1")
    private Long id;

    @Schema(description = "原课程安排ID", example = "1")
    private Long scheduleId;

    @Schema(description = "申请人ID", example = "1")
    private Long applicantId;

    @Schema(description = "申请人姓名", example = "张三")
    private String applicantName;

    @Schema(description = "申请人类型", example = "student", allowableValues = {"student", "teacher"})
    private String applicantType;

    @Schema(description = "申请类型", example = "single", allowableValues = {"single", "recurring", "cancel"})
    private String requestType;

    // 原课程信息
    @Schema(description = "原定日期", example = "2024-01-15")
    private LocalDate originalDate;

    @Schema(description = "原定开始时间", example = "14:00")
    private LocalTime originalStartTime;

    @Schema(description = "原定结束时间", example = "16:00")
    private LocalTime originalEndTime;

    // 新课程信息
    @Schema(description = "新日期", example = "2024-01-16")
    private LocalDate newDate;

    @Schema(description = "新开始时间", example = "15:00")
    private LocalTime newStartTime;

    @Schema(description = "新结束时间", example = "17:00")
    private LocalTime newEndTime;

    @Schema(description = "新的周期性安排", example = "周一,周三 14:00-16:00;周五 18:00-20:00")
    private String newWeeklySchedule;

    @Schema(description = "新的周期性安排的星期几列表", example = "[1, 3, 5]")
    private List<Integer> newRecurringWeekdays;

    @Schema(description = "新的周期性安排的时间段列表", example = "[\"14:00-16:00\", \"18:00-20:00\"]")
    private List<String> newRecurringTimeSlots;

    @Schema(description = "调课原因", example = "临时有事无法参加")
    private String reason;

    @Schema(description = "紧急程度", example = "medium", allowableValues = {"low", "medium", "high"})
    private String urgencyLevel;

    @Schema(description = "提前通知小时数", example = "24")
    private Integer advanceNoticeHours;

    @Schema(description = "申请状态", example = "pending", allowableValues = {"pending", "approved", "rejected", "cancelled"})
    private String status;

    @Schema(description = "状态显示文本", example = "待审批")
    private String statusDisplay;

    @Schema(description = "审核人ID", example = "1")
    private Long reviewerId;

    @Schema(description = "审核人姓名", example = "李老师")
    private String reviewerName;

    @Schema(description = "审核人类型", example = "teacher", allowableValues = {"teacher", "student", "admin"})
    private String reviewerType;

    @Schema(description = "审核备注", example = "同意调课申请")
    private String reviewNotes;

    @Schema(description = "补偿金额", example = "0.00")
    private BigDecimal compensationAmount;

    @Schema(description = "是否影响后续课程", example = "false")
    private Boolean affectsFutureSessions;

    @Schema(description = "审核时间")
    private LocalDateTime reviewedAt;



    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    // 关联课程信息
    @Schema(description = "课程标题", example = "高中数学一对一")
    private String courseTitle;

    @Schema(description = "科目名称", example = "数学")
    private String subjectName;

    @Schema(description = "教师ID", example = "1001")
    private Long teacherId;

    @Schema(description = "教师姓名", example = "王老师")
    private String teacherName;

    @Schema(description = "学生姓名", example = "张三")
    private String studentName;
}
