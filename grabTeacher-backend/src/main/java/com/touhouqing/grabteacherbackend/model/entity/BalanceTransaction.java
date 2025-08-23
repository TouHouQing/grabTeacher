package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@TableName("balance_transactions")
@Schema(description = "学生余额变动记录")
public class BalanceTransaction {
    
    @TableId(type = IdType.AUTO)
    @Schema(description = "记录ID", example = "1")
    private Long id;

    @TableField("user_id")
    @Schema(description = "学生用户ID", example = "1", required = true)
    private Long userId;

    @TableField("name")
    @Schema(description = "学生姓名", example = "张同学", required = true)
    private String name;

    @TableField("amount")
    @Schema(description = "变动金额（正数为充值，负数为扣费）", example = "100.00", required = true)
    private BigDecimal amount;

    @TableField("balance_before")
    @Schema(description = "变动前余额", example = "500.00")
    private BigDecimal balanceBefore;

    @TableField("balance_after")
    @Schema(description = "变动后余额", example = "600.00")
    private BigDecimal balanceAfter;

    @TableField("transaction_type")
    @Schema(description = "交易类型", example = "RECHARGE", allowableValues = {"RECHARGE", "DEDUCT", "REFUND"})
    private String transactionType;

    @TableField("reason")
    @Schema(description = "变动原因", example = "管理员充值")
    private String reason;

    @TableField("booking_id")
    @Schema(description = "关联的预约ID（如果有）", example = "1")
    private Long bookingId;

    @TableField("operator_id")
    @Schema(description = "操作员ID（管理员ID）", example = "1")
    private Long operatorId;

    @TableField("created_at")
    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
