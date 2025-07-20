package com.touhouqing.grabteacherbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "课程请求参数")
public class CourseRequest {
    
    @Schema(description = "授课教师ID（管理员创建课程时需要，教师创建时自动使用当前用户）", example = "1")
    private Long teacherId;

    @NotNull(message = "科目ID不能为空")
    @Schema(description = "课程科目ID", example = "1", required = true)
    private Long subjectId;

    @NotBlank(message = "课程标题不能为空")
    @Size(max = 200, message = "课程标题长度不能超过200个字符")
    @Schema(description = "课程标题", example = "高中数学 - 函数与导数专题", required = true)
    private String title;

    @Schema(description = "课程详细描述", example = "本课程深入讲解高中数学中的函数与导数知识点，适合高二、高三学生")
    private String description;

    @NotNull(message = "课程类型不能为空")
    @Schema(description = "课程类型", example = "one_on_one", allowableValues = {"one_on_one", "large_class"}, required = true)
    private String courseType;

    @NotNull(message = "课程时长不能为空")
    @Min(value = 1, message = "课程时长必须大于0分钟")
    @Schema(description = "单次课程时长（分钟）", example = "120", required = true)
    private Integer durationMinutes;

    @Schema(description = "课程状态", example = "active", allowableValues = {"active", "inactive", "full"})
    private String status = "active";

    @Schema(description = "课程适用年级", example = "小学一年级,小学二年级")
    private String grade;

    @Schema(description = "适合性别", example = "不限", allowableValues = {"男", "女", "不限"})
    private String gender = "不限";
}
