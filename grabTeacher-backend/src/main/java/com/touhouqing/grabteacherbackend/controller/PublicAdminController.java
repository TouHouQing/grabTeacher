package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.entity.Admin;
import com.touhouqing.grabteacherbackend.mapper.AdminMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*", maxAge = 3600)
@Tag(name = "公开-管理员信息", description = "公开获取管理员联系方式")
public class PublicAdminController {

    private final AdminMapper adminMapper;

    public PublicAdminController(AdminMapper adminMapper) {
        this.adminMapper = adminMapper;
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.cache.CacheManager cacheManager;

    @GetMapping("/admins/contacts")
    @Operation(summary = "获取管理员联系方式（公开）")
    public CommonResult<com.touhouqing.grabteacherbackend.model.vo.AdminContactVO> getAdminContacts() {
        String cacheKey = "first";
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache("public:adminContacts");
            if (cache != null) {
                com.touhouqing.grabteacherbackend.model.vo.AdminContactVO cached = cache.get(cacheKey, com.touhouqing.grabteacherbackend.model.vo.AdminContactVO.class);
                if (cached != null) {
                    return CommonResult.success("ok", cached);
                }
            }
        } catch (Exception ignore) {}

        Admin admin = adminMapper.selectOne(new QueryWrapper<Admin>()
                .eq("is_deleted", false)
                .last("limit 1"));
        com.touhouqing.grabteacherbackend.model.vo.AdminContactVO data = null;
        if (admin != null) {
            data = com.touhouqing.grabteacherbackend.model.vo.AdminContactVO.builder()
                    .realName(admin.getRealName())
                    .wechatQrcodeUrl(admin.getWechatQrcodeUrl())
                    .whatsappNumber(admin.getWhatsappNumber())
                    .email(admin.getEmail())
                    .build();
        } else {
            data = com.touhouqing.grabteacherbackend.model.vo.AdminContactVO.builder().build();
        }
        try {
            org.springframework.cache.Cache cache = cacheManager.getCache("public:adminContacts");
            if (cache != null) cache.put(cacheKey, data);
        } catch (Exception ignore) {}
        return CommonResult.success("ok", data);
    }
}

