package com.touhouqing.grabteacherbackend.controller;

import com.touhouqing.grabteacherbackend.common.result.CommonResult;
import com.touhouqing.grabteacherbackend.model.dto.StudentInfoDTO;
import com.touhouqing.grabteacherbackend.model.vo.StudentProfileVO;
import com.touhouqing.grabteacherbackend.model.entity.Student;
import com.touhouqing.grabteacherbackend.security.UserPrincipal;
import com.touhouqing.grabteacherbackend.service.StudentService;
import com.touhouqing.grabteacherbackend.service.MessageService;
import com.touhouqing.grabteacherbackend.model.vo.MessageVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.Map;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.touhouqing.grabteacherbackend.model.entity.BalanceTransaction;
import com.touhouqing.grabteacherbackend.mapper.BalanceTransactionMapper;

@RestController
@RequestMapping("/api/student")
@CrossOrigin(origins = "*", maxAge = 3600)
@PreAuthorize("hasRole('STUDENT')")
public class StudentController {

    private static final Logger logger = LoggerFactory.getLogger(StudentController.class);

    @Autowired
    private StudentService studentService;

    @Autowired
    private MessageService messageService;

    @Autowired
    private BalanceTransactionMapper balanceTransactionMapper;

    /**
     * 获取学生个人信息
     */
    @GetMapping("/profile")
    public ResponseEntity<CommonResult<StudentProfileVO>> getProfile(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            StudentProfileVO studentProfile = studentService.getStudentProfileByUserId(userPrincipal.getId());

            if (studentProfile == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(CommonResult.error("学生信息不存在"));
            }

            return ResponseEntity.ok(CommonResult.success("获取成功", studentProfile));
        } catch (Exception e) {
            logger.error("获取学生信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取当前登录学生的余额明细
     */
    @GetMapping("/balance-transactions/my")
    public ResponseEntity<CommonResult<Page<BalanceTransaction>>> getMyBalanceTransactions(
            Authentication authentication,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String transactionType) {
        try {
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            Page<BalanceTransaction> pageReq = new Page<>(page, size);
            QueryWrapper<BalanceTransaction> qw = new QueryWrapper<>();
            qw.eq("user_id", principal.getId()).orderByDesc("created_at");
            if (transactionType != null && !transactionType.trim().isEmpty()) {
                qw.eq("transaction_type", transactionType.trim());
            }
            Page<BalanceTransaction> result = balanceTransactionMapper.selectPage(pageReq, qw);
            return ResponseEntity.ok(CommonResult.success("获取成功", result));
        } catch (Exception e) {
            logger.error("获取当前学生余额明细异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 更新学生信息
     */
    @PutMapping("/profile")
    public ResponseEntity<CommonResult<Student>> updateProfile(
            @Valid @RequestBody StudentInfoDTO request,
            Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Student updatedStudent = studentService.updateStudentInfo(userPrincipal.getId(), request);

            return ResponseEntity.ok(CommonResult.success("更新成功", updatedStudent));
        } catch (RuntimeException e) {
            logger.warn("更新学生信息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("更新学生信息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("更新失败"));
        }
    }

    /**
     * 获取学生控制台统计数据
     */
    @GetMapping("/statistics")
    public ResponseEntity<CommonResult<Map<String, Object>>> getStatistics(Authentication authentication) {
        try {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            Map<String, Object> statistics = studentService.getStudentStatistics(userPrincipal.getId());

            return ResponseEntity.ok(CommonResult.success("获取成功", statistics));
        } catch (Exception e) {
            logger.error("获取学生统计数据异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 获取学生可见的消息列表
     */
    @GetMapping("/messages")
    public ResponseEntity<CommonResult<IPage<MessageVO>>> getStudentMessages(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        try {
            IPage<MessageVO> messages = messageService.getStudentMessages(pageNum, pageSize);
            return ResponseEntity.ok(CommonResult.success("获取成功", messages));
        } catch (Exception e) {
            logger.error("获取学生消息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }

    /**
     * 根据ID获取消息详情
     */
    @GetMapping("/messages/{id}")
    public ResponseEntity<CommonResult<MessageVO>> getMessageById(@PathVariable Long id) {
        try {
            MessageVO message = messageService.getMessageById(id);
            return ResponseEntity.ok(CommonResult.success("获取成功", message));
        } catch (RuntimeException e) {
            logger.warn("获取消息失败: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(CommonResult.error(e.getMessage()));
        } catch (Exception e) {
            logger.error("获取消息异常: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(CommonResult.error("获取失败"));
        }
    }
} 