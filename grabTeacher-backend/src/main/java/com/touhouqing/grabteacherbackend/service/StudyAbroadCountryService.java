package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.StudyAbroadCountryRequestDTO;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadCountry;

import java.util.List;

public interface StudyAbroadCountryService {
    StudyAbroadCountry create(StudyAbroadCountryRequestDTO request);
    StudyAbroadCountry update(Long id, StudyAbroadCountryRequestDTO request);
    void delete(Long id);
    StudyAbroadCountry getById(Long id);
    Page<StudyAbroadCountry> list(int page, int size, String keyword, Boolean isActive);
    List<StudyAbroadCountry> listActive();
    void updateStatus(Long id, Boolean isActive);
}

