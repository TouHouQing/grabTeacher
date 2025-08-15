package com.touhouqing.grabteacherbackend.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("job_posts")
public class JobPost {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String introduction;

    @TableField("position_tags")
    private String positionTags; // JSON字符串

    @TableField("grade_ids")
    private String gradeIds; // 逗号分隔的ID

    @TableField("grade_names")
    private String gradeNames; // 逗号分隔的名称

    @TableField("subject_ids")
    private String subjectIds;

    @TableField("subject_names")
    private String subjectNames;

    private String status; // active / expired

    private Integer priority; // 越小越靠前

    @TableField("created_at")
    private LocalDateTime createdAt;

    @TableField("updated_at")
    private LocalDateTime updatedAt;

    @TableField("is_deleted")
    private Boolean deleted;

    @TableField("deleted_at")
    private LocalDateTime deletedAt;
}

