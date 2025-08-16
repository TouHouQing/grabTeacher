package com.touhouqing.grabteacherbackend.model.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminContactVO {
    private String realName;
    private String wechatQrcodeUrl;
    private String whatsappNumber;
    private String email;
}

