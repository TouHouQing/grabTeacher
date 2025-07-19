package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.touhouqing.grabteacherbackend.dto.StudentInfoRequest;
import com.touhouqing.grabteacherbackend.entity.Student;
import com.touhouqing.grabteacherbackend.mapper.StudentMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {

    private static final Logger logger = LoggerFactory.getLogger(StudentService.class);

    @Autowired
    private StudentMapper studentMapper;

    /**
     * 根据用户ID获取学生信息
     */
    public Student getStudentByUserId(Long userId) {
        QueryWrapper<Student> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.eq("is_deleted", false);
        return studentMapper.selectOne(queryWrapper);
    }

    /**
     * 更新学生信息
     */
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

        studentMapper.updateById(student);
        logger.info("更新学生信息成功: userId={}", userId);

        return student;
    }
} 