package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.mapper.GradeMapper;
import com.touhouqing.grabteacherbackend.model.dto.GradeDTO;
import com.touhouqing.grabteacherbackend.model.entity.Grade;
import com.touhouqing.grabteacherbackend.service.GradeService;
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
public class GradeServiceImpl implements GradeService {

    private final GradeMapper gradeMapper;

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades", "activeGrades"}, allEntries = true)
    public Grade create(GradeDTO request) {
        // 名称唯一性校验
        QueryWrapper<Grade> qw = new QueryWrapper<>();
        qw.eq("name", request.getName());
        Long count = gradeMapper.selectCount(qw);
        if (count != null && count > 0) {
            throw new RuntimeException("年级名称已存在");
        }

        Grade entity = Grade.builder()
                .name(request.getName())
                .isActive(request.getIsActive() != null ? request.getIsActive() : Boolean.TRUE)
                .sortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0)
                .build();
        gradeMapper.insert(entity);
        log.info("创建年级成功: {}", entity.getName());
        return entity;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades", "activeGrades"}, allEntries = true)
    public Grade update(Long id, GradeDTO request) {
        Grade db = gradeMapper.selectById(id);
        if (db == null) {
            throw new RuntimeException("年级不存在");
        }
        if (StringUtils.hasText(request.getName()) && !request.getName().equals(db.getName())) {
            QueryWrapper<Grade> qw = new QueryWrapper<>();
            qw.eq("name", request.getName());
            qw.ne("id", id);
            Long count = gradeMapper.selectCount(qw);
            if (count != null && count > 0) {
                throw new RuntimeException("年级名称已存在");
            }
            db.setName(request.getName());
        }
        if (request.getIsActive() != null) {
            db.setIsActive(request.getIsActive());
        }
        if (request.getSortOrder() != null) {
            db.setSortOrder(request.getSortOrder());
        }
        gradeMapper.updateById(db);
        log.info("更新年级成功: {}", db.getName());
        return db;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades", "activeGrades"}, allEntries = true)
    public void delete(Long id) {
        Grade db = gradeMapper.selectById(id);
        if (db == null) {
            throw new RuntimeException("年级不存在");
        }
        gradeMapper.deleteById(id);
        log.info("删除年级成功: {}", db.getName());
    }

    @Override
    @Cacheable(cacheNames = "grades", key = "#id", unless = "#result == null")
    public Grade getById(Long id) {
        return gradeMapper.selectById(id);
    }

    @Override
    public Page<Grade> list(int page, int size, String keyword, Boolean isActive) {
        Page<Grade> p = new Page<>(page, size);
        QueryWrapper<Grade> qw = new QueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            qw.like("name", keyword);
        }
        if (isActive != null) {
            qw.eq("is_active", isActive);
        }
        // 先按 sort_order 升序，再按 id 降序，保证稳定排序
        qw.orderByAsc("sort_order").orderByDesc("id");
        return gradeMapper.selectPage(p, qw);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = {"grades", "activeGrades"}, allEntries = true)
    public void updateStatus(Long id, Boolean isActive) {
        Grade db = gradeMapper.selectById(id);
        if (db == null) {
            throw new RuntimeException("年级不存在");
        }
        db.setIsActive(Boolean.TRUE.equals(isActive));
        gradeMapper.updateById(db);
        log.info("更新年级状态成功: {}, status: {}", db.getName(), isActive);
    }
}

