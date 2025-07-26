package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.SubjectRequest;
import com.touhouqing.grabteacherbackend.entity.Subject;
import com.touhouqing.grabteacherbackend.mapper.SubjectMapper;
import com.touhouqing.grabteacherbackend.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectMapper subjectMapper;

    /**
     * 创建科目
     */
    @Override
    @Transactional
    public Subject createSubject(SubjectRequest request) {
        // 检查科目名称是否已存在
        QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", request.getName());
        queryWrapper.eq("is_deleted", false);
        Subject existingSubject = subjectMapper.selectOne(queryWrapper);
        
        if (existingSubject != null) {
            throw new RuntimeException("科目名称已存在");
        }

        Subject subject = Subject.builder()
                .name(request.getName())
                .iconUrl(request.getIconUrl())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .isDeleted(false)
                .build();

        subjectMapper.insert(subject);
        log.info("创建科目成功: {}", subject.getName());
        return subject;
    }

    /**
     * 更新科目
     */
    @Override
    @Transactional
    public Subject updateSubject(Long id, SubjectRequest request) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null || subject.getIsDeleted()) {
            throw new RuntimeException("科目不存在");
        }

        // 检查科目名称是否与其他科目重复
        if (!subject.getName().equals(request.getName())) {
            QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", request.getName());
            queryWrapper.eq("is_deleted", false);
            queryWrapper.ne("id", id);
            Subject existingSubject = subjectMapper.selectOne(queryWrapper);
            
            if (existingSubject != null) {
                throw new RuntimeException("科目名称已存在");
            }
        }

        subject.setName(request.getName());
        subject.setIconUrl(request.getIconUrl());
        subject.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        subjectMapper.updateById(subject);
        log.info("更新科目成功: {}", subject.getName());
        return subject;
    }

    /**
     * 删除科目（软删除）
     */
    @Override
    @Transactional
    public void deleteSubject(Long id) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null || subject.getIsDeleted()) {
            throw new RuntimeException("科目不存在");
        }

        subject.setIsDeleted(true);
        subject.setDeletedAt(LocalDateTime.now());
        subjectMapper.updateById(subject);
        log.info("删除科目成功: {}", subject.getName());
    }

    /**
     * 根据ID获取科目
     */
    @Override
    public Subject getSubjectById(Long id) {
        QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        queryWrapper.eq("is_deleted", false);
        return subjectMapper.selectOne(queryWrapper);
    }

    /**
     * 获取科目列表（分页）
     */
    @Override
    public Page<Subject> getSubjectList(int page, int size, String keyword, Boolean isActive) {
        Page<Subject> pageParam = new Page<>(page, size);
        QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_deleted", false);

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("name", keyword);
        }

        if (isActive != null) {
            queryWrapper.eq("is_active", isActive);
        }

        queryWrapper.orderByDesc("id");

        return subjectMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 获取所有激活的科目
     */
    @Override
    public List<Subject> getAllActiveSubjects() {
        QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);
        queryWrapper.eq("is_active", true);
        queryWrapper.orderByDesc("id");
        return subjectMapper.selectList(queryWrapper);
    }

    /**
     * 更新科目状态
     */
    @Override
    @Transactional
    public void updateSubjectStatus(Long id, Boolean isActive) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null || subject.getIsDeleted()) {
            throw new RuntimeException("科目不存在");
        }

        subject.setIsActive(isActive);
        subjectMapper.updateById(subject);
        log.info("更新科目状态成功: {}, status: {}", subject.getName(), isActive);
    }
} 