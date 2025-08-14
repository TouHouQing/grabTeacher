package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.entity.dto.SubjectRequest;
import com.touhouqing.grabteacherbackend.entity.Subject;

import java.util.List;

public interface SubjectService {
    
    /**
     * 创建科目
     */
    Subject createSubject(SubjectRequest request);
    
    /**
     * 更新科目
     */
    Subject updateSubject(Long id, SubjectRequest request);
    
    /**
     * 删除科目（软删除）
     */
    void deleteSubject(Long id);
    
    /**
     * 根据ID获取科目
     */
    Subject getSubjectById(Long id);
    
    /**
     * 获取科目列表（分页）
     */
    Page<Subject> getSubjectList(int page, int size, String keyword, Boolean isActive);
    
    /**
     * 获取所有激活的科目
     */
    List<Subject> getAllActiveSubjects();
    
    /**
     * 更新科目状态
     */
    void updateSubjectStatus(Long id, Boolean isActive);
} 