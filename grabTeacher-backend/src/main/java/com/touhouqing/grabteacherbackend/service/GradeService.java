package com.touhouqing.grabteacherbackend.service;

import com.touhouqing.grabteacherbackend.model.dto.GradeDTO;
import com.touhouqing.grabteacherbackend.model.vo.GradeVO;

import java.util.List;

/**
 * 年级管理服务接口
 */
public interface GradeService {

    /**
     * 获取所有年级列表
     */
    List<GradeVO> getAllGrades();

    /**
     * 根据ID获取年级信息
     */
    GradeVO getGradeById(Long id);

    /**
     * 创建年级
     */
    GradeVO createGrade(GradeDTO request);

    /**
     * 更新年级
     */
    GradeVO updateGrade(Long id, GradeDTO request);

    /**
     * 删除年级
     */
    void deleteGrade(Long id);

    /**
     * 检查年级名称是否已存在
     */
    boolean isGradeNameExists(String gradeName);

    /**
     * 检查年级名称是否已存在（排除指定ID）
     */
    boolean isGradeNameExists(String gradeName, Long excludeId);
}
