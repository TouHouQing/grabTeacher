package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.entity.Admin;
import com.touhouqing.grabteacherbackend.mapper.AdminMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "公开-管理员信息", description = "公开获取管理员联系方式")
public class PublicAdminController {

    private final AdminMapper adminMapper;

    public PublicAdminController(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @GetMapping("/admins/contacts")
    @Operation(summary = "获取管理员联系方式（公开）")
    public CommonResult<Map<String, Object>> getAdminContacts() {
        // 取第一条未删除的管理员记录作为展示（如有多位管理员，后续可扩展为列表）
        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>()
                .eq("is_deleted", false)
                .last("limit 1"));
        Map<String, Object> data = new HashMap<>();
        if (admin != null) {
            data.put("realName", admin.getRealName());
            data.put("wechatQrcodeUrl", admin.getWechatQrcodeUrl());
            data.put("whatsappNumber", admin.getWhatsappNumber());
            data.put("email", admin.getEmail());
        }
        return CommonResult.success("ok", data);
    }
}

