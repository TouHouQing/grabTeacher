package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.StudyAbroadStageDTO;
import com.touhouqing.grabteacherbackend.model.entity.StudyAbroadStage;

import java.util.List;

public interface StudyAbroadStageService {
    StudyAbroadStage create(StudyAbroadStageDTO request);
    StudyAbroadStage update(Long id, StudyAbroadStageDTO request);
    void delete(Long id);
    StudyAbroadStage getById(Long id);
    Page<StudyAbroadStage> list(int page, int size, String keyword, Boolean isActive);

    // 管理端直查 DB 的分页列表
    Page<StudyAbroadStage> listNoCache(int page, int size, String keyword, Boolean isActive);
    List<StudyAbroadStage> listActive();
    void updateStatus(Long id, Boolean isActive);
}

