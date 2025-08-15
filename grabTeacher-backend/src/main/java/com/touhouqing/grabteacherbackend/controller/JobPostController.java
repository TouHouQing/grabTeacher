package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.JobPostDTO;
import com.touhouqing.grabteacherbackend.model.entity.JobPost;
import com.touhouqing.grabteacherbackend.model.vo.JobPostVO;
import com.touhouqing.grabteacherbackend.service.JobPostService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/job-posts")
@Tag(name = "教师招聘")
public class JobPostController {

    @Autowired
    private JobPostService jobPostService;

    // 公共列表
    @Operation(summary = "招聘列表-公共")
    @GetMapping("/public/list")
    public ResponseEntity<CommonResult<Page<JobPostVO>>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId) {
        Page<JobPostVO> result = jobPostService.pageActive(page, size, gradeId, subjectId);
        return ResponseEntity.ok(CommonResult.success("获取成功", result));
    }

    // 获取详情-公共
    @Operation(summary = "招聘详情-公共")
    @GetMapping("/public/{id}")
    public ResponseEntity<CommonResult<JobPostVO>> detail(@PathVariable Long id) {
        JobPostVO vo = jobPostService.getById(id);
        return ResponseEntity.ok(CommonResult.success("获取成功", vo));
    }

    // 公共详情 - 预序列化JSON（极热Key优化）
    @Operation(summary = "招聘详情-公共-预序列化JSON")
    @GetMapping("/public/{id}/fast")
    public ResponseEntity<String> detailFast(@PathVariable Long id) {
        String json = jobPostService.getDetailJson(id);
        return ResponseEntity.ok(json == null ? "null" : json);
    }

    // 管理员：创建
    @Operation(summary = "创建招聘", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CommonResult<JobPost>> create(@RequestBody JobPostDTO dto) {
        JobPost created = jobPostService.create(dto);
        return ResponseEntity.ok(CommonResult.success("创建成功", created));
    }

    // 管理员：更新
    @Operation(summary = "更新招聘", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<CommonResult<JobPost>> update(@PathVariable Long id, @RequestBody JobPostDTO dto) {
        JobPost updated = jobPostService.update(id, dto);
        return ResponseEntity.ok(CommonResult.success("更新成功", updated));
    }

    // 管理员：删除（逻辑删除）
    @Operation(summary = "删除招聘", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResult<Object>> delete(@PathVariable Long id) {
        jobPostService.delete(id);
        return ResponseEntity.ok(CommonResult.success("删除成功", null));
    }
}

