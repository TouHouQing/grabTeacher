package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.dto.GradeRequestDTO;
import com.touhouqing.grabteacherbackend.dto.GradeResponseDTO;
import com.touhouqing.grabteacherbackend.entity.BookingRequest;
import com.touhouqing.grabteacherbackend.entity.Course;
import com.touhouqing.grabteacherbackend.entity.Grade;
import com.touhouqing.grabteacherbackend.entity.Schedule;
import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseGradeMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.GradeMapper;
import com.touhouqing.grabteacherbackend.mapper.ScheduleMapper;
import com.touhouqing.grabteacherbackend.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final ScheduleMapper scheduleMapper;

    @Override
    @Cacheable(cacheNames = "grades", key = "'all'")
    public List<GradeResponseDTO> getAllGrades() {
        QueryWrapper<Grade> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);

        List<Grade> grades = gradeMapper.selectList(queryWrapper);
        List<GradeResponseDTO> gradeResponsDTOS = grades.stream()
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
    public GradeResponseDTO getGradeById(Long id) {
        Grade grade = gradeMapper.selectById(id);
        if (grade == null || grade.getIsDeleted()) {
            throw new RuntimeException("年级不存在");
        }
        return convertToGradeResponse(grade);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades"}, allEntries = true)
    public GradeResponseDTO createGrade(GradeRequestDTO request) {
        // 检查年级名称是否已存在
        if (isGradeNameExists(request.getGradeName())) {
            throw new RuntimeException("年级名称已存在");
        }

        Grade grade = Grade.builder()
                .gradeName(request.getGradeName())
                .description(request.getDescription())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isDeleted(false)
                .build();

        gradeMapper.insert(grade);
        log.info("年级创建成功: {}", grade.getGradeName());
        return convertToGradeResponse(grade);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades"}, allEntries = true)
    public GradeResponseDTO updateGrade(Long id, GradeRequestDTO request) {
        Grade grade = gradeMapper.selectById(id);
        if (grade == null || grade.getIsDeleted()) {
            throw new RuntimeException("年级不存在");
        }

        // 检查年级名称是否已存在（排除当前年级）
        if (isGradeNameExists(request.getGradeName(), id)) {
            throw new RuntimeException("年级名称已存在");
        }

        grade.setGradeName(request.getGradeName());
        grade.setDescription(request.getDescription());
        grade.setUpdatedAt(LocalDateTime.now());

        gradeMapper.updateById(grade);
        log.info("年级更新成功: {}", grade.getGradeName());
        return convertToGradeResponse(grade);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades"}, allEntries = true)
    public void deleteGrade(Long id) {
        Grade grade = gradeMapper.selectById(id);
        if (grade == null || grade.getIsDeleted()) {
            throw new RuntimeException("年级不存在");
        }

        // 1. 查询使用该年级的所有课程ID
        List<Long> courseIds = courseGradeMapper.findCourseIdsByGrade(grade.getGradeName());
        log.info("找到年级 {} 关联的课程数量: {}", grade.getGradeName(), courseIds.size());

        // 2. 删除这些课程及其相关数据
        for (Long courseId : courseIds) {
            Course course = courseMapper.selectById(courseId);
            if (course != null && !course.getIsDeleted()) {
                // 2.1 处理该课程的预约申请（软删除）
                List<BookingRequest> bookingRequests = bookingRequestMapper.findByCourseId(courseId);
                for (BookingRequest bookingRequest : bookingRequests) {
                    if (!bookingRequest.getIsDeleted()) {
                        bookingRequest.setIsDeleted(true);
                        bookingRequest.setDeletedAt(LocalDateTime.now());
                        bookingRequestMapper.updateById(bookingRequest);
                    }
                }
                log.info("删除课程 {} 的 {} 个预约申请", course.getTitle(), bookingRequests.size());

                // 2.2 处理该课程的课程安排（软删除）
                List<Schedule> schedules = scheduleMapper.findByCourseId(courseId);
                for (Schedule schedule : schedules) {
                    if (!schedule.getIsDeleted()) {
                        schedule.setIsDeleted(true);
                        schedule.setDeletedAt(LocalDateTime.now());
                        scheduleMapper.updateById(schedule);
                    }
                }
                log.info("删除课程 {} 的 {} 个课程安排", course.getTitle(), schedules.size());

                // 2.3 删除课程年级关联
                courseGradeMapper.deleteByCourseId(courseId);

                // 2.4 最后删除课程本身（软删除）
                course.setIsDeleted(true);
                course.setDeletedAt(LocalDateTime.now());
                courseMapper.updateById(course);
                log.info("删除课程: {}", course.getTitle());
            }
        }

        // 3. 最后删除年级本身（软删除）
        grade.setIsDeleted(true);
        grade.setDeletedAt(LocalDateTime.now());
        gradeMapper.updateById(grade);
        log.info("删除年级成功: {}，同时删除了 {} 个相关课程", grade.getGradeName(), courseIds.size());
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
    private GradeResponseDTO convertToGradeResponse(Grade grade) {
        Long courseCount = gradeMapper.countCoursesByGradeName(grade.getGradeName());
        
        return GradeResponseDTO.builder()
                .id(grade.getId())
                .gradeName(grade.getGradeName())
                .description(grade.getDescription())
                .courseCount(courseCount)
                .createdAt(grade.getCreatedAt())
                .build();
    }
}
