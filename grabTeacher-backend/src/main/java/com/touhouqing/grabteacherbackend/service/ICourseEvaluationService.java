package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.touhouqing.grabteacherbackend.model.entity.CourseEvaluation;

/**
 * <p>
 * 课程评价表 服务类
 * </p>
 *
 * @author TouHouQing
 * @since 2025-08-26
 */
public interface ICourseEvaluationService extends IService<CourseEvaluation> {

    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.touhouqing.grabteacherbackend.model.vo.CourseEvaluationVO> pagePublicEvaluations(
            int page,
            int size,
            Long teacherId,
            Long courseId,
            java.math.BigDecimal minRating,
            String teacherName,
            String courseName
    );

    com.baomidou.mybatisplus.extension.plugins.pagination.Page<com.touhouqing.grabteacherbackend.model.vo.CourseEvaluationVO> pageAdmin(
            int page,
            int size,
            Long teacherId,
            Long studentId,
            Long courseId,
            java.math.BigDecimal minRating,
            String teacherName,
            String studentName,
            String courseName
    );

    CourseEvaluation createByAdmin(
            com.touhouqing.grabteacherbackend.model.dto.CourseEvaluationCreateDTO dto
    );

    CourseEvaluation updateByAdmin(
            com.touhouqing.grabteacherbackend.model.dto.CourseEvaluationUpdateDTO dto
    );

    void deleteByAdmin(Long id);

    /** 切换精选状态 */
    CourseEvaluation toggleFeatured(Long id, boolean isFeatured);

}
