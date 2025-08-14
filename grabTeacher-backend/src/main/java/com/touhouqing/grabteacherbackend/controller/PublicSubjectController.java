package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.dto.ApiResponseDTO;
import com.touhouqing.grabteacherbackend.entity.Subject;
import com.touhouqing.grabteacherbackend.service.SubjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/subjects")
@Tag(name = "公共科目接口", description = "供所有用户访问的科目相关接口")
public class PublicSubjectController {

    private static final Logger logger = LoggerFactory.getLogger(PublicSubjectController.class);

    @Autowired
    private SubjectService subjectService;

    /**
     * 获取所有激活的科目列表
     */
    @Operation(summary = "获取激活科目列表", description = "获取所有状态为激活的科目列表，供课程创建等功能使用")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功")
    })
    @GetMapping("/active")
    public ResponseEntity<ApiResponseDTO<List<Subject>>> getAllActiveSubjects() {
        try {
            List<Subject> subjects = subjectService.getAllActiveSubjects();
            return ResponseEntity.ok(ApiResponseDTO.success("获取激活科目成功", subjects));
        } catch (Exception e) {
            logger.error("获取激活科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败，请稍后重试"));
        }
    }

    /**
     * 根据ID获取科目详情
     */
    @Operation(summary = "获取科目详情", description = "根据科目ID获取科目详细信息")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "获取成功"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "科目不存在")
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Subject>> getSubjectById(@PathVariable Long id) {
        try {
            Subject subject = subjectService.getSubjectById(id);
            if (subject == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponseDTO.error("科目不存在"));
            }
            return ResponseEntity.ok(ApiResponseDTO.success("获取科目成功", subject));
        } catch (Exception e) {
            logger.error("获取科目异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponseDTO.error("获取失败，请稍后重试"));
        }
    }
}
