package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("monthly_adjustment_counters")
@Schema(description = "每月调课/请假配额计数（按报名/课程维度）")
public class MonthlyAdjustmentCounter {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("actor_type")
    @Schema(description = "主体类型", allowableValues = {"STUDENT","TEACHER"})
    private String actorType;

    @TableField("actor_id")
    @Schema(description = "主体ID：学生ID或教师ID")
    private Long actorId;

    @TableField("enrollment_id")
    @Schema(description = "报名ID")
    private Long enrollmentId;

    @TableField("month_key")
    @Schema(description = "月份键：YYYY-MM（北京时间）")
    private String monthKey;

    @TableField("allowed_times")
    @Schema(description = "当月允许次数快照")
    private Integer allowedTimes;

    @TableField("used_times")
    @Schema(description = "当月已使用次数")
    private Integer usedTimes;

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @Builder.Default
    private Boolean deleted = false;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}

