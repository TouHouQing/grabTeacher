package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.touhouqing.grabteacherbackend.mapper.TeacherLevelMapper;
import com.touhouqing.grabteacherbackend.model.entity.TeacherLevel;
import com.touhouqing.grabteacherbackend.service.TeacherLevelService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeacherLevelServiceImpl extends ServiceImpl<TeacherLevelMapper, TeacherLevel> implements TeacherLevelService {
    @Override
    public List<TeacherLevel> listAll() {
        return this.lambdaQuery().orderByAsc(TeacherLevel::getId).list();
    }
}


