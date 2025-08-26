package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.vo.CourseEvaluationVO;
import com.touhouqing.grabteacherbackend.service.ICourseEvaluationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/course-evaluations")
@Tag(name = "公开课程评价", description = "公开分页查询课程/教师的学员评价")
public class PublicCourseEvaluationController {

    @Autowired
    private ICourseEvaluationService courseEvaluationService;

    @Operation(summary = "分页查询学员评价", description = "支持按教师、课程、最小评分筛选")
    @GetMapping
    public ResponseEntity<CommonResult<Map<String, Object>>> pageEvaluations(
            @Parameter(description = "页码，从1开始") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "教师ID") @RequestParam(required = false) Long teacherId,
            @Parameter(description = "课程ID") @RequestParam(required = false) Long courseId,
            @Parameter(description = "最低评分") @RequestParam(required = false) BigDecimal minRating
    ) {
        Page<CourseEvaluationVO> voPage = courseEvaluationService.pagePublicEvaluations(page, size, teacherId, courseId, minRating);

        Map<String, Object> data = new HashMap<>();
        data.put("records", voPage.getRecords());
        data.put("total", voPage.getTotal());
        data.put("current", voPage.getCurrent());
        data.put("size", voPage.getSize());
        data.put("pages", voPage.getPages());

        return ResponseEntity.ok(CommonResult.success("获取评价列表成功", data));
    }
}


