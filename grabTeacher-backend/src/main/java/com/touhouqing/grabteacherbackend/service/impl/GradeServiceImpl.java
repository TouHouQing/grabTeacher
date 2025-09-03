package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.touhouqing.grabteacherbackend.model.dto.GradeDTO;
import com.touhouqing.grabteacherbackend.model.entity.*;
import com.touhouqing.grabteacherbackend.model.vo.GradeVO;
import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseGradeMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.GradeMapper;
import com.touhouqing.grabteacherbackend.mapper.JobPostGradeMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseEnrollmentMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseScheduleMapper;
import com.touhouqing.grabteacherbackend.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;

import com.touhouqing.grabteacherbackend.event.GradeChangedEvent;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 年级管理服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GradeServiceImpl implements GradeService {

    private final GradeMapper gradeMapper;
    private final CourseGradeMapper courseGradeMapper;
    private final CourseMapper courseMapper;
    private final BookingRequestMapper bookingRequestMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final JobPostGradeMapper jobPostGradeMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;
    private final CourseScheduleMapper courseScheduleMapper;

    @Override
    @Cacheable(cacheNames = "grades", key = "'all'")
    public List<GradeVO> getAllGrades() {
        return doGetAllGrades();
    }

    @Override
    public List<GradeVO> getAllGradesNoCache() {
        return doGetAllGrades();
    }

    private List<GradeVO> doGetAllGrades() {
        QueryWrapper<Grade> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);

        List<Grade> grades = gradeMapper.selectList(queryWrapper);
        List<GradeVO> gradeResponsDTOS = grades.stream()
                .map(this::convertToGradeResponse)
                .collect(Collectors.toList());

        // 自定义排序：按学段和年级排序
        gradeResponsDTOS.sort((g1, g2) -> {
            // 定义年级顺序
            String[] gradeOrder = {
                "小学一年级", "小学二年级", "小学三年级", "小学四年级", "小学五年级", "小学六年级",
                "初中一年级", "初中二年级", "初中三年级",
                "高中一年级", "高中二年级", "高中三年级"
            };

            int index1 = Arrays.asList(gradeOrder).indexOf(g1.getGradeName());
            int index2 = Arrays.asList(gradeOrder).indexOf(g2.getGradeName());

            // 如果都在预定义列表中，按顺序排序
            if (index1 != -1 && index2 != -1) {
                return Integer.compare(index1, index2);
            }
            // 如果只有一个在预定义列表中，预定义的排在前面
            if (index1 != -1) return -1;
            if (index2 != -1) return 1;
            // 如果都不在预定义列表中，按字典序排序
            return g1.getGradeName().compareTo(g2.getGradeName());
        });

        return gradeResponsDTOS;
    }

    @Override
    public GradeVO getGradeById(Long id) {
        Grade grade = gradeMapper.selectById(id);
        if (grade == null || grade.getDeleted()) {
            throw new RuntimeException("年级不存在");
        }
        return convertToGradeResponse(grade);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades"}, allEntries = true)
    public GradeVO createGrade(GradeDTO request) {
        // 归一化名称，避免前后空格/全角半角差异
        String normalizedName = request.getGradeName() == null ? null : request.getGradeName().trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new RuntimeException("年级名称不能为空");
        }

        // 先检查是否存在同名记录（含已删除）
        Grade any = gradeMapper.findAnyByGradeName(normalizedName);
        if (any != null) {
            boolean isDeleted = Boolean.TRUE.equals(any.getDeleted()) || (any.getDeleted() == null && any.getDeletedAt() != null);
            if (isDeleted) {
                // 软删除记录复活为有效
                any.setDeleted(false);
                any.setDeletedAt(null);
                any.setGradeName(normalizedName);
                any.setDescription(request.getDescription());
                any.setUpdatedAt(LocalDateTime.now());
                gradeMapper.updateById(any);
                log.info("复活已删除的年级: id={}, name={}", any.getId(), any.getGradeName());
                try { eventPublisher.publishEvent(new GradeChangedEvent(this, GradeChangedEvent.ChangeType.CREATE)); } catch (Exception ignore) {}
                return convertToGradeResponse(any);
            } else {
                log.warn("创建年级冲突, 已存在同名记录: id={}, name={}, deleted={}", any.getId(), any.getGradeName(), any.getDeleted());
                throw new RuntimeException("年级名称已存在");
            }
        }

        Grade grade = Grade.builder()
                .gradeName(normalizedName)
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(false)
                .build();

        gradeMapper.insert(grade);
        log.info("年级创建成功: {}", grade.getGradeName());
        // 发布事件：交由监听器统一驱逐缓存
        try { eventPublisher.publishEvent(new GradeChangedEvent(this, GradeChangedEvent.ChangeType.CREATE)); } catch (Exception ignore) {}
        return convertToGradeResponse(grade);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades"}, allEntries = true)
    public GradeVO updateGrade(Long id, GradeDTO request) {
        Grade grade = gradeMapper.selectById(id);
        if (grade == null || grade.getDeleted()) {
            throw new RuntimeException("年级不存在");
        }

        // 归一化名称
        String normalizedName = request.getGradeName() == null ? null : request.getGradeName().trim();
        if (normalizedName == null || normalizedName.isEmpty()) {
            throw new RuntimeException("年级名称不能为空");
        }
        // 检查是否与其他记录（含已删除）重名
        Grade anyOther = gradeMapper.findAnyByGradeNameExcludeId(normalizedName, id);
        if (anyOther != null && !anyOther.getId().equals(id)) {
            throw new RuntimeException("年级名称已存在");
        }

        grade.setGradeName(normalizedName);
        grade.setDescription(request.getDescription());
        grade.setUpdatedAt(LocalDateTime.now());

        gradeMapper.updateById(grade);
        log.info("年级更新成功: {}", grade.getGradeName());
        try { eventPublisher.publishEvent(new GradeChangedEvent(this, GradeChangedEvent.ChangeType.UPDATE)); } catch (Exception ignore) {}
        return convertToGradeResponse(grade);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades"}, allEntries = true)
    public void deleteGrade(Long id) {
        Grade grade = gradeMapper.selectById(id);
        if (grade == null || grade.getDeleted()) {
            throw new RuntimeException("年级不存在");
        }

        // 0. 保护性检查：是否被教师招聘引用（未删除）
        long jobCount = jobPostGradeMapper.countActiveJobPostsByGradeId(id);
        if (jobCount > 0) {
            throw new RuntimeException("该年级存在未删除的教师招聘信息，无法删除");
        }

        // 1. 查询使用该年级的所有课程ID
        List<Long> courseIds = courseGradeMapper.findCourseIdsByGrade(grade.getGradeName());
        log.info("找到年级 {} 关联的课程数量: {}", grade.getGradeName(), courseIds.size());

        // 2. 删除这些课程及其相关数据
        for (Long courseId : courseIds) {
            Course course = courseMapper.selectById(courseId);
            if (course != null && !course.getDeleted()) {
                // 2.1 处理该课程的预约申请（软删除）
                List<BookingRequest> bookingRequests = bookingRequestMapper.findByCourseId(courseId);
                for (BookingRequest bookingRequest : bookingRequests) {
                    if (!bookingRequest.getDeleted()) {
                        bookingRequest.setDeleted(true);
                        bookingRequest.setDeletedAt(LocalDateTime.now());
                        bookingRequestMapper.updateById(bookingRequest);
                    }
                }
                log.info("删除课程 {} 的 {} 个预约申请", course.getTitle(), bookingRequests.size());

                // 2.2 处理该课程的课程安排（软删除）- 新表
                QueryWrapper<CourseEnrollment> ew = new QueryWrapper<>();
                ew.eq("course_id", courseId).eq("is_deleted", 0);
                java.util.List<CourseEnrollment> enrollments = courseEnrollmentMapper.selectList(ew);
                int scheduleDeleteCount = 0;
                for (CourseEnrollment ce : enrollments) {
                    UpdateWrapper<CourseSchedule> su = new UpdateWrapper<>();
                    su.eq("enrollment_id", ce.getId()).eq("is_deleted", 0).set("is_deleted", 1).set("deleted_at", LocalDateTime.now());
                    scheduleDeleteCount += courseScheduleMapper.update(null, su);
                    UpdateWrapper<CourseEnrollment> eu = new UpdateWrapper<>();
                    eu.eq("id", ce.getId()).eq("is_deleted", 0).set("is_deleted", 1).set("deleted_at", LocalDateTime.now());
                    courseEnrollmentMapper.update(null, eu);
                }
                log.info("删除课程 {} 的 {} 个课程安排", course.getTitle(), scheduleDeleteCount);

                // 2.3 删除课程年级关联
                courseGradeMapper.deleteByCourseId(courseId);

                // 2.4 最后删除课程本身（软删除）
                course.setDeleted(true);
                course.setDeletedAt(LocalDateTime.now());
                courseMapper.updateById(course);
                log.info("删除课程: {}", course.getTitle());
            }
        }

        // 3. 最后删除年级本身（软删除）
        grade.setDeleted(true);
        grade.setDeletedAt(LocalDateTime.now());
        gradeMapper.updateById(grade);
        log.info("删除年级成功: {}，同时删除了 {} 个相关课程", grade.getGradeName(), courseIds.size());
        try { eventPublisher.publishEvent(new GradeChangedEvent(this, GradeChangedEvent.ChangeType.DELETE)); } catch (Exception ignore) {}
    }

    @Override
    public boolean isGradeNameExists(String gradeName) {
        Grade grade = gradeMapper.findByGradeName(gradeName);
        return grade != null;
    }

    @Override
    public boolean isGradeNameExists(String gradeName, Long excludeId) {
        Grade grade = gradeMapper.findByGradeNameExcludeId(gradeName, excludeId);
        return grade != null;
    }

    /**
     * 转换为GradeResponse
     */
    private GradeVO convertToGradeResponse(Grade grade) {
        Long courseCount = gradeMapper.countCoursesByGradeName(grade.getGradeName());

        return GradeVO.builder()
                .id(grade.getId())
                .gradeName(grade.getGradeName())
                .description(grade.getDescription())
                .courseCount(courseCount)
                .createdAt(grade.getCreatedAt())
                .build();
    }
}
