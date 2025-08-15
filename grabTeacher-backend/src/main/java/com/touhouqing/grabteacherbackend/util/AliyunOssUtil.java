package com.touhouqing.grabteacherbackend.util;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CannedAccessControlList;
import com.aliyun.oss.model.ObjectMetadata;
import com.touhouqing.grabteacherbackend.config.AliyunOssProperties;
import com.touhouqing.grabteacherbackend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class AliyunOssUtil {

    private final OSS ossClient;
    private final AliyunOssProperties props;

    /**
     * 上传文件到 OSS
     * @param file Spring MultipartFile
     * @param module 可选业务模块目录，如 "avatar"，会拼在 dirPrefix 后
     * @return 可公网访问的 URL
     */
    public String upload(MultipartFile file, String module) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        String original = file.getOriginalFilename();
        String ext = (original != null && original.contains(".")) ? original.substring(original.lastIndexOf('.')) : "";
        String uuid = UUID.randomUUID().toString().replace("-", "");
        StringBuilder key = new StringBuilder();
        String prefix = props.getDirPrefix() != null && !props.getDirPrefix().isEmpty() ? props.getDirPrefix() : "uploads/";
        key.append(prefix);
        if (module != null && !module.isEmpty()) {
            key.append(module).append('/');
        }
        key.append(uuid).append(ext);

        try {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.getSize());
            String contentType = file.getContentType();
            if (contentType == null || contentType.isEmpty()) {
                contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
            }
            metadata.setContentType(contentType);
            metadata.setObjectAcl(CannedAccessControlList.PublicRead);

            ossClient.putObject(props.getBucket(), key.toString(), file.getInputStream(), metadata);

            String url = buildPublicUrl(key.toString());
            log.info("OSS 上传成功 -> {}", url);
            return url;
        } catch (IOException e) {
            log.error("上传到OSS失败", e);
            throw new BusinessException("上传到OSS失败: " + e.getMessage());
        }
    }

    /**
     * 根据完整 URL 删除 OSS 对象
     */
    public void deleteByUrl(String url) {
        if (url == null || url.isEmpty()) {
            return;
        }
        String expectedDomain = props.getPublicDomain();
        if (expectedDomain == null || expectedDomain.isEmpty()) {
            expectedDomain = buildDefaultDomain();
        }
        String key;
        if (url.startsWith(expectedDomain)) {
            key = url.substring(expectedDomain.length());
            if (key.startsWith("/")) key = key.substring(1);
            // URL 可能经过了编码，需要解码一次以获得真实 key
            key = URLDecoder.decode(key, StandardCharsets.UTF_8);
        } else if (url.startsWith("http://") || url.startsWith("https://")) {
            // 传入了其他域名的完整URL，尝试从路径部分提取key
            try {
                java.net.URI uri = java.net.URI.create(url);
                key = uri.getPath();
                if (key.startsWith("/")) key = key.substring(1);
                key = URLDecoder.decode(key, StandardCharsets.UTF_8);
            } catch (Exception e) {
                // 回退为原串
                key = url;
            }
        } else {
            // 兼容传入的是未带域名的 key
            key = url;
        }
        deleteByKey(key);
    }

    /**
     * 按 key 删除
     */
    public void deleteByKey(String key) {
        if (key == null || key.isEmpty()) return;
        log.info("删除 OSS 对象, bucket={}, key={}", props.getBucket(), key);
        ossClient.deleteObject(props.getBucket(), key);
    }

    private String buildPublicUrl(String key) {
        String domain = props.getPublicDomain();
        if (domain == null || domain.isEmpty()) {
            domain = buildDefaultDomain();
        }
        String safeKey = URLEncoder.encode(key, StandardCharsets.UTF_8).replace("+", "%20");
        return domain.endsWith("/") ? domain + safeKey : domain + "/" + safeKey;
    }

    private String buildDefaultDomain() {
        return "https://" + props.getBucket() + "." + props.getEndpoint();
    }
}
