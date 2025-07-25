package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.entity.StudentSubject;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import com.touhouqing.grabteacherbackend.mapper.StudentSubjectMapper;
import com.touhouqing.grabteacherbackend.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentMapper studentMapper;
    private final StudentSubjectMapper studentSubjectMapper;

    /**
     * 根据用户ID获取学生信息
     */
    @Override
    public Student getStudentByUserId(Long userId) {
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_deleted", false);
        return studentMapper.selectOne(queryWrapper);
    }

    /**
     * 更新学生信息
     */
    @Override
    @Transactional
    public Student updateStudentInfo(Long userId, StudentInfoRequest request) {
        Student student = getStudentByUserId(userId);
        if (student == null) {
            throw new RuntimeException("学生信息不存在");
        }

        // 更新学生信息
        if (request.getRealName() != null) {
            student.setRealName(request.getRealName());
        }
        if (request.getGradeLevel() != null) {
            student.setGradeLevel(request.getGradeLevel());
        }
        if (request.getSubjectsInterested() != null) {
            student.setSubjectsInterested(request.getSubjectsInterested());
        }
        if (request.getLearningGoals() != null) {
            student.setLearningGoals(request.getLearningGoals());
        }
        if (request.getPreferredTeachingStyle() != null) {
            student.setPreferredTeachingStyle(request.getPreferredTeachingStyle());
        }
        if (request.getBudgetRange() != null) {
            student.setBudgetRange(request.getBudgetRange());
        }
        if (request.getGender() != null) {
            student.setGender(request.getGender());
        }

        studentMapper.updateById(student);

        // 更新学生感兴趣的科目关联
        if (request.getSubjectIds() != null) {
            // 先删除现有的科目关联
            studentSubjectMapper.deleteByStudentId(student.getId());

            // 添加新的科目关联
            if (!request.getSubjectIds().isEmpty()) {
                for (Long subjectId : request.getSubjectIds()) {
                    StudentSubject studentSubject = new StudentSubject(student.getId(), subjectId);
                    studentSubjectMapper.insert(studentSubject);
                }
            }
        }

        log.info("更新学生信息成功: userId={}", userId);

        return student;
    }
}