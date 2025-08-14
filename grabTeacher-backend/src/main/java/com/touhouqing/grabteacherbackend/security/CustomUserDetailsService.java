package com.touhouqing.grabteacherbackend.security;

import com.touhouqing.grabteacherbackend.model.entity.User;
import com.touhouqing.grabteacherbackend.mapper.UserMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger logger = LoggerFactory.getLogger(CustomUserDetailsService.class);

    @Autowired
    UserMapper userMapper;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        logger.info("尝试加载用户: {}", identifier);
        
        // 支持用户名或邮箱登录
        User user = userMapper.findByUsernameOrEmail(identifier);
        if (user == null) {
            logger.warn("用户不存在: {}", identifier);
            throw new UsernameNotFoundException("用户不存在，用户名或邮箱: " + identifier);
        }

        logger.info("成功找到用户: {}, 用户类型: {}", user.getEmail(), user.getUserType());
        
        // 检查用户状态
        if (!"active".equals(user.getStatus())) {
            logger.warn("用户账户未激活: {}", identifier);
            throw new UsernameNotFoundException("用户账户未激活");
        }

        return UserPrincipal.create(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new UsernameNotFoundException("用户不存在，ID: " + id);
        }

        return UserPrincipal.create(user);
    }
} 