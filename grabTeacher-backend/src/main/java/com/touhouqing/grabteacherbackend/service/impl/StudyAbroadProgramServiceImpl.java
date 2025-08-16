package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.StudyAbroadProgramDTO;
import com.touhouqing.grabteacherbackend.model.entity.StudyAbroadProgram;
import com.touhouqing.grabteacherbackend.mapper.StudyAbroadProgramMapper;
import com.touhouqing.grabteacherbackend.service.StudyAbroadProgramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.context.ApplicationEventPublisher;

import com.touhouqing.grabteacherbackend.event.ProgramChangedEvent;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudyAbroadProgramServiceImpl implements StudyAbroadProgramService {

    private final StudyAbroadProgramMapper programMapper;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:programs:list", "abroad:programs:active"}, allEntries = true)
    public StudyAbroadProgram create(StudyAbroadProgramDTO request) {
        // 重名检查（同国家+阶段下，标题唯一，忽略已删除）
        QueryWrapper<StudyAbroadProgram> qw = new QueryWrapper<>();
        qw.eq("title", request.getTitle())
          .eq("country_id", request.getCountryId())
          .eq("stage_id", request.getStageId())
          .eq("is_deleted", false);
        if (programMapper.selectOne(qw) != null) {
            throw new RuntimeException("同国家与阶段下，项目标题已存在");
        }

        StudyAbroadProgram entity = StudyAbroadProgram.builder()
                .title(request.getTitle())
                .countryId(request.getCountryId())
                .stageId(request.getStageId())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .tags(request.getTags())
                .hot(request.getHot() == null ? false : request.getHot())
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .active(request.getActive() == null ? true : request.getActive())
                .deleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        programMapper.insert(entity);
        log.info("创建留学项目: {}", entity.getTitle());
        try { eventPublisher.publishEvent(new ProgramChangedEvent(this, ProgramChangedEvent.ChangeType.CREATE, entity.getCountryId(), entity.getStageId())); } catch (Exception ignore) {}
        return entity;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:programs:list", "abroad:programs:active", "abroad:programs:get"}, allEntries = true)
    public StudyAbroadProgram update(Long id, StudyAbroadProgramDTO request) {
        StudyAbroadProgram entity = programMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new RuntimeException("项目不存在");
        }
        // 同名冲突（排除自身，忽略软删除）
        QueryWrapper<StudyAbroadProgram> qw = new QueryWrapper<>();
        qw.eq("title", request.getTitle())
          .eq("country_id", request.getCountryId())
          .eq("stage_id", request.getStageId())
          .eq("is_deleted", false)
          .ne("id", id);
        if (programMapper.selectOne(qw) != null) {
            throw new RuntimeException("同国家与阶段下，项目标题已存在");
        }
        entity.setTitle(request.getTitle());
        entity.setCountryId(request.getCountryId());
        entity.setStageId(request.getStageId());
        entity.setDescription(request.getDescription());
        entity.setImageUrl(request.getImageUrl());
        entity.setTags(request.getTags());
        if (request.getHot() != null) entity.setHot(request.getHot());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        if (request.getActive() != null) entity.setActive(request.getActive());
        entity.setUpdatedAt(LocalDateTime.now());
        programMapper.updateById(entity);
        log.info("更新留学项目: {}", entity.getTitle());
        try { eventPublisher.publishEvent(new ProgramChangedEvent(this, ProgramChangedEvent.ChangeType.UPDATE, entity.getCountryId(), entity.getStageId())); } catch (Exception ignore) {}
        return entity;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:programs:list", "abroad:programs:active", "abroad:programs:get"}, allEntries = true)
    public void delete(Long id) {
        StudyAbroadProgram entity = programMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new RuntimeException("项目不存在");
        }
        entity.setDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        programMapper.updateById(entity);
        log.info("删除留学项目: {}", entity.getTitle());
        try { eventPublisher.publishEvent(new ProgramChangedEvent(this, ProgramChangedEvent.ChangeType.DELETE, entity.getCountryId(), entity.getStageId())); } catch (Exception ignore) {}
    }

    @Override
    @Cacheable(cacheNames = "abroad:programs:get", key = "#id")
    public StudyAbroadProgram getById(Long id) {
        QueryWrapper<StudyAbroadProgram> qw = new QueryWrapper<>();
        qw.eq("id", id).eq("is_deleted", false);
        return programMapper.selectOne(qw);
    }

    @Override
    @Cacheable(cacheNames = "abroad:programs:list", keyGenerator = "customCacheKeyGenerator")
    public Page<StudyAbroadProgram> list(int page, int size, String keyword, Boolean isActive, Long countryId, Long stageId, Boolean isHot, Boolean isFeatured) {
        return doList(page, size, keyword, isActive, countryId, stageId, isHot, isFeatured);
    }

    @Override
    public Page<StudyAbroadProgram> listNoCache(int page, int size, String keyword, Boolean isActive, Long countryId, Long stageId, Boolean isHot, Boolean isFeatured) {
        return doList(page, size, keyword, isActive, countryId, stageId, isHot, isFeatured);
    }

    private Page<StudyAbroadProgram> doList(int page, int size, String keyword, Boolean isActive, Long countryId, Long stageId, Boolean isHot, Boolean isFeatured) {
        Page<StudyAbroadProgram> p = new Page<>(page, size);
        QueryWrapper<StudyAbroadProgram> qw = new QueryWrapper<>();
        qw.eq("is_deleted", false);
        if (StringUtils.hasText(keyword)) {
            qw.like("title", keyword);
        }
        if (isActive != null) qw.eq("is_active", isActive);
        if (countryId != null) qw.eq("country_id", countryId);
        if (stageId != null) qw.eq("stage_id", stageId);
        if (isHot != null) qw.eq("is_hot", isHot);
        if (isFeatured != null) qw.eq("is_featured", isFeatured);
        qw.orderByAsc("sort_order").orderByDesc("id");
        return programMapper.selectPage(p, qw);
    }

    @Override
    @Cacheable(cacheNames = "abroad:programs:active", keyGenerator = "customCacheKeyGenerator")
    public List<StudyAbroadProgram> listActive(Integer limit, Long countryId, Long stageId) {
        QueryWrapper<StudyAbroadProgram> qw = new QueryWrapper<>();
        qw.eq("is_deleted", false).eq("is_active", true);
        if (countryId != null) qw.eq("country_id", countryId);
        if (stageId != null) qw.eq("stage_id", stageId);
        qw.orderByAsc("sort_order").orderByDesc("id");
        Page<StudyAbroadProgram> page = new Page<>(1, limit == null ? 100 : limit);
        return programMapper.selectPage(page, qw).getRecords();
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:programs:list", "abroad:programs:active", "abroad:programs:get"}, allEntries = true)
    public void updateStatus(Long id, Boolean isActive) {
        StudyAbroadProgram entity = programMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new RuntimeException("项目不存在");
        }
        entity.setActive(isActive);
        programMapper.updateById(entity);
        log.info("更新项目状态: {} -> {}", entity.getTitle(), isActive);
        try { eventPublisher.publishEvent(new ProgramChangedEvent(this, ProgramChangedEvent.ChangeType.STATUS, entity.getCountryId(), entity.getStageId())); } catch (Exception ignore) {}
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:programs:list", "abroad:programs:active", "abroad:programs:get"}, allEntries = true)
    public void updateFlags(Long id, Boolean isHot, Boolean isFeatured) {
        StudyAbroadProgram entity = programMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new RuntimeException("项目不存在");
        }
        if (isHot != null) entity.setHot(isHot);
        programMapper.updateById(entity);
        log.info("更新项目标记: {} -> hot={}", entity.getTitle(), isHot);
        try { eventPublisher.publishEvent(new ProgramChangedEvent(this, ProgramChangedEvent.ChangeType.FLAGS, entity.getCountryId(), entity.getStageId())); } catch (Exception ignore) {}
    }
}

