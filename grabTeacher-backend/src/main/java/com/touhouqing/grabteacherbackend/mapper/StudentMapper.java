package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface StudentMapper extends BaseMapper<Student> {

    /**
     * 根据用户ID查找学生信息
     */
    @Select("SELECT * FROM students WHERE user_id = #{userId} AND is_deleted = 0")
    Student findByUserId(@Param("userId") Long userId);

    /**
     * 学生余额（M豆）增量变更，可正可负
     */
    @Update("UPDATE students SET balance = COALESCE(balance, 0) + #{amount} WHERE id = #{studentId} AND is_deleted = 0")
    int incrementBalance(@Param("studentId") Long studentId, @Param("amount") java.math.BigDecimal amount);
}