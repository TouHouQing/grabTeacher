package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("suspension_requests")
@Schema(description = "停课申请")
public class SuspensionRequest {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("enrollment_id")
    private Long enrollmentId;

    @TableField("student_id")
    private Long studentId;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("reason")
    private String reason;

    @TableField("start_date")
    private java.time.LocalDate startDate;

    @TableField("end_date")
    private java.time.LocalDate endDate;

    @TableField("status")
    @Builder.Default
    private String status = "pending"; // pending, approved, rejected, cancelled

    @TableField("admin_id")
    private Long adminId;

    @TableField("admin_notes")
    private String adminNotes;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("reviewed_at")
    private LocalDateTime reviewedAt;

    @TableField("is_deleted")
    @Builder.Default
    private Boolean deleted = false;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}


