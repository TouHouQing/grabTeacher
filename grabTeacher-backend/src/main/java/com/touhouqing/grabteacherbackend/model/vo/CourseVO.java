package com.touhouqing.grabteacherbackend.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "课程响应数据")
public class CourseVO {

    @Schema(description = "课程ID", example = "1")
    private Long id;

    @Schema(description = "授课教师ID", example = "1")
    private Long teacherId;

    @Schema(description = "教师姓名", example = "张老师")
    private String teacherName;

    @Schema(description = "教师级别", example = "金牌", allowableValues = {"王牌", "金牌", "银牌", "铜牌"})
    private String teacherLevel;

    @Schema(description = "课程科目ID", example = "1")
    private Long subjectId;

    @Schema(description = "科目名称", example = "数学")
    private String subjectName;

    @Schema(description = "课程标题", example = "高中数学 - 函数与导数专题")
    private String title;

    @Schema(description = "课程详细描述", example = "本课程深入讲解高中数学中的函数与导数知识点，适合高二、高三学生")
    private String description;

    @Schema(description = "课程类型", example = "one_on_one", allowableValues = {"one_on_one", "large_class"})
    private String courseType;

    @Schema(description = "课程类型显示名称", example = "一对一")
    private String courseTypeDisplay;

    @Schema(description = "单次课程时长（分钟）", example = "120")
    private Integer durationMinutes;

    @Schema(description = "课程状态", example = "pending", allowableValues = {"active", "inactive", "full", "pending"})
    private String status;

    @Schema(description = "课程状态显示名称", example = "可报名")
    private String statusDisplay;

    @Schema(description = "是否为精选课程", example = "false")
    private Boolean featured;

    @Schema(description = "课程创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "课程地点", example = "线上", allowableValues = {"线上", "线下"})
    private String courseLocation;

    @Schema(description = "是否支持线上（仅大班课）", example = "true")
    private Boolean supportsOnline;

    @Schema(description = "线下地点ID（仅大班课）", example = "184")
    private Long offlineLocationId;

    @Schema(description = "课程价格（每小时）", example = "299.00")
    private BigDecimal price;

    @Schema(description = "教师时薪（1人报名时的基准），单位：M豆/小时；用于结算（大班课每+1人，时薪+5）")
    private BigDecimal teacherHourlyRate;


    @Schema(description = "开始日期（大班课专用）", example = "2024-01-15")
    private LocalDate startDate;

    @Schema(description = "结束日期（大班课专用）", example = "2024-03-15")
    private LocalDate endDate;

    @Schema(description = "人数限制（大班课专用）", example = "30")
    private Integer personLimit;

    @Schema(description = "当前报名人数", example = "0")
    private Integer enrollmentCount;


    @Schema(description = "课程封面图URL")
    private String imageUrl;


    @Schema(description = "每周上课时间安排（仅大班课），用于管理端编辑回显")
    private java.util.List<com.touhouqing.grabteacherbackend.model.dto.TimeSlotDTO> courseTimeSlots;

    @Schema(description = "课程时间显示（组合：日期区间 + 每周 + 时间段）", example = "2025.10.01-2025.12.31，每周一三五，7pm-9pm")
    private String scheduleDisplay;

    @Schema(description = "当前登录学生是否已报名该课程，未登录或非学生时为空/false")
    private Boolean enrolled;

    // 辅助方法：获取课程类型显示名称
    public String getCourseTypeDisplay() {
        if (courseType == null) return "";
        switch (courseType) {
            case "one_on_one":
                return "一对一";
            case "large_class":
                return "大班课";
            default:
                return courseType;
        }
    }

    // 辅助方法：获取状态显示名称
    public String getStatusDisplay() {
        if (status == null) return "";
        switch (status) {
            case "active":
                return "可报名";
            case "inactive":
                return "已下架";
            case "full":
                return "已满员";
            case "pending":
                return "待审批";
            default:
                return status;
        }
    }
}
