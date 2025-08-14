package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.entity.dto.StudyAbroadCountryRequest;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadCountry;

import java.util.List;

public interface StudyAbroadCountryService {
    StudyAbroadCountry create(StudyAbroadCountryRequest request);
    StudyAbroadCountry update(Long id, StudyAbroadCountryRequest request);
    void delete(Long id);
    StudyAbroadCountry getById(Long id);
    Page<StudyAbroadCountry> list(int page, int size, String keyword, Boolean isActive);
    List<StudyAbroadCountry> listActive();
    void updateStatus(Long id, Boolean isActive);
}

