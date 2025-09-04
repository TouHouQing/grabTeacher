package com.touhouqing.grabteacherbackend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.touhouqing.grabteacherbackend.model.entity.SuspensionRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SuspensionRequestMapper extends BaseMapper<SuspensionRequest> {

    @Select("SELECT * FROM suspension_requests WHERE student_id = #{studentId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<SuspensionRequest> findByStudentId(@Param("studentId") Long studentId);

    @Select("SELECT * FROM suspension_requests WHERE teacher_id = #{teacherId} AND is_deleted = 0 ORDER BY created_at DESC")
    List<SuspensionRequest> findByTeacherId(@Param("teacherId") Long teacherId);
}


