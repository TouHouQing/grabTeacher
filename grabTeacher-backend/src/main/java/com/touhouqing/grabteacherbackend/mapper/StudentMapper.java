package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 根据用户ID查找学生信息
     */
    @Select("SELECT * FROM students WHERE user_id = #{userId} AND is_deleted = 0")
    Student findByUserId(@Param("userId") Long userId);
}