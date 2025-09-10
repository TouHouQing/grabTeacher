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

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("course_enrollments")
@Schema(description = "课程报名关系实体")
public class CourseEnrollment {

    @TableId(type = IdType.AUTO)
    @Schema(description = "报名ID")
    private Long id;

    @TableField("student_id")
    @Schema(description = "学生ID")
    private Long studentId;

    @TableField("teacher_id")
    @Schema(description = "教师ID")
    private Long teacherId;

    @TableField("course_id")
    @Schema(description = "课程ID（大班课为课程ID，一对一可为空）")
    private Long courseId;

    @TableField("enrollment_type")
    @Schema(description = "报名类型", allowableValues = {"one_on_one", "large_class"})
    private String enrollmentType;

    @TableField("total_sessions")
    @Schema(description = "总课次")
    private Integer totalSessions;

    @TableField("completed_sessions")
    @Schema(description = "已完成课次")
    private Integer completedSessions;

    @TableField("enrollment_status")
    @Schema(description = "报名状态", allowableValues = {"active","completed","cancelled","suspended"})
    private String enrollmentStatus;

    @TableField("enrollment_date")
    @Schema(description = "报名日期")
    private LocalDate enrollmentDate;

    @TableField("start_date")
    @Schema(description = "开始日期")
    private LocalDate startDate;

    @TableField("end_date")
    @Schema(description = "结束日期")
    private LocalDate endDate;

    @TableField("is_trial")
    @Schema(description = "是否试听")
    private Boolean trial;

    @TableField("recurring_schedule")
    @Schema(description = "周期性预约安排（JSON）")
    private String recurringSchedule;

    @TableField("duration_minutes")
    @Schema(description = "单次课程时长，单位：分钟，只能选择90分钟或120分钟", example = "120")
    private Integer durationMinutes;

    @TableField("grade")
    @Schema(description = "年级")
    private String grade;

    @TableField("booking_request_id")
    @Schema(description = "关联预约申请ID")
    private Long bookingRequestId;

    @TableField("teacher_notes")
    @Schema(description = "教师备注")
    private String teacherNotes;

    @TableField("student_feedback")
    @Schema(description = "学生反馈")
    private String studentFeedback;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @TableField("is_deleted")
    @Schema(description = "是否删除")
    private Boolean deleted;

    @TableField("deleted_at")
    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}


