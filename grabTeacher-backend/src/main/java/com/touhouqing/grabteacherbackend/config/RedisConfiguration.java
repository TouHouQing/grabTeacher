package com.touhouqing.grabteacherbackend.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;

@Slf4j
@Configuration
public class RedisConfiguration {

    @Bean
    public RedisTemplate redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate  redisTemplate = new RedisTemplate<>();
        //设置redis的连接工厂对象
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        //设置redis key序列化器
        redisTemplate.setKeySerializer(redisTemplate.getStringSerializer());

        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer();
        redisTemplate.setValueSerializer(valueSerializer);

        redisTemplate.setHashKeySerializer(redisTemplate.getStringSerializer());
        redisTemplate.setHashValueSerializer(valueSerializer);

        return redisTemplate;
    }

}
