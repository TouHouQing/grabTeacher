package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.JobPostGrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface JobPostGradeMapper extends BaseMapper<JobPostGrade> {
    @Select("SELECT COUNT(*) FROM job_post_grades jg JOIN job_posts jp ON jg.job_post_id = jp.id WHERE jg.grade_id = #{gradeId} AND jp.is_deleted = 0")
    long countActiveJobPostsByGradeId(@Param("gradeId") Long gradeId);
}

