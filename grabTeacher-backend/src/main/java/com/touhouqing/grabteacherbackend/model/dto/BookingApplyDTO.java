package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 预约申请请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "预约申请请求参数")
public class BookingApplyDTO {

    @NotNull(message = "教师ID不能为空")
    @Schema(description = "教师ID", example = "1")
    private Long teacherId;

    @Schema(description = "课程ID，可为空(自定义预约)", example = "1")
    private Long courseId;

    @Schema(description = "学生选择的科目ID（试听课必填，正式课可为空）", example = "1")
    private Long subjectId;

    @NotBlank(message = "预约类型不能为空")
    @Schema(description = "预约类型", example = "single", allowableValues = {"single", "recurring"})
    private String bookingType;

    // 单次预约相关字段（包括试听课）
    @Schema(description = "请求的上课日期(单次预约/试听课)", example = "2024-01-15")
    private LocalDate requestedDate;

    @Schema(description = "请求的开始时间(单次预约/试听课)", example = "14:00")
    private LocalTime requestedStartTime;

    @Schema(description = "请求的结束时间(单次预约/试听课，试听课固定30分钟)", example = "16:00")
    private LocalTime requestedEndTime;

    // 周期性预约相关字段
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

    @Schema(description = "年级", example = "高一")
    private String grade;


    @Builder.Default
    @Schema(description = "是否为免费试听课（试听课次数有限，固定30分钟）", example = "false")
    private Boolean isTrial = false;

    @Schema(description = "试听课时长（分钟，固定为30）", example = "30")
    private Integer trialDurationMinutes;

    @Schema(description = "学生选择的课程时长（分钟），仅在课程时长为空时可选", example = "90", allowableValues = {"90", "120"})
    private Integer selectedDurationMinutes;

    @Schema(description = "是否为恢复停课的预约申请（不扣费）", example = "true")
    private Boolean resume;

    @Schema(description = "关联的报名ID（用于恢复课程时识别原报名）", example = "1001")
    private Long enrollmentId;

    // 授课地点选择
    @Schema(description = "线下授课地点ID（教师配置的地点之一）", example = "101")
    private Long teachingLocationId;

    @Schema(description = "授课地点名称（例如：线上 或 线下地点名称）。当选择线上授课时传递 '线上'", example = "线上")
    private String teachingLocation;
}
