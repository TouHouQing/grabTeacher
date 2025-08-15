package com.touhouqing.grabteacherbackend.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class JobPostVO {
    private Long id;
    private String title;
    private String introduction;
    private String positionTags; // JSON字符串
    private String gradeNames;
    private String subjectNames;
    private String status;
    private Integer priority;
    private String createdAt;
}

