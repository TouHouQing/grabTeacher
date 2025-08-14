package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 教师科目关联实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("teacher_subjects")
public class TeacherSubject {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("teacher_id")
    private Long teacherId;
    
    @TableField("subject_id")
    private Long subjectId;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 构造函数
    public TeacherSubject(Long teacherId, Long subjectId) {
        this.teacherId = teacherId;
        this.subjectId = subjectId;
    }
}
