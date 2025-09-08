package com.touhouqing.grabteacherbackend.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.touhouqing.grabteacherbackend.model.entity.TeacherLevel;

import java.util.List;

public interface TeacherLevelService extends IService<TeacherLevel> {
    List<TeacherLevel> listAll();
}


