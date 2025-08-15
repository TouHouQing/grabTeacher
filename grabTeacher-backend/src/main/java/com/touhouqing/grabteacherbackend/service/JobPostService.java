package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.JobPostDTO;
import com.touhouqing.grabteacherbackend.model.entity.JobPost;
import com.touhouqing.grabteacherbackend.model.vo.JobPostVO;

public interface JobPostService {
    Page<JobPostVO> pageActive(int page, int size, Long gradeId, Long subjectId);

    JobPost create(JobPostDTO dto);

    JobPost update(Long id, JobPostDTO dto);

    void delete(Long id);

    JobPostVO getById(Long id);

    // 详情页极热Key预序列化JSON（本地一级缓存）
    String getDetailJson(Long id);
}

