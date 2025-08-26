package com.touhouqing.grabteacherbackend.model;

import java.io.Serial;
import java.math.BigDecimal;

import com.baomidou.mybatisplus.annotation.TableField;
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
    @TableField("teacher_id")
    private Long teacherId;

    /**
     * 学生ID，关联students表
     */
    @TableField("student_id")
    private Long studentId;

    /**
     * 课程ID，关联courses表
     */
    @TableField("course_id")
    private Long courseId;

    /**
     * 学生对课程的评价和建议
     */
    @TableField("student_comment")
    private String studentComment;

    /**
     * 课程评分，0-5分
     */
    @TableField("rating")
    private BigDecimal rating;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;

    /**
     * 是否删除：0-未删除，1-已删除
     */
    @TableField("is_deleted")
    private Boolean isDeleted;

    /**
     * 删除时间
     */
    @TableField("deleted_at")
    private LocalDateTime deletedAt;


}
