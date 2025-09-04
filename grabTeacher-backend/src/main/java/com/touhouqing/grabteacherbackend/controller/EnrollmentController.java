package com.touhouqing.grabteacherbackend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.mapper.CourseEnrollmentMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherMapper;
import com.touhouqing.grabteacherbackend.model.entity.Course;
import com.touhouqing.grabteacherbackend.model.entity.CourseEnrollment;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.model.vo.SuspendedEnrollmentVO;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/enrollments")
@Tag(name = "报名关系", description = "报名查询相关接口")
public class EnrollmentController {

    @Autowired
    private CourseEnrollmentMapper courseEnrollmentMapper;
    @Autowired
    private StudentMapper studentMapper;
    @Autowired
    private TeacherMapper teacherMapper;
    @Autowired
    private CourseMapper courseMapper;

    @GetMapping("/student/suspended")
    @PreAuthorize("hasRole('STUDENT')")
    @Operation(summary = "学生查询已停课报名", description = "返回当前学生 enrollment_status = suspended 的报名列表")
    public CommonResult<List<SuspendedEnrollmentVO>> getStudentSuspended(@AuthenticationPrincipal UserPrincipal currentUser) {
        Student student = studentMapper.findByUserId(currentUser.getId());
        if (student == null) {
            return CommonResult.error("学生信息不存在");
        }
        QueryWrapper<CourseEnrollment> qw = new QueryWrapper<>();
        qw.eq("student_id", student.getId()).eq("enrollment_status", "suspended").eq("is_deleted", false);
        List<CourseEnrollment> list = courseEnrollmentMapper.selectList(qw);
        List<SuspendedEnrollmentVO> vos = new ArrayList<>();
        for (CourseEnrollment ce : list) {
            Teacher t = ce.getTeacherId() != null ? teacherMapper.selectById(ce.getTeacherId()) : null;
            Course c = ce.getCourseId() != null ? courseMapper.selectById(ce.getCourseId()) : null;
            SuspendedEnrollmentVO vo = SuspendedEnrollmentVO.builder()
                    .id(ce.getId())
                    .studentId(ce.getStudentId())
                    .studentName(student.getRealName())
                    .teacherId(ce.getTeacherId())
                    .teacherName(t != null ? t.getRealName() : null)
                    .courseId(ce.getCourseId())
                    .courseTitle(c != null ? c.getTitle() : null)
                    .subjectName(c != null ? null : null)
                    .totalTimes(ce.getTotalSessions())
                    .completedTimes(ce.getCompletedSessions())
                    .build();
            vos.add(vo);
        }
        return CommonResult.success("获取成功", vos);
    }
} 