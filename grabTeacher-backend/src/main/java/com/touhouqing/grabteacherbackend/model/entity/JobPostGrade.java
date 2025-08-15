package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("job_post_grades")
public class JobPostGrade {
    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("job_post_id")
    private Long jobPostId;

    @TableField("grade_id")
    private Long gradeId;

    @TableField("created_at")
    private LocalDateTime createdAt;
}

