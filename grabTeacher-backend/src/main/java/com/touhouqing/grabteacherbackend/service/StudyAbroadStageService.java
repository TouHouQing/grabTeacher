package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.dto.StudyAbroadStageRequestDTO;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadStage;

import java.util.List;

public interface StudyAbroadStageService {
    StudyAbroadStage create(StudyAbroadStageRequestDTO request);
    StudyAbroadStage update(Long id, StudyAbroadStageRequestDTO request);
    void delete(Long id);
    StudyAbroadStage getById(Long id);
    Page<StudyAbroadStage> list(int page, int size, String keyword, Boolean isActive);
    List<StudyAbroadStage> listActive();
    void updateStatus(Long id, Boolean isActive);
}

