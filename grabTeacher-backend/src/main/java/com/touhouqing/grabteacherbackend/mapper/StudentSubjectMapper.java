package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.entity.StudentSubject;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface StudentSubjectMapper extends BaseMapper<StudentSubject> {

    /**
     * 根据学生ID查询感兴趣的科目ID列表
     */
    @Select("SELECT subject_id FROM student_subjects WHERE student_id = #{studentId}")
    List<Long> getSubjectIdsByStudentId(@Param("studentId") Long studentId);

    /**
     * 删除学生的所有科目关联
     */
    @Delete("DELETE FROM student_subjects WHERE student_id = #{studentId}")
    void deleteByStudentId(@Param("studentId") Long studentId);
}
