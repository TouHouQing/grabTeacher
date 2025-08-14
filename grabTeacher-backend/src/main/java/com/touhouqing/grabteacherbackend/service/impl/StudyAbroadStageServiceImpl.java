package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.StudyAbroadStageRequestDTO;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadStage;
import com.touhouqing.grabteacherbackend.mapper.StudyAbroadStageMapper;
import com.touhouqing.grabteacherbackend.service.StudyAbroadStageService;
import com.touhouqing.grabteacherbackend.mapper.StudyAbroadProgramMapper;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadProgram;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyAbroadStageServiceImpl implements StudyAbroadStageService {

    private final StudyAbroadStageMapper stageMapper;
    private final StudyAbroadProgramMapper programMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:stages:list", "abroad:stages:active"}, allEntries = true)
    public StudyAbroadStage create(StudyAbroadStageRequestDTO request) {
        // 重名检查（忽略软删除）
        QueryWrapper<StudyAbroadStage> qw = new QueryWrapper<>();
        qw.eq("stage_name", request.getStageName())
          .eq("is_deleted", false);
        if (stageMapper.selectOne(qw) != null) {
            throw new RuntimeException("阶段名称已存在");
        }

        StudyAbroadStage entity = StudyAbroadStage.builder()
                .stageName(request.getStageName())
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .isActive(request.getIsActive() == null ? true : request.getIsActive())
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        stageMapper.insert(entity);
        log.info("创建留学阶段: {}", entity.getStageName());
        return entity;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:stages:list", "abroad:stages:active"}, allEntries = true)
    public StudyAbroadStage update(Long id, StudyAbroadStageRequestDTO request) {
        StudyAbroadStage entity = stageMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new RuntimeException("阶段不存在");
        }

        // 同名冲突（排除自身，忽略软删除）
        QueryWrapper<StudyAbroadStage> qw = new QueryWrapper<>();
        qw.eq("stage_name", request.getStageName())
          .eq("is_deleted", false)
          .ne("id", id);
        if (stageMapper.selectOne(qw) != null) {
            throw new RuntimeException("阶段名称已存在");
        }

        entity.setStageName(request.getStageName());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        entity.setUpdatedAt(LocalDateTime.now());
        stageMapper.updateById(entity);
        log.info("更新留学阶段: {}", entity.getStageName());
        return entity;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:stages:list", "abroad:stages:active"}, allEntries = true)
    public void delete(Long id) {
        StudyAbroadStage entity = stageMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new RuntimeException("阶段不存在");
        }
        // 检查是否存在关联项目
        Long count = programMapper.selectCount(new QueryWrapper<StudyAbroadProgram>()
                .eq("is_deleted", false)
                .eq("stage_id", id));
        if (count != null && count > 0) {
            throw new RuntimeException("该阶段下仍有留学项目，无法删除。请先删除所有关联项目。");
        }

        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        stageMapper.updateById(entity);
        log.info("删除留学阶段: {}", entity.getStageName());
    }

    @Override
    @Cacheable(cacheNames = "abroad:stages:get", key = "#id")
    public StudyAbroadStage getById(Long id) {
        QueryWrapper<StudyAbroadStage> qw = new QueryWrapper<>();
        qw.eq("id", id).eq("is_deleted", false);
        return stageMapper.selectOne(qw);
    }

    @Override
    @Cacheable(cacheNames = "abroad:stages:list", keyGenerator = "customCacheKeyGenerator")
    public Page<StudyAbroadStage> list(int page, int size, String keyword, Boolean isActive) {
        Page<StudyAbroadStage> p = new Page<>(page, size);
        QueryWrapper<StudyAbroadStage> qw = new QueryWrapper<>();
        qw.eq("is_deleted", false);
        if (StringUtils.hasText(keyword)) {
            qw.like("stage_name", keyword);
        }
        if (isActive != null) {
            qw.eq("is_active", isActive);
        }
        qw.orderByAsc("sort_order").orderByDesc("id");
        return stageMapper.selectPage(p, qw);
    }

    @Override
    @Cacheable(cacheNames = "abroad:stages:active")
    public List<StudyAbroadStage> listActive() {
        QueryWrapper<StudyAbroadStage> qw = new QueryWrapper<>();
        qw.eq("is_deleted", false).eq("is_active", true)
          .orderByAsc("sort_order").orderByDesc("id");
        return stageMapper.selectList(qw);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:stages:list", "abroad:stages:active", "abroad:stages:get"}, allEntries = true)
    public void updateStatus(Long id, Boolean isActive) {
        StudyAbroadStage entity = stageMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new RuntimeException("阶段不存在");
        }
        entity.setIsActive(isActive);
        stageMapper.updateById(entity);
        log.info("更新阶段状态: {} -> {}", entity.getStageName(), isActive);
    }
}

