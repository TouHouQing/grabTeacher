package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "学生端“我的课程”卡片（V2）")
public class StudentCourseCardV2VO {
    @Schema(description = "课程报名ID")
    private Long enrollmentId;

    @Schema(description = "预约申请ID")
    private Long bookingRequestId;

    @Schema(description = "课程ID")
    private Long courseId;

    @Schema(description = "课程标题")
    private String title;

    @Schema(description = "教师姓名")
    private String teacherName;

    @Schema(description = "科目名称")
    private String subjectName;

    @Schema(description = "年级")
    private String grade;

    @Schema(description = "课程类型 one_on_one/large_class")
    private String courseType;

    @Schema(description = "总课时")
    private Integer totalLessons;

    @Schema(description = "已完成课时")
    private Integer completedLessons;

    @Schema(description = "最早上课日期")
    private LocalDate startDate;

    @Schema(description = "最晚上课日期")
    private LocalDate endDate;

    @Schema(description = "日历预约开始日期")
    private LocalDate calendarStartDate;

    @Schema(description = "日历预约结束日期")
    private LocalDate calendarEndDate;

    @Schema(description = "课程默认时长(分钟)")
    private Integer durationMinutes;


    @Schema(description = "下一节课程")
    private StudentCourseScheduleV2VO nextSchedule;

    @Schema(description = "课程内节次列表")
    private List<StudentCourseScheduleV2VO> schedules;
}

