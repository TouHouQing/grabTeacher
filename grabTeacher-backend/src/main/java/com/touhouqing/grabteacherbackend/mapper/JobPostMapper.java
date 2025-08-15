package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.JobPost;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface JobPostMapper extends BaseMapper<JobPost> {

    @Select("SELECT * FROM job_posts WHERE is_deleted=0 AND status='active' ORDER BY priority ASC, created_at DESC, id DESC LIMIT #{limit} OFFSET #{offset}")
    List<JobPost> listActive(@Param("offset") int offset, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM job_posts WHERE is_deleted=0 AND status='active'")
    long countActive();
}

