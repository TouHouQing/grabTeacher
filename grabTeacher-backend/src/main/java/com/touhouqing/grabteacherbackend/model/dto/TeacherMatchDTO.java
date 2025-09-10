package com.touhouqing.grabteacherbackend.model.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.List;

/**
 * 教师匹配请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeacherMatchDTO {

    /**
     * 科目名称
     */
    private String subject;


    /**
     * 偏好时间
     */
    private String preferredTime;

    /**
     * 偏好日期范围开始
     */
    private String preferredDateStart;

    /**
     * 偏好日期范围结束
     */
    private String preferredDateEnd;

    /**
     * 偏好的星期几列表 (1=周一, 2=周二, ..., 7=周日)
     */
    private List<Integer> preferredWeekdays;

    /**
     * 偏好的时间段列表 (格式: "HH:mm-HH:mm")
     */
    private List<String> preferredTimeSlots;

    /**
     * 偏好的教师性别
     */
    private String preferredGender;

    /**
     * 偏好的教师级别
     */
    private String teacherLevel;

    /**
     * 年级
     */
    private String grade;


    /**
     * 返回结果数量限制（至少5）
     */
    @Builder.Default
    private Integer limit = 5;
}
