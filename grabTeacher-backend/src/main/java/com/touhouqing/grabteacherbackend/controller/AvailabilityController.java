package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.vo.DayAvailabilityVO;
import com.touhouqing.grabteacherbackend.service.AvailabilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@Slf4j
@RestController
@RequestMapping("/api/booking/availability")
@RequiredArgsConstructor
public class AvailabilityController {

    private final AvailabilityService availabilityService;

    @GetMapping("/day")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "获取某日的可选性", description = "同时返回基础2小时段（正式课）与30分钟试听段的可用性，可按分段生成以降低响应体体积")
    @ApiResponses({})
    public ResponseEntity<CommonResult<DayAvailabilityVO>> getDayAvailability(
            @Parameter(description = "教师ID", required = true) @RequestParam @NotNull Long teacherId,
            @Parameter(description = "日期", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "时间分段: morning/afternoon/evening，可选", required = false) @RequestParam(required = false) String segment
    ) {
        DayAvailabilityVO vo = availabilityService.getDayAvailability(teacherId, date, segment);
        return ResponseEntity.ok(CommonResult.success("获取成功", vo));
    }
}

