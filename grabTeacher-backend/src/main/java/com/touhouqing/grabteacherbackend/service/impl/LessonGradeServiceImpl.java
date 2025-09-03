package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.exception.BusinessException;
import com.touhouqing.grabteacherbackend.mapper.*;
import com.touhouqing.grabteacherbackend.model.dto.LessonGradeCreateDTO;
import com.touhouqing.grabteacherbackend.model.dto.LessonGradeQueryDTO;
import com.touhouqing.grabteacherbackend.model.dto.LessonGradeUpdateDTO;
import com.touhouqing.grabteacherbackend.model.entity.CourseEnrollment;
import com.touhouqing.grabteacherbackend.model.entity.CourseSchedule;
import com.touhouqing.grabteacherbackend.model.entity.LessonGrade;
import com.touhouqing.grabteacherbackend.model.entity.Teacher;
import com.touhouqing.grabteacherbackend.model.vo.LessonGradeVO;
import com.touhouqing.grabteacherbackend.service.LessonGradeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class LessonGradeServiceImpl implements LessonGradeService {

    private final LessonGradeMapper lessonGradeMapper;
    private final TeacherMapper teacherMapper;
    private final CourseScheduleMapper courseScheduleMapper;
    private final CourseEnrollmentMapper courseEnrollmentMapper;

    @Override
    @Transactional
    public LessonGrade createGrade(Long userId, LessonGradeCreateDTO request) {
        log.info("教师录入成绩: userId={}, scheduleId={}, studentId={}", 
                userId, request.getScheduleId(), request.getStudentId());

        // 检查权限
        if (!hasPermissionToGrade(userId, request.getScheduleId())) {
            throw new BusinessException("您没有权限为该课程录入成绩");
        }

        // 检查是否已经有成绩记录
        LessonGrade existingGrade = lessonGradeMapper.selectByScheduleIdAndStudentId(
                request.getScheduleId(), request.getStudentId());
        if (existingGrade != null) {
            throw new BusinessException("该课程已有成绩记录，请使用更新功能");
        }

        // 创建成绩记录
        LessonGrade lessonGrade = LessonGrade.builder()
                .scheduleId(request.getScheduleId())
                .studentId(request.getStudentId())
                .score(request.getScore())
                .teacherComment(request.getTeacherComment())
                .gradedAt(LocalDateTime.now())
                .build();

        lessonGradeMapper.insert(lessonGrade);
        log.info("成绩录入成功: gradeId={}", lessonGrade.getId());

        return lessonGrade;
    }

    @Override
    @Transactional
    public LessonGrade updateGrade(Long userId, LessonGradeUpdateDTO request) {
        log.info("教师更新成绩: userId={}, gradeId={}", userId, request.getId());

        // 查询成绩记录
        LessonGrade existingGrade = lessonGradeMapper.selectById(request.getId());
        if (existingGrade == null || existingGrade.getDeleted()) {
            throw new BusinessException("成绩记录不存在");
        }

        // 检查权限
        if (!hasPermissionToGrade(userId, existingGrade.getScheduleId())) {
            throw new BusinessException("您没有权限修改该成绩");
        }

        // 更新成绩记录
        existingGrade.setScore(request.getScore());
        existingGrade.setTeacherComment(request.getTeacherComment());
        existingGrade.setUpdatedAt(LocalDateTime.now());

        lessonGradeMapper.updateById(existingGrade);
        log.info("成绩更新成功: gradeId={}", existingGrade.getId());

        return existingGrade;
    }

    @Override
    @Transactional
    public void deleteGrade(Long userId, Long gradeId) {
        log.info("教师删除成绩: userId={}, gradeId={}", userId, gradeId);

        // 查询成绩记录
        LessonGrade existingGrade = lessonGradeMapper.selectById(gradeId);
        if (existingGrade == null || existingGrade.getDeleted()) {
            throw new BusinessException("成绩记录不存在");
        }

        // 检查权限
        if (!hasPermissionToGrade(userId, existingGrade.getScheduleId())) {
            throw new BusinessException("您没有权限删除该成绩");
        }

        // 软删除
        existingGrade.setDeleted(true);
        existingGrade.setDeletedAt(LocalDateTime.now());
        existingGrade.setUpdatedAt(LocalDateTime.now());

        lessonGradeMapper.updateById(existingGrade);
        log.info("成绩删除成功: gradeId={}", gradeId);
    }

    @Override
    public LessonGrade getGradeByScheduleAndStudent(Long scheduleId, Long studentId) {
        return lessonGradeMapper.selectByScheduleIdAndStudentId(scheduleId, studentId);
    }

    @Override
    public IPage<LessonGradeVO> getGradesByStudent(LessonGradeQueryDTO request) {
        log.info("查询学生成绩: studentId={}", request.getStudentId());

        Page<LessonGradeVO> page = new Page<>(request.getPage(), request.getSize());
        return lessonGradeMapper.selectGradesByStudentId(
                page, 
                request.getStudentId(),
                request.getSubjectName(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    @Override
    public IPage<LessonGradeVO> getGradesByTeacher(Long userId, LessonGradeQueryDTO request) {
        log.info("查询教师录入的成绩: userId={}", userId);

        // 获取教师信息
        Teacher teacher = getTeacherByUserId(userId);
        
        Page<LessonGradeVO> page = new Page<>(request.getPage(), request.getSize());
        return lessonGradeMapper.selectGradesByTeacherId(
                page,
                teacher.getId(),
                null, // studentName - 后续可从request中获取
                request.getSubjectName(),
                request.getStartDate(),
                request.getEndDate()
        );
    }

    @Override
    public List<LessonGradeVO> getGradesByStudentAndSubject(Long studentId, String subjectName) {
        log.info("查询学生某科目的所有成绩: studentId={}, subjectName={}", studentId, subjectName);
        return lessonGradeMapper.selectGradesByStudentIdAndSubject(studentId, subjectName);
    }

    @Override
    public List<LessonGradeVO> getGradesByStudentAndDateRange(Long studentId, String startDate, String endDate) {
        log.info("查询学生指定日期范围内的成绩: studentId={}, startDate={}, endDate={}", 
                studentId, startDate, endDate);

        LocalDate start = startDate != null ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null ? LocalDate.parse(endDate) : null;

        return lessonGradeMapper.selectGradesByStudentIdAndDateRange(studentId, start, end);
    }

    @Override
    public Long getGradeCount(Long studentId, Long teacherId) {
        if (studentId != null) {
            return lessonGradeMapper.countGradesByStudentId(studentId);
        } else if (teacherId != null) {
            return lessonGradeMapper.countGradesByTeacherId(teacherId);
        }
        return 0L;
    }

    @Override
    public boolean hasPermissionToGrade(Long userId, Long scheduleId) {
        // 获取教师信息
        Teacher teacher = getTeacherByUserId(userId);
        
        // 获取课程安排信息（新表）
        CourseSchedule schedule = courseScheduleMapper.findById(scheduleId);
        if (schedule == null || Boolean.TRUE.equals(schedule.getDeleted())) {
            log.warn("课程安排不存在: scheduleId={}", scheduleId);
            return false;
        }

        // 检查是否是该课程的授课教师：通过报名关系反查
        CourseEnrollment enrollment = courseEnrollmentMapper.selectById(schedule.getEnrollmentId());
        boolean hasPermission = enrollment != null && teacher.getId().equals(enrollment.getTeacherId());
        log.debug("权限检查结果: userId={}, teacherId={}, scheduleTeacherId={}, hasPermission={}", 
                userId, teacher.getId(), enrollment != null ? enrollment.getTeacherId() : null, hasPermission);
        
        return hasPermission;
    }

    /**
     * 根据用户ID获取教师信息
     */
    private Teacher getTeacherByUserId(Long userId) {
        LambdaQueryWrapper<Teacher> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Teacher::getUserId, userId)
                    .eq(Teacher::getDeleted, false);
        
        Teacher teacher = teacherMapper.selectOne(queryWrapper);
        if (teacher == null) {
            throw new BusinessException("教师信息不存在");
        }
        
        return teacher;
    }
}
