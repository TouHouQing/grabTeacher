package com.touhouqing.grabteacherbackend.model.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.util.List;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class    StudentInfoDTO {
    private String realName;
    private String birthDate;
    private String subjectsInterested; // 保留兼容性，但优先使用subjectIds
    private List<Long> subjectIds; // 新增：感兴趣的科目ID列表
    private String learningGoals;
    private String preferredTeachingStyle;
    private String budgetRange;

    @Schema(description = "性别", example = "不愿透露", allowableValues = {"男", "女", "不愿透露"})
    private String gender;

    @Schema(description = "学生余额，单位：M豆（仅管理员可编辑）")
    private BigDecimal balance;

    @Schema(description = "头像URL")
    private String avatarUrl;

    // 管理员添加学生时需要的账号信息
    @Schema(description = "用户名（可选，留空则自动生成）")
    private String username;

    @Schema(description = "密码（管理员可编辑，留空则不修改）")
    private String password;

    @Schema(description = "试听课次数（管理员可编辑）")
    private Integer trialTimes;

    @Schema(description = "本月调课次数（管理员可编辑）")
    private Integer adjustmentTimes;
}