package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.dto.SubjectDTO;
import com.touhouqing.grabteacherbackend.model.entity.BookingRequest;
import com.touhouqing.grabteacherbackend.model.entity.Course;
import com.touhouqing.grabteacherbackend.model.entity.Schedule;
import com.touhouqing.grabteacherbackend.model.entity.Subject;
import com.touhouqing.grabteacherbackend.mapper.BookingRequestMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseGradeMapper;
import com.touhouqing.grabteacherbackend.mapper.CourseMapper;
import com.touhouqing.grabteacherbackend.mapper.ScheduleMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.SubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.TeacherSubjectMapper;
import com.touhouqing.grabteacherbackend.mapper.JobPostSubjectMapper;
import com.touhouqing.grabteacherbackend.service.SubjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.context.ApplicationEventPublisher;

import com.touhouqing.grabteacherbackend.event.SubjectChangedEvent;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubjectServiceImpl implements SubjectService {

    private final SubjectMapper subjectMapper;
    private final CourseMapper courseMapper;
    private final CourseGradeMapper courseGradeMapper;
    private final StudentSubjectMapper studentSubjectMapper;
    private final TeacherSubjectMapper teacherSubjectMapper;
    private final BookingRequestMapper bookingRequestMapper;
    private final ApplicationEventPublisher eventPublisher;

    private final ScheduleMapper scheduleMapper;
    private final JobPostSubjectMapper jobPostSubjectMapper;

    /**
     * 创建科目
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"activeSubjects", "subjects"}, allEntries = true)
    public Subject createSubject(SubjectDTO request) {
        // 检查科目名称是否已存在
        QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("name", request.getName());
        queryWrapper.eq("is_deleted", false);
        Subject existingSubject = subjectMapper.selectOne(queryWrapper);

        if (existingSubject != null) {
            throw new RuntimeException("科目名称已存在");
        }

        Subject subject = Subject.builder()
                .name(request.getName())
                .iconUrl(request.getIconUrl())
                .active(request.getActive() != null ? request.getActive() : true)
                .deleted(false)
                .build();

        subjectMapper.insert(subject);
        log.info("创建科目成功: {}", subject.getName());
        try { eventPublisher.publishEvent(new SubjectChangedEvent(this, SubjectChangedEvent.ChangeType.CREATE)); } catch (Exception ignore) {}
        return subject;
    }

    /**
     * 更新科目
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"activeSubjects", "subjects"}, allEntries = true)
    public Subject updateSubject(Long id, SubjectDTO request) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null || subject.getDeleted()) {
            throw new RuntimeException("科目不存在");
        }

        // 检查科目名称是否与其他科目重复
        if (!subject.getName().equals(request.getName())) {
            QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("name", request.getName());
            queryWrapper.eq("is_deleted", false);
            queryWrapper.ne("id", id);
            Subject existingSubject = subjectMapper.selectOne(queryWrapper);

            if (existingSubject != null) {
                throw new RuntimeException("科目名称已存在");
            }
        }

        subject.setName(request.getName());
        subject.setIconUrl(request.getIconUrl());
        subject.setActive(request.getActive() != null ? request.getActive() : true);

        subjectMapper.updateById(subject);
        log.info("更新科目成功: {}", subject.getName());
        try { eventPublisher.publishEvent(new SubjectChangedEvent(this, SubjectChangedEvent.ChangeType.UPDATE)); } catch (Exception ignore) {}
        return subject;
    }

    /**
     * 删除科目（软删除）
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"activeSubjects", "subjects"}, allEntries = true)
    public void deleteSubject(Long id) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null || subject.getDeleted()) {
            throw new RuntimeException("科目不存在");
        }

        // 0. 保护性检查：是否被教师招聘引用（未删除）
        long jobCount = jobPostSubjectMapper.countActiveJobPostsBySubjectId(id);
        if (jobCount > 0) {
            throw new RuntimeException("该科目存在未删除的教师招聘信息，无法删除");
        }

        // 1. 查询该科目下的所有课程
        List<Course> courses = courseMapper.findAllBySubjectId(id);
        log.info("找到科目 {} 下的课程数量: {}", subject.getName(), courses.size());

        // 2. 删除该科目下的所有课程（软删除）
        for (Course course : courses) {
            if (!course.getDeleted()) {
                // 2.1 处理该课程的预约申请（软删除）
                List<BookingRequest> bookingRequests = bookingRequestMapper.findByCourseId(course.getId());
                for (BookingRequest bookingRequest : bookingRequests) {
                    if (!bookingRequest.getDeleted()) {
                        bookingRequest.setDeleted(true);
                        bookingRequest.setDeletedAt(LocalDateTime.now());
                        bookingRequestMapper.updateById(bookingRequest);
                    }
                }
                log.info("删除课程 {} 的 {} 个预约申请", course.getTitle(), bookingRequests.size());

                // 2.2 处理该课程的课程安排（软删除）
                List<Schedule> schedules = scheduleMapper.findByCourseId(course.getId());
                for (Schedule schedule : schedules) {
                    if (!schedule.getDeleted()) {
                        schedule.setDeleted(true);
                        schedule.setDeletedAt(LocalDateTime.now());
                        scheduleMapper.updateById(schedule);
                    }
                }
                log.info("删除课程 {} 的 {} 个课程安排", course.getTitle(), schedules.size());

                // 2.3 删除课程年级关联
                courseGradeMapper.deleteByCourseId(course.getId());

                // 2.4 最后删除课程本身（软删除）
                course.setDeleted(true);
                course.setDeletedAt(LocalDateTime.now());
                courseMapper.updateById(course);
                log.info("删除课程: {}", course.getTitle());
            }
        }

        // 3. 删除学生科目关联
        studentSubjectMapper.deleteBySubjectId(id);
        log.info("删除科目 {} 的学生关联", subject.getName());

        // 4. 删除教师科目关联
        teacherSubjectMapper.deleteBySubjectId(id);
        log.info("删除科目 {} 的教师关联", subject.getName());

        // 5. 最后删除科目本身（软删除）
        subject.setDeleted(true);
        subject.setDeletedAt(LocalDateTime.now());
        subjectMapper.updateById(subject);
        log.info("删除科目成功: {}，同时删除了 {} 个相关课程", subject.getName(), courses.size());
        try { eventPublisher.publishEvent(new SubjectChangedEvent(this, SubjectChangedEvent.ChangeType.DELETE)); } catch (Exception ignore) {}
    }

    /**
     * 根据ID获取科目
     */
    @Override
    @Cacheable(cacheNames = "subjects", key = "#id", unless = "#result == null")
    public Subject getSubjectById(Long id) {
        QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", id);
        queryWrapper.eq("is_deleted", false);
        return subjectMapper.selectOne(queryWrapper);
    }

    /**
     * 获取科目列表（分页）
     */
    @Override
    public Page<Subject> getSubjectList(int page, int size, String keyword, Boolean isActive) {
        Page<Subject> pageParam = new Page<>(page, size);
        QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();

        queryWrapper.eq("is_deleted", false);

        if (StringUtils.hasText(keyword)) {
            queryWrapper.like("name", keyword);
        }

        if (isActive != null) {
            queryWrapper.eq("is_active", isActive);
        }

        queryWrapper.orderByDesc("id");

        return subjectMapper.selectPage(pageParam, queryWrapper);
    }

    /**
     * 获取所有激活的科目
     */
    @Override
    @Cacheable(cacheNames = "activeSubjects", key = "'all'")
    public List<Subject> getAllActiveSubjects() {
        QueryWrapper<Subject> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("is_deleted", false);
        queryWrapper.eq("is_active", true);
        queryWrapper.orderByDesc("id");
        return subjectMapper.selectList(queryWrapper);
    }

    /**
     * 更新科目状态
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = {"activeSubjects", "subjects"}, allEntries = true)
    public void updateSubjectStatus(Long id, Boolean isActive) {
        Subject subject = subjectMapper.selectById(id);
        if (subject == null || subject.getDeleted()) {
            throw new RuntimeException("科目不存在");
        }

        subject.setActive(isActive);
        subjectMapper.updateById(subject);
        log.info("更新科目状态成功: {}, status: {}", subject.getName(), isActive);
        try { eventPublisher.publishEvent(new SubjectChangedEvent(this, SubjectChangedEvent.ChangeType.UPDATE)); } catch (Exception ignore) {}
    }
}