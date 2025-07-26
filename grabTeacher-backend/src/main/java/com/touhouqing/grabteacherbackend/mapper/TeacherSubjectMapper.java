package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.entity.TeacherSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;

import java.util.List;

/**
 * 教师科目关联Mapper
 */
@Mapper
public interface TeacherSubjectMapper extends BaseMapper<TeacherSubject> {
    
    /**
     * 根据教师ID获取科目ID列表
     */
    @Select("SELECT subject_id FROM teacher_subjects WHERE teacher_id = #{teacherId}")
    List<Long> getSubjectIdsByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 根据教师ID删除所有科目关联
     */
    @Delete("DELETE FROM teacher_subjects WHERE teacher_id = #{teacherId}")
    void deleteByTeacherId(@Param("teacherId") Long teacherId);
    
    /**
     * 根据科目ID获取教师ID列表
     */
    @Select("SELECT teacher_id FROM teacher_subjects WHERE subject_id = #{subjectId}")
    List<Long> getTeacherIdsBySubjectId(@Param("subjectId") Long subjectId);
}
