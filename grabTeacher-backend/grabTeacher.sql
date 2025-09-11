/*
 Navicat Premium Dump SQL

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 80406 (8.4.6)
 Source Host           : localhost:3306
 Source Schema         : grabteacher

 Target Server Type    : MySQL
 Target Server Version : 80406 (8.4.6)
 File Encoding         : 65001

 Date: 11/09/2025 13:16:35
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
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '管理员真实姓名',
  `wechat_qrcode_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '微信二维码图片URL',
  `whatsapp_number` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'WhatsApp号码',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像URL',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '邮箱',
  `notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '管理员备注信息',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理员详细信息表，存储管理员的职务信息和权限';

-- ----------------------------
-- Records of admins
-- ----------------------------
BEGIN;
INSERT INTO `admins` (`id`, `user_id`, `real_name`, `wechat_qrcode_url`, `whatsapp_number`, `avatar_url`, `email`, `notes`, `is_deleted`, `deleted_at`) VALUES (1, 11, '管理员', NULL, NULL, NULL, NULL, '测试', 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for balance_transactions
-- ----------------------------
DROP TABLE IF EXISTS `balance_transactions`;
CREATE TABLE `balance_transactions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '交易ID，主键自增',
  `user_id` bigint NOT NULL COMMENT '学生用户ID，关联users表',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生姓名',
  `amount` decimal(10,2) NOT NULL COMMENT '变动金额，正数表示增加，负数表示减少',
  `balance_before` decimal(10,2) NOT NULL COMMENT '变动前余额',
  `balance_after` decimal(10,2) NOT NULL COMMENT '变动后余额',
  `transaction_type` enum('RECHARGE','DEDUCT','REFUND') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易类型：RECHARGE-充值, DEDUCT-扣费, REFUND-退费',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '变动原因',
  `booking_id` bigint DEFAULT NULL COMMENT '关联的预约ID，可为空',
  `operator_id` bigint DEFAULT NULL COMMENT '操作员ID（如管理员），可为空',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_booking_id` (`booking_id`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生余额变动记录表';

-- ----------------------------
-- Records of balance_transactions
-- ----------------------------
BEGIN;
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
  `subject_id` bigint DEFAULT NULL COMMENT '用户选择的科目ID',
  `grade` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '年级',

  `booking_type` enum('single','recurring') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '预约类型：single-单次，recurring-周期性',
  `requested_date` date DEFAULT NULL COMMENT '请求的上课日期(单次预约)',
  `requested_start_time` time DEFAULT NULL COMMENT '请求的开始时间(单次预约)',
  `requested_end_time` time DEFAULT NULL COMMENT '请求的结束时间(单次预约)',
  `recurring_weekdays` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '周期性预约的星期几，逗号分隔：1,3,5',
  `recurring_time_slots` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '周期性预约的时间段，逗号分隔：14:00-16:00,18:00-20:00',
  `start_date` date DEFAULT NULL COMMENT '周期性预约开始日期',
  `end_date` date DEFAULT NULL COMMENT '周期性预约结束日期',
  `total_times` int DEFAULT NULL COMMENT '总课程次数',
  `student_requirements` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '学生需求说明',
  `status` enum('pending','approved','rejected','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'pending' COMMENT '申请状态pending-待定中，approved-已批准，rejected-已拒绝，cancelled-已取消',
  `teacher_reply` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '教师回复内容',
  `admin_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '管理员备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `approved_at` timestamp NULL DEFAULT NULL COMMENT '批准时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `is_trial` tinyint(1) DEFAULT '0' COMMENT '是否为免费试听课：true-是，false-否',
  `trial_duration_minutes` int DEFAULT NULL COMMENT '试听课时长（分钟）',
  `teaching_location_id` bigint DEFAULT NULL COMMENT '授课地点ID（线下）',
  `teaching_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '线上' COMMENT '授课地点名称',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约申请表，记录学生的课程预约申请';

-- ----------------------------
-- Records of booking_requests
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for course_enrollments
-- ----------------------------
DROP TABLE IF EXISTS `course_enrollments`;
CREATE TABLE `course_enrollments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程报名ID，主键自增',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `teacher_id` bigint NOT NULL COMMENT '授课教师ID',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID（一对一课程可能为空）',
  `grade` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '年级',
  `enrollment_type` enum('one_on_one','large_class') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '报名类型：one_on_one-一对一,large_class-大班课',
  `total_sessions` int DEFAULT NULL COMMENT '总课程次数',
  `completed_sessions` int DEFAULT '0' COMMENT '已完成课程次数',
  `enrollment_status` enum('active','completed','cancelled','suspended') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'active' COMMENT '报名状态：active-进行中，completed-已完成，cancelled-已取消，suspended-暂停',
  `enrollment_date` date NOT NULL COMMENT '报名日期',
  `start_date` date DEFAULT NULL COMMENT '课程开始日期',
  `end_date` date DEFAULT NULL COMMENT '课程结束日期',
  `is_trial` tinyint(1) DEFAULT '0' COMMENT '是否为试听课：true-是，false-否',
  `recurring_schedule` json DEFAULT NULL COMMENT '周期性预约安排，JSON格式：{"weekdays":[1,3,5],"timeSlots":["14:00-16:00","18:00-20:00"]}，weekday: 1=周一,2=周二...7=周日',
  `duration_minutes` int NOT NULL DEFAULT '120' COMMENT '单次课程时长，单位：分钟，只能选择90分钟或120分钟',
  `booking_request_id` bigint DEFAULT NULL COMMENT '关联预约申请ID',
  `teacher_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '教师备注',
  `student_feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '学生反馈',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `teaching_location_id` bigint DEFAULT NULL COMMENT '授课地点ID（线下）',
  `teaching_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '线上' COMMENT '授课地点名称',
  PRIMARY KEY (`id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_course_id` (`course_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程关系表，记录学生课程报名关系';

-- ----------------------------
-- Records of course_enrollments
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for course_evaluation
-- ----------------------------
DROP TABLE IF EXISTS `course_evaluation`;
CREATE TABLE `course_evaluation` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成绩记录ID，主键自增',
  `teacher_id` bigint DEFAULT NULL COMMENT '教师ID，关联teachers表',
  `student_id` bigint DEFAULT NULL COMMENT '学生ID，关联students表',
  `course_id` bigint DEFAULT NULL COMMENT '课程ID，关联courses表',
  `teacher_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教师姓名',
  `student_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '学生姓名',
  `course_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程名称',
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否精选：0-未精选，1-已精选（精选的会展示在首页）',
  `student_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '学生对课程的评价和建议',
  `rating` decimal(10,7) DEFAULT '5.0000000' COMMENT '课程评分，0-5分',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `course_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程地点',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_course` (`student_id`,`course_id`,`teacher_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_course_id` (`course_id`),
  CONSTRAINT `fk_course_evaluation_course` FOREIGN KEY (`course_id`) REFERENCES `courses` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_course_evaluation_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程评价表';

-- ----------------------------
-- Records of course_evaluation
-- ----------------------------
BEGIN;
INSERT INTO `course_evaluation` (`id`, `teacher_id`, `student_id`, `course_id`, `teacher_name`, `student_name`, `course_name`, `is_featured`, `student_comment`, `rating`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `course_location`) VALUES (1, 7, 6, 13, '林雅文', '小明', '小学华文基础班', 1, '老师教学很认真，课程内容很实用，孩子进步很大！', 5.0000000, '2025-08-27 00:39:56', '2025-09-11 13:09:25', 0, NULL, NULL);
INSERT INTO `course_evaluation` (`id`, `teacher_id`, `student_id`, `course_id`, `teacher_name`, `student_name`, `course_name`, `is_featured`, `student_comment`, `rating`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `course_location`) VALUES (2, 7, 6, 14, '林雅文', '小明', '拼音识字启蒙班', 1, '老师很有耐心，教学方法很好，孩子很喜欢！', 5.0000000, '2025-08-27 00:40:27', '2025-09-11 13:09:28', 0, NULL, NULL);
COMMIT;

-- ----------------------------
-- Table structure for course_schedules
-- ----------------------------
DROP TABLE IF EXISTS `course_schedules`;
CREATE TABLE `course_schedules` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程安排ID，主键自增',
  `enrollment_id` bigint NOT NULL COMMENT '课程报名ID，关联course_enrollments表',
  `scheduled_date` date NOT NULL COMMENT '上课日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `session_number` int DEFAULT NULL COMMENT '课程序号（第几次课）',
  `schedule_status` enum('scheduled','completed','cancelled','rescheduled') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'scheduled' COMMENT '安排状态：scheduled-已安排，completed-已完成，cancelled-已取消，rescheduled-已调课',
  `teacher_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '教师课后备注',
  `student_feedback` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '学生课后反馈',
  `reschedule_reason` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '调课原因',
  `reschedule_request_id` bigint DEFAULT NULL COMMENT '关联调课申请ID',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_enrollment_id` (`enrollment_id`),
  KEY `idx_scheduled_date` (`scheduled_date`),
  KEY `idx_teacher_time` (`scheduled_date`,`start_time`,`end_time`)
) ENGINE=InnoDB AUTO_INCREMENT=70 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程安排表，记录具体的上课时间安排';

-- ----------------------------
-- Records of course_schedules
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
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '课程详细描述，包括内容大纲、适合人群等',
  `rating` decimal(10,7) DEFAULT '5.0000000' COMMENT '课程评分，0-5分',
  `course_type` enum('one_on_one','large_class') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程类型：one_on_one-一对一,large_class-大班课',
  `price` decimal(10,2) DEFAULT NULL COMMENT '课程单价，单位：M豆，1M豆=1元，课程价格，1对1代表每小时价格，大班课代表总课程价格',
  `start_date` date DEFAULT NULL COMMENT '课程开始时间，格式：YYYY-MM-DD',
  `end_date` date DEFAULT NULL COMMENT '课程结束时间，格式：YYYY-MM-DD',
  `person_limit` int DEFAULT NULL COMMENT '最大报名人数，null表示不限制',
  `enrollment_count` int DEFAULT '0' COMMENT '当前报名人数',
  `course_time_slots` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '上课时间安排（只有大班课才需要设置上课时间安排），JSON格式存储：[{"weekday":1,"timeSlots":["08:00-10:00","17:00-19:00"]},{"weekday":6,"timeSlots":["13:00-15:00","15:00-17:00"]}]，weekday: 1=周一,2=周二...7=周日',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程封面图URL',
  `duration_minutes` int NOT NULL DEFAULT '120' COMMENT '单次课程时长，单位：分钟，只能选择90分钟或120分钟',
  `status` enum('active','inactive','full','pending') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'active' COMMENT '课程状态：active-可报名，inactive-已下架，full-已满员，pending-待审批',
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否为精选课程，在首页展示：1-是，0-否',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '课程创建时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `course_location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '线上' COMMENT '课程地点',
  `supports_online` tinyint(1) DEFAULT '0' COMMENT '是否支持线上',
  `offline_locations` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '线下地点ID CSV',
  PRIMARY KEY (`id`),
  KEY `idx_courses_status_deleted_created` (`status`,`is_deleted`,`created_at`),
  KEY `idx_courses_teacher_deleted_status` (`teacher_id`,`is_deleted`,`status`),
  KEY `idx_courses_subject_deleted_status` (`subject_id`,`is_deleted`,`status`),
  KEY `idx_courses_featured_status_deleted_created` (`is_featured`,`status`,`is_deleted`,`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=56 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程信息表，存储教师发布的课程详情';

-- ----------------------------
-- Records of courses
-- ----------------------------
BEGIN;
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (1, 1, 1, '小学数学基础班', '小学数学基础知识教学，包括四则运算、几何图形等基础概念。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 1, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (2, 1, 1, '小学奥数启蒙班', '小学奥数启蒙课程，培养数学思维和解题技巧。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 1, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (3, 2, 1, '小学应用题专项班', '专门针对小学应用题的解题方法和技巧训练。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 1, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (4, 2, 1, '小学计算能力提升班', '提升小学生的计算速度和准确性。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 1, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (5, 3, 1, '小学几何启蒙班', '小学几何基础知识，培养空间想象能力。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 1, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (6, 3, 1, '数学游戏趣味班', '通过数学游戏让孩子爱上数学学习。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 1, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (7, 4, 2, '小学科学探索班', '通过有趣的科学实验，让小学生了解自然现象。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (8, 4, 2, '自然观察实践班', '户外自然观察，培养学生的观察能力和科学精神。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 90, 'active', 0, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (9, 5, 2, '科学启蒙实验班', '适合小学生的科学启蒙课程，通过动手实验学习科学。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (10, 5, 2, '生活中的科学', '从生活现象中学习科学原理，培养科学思维。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (11, 6, 2, '环境科学启蒙班', '环境保护和生态科学的启蒙教育。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (12, 6, 2, '科学探究方法班', '教授科学探究的基本方法和思维方式。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:36:22', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (13, 7, 6, '小学华文基础班', '小学华文基础教学，包括拼音、汉字、词汇学习。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (14, 7, 6, '拼音识字启蒙班', '专注拼音教学和汉字识字启蒙。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (15, 8, 6, '小学阅读理解班', '提升小学生的阅读理解能力和语言表达。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (16, 8, 6, '写作启蒙班', '小学写作启蒙，从看图写话开始。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (17, 9, 6, '古诗词启蒙班', '小学古诗词学习，感受传统文化魅力。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (18, 9, 6, '传统文化班', '通过故事和游戏学习中华传统文化。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (19, 10, 4, 'KET考试冲刺班', '专为KET考试设计的冲刺课程，涵盖听说读写四项技能。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (20, 10, 4, 'KET口语训练班', 'KET考试口语专项训练，提升口语表达能力。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (21, 11, 4, 'KET语法基础班', 'KET考试语法基础课程，系统学习英语语法。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (22, 11, 4, 'KET阅读理解班', 'KET阅读理解专项训练，提升阅读技巧。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (23, 12, 4, 'KET写作训练班', 'KET写作专项训练，掌握写作技巧和方法。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (24, 12, 4, 'KET听力突破班', 'KET听力专项训练，提升听力理解能力。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:38:04', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (25, 13, 5, 'PET考试精品班', 'PET考试专项训练，重点突破语法和词汇难点。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (26, 13, 5, 'PET学术写作班', 'PET考试学术写作训练，提升写作水平。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (27, 14, 5, 'PET口语提升班', 'PET口语专项训练，提升口语流利度和准确性。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (28, 14, 5, 'PET商务英语班', 'PET水平的商务英语应用训练。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (29, 15, 5, 'PET英语文学班', 'PET水平的英语文学鉴赏和分析。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (30, 15, 5, 'PET批判性思维班', '通过英语培养批判性思维能力。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (31, 16, 1, '中学数学竞赛班', '中学数学竞赛训练，培养高级数学思维。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (32, 16, 1, '高等数学预备班', '为学习高等数学打下坚实基础。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (33, 17, 1, '中学函数专题班', '中学数学函数专题深度学习。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (34, 17, 1, '中学几何证明班', '中学几何证明专项训练。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (35, 18, 1, '数学建模班', '中学数学建模训练，培养应用能力。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (36, 18, 1, '创新数学思维班', '培养创新数学思维和解题能力。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:40:53', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (37, 19, 7, '中学物理实验班', '中学物理实验课程，培养实验技能和科学思维。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 90, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (38, 19, 7, '科学研究方法班', '教授科学研究的基本方法和实验设计。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (39, 20, 8, '中学化学实验班', '中学化学实验课程，深入理解化学原理。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (40, 20, 8, '有机化学专题班', '有机化学专题学习，掌握有机反应机理。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (41, 21, 2, '中学生物实验班', '中学生物实验课程，探索生命科学奥秘。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (42, 21, 2, '分子生物学班', '分子生物学基础，了解生命的分子机制。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (43, 22, 6, '中学古代文学班', '中学古代文学学习，深入理解经典作品。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线下', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (44, 22, 6, '文言文精读班', '文言文精读训练，提升古文理解能力。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (45, 23, 6, '中学现代文学班', '中学现代文学鉴赏，培养文学素养。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (46, 23, 6, '中学写作指导班', '中学写作技巧指导，提升表达能力。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (47, 24, 6, '语言学基础班', '语言学基础知识，了解语言的科学原理。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (48, 24, 6, '修辞学应用班', '修辞学应用训练，掌握高级语言技巧。', 5.0000000, 'one_on_one', 199.00, NULL, NULL, NULL, 0, NULL, NULL, 120, 'active', 0, '2025-07-28 21:44:29', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (49, 1, 1, '小学数学思维训练营', '为期8周的小学数学思维训练大班课，培养逻辑思维和解题能力。每周2次课，系统性提升数学素养。', 5.0000000, 'large_class', 299.00, '2025-09-01', '2025-10-26', 25, 0, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', NULL, 120, 'active', 1, '2025-07-28 22:00:00', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (50, 7, 6, '小学语文阅读写作提升班', '专注提升小学生阅读理解和写作能力的大班课程。通过经典文本阅读和写作练习，全面提升语文素养。', 5.0000000, 'large_class', 399.00, '2025-09-02', '2025-11-25', 30, 0, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', NULL, 120, 'active', 1, '2025-07-28 22:00:00', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (51, 10, 4, 'KET考试冲刺大班课', '针对KET考试的集中冲刺训练，12周系统复习，模拟考试练习。适合有一定英语基础的学生。', 5.0000000, 'large_class', 599.00, '2025-09-03', '2025-11-26', 20, 0, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', NULL, 120, 'active', 1, '2025-07-28 22:00:00', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (52, 4, 2, '小学科学实验探索营', '动手实验为主的科学启蒙大班课，每周进行有趣的科学实验，培养科学思维和动手能力。', 5.0000000, 'large_class', 499.00, '2025-09-05', '2025-12-05', NULL, 0, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', NULL, 120, 'active', 0, '2025-07-28 22:00:00', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (53, 16, 1, '中学数学竞赛训练班', '面向有数学天赋学生的竞赛训练大班课，涵盖代数、几何、数论等竞赛专题。', 5.0000000, 'large_class', 899.00, '2025-09-07', '2025-12-07', 15, 0, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', NULL, 120, 'active', 1, '2025-07-28 22:00:00', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (54, 13, 5, 'PET英语能力提升班', '系统提升PET水平英语能力的大班课程，注重听说读写全面发展，为PET考试做准备。', 5.0000000, 'large_class', 699.00, '2025-09-09', '2025-12-09', 18, 0, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', NULL, 120, 'pending', 0, '2025-07-28 22:00:00', 0, NULL, '线上', 1, '184');
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `rating`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `enrollment_count`, `course_time_slots`, `image_url`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`, `course_location`, `supports_online`, `offline_locations`) VALUES (55, 19, 7, '中学物理实验精品班', '中学物理实验大班课，通过系统的实验操作，深入理解物理原理，培养实验技能。', 5.0000000, 'large_class', 499.00, '2025-09-10', '2025-12-10', 12, 0, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', NULL, 120, 'active', 0, '2025-07-28 22:00:00', 0, NULL, '线上', 1, '184');
COMMIT;

-- ----------------------------
-- Table structure for grades
-- ----------------------------
DROP TABLE IF EXISTS `grades`;
CREATE TABLE `grades` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '年级ID，主键自增',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '年级名称',
  `is_active` tinyint NOT NULL COMMENT '是否激活：1-是，0-否',
  `sort_order` int NOT NULL COMMENT '排序顺序',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=185 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='年级表，存储所有年级的信息';

-- ----------------------------
-- Records of grades
-- ----------------------------
BEGIN;
INSERT INTO `grades` (`id`, `name`, `is_active`, `sort_order`, `created_at`, `updated_at`) VALUES (184, '一年级', 1, 0, '2025-09-10 14:14:38', '2025-09-10 14:14:38');
COMMIT;

-- ----------------------------
-- Table structure for hour_details
-- ----------------------------
DROP TABLE IF EXISTS `hour_details`;
CREATE TABLE `hour_details` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课时变动ID，主键自增',
  `user_id` bigint NOT NULL COMMENT '教师用户ID，关联users表',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师姓名',
  `hours` decimal(10,2) NOT NULL COMMENT '变动课时数，正数表示增加，负数表示减少',
  `hours_before` decimal(10,2) NOT NULL COMMENT '变动前课时数',
  `hours_after` decimal(10,2) NOT NULL COMMENT '变动后课时数',
  `transaction_type` tinyint(1) NOT NULL COMMENT '交易类型： 1-增加, 0-减少',
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '变动原因',
  `booking_id` bigint DEFAULT NULL COMMENT '关联的课程ID，可为空',
  `operator_id` bigint DEFAULT NULL COMMENT '操作员ID（如管理员），可为空',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_booking_id` (`booking_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师课时变动记录表';

-- ----------------------------
-- Records of hour_details
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for job_post_subjects
-- ----------------------------
DROP TABLE IF EXISTS `job_post_subjects`;
CREATE TABLE `job_post_subjects` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_post_id` bigint NOT NULL COMMENT '招聘ID，关联job_posts表',
  `subject_id` bigint NOT NULL COMMENT '科目ID，关联subjects表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_subject` (`job_post_id`,`subject_id`),
  KEY `idx_job_post_id` (`job_post_id`),
  KEY `idx_subject_id` (`subject_id`),
  KEY `idx_subject_job` (`subject_id`,`job_post_id`),
  CONSTRAINT `fk_jps_job` FOREIGN KEY (`job_post_id`) REFERENCES `job_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_jps_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='招聘与科目的多对多关联表';

-- ----------------------------
-- Records of job_post_subjects
-- ----------------------------
BEGIN;
INSERT INTO `job_post_subjects` (`id`, `job_post_id`, `subject_id`, `created_at`) VALUES (1, 1001, 1, '2025-09-04 23:38:29');
INSERT INTO `job_post_subjects` (`id`, `job_post_id`, `subject_id`, `created_at`) VALUES (2, 1002, 4, '2025-09-04 23:38:29');
INSERT INTO `job_post_subjects` (`id`, `job_post_id`, `subject_id`, `created_at`) VALUES (3, 1003, 2, '2025-09-04 23:38:29');
INSERT INTO `job_post_subjects` (`id`, `job_post_id`, `subject_id`, `created_at`) VALUES (4, 1004, 6, '2025-09-04 23:38:29');
COMMIT;

-- ----------------------------
-- Table structure for job_posts
-- ----------------------------
DROP TABLE IF EXISTS `job_posts`;
CREATE TABLE `job_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '招聘ID，主键自增',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '招聘标题',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '岗位介绍/职位描述',
  `position_tags` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职位标签，JSON数组字符串，如：["兼职","线上"]',
  `subject_ids` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联科目ID列表，逗号分隔，如：1,3,6',
  `subject_names` varchar(300) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '科目名称列表，逗号分隔，如：小学数学,小学华文,中学数学',
  `status` enum('active','expired') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'active' COMMENT '招聘状态：active-招聘中，expired-已过期',
  `priority` int DEFAULT '0' COMMENT '优先级排序，值越小越靠前',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：1-已删除，0-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_job_posts_deleted_created` (`is_deleted`,`created_at`),
  KEY `idx_job_posts_created` (`created_at`),
  KEY `idx_job_posts_list` (`is_deleted`,`status`,`priority`,`created_at`,`id`),
  FULLTEXT KEY `ft_title_intro` (`title`,`introduction`)
) ENGINE=InnoDB AUTO_INCREMENT=1005 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师招聘信息表，供前台招聘页面展示';

-- ----------------------------
-- Records of job_posts
-- ----------------------------
BEGIN;
INSERT INTO `job_posts` (`id`, `title`, `introduction`, `position_tags`, `subject_ids`, `subject_names`, `status`, `priority`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (1001, 'AEIS数学兼职教师', '负责AEIS数学线上/线下授课，要求有耐心，有相关教学经验。', '[\"兼职\",\"线上\",\"线下\"]', '1', '数学', 'active', 10, '2025-08-15 22:38:47', '2025-08-15 22:38:47', 0, NULL);
INSERT INTO `job_posts` (`id`, `title`, `introduction`, `position_tags`, `subject_ids`, `subject_names`, `status`, `priority`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (1002, 'AEIS英语教师', '负责AEIS英语课程，小班教学，注重口语与阅读能力提升。', '[\"全职\",\"远程\"]', '4', 'KET', 'active', 20, '2025-08-15 22:38:47', '2025-08-15 22:38:47', 0, NULL);
INSERT INTO `job_posts` (`id`, `title`, `introduction`, `position_tags`, `subject_ids`, `subject_names`, `status`, `priority`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (1003, '高中JC科学精品课主讲', '主讲高中科学精品课程，擅长模型化与解题思维训练；可提供讲义。', '[\"全职\",\"线下优先\"]', '2', '科学', 'active', 30, '2025-08-15 22:38:47', '2025-08-21 12:18:03', 0, NULL);
INSERT INTO `job_posts` (`id`, `title`, `introduction`, `position_tags`, `subject_ids`, `subject_names`, `status`, `priority`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (1004, '华文阅读写作指导', '教授阅读理解与写作技巧，面向小学和中学段学生；支持晚间时段授课。', '[\"兼职\",\"晚间\"]', '6', '华文', 'active', 40, '2025-08-15 22:38:47', '2025-08-15 22:38:47', 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for lesson_grades
-- ----------------------------
DROP TABLE IF EXISTS `lesson_grades`;
CREATE TABLE `lesson_grades` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '成绩记录ID，主键自增',
  `schedule_id` bigint NOT NULL COMMENT '课程安排ID，关联course_schedules表',
  `student_id` bigint NOT NULL COMMENT '学生ID，关联students表',
  `score` decimal(5,2) DEFAULT NULL COMMENT '本节课成绩分数',
  `teacher_comment` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '教师对本节课的评价和建议',
  `graded_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成绩录入时间',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：0-未删除，1-已删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schedule_student` (`schedule_id`,`student_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_schedule_id` (`schedule_id`),
  KEY `idx_graded_at` (`graded_at`),
  CONSTRAINT `fk_lesson_grades_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `course_schedules` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_lesson_grades_student` FOREIGN KEY (`student_id`) REFERENCES `students` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程成绩表';

-- ----------------------------
-- Records of lesson_grades
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message` (
  `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '消息ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '消息内容',
  `target_type` enum('STUDENT','TEACHER','ALL') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ALL' COMMENT '目标用户类型：STUDENT-学生，TEACHER-教师，ALL-全体',
  `admin_id` bigint unsigned NOT NULL COMMENT '发布管理员ID',
  `admin_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '发布管理员姓名',
  `is_active` tinyint(1) NOT NULL DEFAULT '1' COMMENT '是否激活：1-激活，0-停用',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_target_type` (`target_type`),
  KEY `idx_admin_id` (`admin_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB AUTO_INCREMENT=34 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='消息表';

-- ----------------------------
-- Records of message
-- ----------------------------
BEGIN;
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (1, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-08-24 00:32:33', '2025-08-24 00:32:33');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (2, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-08-24 00:32:33', '2025-08-24 00:32:33');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (3, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-08-24 00:32:33', '2025-08-24 00:32:33');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (4, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-08-24 21:31:08', '2025-08-24 21:31:08');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (5, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-08-24 21:31:08', '2025-08-24 21:31:08');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (6, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-08-24 21:31:08', '2025-08-24 21:31:08');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (7, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-08-27 00:39:04', '2025-08-27 10:17:00');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (8, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-08-27 00:39:04', '2025-08-27 00:39:04');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (9, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-08-27 00:39:04', '2025-08-27 00:39:04');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (10, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-08-31 00:29:03', '2025-08-31 00:29:03');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (11, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-08-31 00:29:03', '2025-08-31 00:29:03');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (12, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-08-31 00:29:03', '2025-08-31 00:29:03');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (13, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-09-01 00:27:58', '2025-09-01 00:27:58');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (14, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-09-01 00:27:58', '2025-09-01 00:27:58');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (15, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-09-01 00:27:58', '2025-09-01 00:27:58');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (16, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-09-02 17:54:42', '2025-09-02 17:54:42');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (17, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-09-02 17:54:42', '2025-09-02 17:54:42');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (18, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-09-02 17:54:42', '2025-09-02 17:54:42');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (19, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-09-02 21:13:13', '2025-09-02 21:13:13');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (20, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-09-02 21:13:13', '2025-09-02 21:13:13');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (21, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-09-02 21:13:13', '2025-09-02 21:13:13');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (22, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-09-02 22:03:21', '2025-09-02 22:03:21');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (23, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-09-02 22:03:21', '2025-09-02 22:03:21');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (24, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-09-02 22:03:21', '2025-09-02 22:03:21');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (25, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-09-04 22:17:33', '2025-09-04 22:17:33');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (26, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-09-04 22:17:33', '2025-09-04 22:17:33');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (27, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-09-04 22:17:33', '2025-09-04 22:17:33');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (28, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-09-04 23:38:18', '2025-09-04 23:38:18');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (29, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-09-04 23:38:18', '2025-09-04 23:38:18');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (30, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-09-04 23:38:18', '2025-09-04 23:38:18');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (31, '欢迎加入抢老师平台', '欢迎各位同学加入我们的在线教育平台，这里有优质的师资资源等待您的发现！', 'STUDENT', 1, '系统管理员', 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (32, '平台教师指导手册', '各位老师，感谢您加入我们的平台。请仔细阅读教师指导手册，了解平台的各项功能和操作流程。', 'TEACHER', 1, '系统管理员', 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29');
INSERT INTO `message` (`id`, `title`, `content`, `target_type`, `admin_id`, `admin_name`, `is_active`, `created_at`, `updated_at`) VALUES (33, '平台维护通知', '为了提供更好的服务体验，平台将于本周日凌晨2:00-4:00进行系统维护，届时可能会影响正常使用，敬请谅解。', 'ALL', 1, '系统管理员', 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29');
COMMIT;

-- ----------------------------
-- Table structure for reschedule_requests
-- ----------------------------
DROP TABLE IF EXISTS `reschedule_requests`;
CREATE TABLE `reschedule_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '调课申请ID',
  `schedule_id` bigint NOT NULL COMMENT '原课程安排ID',
  `applicant_id` bigint NOT NULL COMMENT '申请人ID(学生或教师)',
  `applicant_type` enum('student','teacher') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请人类型',
  `request_type` enum('single','recurring','cancel') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '申请类型：single-单次调课，recurring-周期性调课，cancel-取消课程',
  `original_date` date NOT NULL COMMENT '原定日期',
  `original_start_time` time NOT NULL COMMENT '原定开始时间',
  `original_end_time` time NOT NULL COMMENT '原定结束时间',
  `new_date` date DEFAULT NULL COMMENT '新日期',
  `new_start_time` time DEFAULT NULL COMMENT '新开始时间',
  `new_end_time` time DEFAULT NULL COMMENT '新结束时间',
  `new_weekly_schedule` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '新的周期性安排，格式：周一,周三 14:00-16:00;周五 18:00-20:00',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '调课原因',
  `urgency_level` enum('low','medium','high') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'medium' COMMENT '紧急程度',
  `advance_notice_hours` int DEFAULT NULL COMMENT '提前通知小时数',
  `status` enum('pending','approved','rejected','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'pending' COMMENT '申请状态',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人ID',
  `reviewer_type` enum('teacher','student','admin') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '审核人类型',
  `review_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '审核备注',
  `compensation_amount` decimal(10,2) DEFAULT '0.00' COMMENT '补偿金额(如需要)',
  `affects_future_sessions` tinyint(1) DEFAULT '0' COMMENT '是否影响后续课程',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `reviewed_at` timestamp NULL DEFAULT NULL COMMENT '审核时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='调课申请表，记录课程时间调整申请';

-- ----------------------------
-- Records of reschedule_requests
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for student_subjects
-- ----------------------------
DROP TABLE IF EXISTS `student_subjects`;
CREATE TABLE `student_subjects` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint NOT NULL COMMENT '学生ID，关联students表',
  `subject_id` bigint NOT NULL COMMENT '科目ID，关联subjects表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_subject` (`student_id`,`subject_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_subject_id` (`subject_id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生感兴趣科目关联表';

-- ----------------------------
-- Records of student_subjects
-- ----------------------------
BEGIN;
INSERT INTO `student_subjects` (`id`, `student_id`, `subject_id`, `created_at`) VALUES (10, 6, 1, '2025-09-11 10:36:03');
INSERT INTO `student_subjects` (`id`, `student_id`, `subject_id`, `created_at`) VALUES (11, 6, 2, '2025-09-11 10:36:03');
INSERT INTO `student_subjects` (`id`, `student_id`, `subject_id`, `created_at`) VALUES (12, 6, 6, '2025-09-11 10:36:03');
COMMIT;

-- ----------------------------
-- Table structure for students
-- ----------------------------
DROP TABLE IF EXISTS `students`;
CREATE TABLE `students` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '学生ID，主键自增',
  `user_id` bigint NOT NULL COMMENT '关联用户表的用户ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生真实姓名',
  `subjects_interested` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '感兴趣的科目列表如：[数学,英语,物理]',
  `learning_goals` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '学习目标和需求描述',
  `preferred_teaching_style` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '偏好的教学风格，如：严格型、温和型、互动型',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '学生余额，单位：M豆',
  `budget_range` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '预算范围，如：100-200元/小时',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `gender` enum('男','女','不愿透露') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '不愿透露' COMMENT '性别：男、女、不愿透露',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生详细信息表，存储学生的个人资料和学习偏好';

-- ----------------------------
-- Records of students
-- ----------------------------
BEGIN;
INSERT INTO `students` (`id`, `user_id`, `real_name`, `subjects_interested`, `learning_goals`, `preferred_teaching_style`, `balance`, `budget_range`, `is_deleted`, `deleted_at`, `gender`) VALUES (6, 10, '小明', '数学,科学,华文', '希望提高数学和科学成绩，加强华文阅读能力', '实践型教学', 990438.00, '100-200', 0, NULL, '不愿透露');
COMMIT;

-- ----------------------------
-- Table structure for study_abroad_countries
-- ----------------------------
DROP TABLE IF EXISTS `study_abroad_countries`;
CREATE TABLE `study_abroad_countries` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '留学国家ID，主键自增',
  `country_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '国家名称',
  `sort_order` int DEFAULT '0' COMMENT '排序权重，数值越小越靠前',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：1-启用，0-禁用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：1-已删除，0-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country_name_isdel` (`country_name`,`is_deleted`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_sac_active_deleted_sort` (`is_active`,`is_deleted`,`sort_order`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='留学国家管理表';

-- ----------------------------
-- Records of study_abroad_countries
-- ----------------------------
BEGIN;
INSERT INTO `study_abroad_countries` (`id`, `country_name`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (1, '中国', 0, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
INSERT INTO `study_abroad_countries` (`id`, `country_name`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (2, '美国', 0, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
INSERT INTO `study_abroad_countries` (`id`, `country_name`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (3, '英国', 0, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
INSERT INTO `study_abroad_countries` (`id`, `country_name`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (4, '加拿大', 0, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for study_abroad_programs
-- ----------------------------
DROP TABLE IF EXISTS `study_abroad_programs`;
CREATE TABLE `study_abroad_programs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '留学项目ID，主键自增',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目标题',
  `country_id` bigint NOT NULL COMMENT '留学国家ID，关联study_abroad_countries表',
  `stage_id` bigint NOT NULL COMMENT '留学阶段ID，关联study_abroad_stages表',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '项目详细描述',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目图片URL',
  `tags` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '项目标签，JSON格式存储',
  `is_hot` tinyint(1) DEFAULT '0' COMMENT '是否为热门项目：1-是，0-否',
  `sort_order` int DEFAULT '0' COMMENT '排序权重，数值越小越靠前',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：1-启用，0-禁用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：1-已删除，0-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_country_id` (`country_id`),
  KEY `idx_stage_id` (`stage_id`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_is_hot` (`is_hot`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_sap_active_deleted_sort` (`is_active`,`is_deleted`,`sort_order`,`id`),
  KEY `idx_sap_country_stage_active` (`country_id`,`stage_id`,`is_active`,`is_deleted`),
  KEY `idx_sap_country_active` (`country_id`,`is_active`,`is_deleted`),
  KEY `idx_sap_stage_active` (`stage_id`,`is_active`,`is_deleted`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='留学项目管理表';

-- ----------------------------
-- Records of study_abroad_programs
-- ----------------------------
BEGIN;
INSERT INTO `study_abroad_programs` (`id`, `title`, `country_id`, `stage_id`, `description`, `image_url`, `tags`, `is_hot`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (1, '英国高中A-Level申请指导', 3, 1, 'A-Level 选课、文书、面试全流程辅导', NULL, '[\"文书指导\",\"面试培训\",\"选课规划\"]', 1, 10, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
INSERT INTO `study_abroad_programs` (`id`, `title`, `country_id`, `stage_id`, `description`, `image_url`, `tags`, `is_hot`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (2, '美国本科通用申请（Common App）', 2, 2, 'Common App 文书、活动梳理、网申提交', NULL, '[\"文书指导\",\"活动背景\",\"网申\"]', 1, 20, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
INSERT INTO `study_abroad_programs` (`id`, `title`, `country_id`, `stage_id`, `description`, `image_url`, `tags`, `is_hot`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (3, '加拿大硕士工科申请', 4, 3, '套磁、简历优化、研究计划书', NULL, '[\"套磁\",\"简历优化\",\"研究计划\"]', 0, 30, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
INSERT INTO `study_abroad_programs` (`id`, `title`, `country_id`, `stage_id`, `description`, `image_url`, `tags`, `is_hot`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (4, '中国港澳本科申请', 1, 2, '港大/港中文等院校申请咨询', NULL, '[\"院校选择\",\"文书指导\"]', 0, 40, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for study_abroad_stages
-- ----------------------------
DROP TABLE IF EXISTS `study_abroad_stages`;
CREATE TABLE `study_abroad_stages` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '留学阶段ID，主键自增',
  `stage_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '阶段名称，如：高中、本科、硕士',
  `sort_order` int DEFAULT '0' COMMENT '排序权重，数值越小越靠前',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：1-启用，0-禁用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：1-已删除，0-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_name_isdel` (`stage_name`,`is_deleted`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_sort_order` (`sort_order`),
  KEY `idx_sas_active_deleted_sort` (`is_active`,`is_deleted`,`sort_order`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='留学阶段管理表';

-- ----------------------------
-- Records of study_abroad_stages
-- ----------------------------
BEGIN;
INSERT INTO `study_abroad_stages` (`id`, `stage_name`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (1, '高中', 0, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
INSERT INTO `study_abroad_stages` (`id`, `stage_name`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (2, '本科', 0, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
INSERT INTO `study_abroad_stages` (`id`, `stage_name`, `sort_order`, `is_active`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (3, '硕士', 0, 1, '2025-09-04 23:38:29', '2025-09-04 23:38:29', 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for subjects
-- ----------------------------
DROP TABLE IF EXISTS `subjects`;
CREATE TABLE `subjects` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '科目ID，主键自增',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '科目名称，如：数学、语文、英语',
  `icon_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '科目图标URL地址',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：true-启用，false-禁用',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目分类表，定义平台支持的所有教学科目';

-- ----------------------------
-- Records of subjects
-- ----------------------------
BEGIN;
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (1, '数学', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (2, '科学', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (3, '英文', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (4, 'KET', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (5, 'PET', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (6, '华文', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (7, '物理', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (8, '化学', '', 1, 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for suspension_requests
-- ----------------------------
DROP TABLE IF EXISTS `suspension_requests`;
CREATE TABLE `suspension_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '停课申请ID',
  `enrollment_id` bigint NOT NULL COMMENT '课程报名ID，关联course_enrollments',
  `student_id` bigint NOT NULL COMMENT '学生ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID',
  `reason` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '停课原因',
  `start_date` date DEFAULT NULL COMMENT '停课开始日期',
  `end_date` date DEFAULT NULL COMMENT '停课结束日期',
  `status` enum('pending','approved','rejected','cancelled') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'pending' COMMENT '申请状态',
  `admin_id` bigint DEFAULT NULL COMMENT '审核管理员ID',
  `admin_notes` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '管理员审核备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `reviewed_at` timestamp NULL DEFAULT NULL COMMENT '审核时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  KEY `idx_enrollment_id` (`enrollment_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_susp_req_enrollment_status` (`enrollment_id`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='停课申请表，记录学生发起的课程暂停申请';

-- ----------------------------
-- Records of suspension_requests
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for teacher_level
-- ----------------------------
DROP TABLE IF EXISTS `teacher_level`;
CREATE TABLE `teacher_level` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '教师级别ID，主键自增',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师级别名称',
  `is_active` tinyint NOT NULL COMMENT '是否激活：1-是，0-否',
  `sort_order` int NOT NULL COMMENT '排序顺序',
  `create_time` datetime DEFAULT NULL COMMENT '创建时间',
  `update_time` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师级别表';

-- ----------------------------
-- Records of teacher_level
-- ----------------------------
BEGIN;
INSERT INTO `teacher_level` (`id`, `name`, `is_active`, `sort_order`, `create_time`, `update_time`) VALUES (8, '铜牌', 0, 0, '2025-01-10 10:00:00', '2025-01-10 10:00:00');
INSERT INTO `teacher_level` (`id`, `name`, `is_active`, `sort_order`, `create_time`, `update_time`) VALUES (9, '银牌', 0, 0, '2025-03-15 14:30:00', '2025-03-15 14:30:00');
INSERT INTO `teacher_level` (`id`, `name`, `is_active`, `sort_order`, `create_time`, `update_time`) VALUES (10, '金牌', 0, 0, '2025-05-20 09:15:00', '2025-05-20 09:15:00');
INSERT INTO `teacher_level` (`id`, `name`, `is_active`, `sort_order`, `create_time`, `update_time`) VALUES (11, '王牌', 0, 0, '2025-08-28 16:45:00', '2025-08-28 16:45:00');
COMMIT;

-- ----------------------------
-- Table structure for teacher_subjects
-- ----------------------------
DROP TABLE IF EXISTS `teacher_subjects`;
CREATE TABLE `teacher_subjects` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacher_id` bigint NOT NULL COMMENT '教师ID，关联teachers表',
  `subject_id` bigint NOT NULL COMMENT '科目ID，关联subjects表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_subject` (`teacher_id`,`subject_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_subject_id` (`subject_id`),
  KEY `idx_ts_teacher_subject` (`teacher_id`,`subject_id`),
  KEY `idx_ts_subject_teacher` (`subject_id`,`teacher_id`),
  CONSTRAINT `fk_teacher_subjects_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_teacher_subjects_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=26 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师科目关联表';

-- ----------------------------
-- Records of teacher_subjects
-- ----------------------------
BEGIN;
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (1, 1, 1, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (2, 2, 1, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (3, 3, 1, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (4, 4, 2, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (5, 5, 2, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (6, 6, 2, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (7, 7, 6, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (8, 8, 6, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (9, 9, 6, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (10, 10, 4, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (11, 11, 4, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (12, 12, 4, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (13, 13, 5, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (14, 14, 5, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (15, 15, 5, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (16, 16, 1, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (17, 17, 1, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (18, 18, 1, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (19, 19, 7, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (20, 20, 8, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (21, 21, 2, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (23, 23, 6, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (24, 24, 6, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (25, 22, 6, '2025-09-10 15:00:11');
COMMIT;

-- ----------------------------
-- Table structure for teachers
-- ----------------------------
DROP TABLE IF EXISTS `teachers`;
CREATE TABLE `teachers` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '教师ID，主键自增',
  `user_id` bigint NOT NULL COMMENT '关联用户表的用户ID',
  `real_name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '教师真实姓名',
  `education_background` enum('专科及以下','本科','硕士','博士') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '教育背景：专科及以下、本科、硕士、博士',
  `rating` decimal(10,7) DEFAULT '5.0000000' COMMENT '教师评分，0-5分',
  `teaching_experience` int DEFAULT NULL COMMENT '教学经验年数',
  `specialties` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '专业领域如：[高考数学,竞赛辅导,基础提升]',
  `hourly_rate` decimal(10,2) DEFAULT NULL COMMENT '每小时收费标准，单位：元',
  `introduction` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '个人介绍和教学理念',
  `photo_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '个人照片',
  `gender` enum('男','女','不愿透露') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '不愿透露' COMMENT '性别：男、女、不愿透露',
  `is_verified` tinyint(1) DEFAULT '0' COMMENT '是否已认证：true-已认证，false-未认证',
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否展示到首页：true-是，false-否',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `current_hours` decimal(10,2) DEFAULT '0.00' COMMENT '本月课时数',
  `last_hours` decimal(10,2) DEFAULT '0.00' COMMENT '上个月课时数',
  `available_time_slots` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci COMMENT '可上课时间安排，JSON格式存储：[{"weekday":1,"timeSlots":["08:00-10:00","17:00-19:00"]},{"weekday":6,"timeSlots":["13:00-15:00","15:00-17:00"]}]，weekday: 1=周一,2=周二...7=周日',
  `level` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT '王牌' COMMENT '教育级别：王牌、金牌、银牌、铜牌等',
  `teaching_locations` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '授课地点',
  `supports_online` tinyint(1) DEFAULT '0' COMMENT '是否支持线上',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `idx_is_featured` (`is_featured`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师详细信息表，存储教师的专业资料和教学信息';

-- ----------------------------
-- Records of teachers
-- ----------------------------
BEGIN;
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (1, 143, '李明华', '硕士', 5.0000000, 10, '小学数学基础,思维训练,奥数启蒙', 150.00, '师范大学数学教育专业，10年小学数学教学经验。擅长培养学生数学思维，让孩子在游戏中学会数学。', NULL, '男', 1, 1, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (2, 144, '王雅琳', '硕士', 5.0000000, 8, '小学数学,应用题专项,计算能力', 140.00, '华师大数学硕士，8年小学数学教学经验。特别擅长应用题教学，帮助学生建立数学思维模式。', NULL, '女', 1, 1, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (3, 145, '张志强', '硕士', 5.0000000, 12, '小学数学,几何启蒙,数学游戏', 160.00, '南师大数学教育专业，12年教学经验。善于用生动有趣的方式教授数学，让学生爱上数学学习。', NULL, '男', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (4, 146, '陈博士', '博士', 5.0000000, 15, '小学科学,自然观察,科学实验', 180.00, '中科院博士，15年科学教育经验。擅长通过实验和观察培养学生科学思维，让抽象的科学概念变得生动有趣。', NULL, '男', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (5, 147, '刘晓敏', '硕士', 5.0000000, 9, '小学科学,科学启蒙,动手实验', 160.00, '师范大学科学教育专业，专注小学科学教育9年。善于用生活中的例子解释科学原理，激发学生对科学的兴趣。', NULL, '女', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (6, 148, '赵宇航', '硕士', 5.0000000, 7, '小学科学,环境科学,科学探究', 150.00, '华师科学教育硕士，7年教学经验。特别擅长环境科学教学，通过户外观察让学生了解自然，培养环保意识。', NULL, '男', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (7, 149, '林雅文', '硕士', 5.0000000, 11, '小学华文,拼音教学,识字启蒙', 160.00, '台师大中文硕士，11年小学华文教学经验。擅长拼音和识字教学，让孩子轻松掌握华文基础。', NULL, '女', 1, 1, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (8, 150, '黄志华', '硕士', 5.0000000, 8, '小学华文,阅读理解,写作启蒙', 150.00, '北语汉教硕士，专注小学华文教育8年。特别擅长阅读理解和写作启蒙，培养学生的语言表达能力。', NULL, '男', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (9, 151, '郑美玲', '硕士', 5.0000000, 6, '小学华文,古诗词,传统文化', 140.00, '港中大中文硕士，6年小学华文教学经验。专注传统文化教育，通过古诗词让孩子感受中华文化魅力。', NULL, '女', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (10, 152, 'Emma Wilson', '硕士', 5.0000000, 8, 'KET考试辅导,少儿英语,口语训练', 200.00, '英国剑桥大学硕士，专注于KET考试辅导8年。帮助超过200名学生成功通过KET考试，教学风格生动有趣。', NULL, '女', 1, 1, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (11, 153, 'David Smith', '硕士', 5.0000000, 6, 'KET考试,英语语法,阅读理解', 180.00, '美国外教，专业TESOL认证，擅长KET考试技巧指导。课堂氛围轻松愉快，让学生在快乐中学习英语。', NULL, '男', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (12, 154, 'Sarah Johnson', '硕士', 5.0000000, 5, 'KET考试,写作训练,听力提升', 170.00, '澳洲海归教师，专注KET考试培训5年。特别擅长写作和听力训练，学生通过率高达95%。', NULL, '女', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (13, 155, 'Michael Brown', '硕士', 5.0000000, 10, 'PET考试辅导,高级英语,学术写作', 220.00, '牛津大学硕士，10年PET考试辅导经验。专注高级英语教学，帮助学生达到更高的英语水平。', NULL, '男', 1, 1, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (14, 156, 'Jennifer Lee', '硕士', 5.0000000, 7, 'PET考试,英语口语,商务英语', 200.00, '加拿大外教，应用语言学硕士，7年PET教学经验。擅长口语训练和商务英语，让学生自信开口说英语。', NULL, '女', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (15, 157, 'Robert Taylor', '硕士', 5.0000000, 9, 'PET考试,英语文学,批判性思维', 210.00, '美国外教，英语教育硕士，9年教学经验。注重培养学生的批判性思维和英语文学鉴赏能力。', NULL, '男', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (16, 158, '李教授', '博士', 5.0000000, 15, '中学数学,高等数学,竞赛数学', 250.00, '清华数学博士，15年中学数学教学经验。擅长高等数学和竞赛数学，所教学生多次获得数学竞赛奖项。', NULL, '男', 1, 1, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (17, 159, '王博士', '博士', 5.0000000, 12, '中学数学,函数专题,几何证明', 230.00, '北大数学博士，专注中学数学教学12年。特别擅长函数、几何等难点突破，帮助学生建立完整的数学知识体系。', NULL, '女', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (18, 160, '张院士', '博士', 5.0000000, 18, '中学数学,数学建模,创新思维', 280.00, '中科院博士，18年教学经验。擅长数学建模和创新思维培养，让学生学会用数学解决实际问题。', NULL, '男', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (19, 161, '陈院士', '博士', 5.0000000, 20, '中学物理,实验物理,科学研究', 300.00, '中科院物理博士，20年科学教育经验。擅长实验物理教学，培养学生的科学研究能力和创新精神。', NULL, '男', 1, 1, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (20, 162, '刘博士', '博士', 5.0000000, 14, '中学化学,有机化学,化学实验', 260.00, '北理工化学博士，14年中学化学教学经验。特别擅长有机化学和实验教学，让抽象的化学概念变得具体可感。', NULL, '女', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (21, 163, '赵教授', '博士', 5.0000000, 16, '中学生物,分子生物学,生命科学', 270.00, '华科生物博士，16年教学经验。专注分子生物学和生命科学教育，培养学生对生命科学的深度理解。', NULL, '男', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (22, 164, '林教授', '博士', 5.0000000, 17, '中学华文,古代文学,文言文', 240.00, '北师大中文博士，17年中学华文教学经验。专精古代文学和文言文，让学生深入理解中华文化精髓。', NULL, '女', 1, 1, 0, NULL, 0.00, 0.00, '[{\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"],\"weekday\":1},{\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"],\"weekday\":2},{\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"],\"weekday\":3},{\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"],\"weekday\":4},{\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"],\"weekday\":5},{\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"],\"weekday\":6},{\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"],\"weekday\":7}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (23, 165, '黄博士', '博士', 5.0000000, 13, '中学华文,现代文学,写作指导', 220.00, '复旦中文博士，专注中学华文教育13年。擅长现代文学和写作指导，培养学生的文学素养和表达能力。', NULL, '男', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (24, 166, '郑教授', '博士', 5.0000000, 14, '中学华文,语言学,修辞学', 230.00, '中大中文博士，14年教学经验。专精语言学和修辞学，帮助学生掌握高级语言运用技巧。', NULL, '女', 1, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `rating`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `photo_url`, `gender`, `is_verified`, `is_featured`, `is_deleted`, `deleted_at`, `current_hours`, `last_hours`, `available_time_slots`, `level`, `teaching_locations`, `supports_online`) VALUES (37, 183, '青', '博士', 5.0000000, 15, '语言学', 12.00, '博士', NULL, '男', 0, 0, 0, NULL, 0.00, 0.00, '[{\"weekday\":1,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":2,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":3,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":4,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":5,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":6,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]},{\"weekday\":7,\"timeSlots\":[\"08:00-10:00\",\"10:00-12:00\",\"13:00-15:00\",\"15:00-17:00\",\"17:00-19:00\",\"19:00-21:00\"]}]', '王牌', '184', 1);
COMMIT;

-- ----------------------------
-- Table structure for teaching_locations
-- ----------------------------
DROP TABLE IF EXISTS `teaching_locations`;
CREATE TABLE `teaching_locations` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '授课地址ID，主键自增',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授课地点名称',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授课地点详细地址',
  `is_active` tinyint NOT NULL COMMENT '是否激活：1-是，0-否',
  `sort_order` int NOT NULL COMMENT '排序顺序',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=185 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='授课地址表，存储所有授课地点的信息';

-- ----------------------------
-- Records of teaching_locations
-- ----------------------------
BEGIN;
INSERT INTO `teaching_locations` (`id`, `name`, `address`, `is_active`, `sort_order`, `created_at`, `updated_at`) VALUES (184, '南宁市', '南宁市青秀区', 1, 0, '2025-09-10 11:18:30', '2025-09-10 11:18:30');
COMMIT;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键自增',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名，唯一标识',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '邮箱地址，用于登录和通知',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（加密）Bcrypt',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '手机号码',
  `birth_date` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '出生年月，格式：YYYY-MM',
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '头像图片URL地址',
  `user_type` enum('student','teacher','admin') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户类型：student-学生，teacher-教师，admin-管理员',
  `status` enum('active','inactive','banned') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'active' COMMENT '账户状态：active-激活，inactive-未激活，banned-封禁',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `trial_times` int DEFAULT '1' COMMENT '免费试听课次数，新用户默认为1',
  `adjustment_times` int DEFAULT '3' COMMENT '教师/学生调课次数，每个月三次，月初刷新',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=184 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户基础信息表，存储所有用户的通用信息';

-- ----------------------------
-- Records of users
-- ----------------------------
BEGIN;
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (10, 'student', 'qinghaoyang@foxmail.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '', NULL, NULL, 'student', 'active', '2025-07-19 13:21:54', '2025-09-11 10:36:03', 0, NULL, 4, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (11, 'admin', 'admin@admin.com', '$2a$10$CLJSuGd2ptKI9VlCz3r4buGyY7HfKg1qivwbKEfkk8/6Pz57oKjWK', NULL, NULL, NULL, 'admin', 'active', '2025-07-19 16:23:39', '2025-07-29 13:24:12', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (143, 'teacher_primary_math_01', 'primary_math01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800001001', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (144, 'teacher_primary_math_02', 'primary_math02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800001002', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (145, 'teacher_primary_math_03', 'primary_math03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800001003', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (146, 'teacher_primary_science_01', 'primary_science01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800002001', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (147, 'teacher_primary_science_02', 'primary_science02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800002002', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (148, 'teacher_primary_science_03', 'primary_science03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800002003', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (149, 'teacher_primary_chinese_01', 'primary_chinese01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800003001', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (150, 'teacher_primary_chinese_02', 'primary_chinese02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800003002', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (151, 'teacher_primary_chinese_03', 'primary_chinese03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800003003', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (152, 'teacher_ket_01', 'ket01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800004001', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (153, 'teacher_ket_02', 'ket02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800004002', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (154, 'teacher_ket_03', 'ket03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800004003', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (155, 'teacher_pet_01', 'pet01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800005001', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (156, 'teacher_pet_02', 'pet02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800005002', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (157, 'teacher_pet_03', 'pet03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800005003', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (158, 'teacher_middle_math_01', 'middle_math01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800006001', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (159, 'teacher_middle_math_02', 'middle_math02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800006002', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (160, 'teacher_middle_math_03', 'middle_math03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800006003', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (161, 'teacher_middle_science_01', 'middle_science01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800007001', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (162, 'teacher_middle_science_02', 'middle_science02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800007002', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (163, 'teacher_middle_science_03', 'middle_science03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800007003', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (164, 'teacher_middle_chinese_01', 'middle_chinese01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800008001', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-09-10 15:00:11', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (165, 'teacher_middle_chinese_02', 'middle_chinese02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800008002', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (166, 'teacher_middle_chinese_03', 'middle_chinese03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800008003', NULL, NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 1, 3);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `trial_times`, `adjustment_times`) VALUES (183, 'teacher', 'qhycursor@126.com', '$2a$10$jlFOPIGNKGflN7bUxRAR2OZ9F8NdnZotJaRsZ/cZ.IBPFPz5Gc/da', '', NULL, NULL, 'teacher', 'active', '2025-07-29 13:07:29', '2025-07-29 13:07:29', 0, NULL, 1, 3);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
