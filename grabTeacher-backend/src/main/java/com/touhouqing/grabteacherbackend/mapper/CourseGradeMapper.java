package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.entity.CourseGrade;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 课程年级关联Mapper
 */
@Mapper
public interface CourseGradeMapper extends BaseMapper<CourseGrade> {

    /**
     * 根据课程ID查询所有年级
     */
    @Select("SELECT * FROM course_grades WHERE course_id = #{courseId}")
    List<CourseGrade> findByCourseId(@Param("courseId") Long courseId);

    /**
     * 根据年级查询所有课程ID
     */
    @Select("SELECT course_id FROM course_grades WHERE grade = #{grade}")
    List<Long> findCourseIdsByGrade(@Param("grade") String grade);

    /**
     * 获取所有不同的年级
     */
    @Select("SELECT DISTINCT grade FROM course_grades ORDER BY grade")
    List<String> findAllDistinctGrades();

    /**
     * 删除课程的所有年级关联
     */
    @Select("DELETE FROM course_grades WHERE course_id = #{courseId}")
    int deleteByCourseId(@Param("courseId") Long courseId);
}
