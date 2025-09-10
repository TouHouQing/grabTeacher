package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.entity.TeachingLocation;
import com.touhouqing.grabteacherbackend.service.TeachingLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/teaching-locations")
@Tag(name = "公开授课地点接口", description = "供所有用户访问的授课地点相关接口")
public class PublicTeachingLocationController {

    @Autowired
    private TeachingLocationService teachingLocationService;

    /**
     * 获取所有激活的授课地点列表（公开接口）
     */
    @Operation(summary = "获取激活授课地点列表", description = "获取所有状态为激活的授课地点列表，按sort_order升序、id降序")
    @GetMapping("/active")
    public ResponseEntity<CommonResult<List<TeachingLocation>>> getActiveTeachingLocations() {
        try {
            List<TeachingLocation> list = teachingLocationService.getAllActiveLocations();
            return ResponseEntity.ok(CommonResult.success("获取成功", list));
        } catch (Exception e) {
            return ResponseEntity.status(500)
                    .body(CommonResult.error("获取授课地点列表失败"));
        }
    }
}

