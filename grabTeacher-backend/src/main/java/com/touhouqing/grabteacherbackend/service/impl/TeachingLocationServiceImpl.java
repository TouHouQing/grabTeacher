package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.mapper.TeachingLocationMapper;
import com.touhouqing.grabteacherbackend.model.dto.TeachingLocationDTO;
import com.touhouqing.grabteacherbackend.model.entity.TeachingLocation;
import com.touhouqing.grabteacherbackend.service.TeachingLocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeachingLocationServiceImpl implements TeachingLocationService {

    private final TeachingLocationMapper teachingLocationMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"teachingLocations", "activeTeachingLocations"}, allEntries = true)
    public TeachingLocation create(TeachingLocationDTO request) {
        // 名称唯一校验
        QueryWrapper<TeachingLocation> qw = new QueryWrapper<>();
        qw.eq("name", request.getName());
        Long count = teachingLocationMapper.selectCount(qw);
        if (count != null && count > 0) {
            throw new RuntimeException("授课地点名称已存在");
        }

        TeachingLocation entity = TeachingLocation.builder()
                .name(request.getName())
                .address(request.getAddress())
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        teachingLocationMapper.insert(entity);
        log.info("创建授课地点成功: {}", entity.getName());
        return entity;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"teachingLocations", "activeTeachingLocations"}, allEntries = true)
    public TeachingLocation update(Long id, TeachingLocationDTO request) {
        TeachingLocation db = teachingLocationMapper.selectById(id);
        if (db == null) {
            throw new RuntimeException("授课地点不存在");
        }
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(db.getName())) {
            QueryWrapper<TeachingLocation> qw = new QueryWrapper<>();
            qw.eq("name", request.getName());
            qw.ne("id", id);
            Long count = teachingLocationMapper.selectCount(qw);
            if (count != null && count > 0) {
                throw new RuntimeException("授课地点名称已存在");
            }
            db.setName(request.getName());
        }
        db.setAddress(request.getAddress());
        db.setIsActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE);
        db.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        teachingLocationMapper.updateById(db);
        log.info("更新授课地点成功: {}", db.getName());
        return db;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"teachingLocations", "activeTeachingLocations"}, allEntries = true)
    public void delete(Long id) {
        TeachingLocation db = teachingLocationMapper.selectById(id);
        if (db == null) {
            throw new RuntimeException("授课地点不存在");
        }
        teachingLocationMapper.deleteById(id);
        log.info("删除授课地点成功: {}", db.getName());
    }

    @Override
    @Cacheable(cacheNames = "teachingLocations", key = "#id", unless = "#result == null")
    public TeachingLocation getById(Long id) {
        return teachingLocationMapper.selectById(id);
    }

    @Override
    public Page<TeachingLocation> list(int page, int size, String keyword, Boolean isActive) {
        Page<TeachingLocation> p = new Page<>(page, size);
        QueryWrapper<TeachingLocation> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like("name", keyword).or().like("address", keyword);
        }
        if (isActive != null) {
            qw.eq("is_active", isActive);
        }
        // 先按 sort_order 升序，再按 id 降序，保证稳定排序
        qw.orderByAsc("sort_order").orderByDesc("id");
        return teachingLocationMapper.selectPage(p, qw);
    }

    @Override
    @Cacheable(cacheNames = "activeTeachingLocations", key = "'all'", unless = "#result == null || #result.isEmpty()")
    public java.util.List<TeachingLocation> getAllActiveLocations() {
        QueryWrapper<TeachingLocation> qw = new QueryWrapper<>();
        qw.eq("is_active", true).orderByAsc("sort_order").orderByDesc("id");
        return teachingLocationMapper.selectList(qw);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"teachingLocations", "activeTeachingLocations"}, allEntries = true)
    public void updateStatus(Long id, Boolean isActive) {
        TeachingLocation db = teachingLocationMapper.selectById(id);
        if (db == null) {
            throw new RuntimeException("授课地点不存在");
        }
        db.setIsActive(Boolean.TRUE.equals(isActive));
        teachingLocationMapper.updateById(db);
        log.info("更新授课地点状态成功: {}, status: {}", db.getName(), isActive);
    }
}

