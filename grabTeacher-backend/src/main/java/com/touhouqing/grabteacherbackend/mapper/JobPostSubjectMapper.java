package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.JobPostSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobPostSubjectMapper extends BaseMapper<JobPostSubject> {
    @Select("SELECT COUNT(*) FROM job_post_subjects js JOIN job_posts jp ON js.job_post_id = jp.id WHERE js.subject_id = #{subjectId} AND jp.is_deleted = 0")
    long countActiveJobPostsBySubjectId(@Param("subjectId") Long subjectId);
}

