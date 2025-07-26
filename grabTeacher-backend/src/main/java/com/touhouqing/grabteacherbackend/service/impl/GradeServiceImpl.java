package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.dto.GradeRequest;
import com.touhouqing.grabteacherbackend.dto.GradeResponse;
import com.touhouqing.grabteacherbackend.entity.Grade;
import com.touhouqing.grabteacherbackend.mapper.GradeMapper;
import com.touhouqing.grabteacherbackend.service.GradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public List<GradeResponse> getAllGrades() {
        QueryWrapper<Grade> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);

        List<Grade> grades = gradeMapper.selectList(queryWrapper);
        List<GradeResponse> gradeResponses = grades.stream()
                .map(this::convertToGradeResponse)
                .collect(Collectors.toList());

        // 自定义排序：按学段和年级排序
        gradeResponses.sort((g1, g2) -> {
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

        return gradeResponses;
    }

    @Override
    public GradeResponse getGradeById(Long id) {
        Grade grade = gradeMapper.selectById(id);
        if (grade == null || grade.getIsDeleted()) {
            throw new RuntimeException("年级不存在");
        }
        return convertToGradeResponse(grade);
    }

    @Override
    @Transactional
    public GradeResponse createGrade(GradeRequest request) {
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
    public GradeResponse updateGrade(Long id, GradeRequest request) {
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
    public void deleteGrade(Long id) {
        Grade grade = gradeMapper.selectById(id);
        if (grade == null || grade.getIsDeleted()) {
            throw new RuntimeException("年级不存在");
        }

        // 检查是否有课程使用该年级
        Long courseCount = gradeMapper.countCoursesByGradeName(grade.getGradeName());
        if (courseCount > 0) {
            throw new RuntimeException("该年级正在被 " + courseCount + " 个课程使用，无法删除");
        }

        grade.setIsDeleted(true);
        grade.setDeletedAt(LocalDateTime.now());
        gradeMapper.updateById(grade);
        log.info("年级删除成功: {}", grade.getGradeName());
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
    private GradeResponse convertToGradeResponse(Grade grade) {
        Long courseCount = gradeMapper.countCoursesByGradeName(grade.getGradeName());
        
        return GradeResponse.builder()
                .id(grade.getId())
                .gradeName(grade.getGradeName())
                .description(grade.getDescription())
                .courseCount(courseCount)
                .createdAt(grade.getCreatedAt())
                .build();
    }
}
