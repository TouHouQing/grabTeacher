package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("hour_details")
@Schema(description = "教师课时变动记录")
public class HourDetail {

    @TableId(type = IdType.AUTO)
    @Schema(description = "课时变动ID")
    private Long id;

    @TableField("user_id")
    @Schema(description = "教师用户ID")
    private Long userId;

    @TableField("name")
    @Schema(description = "教师姓名")
    private String name;

    @TableField("hours")
    @Schema(description = "变动课时数，正数为增加，负数为减少")
    private BigDecimal hours;

    @TableField("hours_before")
    @Schema(description = "变动前课时数")
    private BigDecimal hoursBefore;

    @TableField("hours_after")
    @Schema(description = "变动后课时数")
    private BigDecimal hoursAfter;

    @TableField("transaction_type")
    @Schema(description = "交易类型：1-增加，0-减少")
    private Integer transactionType;

    @TableField("reason")
    @Schema(description = "变动原因")
    private String reason;

    @TableField("booking_id")
    @Schema(description = "关联的课程/预约ID")
    private Long bookingId;

    @TableField("operator_id")
    @Schema(description = "操作员ID（管理员）")
    private Long operatorId;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}


