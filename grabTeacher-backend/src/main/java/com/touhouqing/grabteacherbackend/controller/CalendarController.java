package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.model.vo.MonthlyCalendarVO;
import com.touhouqing.grabteacherbackend.service.CalendarService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/calendar")
@RequiredArgsConstructor
@Tag(name = "Calendar API", description = "Monthly status for teacher/day/base slots")
public class CalendarController {

    private final CalendarService calendarService;

    @GetMapping("/teacher/{teacherId}/month")
    @Operation(summary = "Get monthly base-slot statuses", description = "Return 6-slot status per day for a month")
    @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = MonthlyCalendarVO.class)))
    public ResponseEntity<MonthlyCalendarVO> getMonthly(
            @PathVariable("teacherId") Long teacherId,
            @RequestParam("year") int year,
            @RequestParam("month") int month
    ) {
        log.info("Query monthly calendar: teacherId={}, year={}, month={}", teacherId, year, month);
        MonthlyCalendarVO vo = calendarService.getTeacherMonthlyCalendar(teacherId, year, month);
        return ResponseEntity.ok(vo);
    }
}

