package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.TeachingLocationDTO;
import com.touhouqing.grabteacherbackend.model.entity.TeachingLocation;

public interface TeachingLocationService {

    TeachingLocation create(TeachingLocationDTO request);

    TeachingLocation update(Long id, TeachingLocationDTO request);

    void delete(Long id);

    TeachingLocation getById(Long id);

    Page<TeachingLocation> list(int page, int size, String keyword, Boolean isActive);

    void updateStatus(Long id, Boolean isActive);

    java.util.List<TeachingLocation> getAllActiveLocations();
}

