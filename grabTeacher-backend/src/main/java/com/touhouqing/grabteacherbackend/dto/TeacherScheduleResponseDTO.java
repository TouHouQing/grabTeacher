package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "教师课表响应")
public class TeacherScheduleResponseDTO {

    @Schema(description = "教师ID", example = "1")
    private Long teacherId;

    @Schema(description = "教师姓名", example = "张老师")
    private String teacherName;

    @Schema(description = "查询开始日期", example = "2024-01-01")
    private LocalDate startDate;

    @Schema(description = "查询结束日期", example = "2024-01-31")
    private LocalDate endDate;

    @Schema(description = "日程安排列表")
    private List<DaySchedule> daySchedules;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "单日课程安排")
    public static class DaySchedule {

        @Schema(description = "日期", example = "2024-01-15")
        private LocalDate date;

        @Schema(description = "星期几", example = "1", allowableValues = {"0", "1", "2", "3", "4", "5", "6"})
        private Integer dayOfWeek; // 0=周日, 1=周一, ..., 6=周六

        @Schema(description = "该日的时间段列表")
        private List<TimeSlotInfo> timeSlots;

        @Schema(description = "可用时间段数量", example = "5")
        private Integer availableCount;

        @Schema(description = "已预约时间段数量", example = "3")
        private Integer bookedCount;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "时间段信息")
    public static class TimeSlotInfo {

        @Schema(description = "时间段", example = "14:00-16:00")
        private String timeSlot;

        @Schema(description = "开始时间", example = "14:00")
        private LocalTime startTime;

        @Schema(description = "结束时间", example = "16:00")
        private LocalTime endTime;

        @Schema(description = "是否可用", example = "true")
        private Boolean available;

        @Schema(description = "是否已预约", example = "false")
        private Boolean booked;

        @Schema(description = "学生姓名（如果已预约）", example = "李同学")
        private String studentName;

        @Schema(description = "课程标题（如果已预约）", example = "高中数学")
        private String courseTitle;

        @Schema(description = "课程状态", example = "progressing", allowableValues = {"progressing", "completed", "cancelled"})
        private String status;

        @Schema(description = "是否为试听课", example = "false")
        private Boolean isTrial;
    }
}
