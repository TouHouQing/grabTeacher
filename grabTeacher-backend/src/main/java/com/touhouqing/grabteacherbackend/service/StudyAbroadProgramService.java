package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.entity.dto.StudyAbroadProgramRequest;
import com.touhouqing.grabteacherbackend.entity.StudyAbroadProgram;

import java.util.List;

public interface StudyAbroadProgramService {
    StudyAbroadProgram create(StudyAbroadProgramRequest request);
    StudyAbroadProgram update(Long id, StudyAbroadProgramRequest request);
    void delete(Long id);
    StudyAbroadProgram getById(Long id);
    Page<StudyAbroadProgram> list(int page, int size, String keyword, Boolean isActive, Long countryId, Long stageId, Boolean isHot, Boolean isFeatured);
    List<StudyAbroadProgram> listActive(Integer limit, Long countryId, Long stageId);
    void updateStatus(Long id, Boolean isActive);
    void updateFlags(Long id, Boolean isHot, Boolean isFeatured);
}

