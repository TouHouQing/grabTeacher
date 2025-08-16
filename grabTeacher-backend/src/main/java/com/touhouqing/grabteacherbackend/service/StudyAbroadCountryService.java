package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.StudyAbroadCountryDTO;
import com.touhouqing.grabteacherbackend.model.entity.StudyAbroadCountry;

import java.util.List;

public interface StudyAbroadCountryService {
    StudyAbroadCountry create(StudyAbroadCountryDTO request);
    StudyAbroadCountry update(Long id, StudyAbroadCountryDTO request);
    void delete(Long id);
    StudyAbroadCountry getById(Long id);
    Page<StudyAbroadCountry> list(int page, int size, String keyword, Boolean isActive);

    // 管理端直查 DB 的分页列表
    Page<StudyAbroadCountry> listNoCache(int page, int size, String keyword, Boolean isActive);
    List<StudyAbroadCountry> listActive();
    void updateStatus(Long id, Boolean isActive);
}

