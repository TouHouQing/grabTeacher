package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.entity.Teacher;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TeacherMapper extends BaseMapper<Teacher> {

    /**
     * 根据用户ID查找教师信息
     */
    @Select("SELECT * FROM teachers WHERE user_id = #{userId} AND is_deleted = 0")
    Teacher findByUserId(@Param("userId") Long userId);
}