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

import java.util.HashMap;
import java.util.Map;

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

    // 管理端：分页列表
    @Operation(summary = "招聘列表-管理端", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/list")
    public ResponseEntity<CommonResult<Map<String, Object>>> adminList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Long gradeId,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String createdStart,
            @RequestParam(required = false) String createdEnd,
            @RequestParam(required = false) Boolean includeDeleted) {
        java.time.LocalDateTime cs = (createdStart==null||createdStart.isEmpty())?null:java.time.LocalDateTime.parse(createdStart.replace(" ","T"));
        java.time.LocalDateTime ce = (createdEnd==null||createdEnd.isEmpty())?null:java.time.LocalDateTime.parse(createdEnd.replace(" ","T"));
        Page<JobPostVO> voPage = jobPostService.pageAdmin(page, size, gradeId, subjectId, status, keyword, cs, ce, includeDeleted);
        Map<String, Object> resp = new HashMap<>();
        resp.put("records", voPage.getRecords());
        resp.put("total", voPage.getTotal());
        resp.put("current", voPage.getCurrent());
        resp.put("size", voPage.getSize());
        resp.put("pages", voPage.getPages());
        return ResponseEntity.ok(CommonResult.success("获取成功", resp));
    }

    // 管理端：获取原始实体（包含冗余的 gradeIds/subjectIds 字符串）
    @Operation(summary = "招聘详情-管理端", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/{id}")
    public ResponseEntity<CommonResult<JobPost>> adminDetail(@PathVariable Long id) {
        JobPost jp = jobPostService.getAdminById(id);
        return ResponseEntity.ok(CommonResult.success("获取成功", jp));
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

    // 管理员：批量删除（逻辑删除）
    @Operation(summary = "批量删除招聘", security = @SecurityRequirement(name = "Bearer Authentication"))
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/batch-delete")
    public ResponseEntity<CommonResult<Object>> batchDelete(@RequestBody java.util.List<Long> ids) {
        jobPostService.batchDelete(ids);
        return ResponseEntity.ok(CommonResult.success("批量删除成功", null));
    }
}

