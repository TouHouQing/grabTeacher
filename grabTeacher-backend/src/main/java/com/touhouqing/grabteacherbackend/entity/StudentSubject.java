package com.touhouqing.grabteacherbackend.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 学生感兴趣科目关联实体类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("student_subjects")
public class StudentSubject {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    @TableField("student_id")
    private Long studentId;
    
    @TableField("subject_id")
    private Long subjectId;
    
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    // 构造函数
    public StudentSubject(Long studentId, Long subjectId) {
        this.studentId = studentId;
        this.subjectId = subjectId;
    }
}
