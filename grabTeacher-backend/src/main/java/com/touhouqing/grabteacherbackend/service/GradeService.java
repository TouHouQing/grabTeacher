package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.GradeDTO;
import com.touhouqing.grabteacherbackend.model.entity.Grade;

public interface GradeService {

    Grade create(GradeDTO request);

    Grade update(Long id, GradeDTO request);

    void delete(Long id);

    Grade getById(Long id);

    Page<Grade> list(int page, int size, String keyword, Boolean isActive);

    void updateStatus(Long id, Boolean isActive);
}

