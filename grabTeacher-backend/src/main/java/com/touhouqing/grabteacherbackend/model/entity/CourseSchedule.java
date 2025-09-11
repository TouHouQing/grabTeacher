package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("course_schedules")
@Schema(description = "课程单次安排实体")
public class CourseSchedule {

    @TableId(type = IdType.AUTO)
    @Schema(description = "课程安排ID")
    private Long id;

    @TableField("enrollment_id")
    @Schema(description = "报名关系ID")
    private Long enrollmentId;

    @TableField("scheduled_date")
    @Schema(description = "上课日期")
    private LocalDate scheduledDate;

    @TableField("start_time")
    @Schema(description = "开始时间")
    private LocalTime startTime;

    @TableField("end_time")
    @Schema(description = "结束时间")
    private LocalTime endTime;

    @TableField("session_number")
    @Schema(description = "第几次课")
    private Integer sessionNumber;

    @TableField("schedule_status")
    @Schema(description = "安排状态", allowableValues = {"scheduled","completed","cancelled","rescheduled"})
    private String scheduleStatus;

    @TableField("teacher_notes")
    @Schema(description = "教师课后备注")
    private String teacherNotes;

    @TableField("student_feedback")
    @Schema(description = "学生课后反馈")
    private String studentFeedback;

    @TableField("reschedule_reason")
    @Schema(description = "调课原因")
    private String rescheduleReason;

    @TableField("reschedule_request_id")
    @Schema(description = "调课申请ID")
    private Long rescheduleRequestId;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @Schema(description = "是否删除")
    private Boolean deleted;

    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;

    // 以下字段来源于 course_enrollments 关联，用于业务便捷读取，不直接持久化
    @Schema(description = "教师ID（来自course_enrollments）")
    private Long teacherId;

    @Schema(description = "学生ID（来自course_enrollments）")
    private Long studentId;

    @Schema(description = "课程ID（来自course_enrollments）")
    private Long courseId;

    @Schema(description = "预约申请ID（来自course_enrollments）")
    private Long bookingRequestId;

    // 便捷展示用的非持久化字段（JOIN 或 关联派生）
    @Schema(description = "教师姓名（JOIN）")
    private String teacherName;

    @Schema(description = "学生姓名（JOIN）")
    private String studentName;

    @Schema(description = "课程标题（JOIN）")
    private String courseTitle;

    @Schema(description = "科目名称（JOIN）")
    private String subjectName;

    @Schema(description = "年级（报名信息）")
    private String grade;

    @Schema(description = "是否试听（报名信息）")
    private Boolean trial;

    @Schema(description = "课程类型（报名信息）")
    private String courseType;

    @Schema(description = "单次时长（分钟，报名信息）")
    private Integer durationMinutes;

}


