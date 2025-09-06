package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.JobPostDTO;
import com.touhouqing.grabteacherbackend.model.entity.JobPost;
import com.touhouqing.grabteacherbackend.model.vo.JobPostVO;

public interface JobPostService {
    Page<JobPostVO> pageActive(int page, int size, Long subjectId);

    // 管理端分页（可按状态、关键词、创建时间范围、是否包含已删除）
    Page<JobPostVO> pageAdmin(int page, int size,
                              Long subjectId,
                              String status, String keyword,
                              java.time.LocalDateTime createdStart, java.time.LocalDateTime createdEnd,
                              Boolean includeDeleted);

    JobPost create(JobPostDTO dto);

    JobPost update(Long id, JobPostDTO dto);

    void delete(Long id);

    // 批量删除（逻辑删除）
    void batchDelete(java.util.List<Long> ids);

    JobPostVO getById(Long id);

    // 管理端原始实体（包含 subjectIds 等冗余字段；包含已删除记录）
    JobPost getAdminById(Long id);

    // 详情页极热Key预序列化JSON（本地一级缓存）
    String getDetailJson(Long id);
}

