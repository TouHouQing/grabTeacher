package com.touhouqing.grabteacherbackend.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class JobPostDTO {
    private String title;
    private String introduction;
    private List<Long> gradeIds;
    private List<Long> subjectIds;
    private List<String> tags; // 职位标签
    private String status; // active/expired
    private Integer priority;
}

