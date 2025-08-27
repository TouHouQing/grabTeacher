package com.touhouqing.grabteacherbackend.service.impl;

import com.touhouqing.grabteacherbackend.mapper.CourseEvaluationMapper;
import com.touhouqing.grabteacherbackend.model.entity.CourseEvaluation;
import com.touhouqing.grabteacherbackend.service.CourseEvaluationService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.touhouqing.grabteacherbackend.model.vo.CourseEvaluationVO;
import com.touhouqing.grabteacherbackend.model.dto.CourseEvaluationCreateDTO;
import com.touhouqing.grabteacherbackend.model.dto.CourseEvaluationUpdateDTO;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 课程评价表 服务实现类
 * </p>
 *
 * @author TouHouQing
 * @since 2025-08-26
 */
@Service
public class CourseEvaluationServiceImpl extends ServiceImpl<CourseEvaluationMapper, CourseEvaluation> implements CourseEvaluationService {

    @Override
    @Cacheable(value = "courseEvaluations", key = "#page + '_' + #size + '_' + (#teacherId ?: 'null') + '_' + (#courseId ?: 'null') + '_' + (#minRating ?: 'null') + '_' + (#teacherName ?: 'null') + '_' + (#courseName ?: 'null')")
    public Page<CourseEvaluationVO> pagePublicEvaluations(int page, int size, Long teacherId, Long courseId, java.math.BigDecimal minRating, String teacherName, String courseName) {
        Page<CourseEvaluation> entityPage = new Page<>(page, size);
        LambdaQueryWrapper<CourseEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(teacherId != null, CourseEvaluation::getTeacherId, teacherId)
               .eq(courseId != null, CourseEvaluation::getCourseId, courseId)
               .ge(minRating != null, CourseEvaluation::getRating, minRating)
               .like(teacherName != null && !teacherName.isEmpty(), CourseEvaluation::getTeacherName, teacherName)
               .like(courseName != null && !courseName.isEmpty(), CourseEvaluation::getCourseName, courseName)
               .eq(CourseEvaluation::getIsDeleted, false)
               .eq(CourseEvaluation::getIsFeatured, true)  // 只显示已精选的评价
               // 按评分从高到低，其次按创建时间从新到旧
               .orderByDesc(CourseEvaluation::getRating)
               .orderByDesc(CourseEvaluation::getCreatedAt);

        Page<CourseEvaluation> result = this.page(entityPage, wrapper);

        Page<CourseEvaluationVO> voPage = new Page<>();
        voPage.setCurrent(result.getCurrent());
        voPage.setSize(result.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setPages(result.getPages());
        voPage.setRecords(result.getRecords().stream().map(e -> {
            CourseEvaluationVO vo = new CourseEvaluationVO();
            vo.setId(e.getId());
            vo.setTeacherId(e.getTeacherId());
            vo.setStudentId(e.getStudentId());
            vo.setCourseId(e.getCourseId());
            vo.setTeacherName(e.getTeacherName());
            vo.setStudentName(e.getStudentName());
            vo.setCourseName(e.getCourseName());
            vo.setStudentComment(e.getStudentComment());
            vo.setRating(e.getRating());
            vo.setCreatedAt(e.getCreatedAt());
            vo.setUpdatedAt(e.getUpdatedAt());
            vo.setIsFeatured(e.getIsFeatured());
            return vo;
        }).toList());

        return voPage;
    }

    @Override
    public Page<CourseEvaluationVO> pageAdmin(int page, int size, Long teacherId, Long studentId, Long courseId, java.math.BigDecimal minRating, String teacherName, String studentName, String courseName) {
        Page<CourseEvaluation> entityPage = new Page<>(page, size);
        LambdaQueryWrapper<CourseEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(teacherId != null, CourseEvaluation::getTeacherId, teacherId)
               .eq(studentId != null, CourseEvaluation::getStudentId, studentId)
               .eq(courseId != null, CourseEvaluation::getCourseId, courseId)
               .ge(minRating != null, CourseEvaluation::getRating, minRating)
               .like(teacherName != null && !teacherName.isEmpty(), CourseEvaluation::getTeacherName, teacherName)
               .like(studentName != null && !studentName.isEmpty(), CourseEvaluation::getStudentName, studentName)
               .like(courseName != null && !courseName.isEmpty(), CourseEvaluation::getCourseName, courseName)
               .eq(CourseEvaluation::getIsDeleted, false)
               .orderByDesc(CourseEvaluation::getCreatedAt);

        Page<CourseEvaluation> result = this.page(entityPage, wrapper);

        Page<CourseEvaluationVO> voPage = new Page<>();
        voPage.setCurrent(result.getCurrent());
        voPage.setSize(result.getSize());
        voPage.setTotal(result.getTotal());
        voPage.setPages(result.getPages());
        voPage.setRecords(result.getRecords().stream().map(e -> {
            CourseEvaluationVO vo = new CourseEvaluationVO();
            vo.setId(e.getId());
            vo.setTeacherId(e.getTeacherId());
            vo.setStudentId(e.getStudentId());
            vo.setCourseId(e.getCourseId());
            vo.setTeacherName(e.getTeacherName());
            vo.setStudentName(e.getStudentName());
            vo.setCourseName(e.getCourseName());
            vo.setStudentComment(e.getStudentComment());
            vo.setRating(e.getRating());
            vo.setCreatedAt(e.getCreatedAt());
            vo.setUpdatedAt(e.getUpdatedAt());
            vo.setIsFeatured(e.getIsFeatured());
            return vo;
        }).toList());

        return voPage;
    }

    @Override
    @CacheEvict(value = "courseEvaluations", allEntries = true)    // 清除缓存
    @Transactional
    public CourseEvaluation createByAdmin(CourseEvaluationCreateDTO dto) {
        CourseEvaluation entity = new CourseEvaluation();
        entity.setTeacherId(dto.getTeacherId());
        entity.setStudentId(dto.getStudentId());
        entity.setCourseId(dto.getCourseId());
        entity.setTeacherName(dto.getTeacherName());
        entity.setStudentName(dto.getStudentName());
        entity.setCourseName(dto.getCourseName());
        entity.setStudentComment(dto.getStudentComment());
        entity.setRating(dto.getRating());
        entity.setIsFeatured(Boolean.TRUE.equals(dto.getIsFeatured()));
        entity.setIsDeleted(false);
        this.save(entity);
        return entity;
    }

    @Override
    @CacheEvict(value = "courseEvaluations", allEntries = true)    // 清除缓存
    @Transactional
    public CourseEvaluation updateByAdmin(CourseEvaluationUpdateDTO dto) {
        CourseEvaluation entity = this.getById(dto.getId());
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new IllegalArgumentException("评价不存在或已删除");
        }
        entity.setTeacherId(dto.getTeacherId());
        entity.setStudentId(dto.getStudentId());
        entity.setCourseId(dto.getCourseId());
        entity.setTeacherName(dto.getTeacherName());
        entity.setStudentName(dto.getStudentName());
        entity.setCourseName(dto.getCourseName());
        entity.setStudentComment(dto.getStudentComment());
        entity.setRating(dto.getRating());
        if (dto.getIsFeatured() != null) {
            entity.setIsFeatured(dto.getIsFeatured());
        }
        this.updateById(entity);
        return entity;
    }

