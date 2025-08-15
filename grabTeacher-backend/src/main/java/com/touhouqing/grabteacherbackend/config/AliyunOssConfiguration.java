package com.touhouqing.grabteacherbackend.config;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
@EnableConfigurationProperties(AliyunOssProperties.class)
@RequiredArgsConstructor
public class AliyunOssConfiguration {

    private final AliyunOssProperties props;

    @Bean(destroyMethod = "shutdown")
    public OSS ossClient() {
        log.info("Initializing Aliyun OSS client for endpoint {} and bucket {}", props.getEndpoint(), props.getBucket());
        return new OSSClientBuilder().build(props.getEndpoint(), props.getAccessKeyId(), props.getAccessKeySecret());
    }
}
