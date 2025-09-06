package com.touhouqing.grabteacherbackend.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.mapper.JobPostMapper;
import com.touhouqing.grabteacherbackend.mapper.JobPostSubjectMapper;
import com.touhouqing.grabteacherbackend.cache.JobPostCacheManager;
import com.touhouqing.grabteacherbackend.cache.JobPostDetailLocalCache;
import com.touhouqing.grabteacherbackend.mapper.SubjectMapper;
import com.touhouqing.grabteacherbackend.model.dto.JobPostDTO;
import com.touhouqing.grabteacherbackend.model.entity.JobPost;
import com.touhouqing.grabteacherbackend.model.entity.JobPostSubject;
import com.touhouqing.grabteacherbackend.model.entity.Subject;
import com.touhouqing.grabteacherbackend.model.vo.JobPostVO;
import com.touhouqing.grabteacherbackend.service.JobPostService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class JobPostServiceImpl implements JobPostService {

    @Autowired
    private JobPostMapper jobPostMapper;
    @Autowired
    private JobPostSubjectMapper jobPostSubjectMapper;
    @Autowired
    private SubjectMapper subjectMapper;
    @Autowired
    private JobPostCacheManager cacheManager;
    @Autowired
    private JobPostDetailLocalCache detailLocalCache;

    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Page<JobPostVO> pageActive(int page, int size, Long subjectId) {
        // 细化缓存键并防穿透/击穿
        String cacheKey = cacheManager.buildListKey(page, size, subjectId);
        Object cached = cacheManager.getList(cacheKey);
        if (cached instanceof Page) {
            return (Page<JobPostVO>) cached;
        }
        // 空值保护：存在空标记直接返回空页（短 TTL 的缓存由 saveList 负责）
        if (cached != null && "EMPTY".equals(cached)) {
            return new Page<>(page, size);
        }

        boolean locked = cacheManager.tryLock(cacheKey, java.time.Duration.ofSeconds(5));
        try {
            if (!locked) {
                // 无法获取锁，兜底直接按 DB 查询，避免被锁阻塞
                return queryPageFromDb(page, size, subjectId);
            }
            // 双检缓存
            Object second = cacheManager.getList(cacheKey);
            if (second instanceof Page) {
                return (Page<JobPostVO>) second;
            }
            Page<JobPostVO> result = queryPageFromDb(page, size, subjectId);
            if (result.getTotal() == 0) {
                // 穿透保护：写入空标记，短 TTL
                cacheManager.saveList(cacheKey, "EMPTY", subjectId, java.time.Duration.ofSeconds(30));
            } else {
                cacheManager.saveList(cacheKey, result, subjectId, java.time.Duration.ofSeconds(60));
            }
            return result;
        } finally {
            if (locked) cacheManager.unlock(cacheKey);
        }
    }

    private Page<JobPostVO> queryPageFromDb(int page, int size, Long subjectId) {
        // 先从关联表筛选 id 列表（小集合），再回表取详情
        Set<Long> idSet = null;
        if (subjectId != null) {
            List<JobPostSubject> list = jobPostSubjectMapper.selectList(new QueryWrapper<JobPostSubject>().eq("subject_id", subjectId));
            Set<Long> subIds = list.stream().map(JobPostSubject::getJobPostId).collect(Collectors.toSet());
            idSet = (idSet == null) ? subIds : idSet.stream().filter(subIds::contains).collect(Collectors.toSet());
        }

        QueryWrapper<JobPost> qw = new QueryWrapper<JobPost>()
                .eq("is_deleted", 0)
                .eq("status", "active")
                .orderByAsc("priority")
                .orderByDesc("created_at")
                .orderByDesc("id");
        if (idSet != null) {
            if (idSet.isEmpty()) {
                return new Page<>(page, size); // 直接空
            }
            qw.in("id", idSet);
        }

        Page<JobPost> p = new Page<>(page, size);
        jobPostMapper.selectPage(p, qw);
        Page<JobPostVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        voPage.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        // 缓存登记（这里不再存，存放在 pageActive 顶层已处理）
        return voPage;
    }

    @Override
    public Page<JobPostVO> pageAdmin(int page, int size,
                                     Long subjectId,
                                     String status, String keyword,
                                     java.time.LocalDateTime createdStart, java.time.LocalDateTime createdEnd,
                                     Boolean includeDeleted) {
        // 管理端：一致性优先，直接查询数据库（移除缓存）
        return queryAdminFromDb(page, size, subjectId, status, keyword, createdStart, createdEnd, includeDeleted);
    }

    private Page<JobPostVO> queryAdminFromDb(int page, int size,
                                             Long subjectId,
                                             String status, String keyword,
                                             java.time.LocalDateTime createdStart, java.time.LocalDateTime createdEnd,
                                             Boolean includeDeleted) {
        Set<Long> idSet = null;
        if (subjectId != null) {
            List<JobPostSubject> list = jobPostSubjectMapper.selectList(new QueryWrapper<JobPostSubject>().eq("subject_id", subjectId));
            Set<Long> subIds = list.stream().map(JobPostSubject::getJobPostId).collect(Collectors.toSet());
            idSet = (idSet == null) ? subIds : idSet.stream().filter(subIds::contains).collect(Collectors.toSet());
        }
        QueryWrapper<JobPost> qw = new QueryWrapper<>();
        if (status != null && !status.isEmpty()) {
            qw.eq("status", status);
        }
        if (keyword != null && !keyword.isEmpty()) {
            qw.like("title", keyword);
        }
        if (createdStart != null) {
            qw.ge("created_at", createdStart);
        }
        if (createdEnd != null) {
            qw.le("created_at", createdEnd);
        }
        if (Boolean.FALSE.equals(includeDeleted)) {
            qw.eq("is_deleted", 0);
        }
        if (idSet != null) {
            if (idSet.isEmpty()) return new Page<>(page, size);
            qw.in("id", idSet);
        }
        qw.orderByAsc("is_deleted").orderByAsc("priority").orderByDesc("created_at").orderByDesc("id");
        Page<JobPost> p = new Page<>(page, size);
        jobPostMapper.selectPage(p, qw);
        Page<JobPostVO> voPage = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        voPage.setRecords(p.getRecords().stream().map(this::toVO).collect(Collectors.toList()));
        return voPage;
    }

    @Override
    @Transactional
    public JobPost create(JobPostDTO dto) {
        // 基本校验：必须至少一个科目，且ID有效
        validateAndNormalize(dto);

        JobPost entity = new JobPost();
        entity.setTitle(dto.getTitle());
        entity.setIntroduction(dto.getIntroduction());
        entity.setPositionTags(dto.getTags()==null?null:toJsonArray(dto.getTags()));
        entity.setStatus(dto.getStatus()==null?"active":dto.getStatus());
        entity.setPriority(dto.getPriority()==null?0:dto.getPriority());
        entity.setDeleted(false);
        jobPostMapper.insert(entity);

        // 维护关联
        saveMapping(entity.getId(), dto.getSubjectIds());
        // 冗余回填
        refreshDenormalizedFields(entity.getId());
        // 精准驱逐：该岗位涉及的维度
        java.util.Set<Long> sset = dto.getSubjectIds()==null?null:new java.util.HashSet<>(dto.getSubjectIds());
        cacheManager.evictByDimensions(sset);
        // 管理端列表：短TTL缓存直接全清，保证新建后列表即时更新
        cacheManager.evictAllAdminLists();
        // 清掉本地详情缓存
        detailLocalCache.evict(entity.getId());

        return jobPostMapper.selectById(entity.getId());
    }

    @Override
    @Transactional
    public JobPost update(Long id, JobPostDTO dto) {
        JobPost entity = jobPostMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) {
            throw new RuntimeException("岗位不存在");
        }

        // 读取旧维度集合
        java.util.Set<Long> oldS = jobPostSubjectMapper.selectList(new QueryWrapper<JobPostSubject>().eq("job_post_id", id))
                .stream().map(JobPostSubject::getSubjectId).collect(java.util.stream.Collectors.toSet());

        if (dto.getTitle() != null) entity.setTitle(dto.getTitle());
        if (dto.getIntroduction() != null) entity.setIntroduction(dto.getIntroduction());
        if (dto.getTags() != null) entity.setPositionTags(toJsonArray(dto.getTags()));
        if (dto.getStatus() != null) entity.setStatus(dto.getStatus());
        if (dto.getPriority() != null) entity.setPriority(dto.getPriority());

        // 当请求携带科目时，检查有效性；若提供为空列表，视为非法
        if (dto.getSubjectIds() != null) {
            validateAndNormalize(dto);
        }

        jobPostMapper.updateById(entity);

        // 精准驱逐：合并旧+新维度
        java.util.Set<Long> newS = dto.getSubjectIds()==null?java.util.Collections.emptySet():new java.util.HashSet<>(dto.getSubjectIds());
        java.util.Set<Long> sAll = new java.util.HashSet<>(oldS); sAll.addAll(newS);
        cacheManager.evictByDimensions(sAll);
        cacheManager.evictAllAdminLists();
        detailLocalCache.evict(id);

        if (dto.getSubjectIds() != null) {
            // 先清除再写入
            jobPostSubjectMapper.delete(new QueryWrapper<JobPostSubject>().eq("job_post_id", id));
            saveMapping(id, dto.getSubjectIds());
            refreshDenormalizedFields(id);
        }
        return jobPostMapper.selectById(id);
    }

    private void validateAndNormalize(JobPostDTO dto) {
        if (dto.getSubjectIds() == null || dto.getSubjectIds().isEmpty()) {
            throw new RuntimeException("教师招聘必须至少关联一个科目");
        }
        // 去重
        dto.setSubjectIds(new java.util.ArrayList<>(new java.util.LinkedHashSet<>(dto.getSubjectIds())));
        // 校验ID是否存在且未删除
        List<Subject> validSubjects = subjectMapper.selectList(new QueryWrapper<Subject>().in("id", dto.getSubjectIds()).eq("is_deleted", 0));
        if (validSubjects.size() != dto.getSubjectIds().size()) {
            throw new RuntimeException("存在无效的科目ID");
        }
    }


    @Override
    public String getDetailJson(Long id) {
        String cached = detailLocalCache.get(id);
        if (cached != null) return cached;
        JobPostVO vo = getById(id);
        if (vo == null) return null;
        String json = toJson(vo);
        detailLocalCache.put(id, json);
        return json;
    }

    @Override
    @Transactional
    public void delete(Long id) {
        JobPost entity = jobPostMapper.selectById(id);
        if (entity == null) return;
        entity.setDeleted(true);
        jobPostMapper.updateById(entity);
        // 清理所有维度缓存与本地详情
        java.util.Set<Long> sset = jobPostSubjectMapper.selectList(new QueryWrapper<JobPostSubject>().eq("job_post_id", id))
                .stream().map(JobPostSubject::getSubjectId).collect(java.util.stream.Collectors.toSet());
        cacheManager.evictByDimensions(sset);
        // 管理端列表：短TTL缓存直接全清，保证列表即时更新
        cacheManager.evictAllAdminLists();
        detailLocalCache.evict(id);
    }

    @Override
    @Transactional
    public void batchDelete(java.util.List<Long> ids) {
        if (ids == null || ids.isEmpty()) return;
        // 查询有效岗位（未删除）
        List<JobPost> list = jobPostMapper.selectBatchIds(ids);
        List<JobPost> valid = list == null ? java.util.Collections.emptyList() : list.stream().filter(j -> !Boolean.TRUE.equals(j.getDeleted())).toList();
        if (valid.isEmpty()) return;
        // 批量逻辑删除
        for (JobPost jp : valid) {
            jp.setDeleted(true);
            jobPostMapper.updateById(jp);
        }
        // 汇总维度做一次性精准驱逐
        java.util.Set<Long> ssetAll = new java.util.HashSet<>();
        for (Long id0 : valid.stream().map(JobPost::getId).toList()) {
            java.util.Set<Long> sset = jobPostSubjectMapper.selectList(new QueryWrapper<JobPostSubject>().eq("job_post_id", id0))
                    .stream().map(JobPostSubject::getSubjectId).collect(java.util.stream.Collectors.toSet());
            ssetAll.addAll(sset);
            // 本地详情逐个驱逐
            detailLocalCache.evict(id0);
        }
        cacheManager.evictByDimensions(ssetAll);
        cacheManager.evictAllAdminLists();
    }


    @Override
    public JobPostVO getById(Long id) {
        JobPost entity = jobPostMapper.selectById(id);
        if (entity == null || Boolean.TRUE.equals(entity.getDeleted())) return null;
        return toVO(entity);
    }

    @Override
    public JobPost getAdminById(Long id) {
        return jobPostMapper.selectById(id);
    }

    private void saveMapping(Long jobId, List<Long> subjectIds) {
        if (subjectIds != null) {
            for (Long sid : new HashSet<>(subjectIds)) {
                JobPostSubject s = new JobPostSubject();
                s.setJobPostId(jobId);
                s.setSubjectId(sid);
                jobPostSubjectMapper.insert(s);
            }
        }
    }

    private void refreshDenormalizedFields(Long jobId) {
        // 科目
        List<JobPostSubject> jss = jobPostSubjectMapper.selectList(new QueryWrapper<JobPostSubject>().eq("job_post_id", jobId));
        List<Long> subjectIds = jss.stream().map(JobPostSubject::getSubjectId).sorted().toList();
        String sidStr = subjectIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        Map<Long, String> subjectNameMap = Optional.ofNullable(subjectMapper.selectBatchIds(subjectIds))
                .orElse(Collections.emptyList())
                .stream().collect(Collectors.toMap(Subject::getId, Subject::getName));
        String snameStr = subjectIds.stream().map(subjectNameMap::get).filter(Objects::nonNull).collect(Collectors.joining(","));

        JobPost entity = new JobPost();
        entity.setId(jobId);
        entity.setSubjectIds(sidStr);
        entity.setSubjectNames(snameStr);
        jobPostMapper.updateById(entity);
    }

    private JobPostVO toVO(JobPost e) {
        return JobPostVO.builder()
                .id(e.getId())
                .title(e.getTitle())
                .introduction(e.getIntroduction())
                .positionTags(e.getPositionTags())
                .subjectNames(e.getSubjectNames())
                .status(e.getStatus())
                .priority(e.getPriority())
                .createdAt(e.getCreatedAt()==null?null:e.getCreatedAt().format(DTF))
                .build();
    }

    private String toJsonArray(List<String> list) {
        if (list == null) return null;
        return "[" + list.stream().map(s -> "\"" + s.replace("\"", "\\\"") + "\"").collect(Collectors.joining(",")) + "]";
    }

    // 轻量JSON序列化，避免引入额外依赖
    private String toJson(JobPostVO vo) {
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        sb.append("\"id\":").append(vo.getId());
        if (vo.getTitle()!=null) sb.append(",\"title\":\"").append(vo.getTitle().replace("\"","\\\"")).append('\"');
        if (vo.getIntroduction()!=null) sb.append(",\"introduction\":\"").append(vo.getIntroduction().replace("\"","\\\"")).append('\"');
        if (vo.getPositionTags()!=null) sb.append(",\"positionTags\":").append(vo.getPositionTags());
        if (vo.getSubjectNames()!=null) sb.append(",\"subjectNames\":\"").append(vo.getSubjectNames().replace("\"","\\\"")).append('\"');
        if (vo.getStatus()!=null) sb.append(",\"status\":\"").append(vo.getStatus().replace("\"","\\\"")).append('\"');
        if (vo.getPriority()!=null) sb.append(",\"priority\":").append(vo.getPriority());
        if (vo.getCreatedAt()!=null) sb.append(",\"createdAt\":\"").append(vo.getCreatedAt().replace("\"","\\\"")).append('\"');
        sb.append('}');
        return sb.toString();
    }
}
