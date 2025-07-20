/*
 Navicat Premium Dump SQL

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 90100 (9.1.0)
 Source Host           : localhost:3306
 Source Schema         : grabTeacher

 Target Server Type    : MySQL
 Target Server Version : 90100 (9.1.0)
 File Encoding         : 65001

 Date: 20/07/2025 19:36:36
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admins
-- ----------------------------
DROP TABLE IF EXISTS `admins`;
CREATE TABLE `admins` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '管理员ID，主键自增',
  `user_id` bigint NOT NULL COMMENT '关联用户表的用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '管理员真实姓名',
  `notes` text COMMENT '管理员备注信息',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='管理员详细信息表，存储管理员的职务信息和权限';

-- ----------------------------
-- Records of admins
-- ----------------------------
BEGIN;
INSERT INTO `admins` (`id`, `user_id`, `real_name`, `notes`, `is_deleted`, `deleted_at`) VALUES (1, 11, '管理员', '测试', 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for booking_requests
-- ----------------------------
DROP TABLE IF EXISTS `booking_requests`;
CREATE TABLE `booking_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '预约申请ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID，可为空(自定义预约)',
  `booking_type` enum('single','recurring') NOT NULL COMMENT '预约类型：single-单次，recurring-周期性',
  `requested_date` date DEFAULT NULL COMMENT '请求的上课日期(单次预约)',
  `requested_start_time` time DEFAULT NULL COMMENT '请求的开始时间(单次预约)',
  `requested_end_time` time DEFAULT NULL COMMENT '请求的结束时间(单次预约)',
  `recurring_weekdays` varchar(50) DEFAULT NULL COMMENT '周期性预约的星期几，逗号分隔：1,3,5',
  `recurring_time_slots` varchar(200) DEFAULT NULL COMMENT '周期性预约的时间段，逗号分隔：14:00-16:00,18:00-20:00',
  `start_date` date DEFAULT NULL COMMENT '周期性预约开始日期',
  `end_date` date DEFAULT NULL COMMENT '周期性预约结束日期',
  `total_times` int DEFAULT NULL COMMENT '总课程次数',
  `student_requirements` text COMMENT '学生需求说明',
  `status` enum('pending','approved','rejected','cancelled') DEFAULT 'pending' COMMENT '申请状态pending-待定中，approved-已批准，rejected-已拒绝，cancelled-已取消',
  `teacher_reply` text COMMENT '教师回复内容',
  `admin_notes` text COMMENT '管理员备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `approved_at` timestamp NULL DEFAULT NULL COMMENT '批准时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预约申请表，记录学生的课程预约申请';

-- ----------------------------
-- Records of booking_requests
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for courses
-- ----------------------------
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程ID，主键自增',
  `teacher_id` bigint NOT NULL COMMENT '授课教师ID，关联teachers表',
  `subject_id` bigint NOT NULL COMMENT '课程科目ID，关联subjects表',
  `title` varchar(200) NOT NULL COMMENT '课程标题',
  `description` text COMMENT '课程详细描述，包括内容大纲、适合人群等',
  `course_type` enum('one_on_one','large_class') NOT NULL COMMENT '课程类型：one_on_one-一对一,large_class-大班课',
  `duration_minutes` int NOT NULL COMMENT '单次课程时长，单位：分钟',
  `status` enum('active','inactive','full') DEFAULT 'active' COMMENT '课程状态：active-可报名，inactive-已下架，full-已满员',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '课程创建时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `grade` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '课程适用年级如[小学一年级,小学二年级]',
  `gender` enum('男','女','不限') DEFAULT '不限' COMMENT '适合性别：男、女、不限',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程信息表，存储教师发布的课程详情';

-- ----------------------------
-- Records of courses
-- ----------------------------
BEGIN;
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`, `grade`, `gender`) VALUES (1, 7, 1, '666', '222', 'one_on_one', 120, 'active', '2025-07-20 14:05:18', 1, '2025-07-20 14:07:01', NULL, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`, `grade`, `gender`) VALUES (2, 7, 4, '123', '123', 'one_on_one', 120, 'active', '2025-07-20 14:05:40', 0, NULL, NULL, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`, `grade`, `gender`) VALUES (3, 7, 1, '语文', '我爱语文', 'one_on_one', 120, 'active', '2025-07-20 18:45:57', 0, NULL, '小学一年级', NULL);
COMMIT;

-- ----------------------------
-- Table structure for reschedule_requests
-- ----------------------------
DROP TABLE IF EXISTS `reschedule_requests`;
CREATE TABLE `reschedule_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '调课申请ID',
  `schedule_id` bigint NOT NULL COMMENT '原课程安排ID',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID(学生或教师)',
  `applicant_type` enum('student','teacher') NOT NULL COMMENT '申请人类型',
  `request_type` enum('single','recurring','cancel') NOT NULL COMMENT '申请类型：single-单次调课，recurring-周期性调课，cancel-取消课程',
  `original_date` date NOT NULL COMMENT '原定日期',
  `original_start_time` time NOT NULL COMMENT '原定开始时间',
  `original_end_time` time NOT NULL COMMENT '原定结束时间',
  `new_date` date DEFAULT NULL COMMENT '新日期',
  `new_start_time` time DEFAULT NULL COMMENT '新开始时间',
  `new_end_time` time DEFAULT NULL COMMENT '新结束时间',
  `new_weekly_schedule` varchar(500) DEFAULT NULL COMMENT '新的周期性安排，格式：周一,周三 14:00-16:00;周五 18:00-20:00',
  `reason` text NOT NULL COMMENT '调课原因',
  `urgency_level` enum('low','medium','high') DEFAULT 'medium' COMMENT '紧急程度',
  `advance_notice_hours` int DEFAULT NULL COMMENT '提前通知小时数',
  `status` enum('pending','approved','rejected','cancelled') DEFAULT 'pending' COMMENT '申请状态',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `reviewer_type` enum('teacher','student','admin') DEFAULT NULL COMMENT '审核人类型',
  `review_notes` text COMMENT '审核备注',
  `compensation_amount` decimal(10,2) DEFAULT '0.00' COMMENT '补偿金额(如需要)',
  `affects_future_sessions` tinyint(1) DEFAULT '0' COMMENT '是否影响后续课程',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `reviewed_at` timestamp NULL DEFAULT NULL COMMENT '审核时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='调课申请表，记录课程时间调整申请';

-- ----------------------------
-- Records of reschedule_requests
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for schedules
-- ----------------------------
DROP TABLE IF EXISTS `schedules`;
CREATE TABLE `schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '排课ID，主键自增',
  `teacher_id` bigint NOT NULL COMMENT '授课教师ID',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `course_id` bigint NOT NULL COMMENT '课程ID',
  `scheduled_date` date NOT NULL COMMENT '上课日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `total_times` int DEFAULT NULL COMMENT '总课程次数',
  `status` enum('progressing','completed','cancelled') DEFAULT 'progressing' COMMENT '课程状态：progressing-进行中，confirmed-已确认，cancelled-已取消',
  `teacher_notes` text COMMENT '教师课后备注和反馈',
  `student_feedback` text COMMENT '学生课后反馈',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '排课创建时间',
  `booking_request_id` bigint DEFAULT NULL COMMENT '关联预约申请ID',
  `booking_source` enum('request','admin') DEFAULT 'request' COMMENT '预约来源：request-申请预约，admin-管理员安排',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `recurring_weekdays` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '周期性预约的星期几，逗号分隔：1,3,5',
  `recurring_time_slots` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '周期性预约的时间段，逗号分隔：14:00-16:00,18:00-20:00',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程安排表，记录具体的上课时间';

-- ----------------------------
-- Records of schedules
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for students
-- ----------------------------
DROP TABLE IF EXISTS `students`;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '学生ID，主键自增',
  `user_id` bigint NOT NULL COMMENT '关联用户表的用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '学生真实姓名',
  `grade_level` varchar(20) DEFAULT NULL COMMENT '年级水平，如：小学三年级、初中一年级、高中二年级',
  `subjects_interested` varchar(50) DEFAULT NULL COMMENT '感兴趣的科目列表如：[数学,英语,物理]',
  `learning_goals` text COMMENT '学习目标和需求描述',
  `preferred_teaching_style` varchar(100) DEFAULT NULL COMMENT '偏好的教学风格，如：严格型、温和型、互动型',
  `budget_range` varchar(50) DEFAULT NULL COMMENT '预算范围，如：100-200元/小时',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生详细信息表，存储学生的个人资料和学习偏好';

-- ----------------------------
-- Records of students
-- ----------------------------
BEGIN;
INSERT INTO `students` (`id`, `user_id`, `real_name`, `grade_level`, `subjects_interested`, `learning_goals`, `preferred_teaching_style`, `budget_range`, `is_deleted`, `deleted_at`) VALUES (6, 10, 'student23', '小学一年级', '数学', '123131123', '实践型教学', '100-200', 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for subjects
-- ----------------------------
DROP TABLE IF EXISTS `subjects`;
CREATE TABLE `subjects` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '科目ID，主键自增',
  `name` varchar(50) NOT NULL COMMENT '科目名称，如：数学、语文、英语',
  `grade_levels` varchar(20) DEFAULT NULL COMMENT '适用年级范围如：[小学,初中,高中]',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '科目图标URL地址',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：true-启用，false-禁用',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科目分类表，定义平台支持的所有教学科目';

-- ----------------------------
-- Records of subjects
-- ----------------------------
BEGIN;
INSERT INTO `subjects` (`id`, `name`, `grade_levels`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (1, '语文', '小学', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `grade_levels`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (2, '123', '123', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `grade_levels`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (3, '555', '555', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `grade_levels`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (4, '666', '6665', '', 1, 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for teachers
-- ----------------------------
DROP TABLE IF EXISTS `teachers`;
CREATE TABLE `teachers` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '教师ID，主键自增',
  `user_id` bigint NOT NULL COMMENT '关联用户表的用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '教师真实姓名',
  `education_background` text COMMENT '教育背景描述，包括学历、毕业院校等',
  `teaching_experience` int DEFAULT NULL COMMENT '教学经验年数',
  `specialties` varchar(50) DEFAULT NULL COMMENT '专业领域如：[高考数学,竞赛辅导,基础提升]',
  `subjects` varchar(50) DEFAULT NULL COMMENT '可教授科目列表如：[数学,物理,化学]',
  `hourly_rate` decimal(10,2) DEFAULT NULL COMMENT '每小时收费标准，单位：元',
  `introduction` text COMMENT '个人介绍和教学理念',
  `video_intro_url` varchar(255) DEFAULT NULL COMMENT '个人介绍视频URL地址',
  `gender` enum('男','女','不愿透露') DEFAULT '不愿透露' COMMENT '性别：男、女、不愿透露',
  `is_verified` tinyint(1) DEFAULT '0' COMMENT '是否已认证：true-已认证，false-未认证',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师详细信息表，存储教师的专业资料和教学信息';

-- ----------------------------
-- Records of teachers
-- ----------------------------
BEGIN;
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `subjects`, `hourly_rate`, `introduction`, `video_intro_url`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (7, 14, '12355', '硕士研究生', 1, '1231236', '英语,数学', 50.00, '123123555', NULL, 0, 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键自增',
  `username` varchar(50) NOT NULL COMMENT '用户名，唯一标识',
  `email` varchar(100) NOT NULL COMMENT '邮箱地址，用于登录和通知',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码（加密）Bcrypt',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号码',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像图片URL地址',
  `user_type` enum('student','teacher','admin') NOT NULL COMMENT '用户类型：student-学生，teacher-教师，admin-管理员',
  `status` enum('active','inactive','banned') DEFAULT 'active' COMMENT '账户状态：active-激活，inactive-未激活，banned-封禁',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=15 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基础信息表，存储所有用户的通用信息';

-- ----------------------------
-- Records of users
-- ----------------------------
BEGIN;
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (10, 'student', 'qinghaoyang@foxmail.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '', NULL, 'student', 'active', '2025-07-19 13:21:54', '2025-07-19 13:21:54', 0, '2025-07-19 23:32:59');
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (11, 'admin', 'admin@admin.com', '$2a$10$CLJSuGd2ptKI9VlCz3r4buGyY7HfKg1qivwbKEfkk8/6Pz57oKjWK', NULL, NULL, 'admin', 'active', '2025-07-19 16:23:39', '2025-07-19 19:54:42', 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (14, 'teacher', '123456@qq.com', '$2a$10$eNVMo.XWzg/OG1RFYwzX0etNFyhahK3Cx3qVmPWvXP2hlOVstrVPm', '', NULL, 'teacher', 'active', '2025-07-19 14:38:47', '2025-07-19 14:38:47', 0, '2025-07-19 23:26:32');
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
