package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 按“日期”存储教师可上课时间段
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("teacher_daily_availability")
@Schema(description = "教师日历可上课时间实体")
public class TeacherDailyAvailability {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("teacher_id")
    private Long teacherId;

    @TableField("available_date")
    private LocalDate availableDate;

    /**
     * JSON 数组字符串，例如：["08:00-10:00","10:00-12:00"]
     */
    @TableField("time_slots_json")
    private String timeSlotsJson;

    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(value = "updated_at", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    @Builder.Default
    private Boolean deleted = false;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}

