package com.touhouqing.grabteacherbackend.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class DatabaseConfig {

    @Bean
    public DataSource dataSource() {
        HikariDataSource dataSource = new HikariDataSource();
        // 数据库连接 URL
        dataSource.setJdbcUrl("DATABASE_URL");
        // 数据库用户名
        dataSource.setUsername("DATABASE_USERNAME");
        // 数据库密码
        dataSource.setPassword("DATABASE_PASSWORD");
        // 数据库驱动类
        dataSource.setDriverClassName("com.mysql.cj.jdbc.Driver");
        return dataSource;
    }
}