package com.touhouqing.grabteacherbackend.controller;

import com.aliyun.oss.HttpMethod;
import com.aliyun.oss.OSS;
import com.aliyun.oss.model.CompleteMultipartUploadRequest;
import com.aliyun.oss.model.GeneratePresignedUrlRequest;
import com.aliyun.oss.model.InitiateMultipartUploadRequest;
import com.aliyun.oss.model.InitiateMultipartUploadResult;
import com.aliyun.oss.model.PartETag;
import com.aliyun.oss.model.CompleteMultipartUploadResult;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.config.AliyunOssProperties;
import com.touhouqing.grabteacherbackend.util.AliyunOssUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URL;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Tag(name = "文件上传")
@RestController
@RequestMapping("/api/file")
@RequiredArgsConstructor
public class FileController {

    private final AliyunOssUtil ossUtil;
    private final OSS ossClient;
    private final AliyunOssProperties props;

    @Operation(summary = "上传文件到阿里云OSS（服务端直传，保留；推荐使用预签名直传）")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("isAuthenticated()")
    public CommonResult<String> upload(@RequestPart("file") MultipartFile file,
                                       @RequestParam(value = "module", required = false) String module) {
        String url = ossUtil.upload(file, module);
        return CommonResult.success(url);
    }

    @Operation(summary = "删除阿里云OSS文件（支持传 URL 或 key）")
    @DeleteMapping("/delete")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Void> delete(@RequestParam("target") String target) {
        ossUtil.deleteByUrl(target);
        return CommonResult.success(null);
    }

    @Operation(summary = "获取单次直传的预签名URL（PUT）")
    @GetMapping("/presign")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<String> presign(@RequestParam("module") String module,
                                        @RequestParam("filename") String filename,
                                        @RequestParam("contentType") String contentType,
                                        @RequestParam(value = "ttlSeconds", required = false, defaultValue = "300") int ttlSeconds) {
        String key = buildObjectKey(module, filename);
        Date expiration = new Date(System.currentTimeMillis() + Math.max(60, ttlSeconds) * 1000L);
        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(props.getBucket(), key, HttpMethod.PUT);
        req.setExpiration(expiration);
        req.setContentType(contentType);
        URL signedUrl = ossClient.generatePresignedUrl(req);
        return CommonResult.success(signedUrl.toString());
    }

    // =================== 分片上传流程（大文件） ===================
    @Operation(summary = "初始化分片上传，返回 uploadId 与 key")
    @PostMapping("/multipart/init")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<InitMultipartResp> initMultipart(@RequestParam("module") String module,
                                                         @RequestParam("filename") String filename) {
        String key = buildObjectKey(module, filename);
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(props.getBucket(), key);
        InitiateMultipartUploadResult result = ossClient.initiateMultipartUpload(request);
        InitMultipartResp resp = new InitMultipartResp();
        resp.setUploadId(result.getUploadId());
        resp.setKey(key);
        return CommonResult.success(resp);
    }

    @Operation(summary = "获取某个分片的预签名URL（PUT part）")
    @GetMapping("/multipart/presign")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<String> presignPart(@RequestParam("key") String key,
                                            @RequestParam("uploadId") String uploadId,
                                            @RequestParam("partNumber") int partNumber,
                                            @RequestParam("contentType") String contentType,
                                            @RequestParam(value = "ttlSeconds", required = false, defaultValue = "300") int ttlSeconds) {
        Date expiration = new Date(System.currentTimeMillis() + Math.max(60, ttlSeconds) * 1000L);
        GeneratePresignedUrlRequest req = new GeneratePresignedUrlRequest(props.getBucket(), key, HttpMethod.PUT);
        req.setExpiration(expiration);
        req.setContentType(contentType);
        req.addQueryParameter("uploadId", uploadId);
        req.addQueryParameter("partNumber", String.valueOf(partNumber));
        URL signedUrl = ossClient.generatePresignedUrl(req);
        return CommonResult.success(signedUrl.toString());
    }

    @Operation(summary = "完成分片上传（合并所有分片）")
    @PostMapping("/multipart/complete")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<String> completeMultipart(@RequestBody CompleteReq body) {
        CompleteMultipartUploadRequest completeReq = new CompleteMultipartUploadRequest(
                props.getBucket(), body.getKey(), body.getUploadId(), body.getParts());
        CompleteMultipartUploadResult result = ossClient.completeMultipartUpload(completeReq);
        // 返回最终可访问URL
        String finalUrl = (props.getPublicDomain() != null && !props.getPublicDomain().isEmpty())
                ? (props.getPublicDomain().endsWith("/") ? props.getPublicDomain() + body.getKey() : props.getPublicDomain() + "/" + body.getKey())
                : ("https://" + props.getBucket() + "." + props.getEndpoint() + "/" + body.getKey());
        return CommonResult.success(finalUrl);
    }

    @Operation(summary = "中止分片上传（取消）")
    @PostMapping("/multipart/abort")
    @PreAuthorize("isAuthenticated()")
    public CommonResult<Void> abortMultipart(@RequestParam("key") String key,
                                             @RequestParam("uploadId") String uploadId) {
        ossClient.abortMultipartUpload(new com.aliyun.oss.model.AbortMultipartUploadRequest(props.getBucket(), key, uploadId));
        return CommonResult.success(null);
    }

    private String buildObjectKey(String module, String filename) {
        String ext = "";
        int dot = filename.lastIndexOf('.');
        if (dot >= 0) ext = filename.substring(dot);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String dirPrefix = props.getDirPrefix() != null && !props.getDirPrefix().isEmpty() ? props.getDirPrefix() : "uploads/";
        StringBuilder sb = new StringBuilder();
        sb.append(dirPrefix);
        if (module != null && !module.isEmpty()) sb.append(module).append('/');
        return sb.append(uuid).append(ext).toString();
    }

    @Data
    public static class InitMultipartResp {
        private String uploadId;
        private String key;
    }

    @Data
    public static class CompleteReq {
        private String key;
        private String uploadId;
        private List<PartETag> parts; // 需要按 partNumber 升序
    }
}

