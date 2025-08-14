package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    
    @Select("SELECT * FROM users WHERE (email = #{identifier} OR username = #{identifier}) AND is_deleted = 0")
    User findByUsernameOrEmail(@Param("identifier") String identifier);
    
    @Select("SELECT COUNT(*) > 0 FROM users WHERE username = #{username} AND is_deleted = 0")
    boolean existsByUsername(@Param("username") String username);
    
    @Select("SELECT COUNT(*) > 0 FROM users WHERE email = #{email} AND is_deleted = 0")
    boolean existsByEmail(@Param("email") String email);
} 