package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.StudyAbroadCountryRequest;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadCountry;
import com.touhouqing.grabteacherbackend.mapper.StudyAbroadCountryMapper;
import com.touhouqing.grabteacherbackend.mapper.StudyAbroadProgramMapper;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadProgram;
import com.touhouqing.grabteacherbackend.service.StudyAbroadCountryService;
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
public class StudyAbroadCountryServiceImpl implements StudyAbroadCountryService {

    private final StudyAbroadCountryMapper countryMapper;
    private final StudyAbroadProgramMapper programMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:countries:list", "abroad:countries:active"}, allEntries = true)
    public StudyAbroadCountry create(StudyAbroadCountryRequest request) {
        // 重名检查（忽略软删除）
        QueryWrapper<StudyAbroadCountry> qw = new QueryWrapper<>();
        qw.eq("country_name", request.getCountryName())
          .eq("is_deleted", false);
        if (countryMapper.selectOne(qw) != null) {
            throw new RuntimeException("国家名称已存在");
        }

        StudyAbroadCountry entity = StudyAbroadCountry.builder()
                .countryName(request.getCountryName())
                .sortOrder(request.getSortOrder() == null ? 0 : request.getSortOrder())
                .isActive(request.getIsActive() == null ? true : request.getIsActive())
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        countryMapper.insert(entity);
        log.info("创建留学国家: {}", entity.getCountryName());
        return entity;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:countries:list", "abroad:countries:active"}, allEntries = true)
    public StudyAbroadCountry update(Long id, StudyAbroadCountryRequest request) {
        StudyAbroadCountry entity = countryMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new RuntimeException("国家不存在");
        }

        // 同名冲突（排除自身，忽略软删除）
        QueryWrapper<StudyAbroadCountry> qw = new QueryWrapper<>();
        qw.eq("country_name", request.getCountryName())
          .eq("is_deleted", false)
          .ne("id", id);
        if (countryMapper.selectOne(qw) != null) {
            throw new RuntimeException("国家名称已存在");
        }

        entity.setCountryName(request.getCountryName());
        if (request.getSortOrder() != null) entity.setSortOrder(request.getSortOrder());
        if (request.getIsActive() != null) entity.setIsActive(request.getIsActive());
        entity.setUpdatedAt(LocalDateTime.now());
        countryMapper.updateById(entity);
        log.info("更新留学国家: {}", entity.getCountryName());
        return entity;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:countries:list", "abroad:countries:active"}, allEntries = true)
    public void delete(Long id) {
        StudyAbroadCountry entity = countryMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new RuntimeException("国家不存在");
        }
        // 检查是否存在关联项目
        Long count = programMapper.selectCount(new QueryWrapper<StudyAbroadProgram>()
                .eq("is_deleted", false)
                .eq("country_id", id));
        if (count != null && count > 0) {
            throw new RuntimeException("该国家下仍有留学项目，无法删除。请先删除所有关联项目。");
        }

        entity.setIsDeleted(true);
        entity.setDeletedAt(LocalDateTime.now());
        countryMapper.updateById(entity);
        log.info("删除留学国家: {}", entity.getCountryName());
    }

    @Override
    @Cacheable(cacheNames = "abroad:countries:get", key = "#id")
    public StudyAbroadCountry getById(Long id) {
        QueryWrapper<StudyAbroadCountry> qw = new QueryWrapper<>();
        qw.eq("id", id).eq("is_deleted", false);
        return countryMapper.selectOne(qw);
    }

    @Override
    @Cacheable(cacheNames = "abroad:countries:list", keyGenerator = "customCacheKeyGenerator")
    public Page<StudyAbroadCountry> list(int page, int size, String keyword, Boolean isActive) {
        Page<StudyAbroadCountry> p = new Page<>(page, size);
        QueryWrapper<StudyAbroadCountry> qw = new QueryWrapper<>();
        qw.eq("is_deleted", false);
        if (StringUtils.hasText(keyword)) {
            qw.like("country_name", keyword);
        }
        if (isActive != null) {
            qw.eq("is_active", isActive);
        }
        qw.orderByAsc("sort_order").orderByDesc("id");
        return countryMapper.selectPage(p, qw);
    }

    @Override
    @Cacheable(cacheNames = "abroad:countries:active")
    public List<StudyAbroadCountry> listActive() {
        QueryWrapper<StudyAbroadCountry> qw = new QueryWrapper<>();
        qw.eq("is_deleted", false).eq("is_active", true)
          .orderByAsc("sort_order").orderByDesc("id");
        return countryMapper.selectList(qw);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"abroad:countries:list", "abroad:countries:active", "abroad:countries:get"}, allEntries = true)
    public void updateStatus(Long id, Boolean isActive) {
        StudyAbroadCountry entity = countryMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getIsDeleted())) {
            throw new RuntimeException("国家不存在");
        }
        entity.setIsActive(isActive);
        countryMapper.updateById(entity);
        log.info("更新国家状态: {} -> {}", entity.getCountryName(), isActive);
    }
}

