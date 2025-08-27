package com.touhouqing.grabteacherbackend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aliyun.oss")
public class AliyunOssProperties {
    /**
     * 例如：oss-cn-hangzhou.aliyuncs.com
     */
    private String endpoint;
    private String accessKeyId;
    private String accessKeySecret;
    /**
     * 桶名
     */
    private String bucket;
    /**
     * 公网访问域名（可填自定义 CDN/域名）
     */
    private String publicDomain;
    /**
     * 对象前缀，默认 uploads/
     */
    private String dirPrefix = "uploads/";
}