    @Override
    @CacheEvict(value = "courseEvaluations", allEntries = true)    // 清除缓存
    @Transactional
    public void deleteByAdmin(Long id) {
        CourseEvaluation entity = this.getById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            return;
        }
        entity.setIsDeleted(true);
        entity.setDeletedAt(java.time.LocalDateTime.now());
        this.updateById(entity);
    }

    @Override
    @CacheEvict(value = "courseEvaluations", allEntries = true)    // 清除缓存
    @Transactional
    public CourseEvaluation toggleFeatured(Long id, boolean isFeatured) {
        CourseEvaluation entity = this.getById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new IllegalArgumentException("评价不存在或已删除");
        }
        entity.setIsFeatured(isFeatured);
        this.updateById(entity);
        return entity;
    }

    @Override
    @CacheEvict(value = "courseEvaluations", allEntries = true)    // 清除缓存
    @Transactional
    public CourseEvaluation createByStudent(CourseEvaluationCreateDTO dto) {
        // 检查学生是否已经评价过这门课程
        if (hasStudentEvaluatedCourse(dto.getStudentId(), dto.getCourseId())) {
            throw new IllegalArgumentException("您已经评价过这门课程，不能重复评价");
        }
        
        CourseEvaluation entity = new CourseEvaluation();
        entity.setTeacherId(dto.getTeacherId());
        entity.setStudentId(dto.getStudentId());
        entity.setCourseId(dto.getCourseId());
        entity.setTeacherName(dto.getTeacherName());
        entity.setStudentName(dto.getStudentName());
        entity.setCourseName(dto.getCourseName());
        entity.setStudentComment(dto.getStudentComment());
        entity.setRating(dto.getRating());
        entity.setIsFeatured(false); // 学生评价默认不精选
        entity.setIsDeleted(false);
        this.save(entity);
        return entity;
    }

    @Override
    public boolean hasStudentEvaluatedCourse(Long studentId, Long courseId) {
        LambdaQueryWrapper<CourseEvaluation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CourseEvaluation::getStudentId, studentId)
               .eq(CourseEvaluation::getCourseId, courseId)
               .eq(CourseEvaluation::getIsDeleted, false);
        
        return this.count(wrapper) > 0;
    }
}
