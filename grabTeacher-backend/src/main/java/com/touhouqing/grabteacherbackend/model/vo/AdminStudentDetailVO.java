package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;
import java.math.BigDecimal;

/**
 * 管理员端学生详情响应VO
 * 包含学生基本信息、用户信息、科目信息等完整信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "管理员端学生详情响应")
public class AdminStudentDetailVO {

    @Schema(description = "学生ID", example = "1")
    private Long id;

    @Schema(description = "用户ID", example = "1")
    private Long userId;

    @Schema(description = "真实姓名", example = "张同学")
    private String realName;

    @Schema(description = "用户名", example = "zhangstudent")
    private String username;

    @Schema(description = "邮箱", example = "zhang@example.com")
    private String email;

    @Schema(description = "电话", example = "+65 8888 8888")
    private String phone;

    @Schema(description = "头像URL", example = "https://example.com/avatar.jpg")
    private String avatarUrl;

    @Schema(description = "出生年月", example = "2005-01")
    private String birthDate;


    @Schema(description = "感兴趣的科目", example = "数学,物理,化学")
    private String subjectsInterested;

    @Schema(description = "感兴趣的科目ID列表", example = "[1, 2, 3]")
    private List<Long> subjectIds;

    @Schema(description = "学习目标", example = "提高数学成绩，准备高考")
    private String learningGoals;

    @Schema(description = "偏好的教学风格", example = "互动型")
    private String preferredTeachingStyle;

    @Schema(description = "预算范围", example = "100-200")
    private String budgetRange;

    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    private String gender;

    @Schema(description = "学生余额，单位：M豆", example = "500.00")
    private BigDecimal balance;

    @Schema(description = "试听课次数", example = "1")
    private Integer trialTimes;

    @Schema(description = "本月剩余调课次数", example = "3")
    private Integer adjustmentTimes;

    @Schema(description = "是否已删除", example = "false")
    private Boolean deleted;

    @Schema(description = "删除时间")
    private LocalDateTime deletedAt;
}
