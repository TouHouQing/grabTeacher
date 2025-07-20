package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.entity.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
    
    /**
     * 根据教师ID查询课程列表
     */
    @Select("SELECT * FROM courses WHERE teacher_id = #{teacherId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<Course> findByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 根据科目ID查询课程列表
     */
    @Select("SELECT * FROM courses WHERE subject_id = #{subjectId} AND is_deleted = 0 AND status = 'active' ORDER BY created_at DESC")
    List<Course> findBySubjectId(@Param("subjectId") Long subjectId);
    
    /**
     * 查询指定教师的课程数量
     */
    @Select("SELECT COUNT(*) FROM courses WHERE teacher_id = #{teacherId} AND is_deleted = 0")
    int countByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 查询活跃状态的课程列表
     */
    @Select("SELECT * FROM courses WHERE status = 'active' AND is_deleted = 0 ORDER BY created_at DESC")
    List<Course> findActiveCourses();
}
