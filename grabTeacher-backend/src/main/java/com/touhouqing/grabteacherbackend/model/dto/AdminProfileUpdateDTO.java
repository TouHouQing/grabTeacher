package com.touhouqing.grabteacherbackend.model.dto;

import lombok.Data;

@Data
public class AdminProfileUpdateDTO {
    private String realName;
    private String whatsappNumber;
    private String email;
    // 头像与微信二维码均为 URL（前端直传 OSS 后拿到的最终 URL）
    private String avatarUrl;
    private String wechatQrcodeUrl;
}

