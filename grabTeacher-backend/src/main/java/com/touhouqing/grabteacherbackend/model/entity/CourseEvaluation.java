package com.touhouqing.grabteacherbackend.model.entity;

import java.io.Serial;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import java.time.LocalDateTime;
import java.io.Serializable;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

/**
 * <p>
 * 课程评价表
 * </p>
 *
 * @author TouHouQing
 * @since 2025-08-26
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@TableName("course_evaluation")
public class CourseEvaluation implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 成绩记录ID，主键自增
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 教师ID，关联teachers表
     */
    private Long teacherId;

    /**
     * 学生ID，关联students表
     */
    private Long studentId;

    /**
     * 课程ID，关联courses表
     */
    private Long courseId;

    /**
     * 教师姓名
     */
    @NotBlank
    private String teacherName;

    /**
     * 学生姓名
     */
    @NotBlank
    private String studentName;

    /**
     * 课程名称
     */
    @NotBlank
    private String courseName;

    /**
     * 学生对课程的评价和建议
     */
    @NotBlank
    @Size(max = 255)
    private String studentComment;

    /**
     * 课程评分，0-5分
     */
    @NotNull
    @DecimalMin(value = "0")
    @DecimalMax(value = "5")
    @Digits(integer = 1, fraction = 2)
    private BigDecimal rating;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    private LocalDateTime deletedAt;


}
