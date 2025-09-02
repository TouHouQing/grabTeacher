package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM users WHERE (email = #{identifier} OR username = #{identifier}) AND is_deleted = 0")
    User findByUsernameOrEmail(@Param("identifier") String identifier);
    
    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username} AND is_deleted = 0")
    boolean existsByUsername(@Param("username") String username);
    
    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email} AND is_deleted = 0")
    boolean existsByEmail(@Param("email") String email);

    @Update("UPDATE users SET adjustment_times = adjustment_times - 1 WHERE id = #{userId} AND is_deleted = 0 AND adjustment_times > 0")
    int decrementAdjustmentTimes(@Param("userId") Long userId);

    @Update("UPDATE users SET adjustment_times = adjustment_times + 1 WHERE id = #{userId} AND is_deleted = 0")
    int incrementAdjustmentTimes(@Param("userId") Long userId);

    @Update("UPDATE users SET adjustment_times = #{times} WHERE is_deleted = 0")
    int resetAllAdjustmentTimes(@Param("times") int times);
} 