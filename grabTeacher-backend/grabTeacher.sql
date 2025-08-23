/*
 Navicat Premium Dump SQL

 Source Server         : mysql
 Source Server Type    : MySQL
 Source Server Version : 90100 (9.1.0)
 Source Host           : localhost:3306
 Source Schema         : grabTeacher

 Target Server Type    : MySQL
 Target Server Version : 50700 (5.7.0) - Compatible Version
 File Encoding         : 65001

 Date: 28/07/2025 23:10:14
 Modified for MySQL 5.7 compatibility
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for admins
-- ----------------------------
DROP TABLE IF EXISTS `admins`;
CREATE TABLE `admins` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '管理员ID，主键自增',
  `user_id` bigint(20) NOT NULL COMMENT '关联用户表的用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '管理员真实姓名',
  `wechat_qrcode_url` varchar(255) DEFAULT NULL COMMENT '微信二维码图片URL',
  `whatsapp_number` varchar(30) DEFAULT NULL COMMENT 'WhatsApp号码',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `email` varchar(100) DEFAULT NULL COMMENT '邮箱',
  `notes` text COMMENT '管理员备注信息',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='管理员详细信息表，存储管理员的职务信息和权限';

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
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '预约申请ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生ID',
  `teacher_id` bigint(20) NOT NULL COMMENT '教师ID',
  `course_id` bigint(20) DEFAULT NULL COMMENT '课程ID，可为空(自定义预约)',
  `booking_type` enum('single','recurring') NOT NULL COMMENT '预约类型：single-单次，recurring-周期性',
  `requested_date` date DEFAULT NULL COMMENT '请求的上课日期(单次预约)',
  `requested_start_time` time DEFAULT NULL COMMENT '请求的开始时间(单次预约)',
  `requested_end_time` time DEFAULT NULL COMMENT '请求的结束时间(单次预约)',
  `recurring_weekdays` varchar(50) DEFAULT NULL COMMENT '周期性预约的星期几，逗号分隔：1,3,5',
  `recurring_time_slots` varchar(200) DEFAULT NULL COMMENT '周期性预约的时间段，逗号分隔：14:00-16:00,18:00-20:00',
  `start_date` date DEFAULT NULL COMMENT '周期性预约开始日期',
  `end_date` date DEFAULT NULL COMMENT '周期性预约结束日期',
  `total_times` int(11) DEFAULT NULL COMMENT '总课程次数',
  `student_requirements` text COMMENT '学生需求说明',
  `status` enum('pending','approved','rejected','cancelled') DEFAULT 'pending' COMMENT '申请状态pending-待定中，approved-已批准，rejected-已拒绝，cancelled-已取消',
  `teacher_reply` text COMMENT '教师回复内容',
  `admin_notes` text COMMENT '管理员备注',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `approved_at` timestamp NULL DEFAULT NULL COMMENT '批准时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `is_trial` tinyint(1) DEFAULT '0' COMMENT '是否为免费试听课：true-是，false-否',
  `trial_duration_minutes` int(11) DEFAULT NULL COMMENT '试听课时长（分钟）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='预约申请表，记录学生的课程预约申请';

-- ----------------------------
-- Records of booking_requests
-- ----------------------------
BEGIN;
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (3, 6, 7, NULL, 'recurring', NULL, NULL, NULL, '2,3', '17:00-18:00', '2025-07-22', '2025-08-26', 12, '希望预约12355老师的语文课程', 'approved', '可以', NULL, '2025-07-21 13:07:49', '2025-07-21 13:12:05', '2025-07-21 13:12:05', 0, NULL, 0, NULL);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (4, 6, 7, 3, 'recurring', NULL, NULL, NULL, '2,5', '14:00-15:00', '2025-07-23', '2025-08-27', 12, '希望预约12355老师的《语文》课程', 'approved', '可以', NULL, '2025-07-21 13:47:39', '2025-07-21 13:47:58', '2025-07-21 13:47:58', 1, '2025-07-28 15:23:31', 0, NULL);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (5, 6, 7, 3, 'single', '2025-08-16', '11:00:00', '11:30:00', NULL, NULL, NULL, NULL, NULL, '希望预约12355老师的《语文》课程', 'pending', NULL, NULL, '2025-07-21 22:03:14', '2025-07-21 22:03:14', NULL, 1, '2025-07-28 15:23:31', 1, 30);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (6, 6, 7, 3, 'single', '2025-07-30', '10:30:00', '11:00:00', NULL, NULL, NULL, NULL, NULL, '希望预约12355老师的《语文》课程', 'pending', NULL, NULL, '2025-07-21 22:03:24', '2025-07-21 22:03:24', NULL, 1, '2025-07-28 15:23:31', 1, 30);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (7, 6, 115, 5001, 'single', '2025-08-08', '10:30:00', '11:00:00', NULL, NULL, NULL, NULL, NULL, '希望预约门捷列夫老师的《初中化学基础课程》课程', 'pending', NULL, NULL, '2025-07-26 18:41:46', '2025-07-26 18:41:46', NULL, 1, '2025-07-28 15:12:55', 1, 30);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (8, 6, 7, 1, 'recurring', NULL, NULL, NULL, '2', '18:00-19:00', '2025-07-28', '2025-09-29', 10, '希望预约12355老师的《小学语文基础班》课程', 'rejected', NULL, '我不同意', '2025-07-28 15:53:28', '2025-07-28 16:12:38', NULL, 0, NULL, 0, NULL);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (9, 6, 7, 1, 'recurring', NULL, NULL, NULL, '1', '15:00-16:00', '2025-07-30', '2025-10-15', 12, '希望预约12355老师的《小学语文基础班》课程', 'approved', NULL, '可以', '2025-07-28 16:13:38', '2025-07-28 16:14:46', '2025-07-28 16:14:46', 0, NULL, 0, NULL);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (10, 6, 110, 3003, 'recurring', NULL, NULL, NULL, '3', '09:00-10:00', '2025-07-29', '2025-10-14', 12, '希望预约Johnson老师的《小学英语精品课程》课程', 'rejected', NULL, '测试', '2025-07-28 21:09:03', '2025-07-28 21:09:23', NULL, 0, NULL, 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for course_grades
-- ----------------------------
DROP TABLE IF EXISTS `course_grades`;
CREATE TABLE `course_grades` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint(20) NOT NULL COMMENT '课程ID，关联courses表',
  `grade` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '适用年级',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_grade` (`grade`)
) ENGINE=InnoDB AUTO_INCREMENT=554 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程年级关联表';

-- ----------------------------
-- Records of course_grades
-- ----------------------------
BEGIN;
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (506, 1, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (507, 2, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (508, 3, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (509, 4, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (510, 5, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (511, 6, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (512, 7, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (513, 8, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (514, 9, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (515, 10, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (516, 11, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (517, 12, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (518, 13, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (519, 14, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (520, 15, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (521, 16, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (522, 17, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (523, 18, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (524, 19, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (525, 20, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (526, 21, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (527, 22, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (528, 23, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (529, 24, '小学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (530, 25, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (531, 26, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (532, 27, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (533, 28, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (534, 29, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (535, 30, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (536, 31, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (537, 32, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (538, 33, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (539, 34, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (540, 35, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (541, 36, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (542, 37, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (543, 38, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (544, 39, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (545, 40, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (546, 41, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (547, 42, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (548, 43, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (549, 44, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (550, 45, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (551, 46, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (552, 47, '中学', '2025-07-28 21:53:43');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (553, 48, '中学', '2025-07-28 22:00:56');
-- 为新增大班课添加年级关联
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (554, 49, '小学', '2025-07-28 22:00:56');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (555, 50, '小学', '2025-07-28 22:00:56');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (556, 51, '小学', '2025-07-28 22:00:56');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (557, 52, '小学', '2025-07-28 22:00:56');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (558, 53, '中学', '2025-07-28 22:00:56');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (559, 54, '中学', '2025-07-28 22:00:56');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (560, 55, '中学', '2025-07-28 22:00:56');
COMMIT;

-- ----------------------------
-- Table structure for courses
-- ----------------------------
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '课程ID，主键自增',
  `teacher_id` bigint(20) NOT NULL COMMENT '授课教师ID，关联teachers表',
  `subject_id` bigint(20) NOT NULL COMMENT '课程科目ID，关联subjects表',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程标题',
  `description` text COLLATE utf8mb4_general_ci COMMENT '课程详细描述，包括内容大纲、适合人群等',
  `course_type` enum('one_on_one','large_class') COLLATE utf8mb4_general_ci NOT NULL COMMENT '课程类型：one_on_one-一对一,large_class-大班课',
  `price` decimal(10,2) DEFAULT NULL COMMENT '课程单价，单位：M豆，1M豆=1元，课程价格，1对1代表每小时价格，大班课代表总课程价格',
  `start_date` date DEFAULT NULL COMMENT '课程开始时间，格式：YYYY-MM-DD',
  `end_date` date DEFAULT NULL COMMENT '课程结束时间，格式：YYYY-MM-DD',
  `person_limit` int(11) DEFAULT NULL COMMENT '最大报名人数，null表示不限制',
  `course_time_slots` text COMMENT '上课时间安排（只有大班课才需要设置上课时间安排），JSON格式存储：[{"weekday":1,"timeSlots":["08:00-09:00","10:00-11:00"]},{"weekday":2,"timeSlots":["14:00-15:00"]}]，weekday: 1=周一,2=周二...7=周日',
  `image_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '课程封面图URL',
  `duration_minutes` int(11) NOT NULL COMMENT '单次课程时长，单位：分钟',
  `status` enum('active','inactive','full','pending') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT 'active' COMMENT '课程状态：active-可报名，inactive-已下架，full-已满员，pending-待审批',
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否为精选课程，在首页展示：1-是，0-否',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '课程创建时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程信息表，存储教师发布的课程详情';

-- ----------------------------
-- Records of courses
-- ----------------------------
BEGIN;
-- 一对一课程示例（保持原有字段为NULL）
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (1, 1, 1, '小学数学基础班', '小学数学基础知识教学，包括四则运算、几何图形等基础概念。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 1, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2, 1, 1, '小学奥数启蒙班', '小学奥数启蒙课程，培养数学思维和解题技巧。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 1, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3, 2, 1, '小学应用题专项班', '专门针对小学应用题的解题方法和技巧训练。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 1, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (4, 2, 1, '小学计算能力提升班', '提升小学生的计算速度和准确性。', 'one_on_one', NULL, NULL, NULL, NULL, 45, 'active', 1, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (5, 3, 1, '小学几何启蒙班', '小学几何基础知识，培养空间想象能力。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 1, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (6, 3, 1, '数学游戏趣味班', '通过数学游戏让孩子爱上数学学习。', 'one_on_one', NULL, NULL, NULL, NULL, 45, 'active', 1, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (7, 4, 2, '小学科学探索班', '通过有趣的科学实验，让小学生了解自然现象。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (8, 4, 2, '自然观察实践班', '户外自然观察，培养学生的观察能力和科学精神。', 'one_on_one', NULL, NULL, NULL, NULL, 90, 'active', 0, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (9, 5, 2, '科学启蒙实验班', '适合小学生的科学启蒙课程，通过动手实验学习科学。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (10, 5, 2, '生活中的科学', '从生活现象中学习科学原理，培养科学思维。', 'one_on_one', NULL, NULL, NULL, NULL, 45, 'active', 0, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (11, 6, 2, '环境科学启蒙班', '环境保护和生态科学的启蒙教育。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (12, 6, 2, '科学探究方法班', '教授科学探究的基本方法和思维方式。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:36:22', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (13, 7, 3, '小学华文基础班', '小学华文基础教学，包括拼音、汉字、词汇学习。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (14, 7, 3, '拼音识字启蒙班', '专注拼音教学和汉字识字启蒙。', 'one_on_one', NULL, NULL, NULL, NULL, 45, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (15, 8, 3, '小学阅读理解班', '提升小学生的阅读理解能力和语言表达。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (16, 8, 3, '写作启蒙班', '小学写作启蒙，从看图写话开始。', 'one_on_one', NULL, NULL, NULL, NULL, 45, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (17, 9, 3, '古诗词启蒙班', '小学古诗词学习，感受传统文化魅力。', 'one_on_one', NULL, NULL, NULL, NULL, 45, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (18, 9, 3, '传统文化班', '通过故事和游戏学习中华传统文化。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (19, 10, 4, 'KET考试冲刺班', '专为KET考试设计的冲刺课程，涵盖听说读写四项技能。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (20, 10, 4, 'KET口语训练班', 'KET考试口语专项训练，提升口语表达能力。', 'one_on_one', NULL, NULL, NULL, NULL, 45, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (21, 11, 4, 'KET语法基础班', 'KET考试语法基础课程，系统学习英语语法。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (22, 11, 4, 'KET阅读理解班', 'KET阅读理解专项训练，提升阅读技巧。', 'one_on_one', NULL, NULL, NULL, NULL, 45, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (23, 12, 4, 'KET写作训练班', 'KET写作专项训练，掌握写作技巧和方法。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (24, 12, 4, 'KET听力突破班', 'KET听力专项训练，提升听力理解能力。', 'one_on_one', NULL, NULL, NULL, NULL, 45, 'active', 0, '2025-07-28 21:38:04', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (25, 13, 5, 'PET考试精品班', 'PET考试专项训练，重点突破语法和词汇难点。', 'one_on_one', NULL, NULL, NULL, NULL, 90, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (26, 13, 5, 'PET学术写作班', 'PET考试学术写作训练，提升写作水平。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (27, 14, 5, 'PET口语提升班', 'PET口语专项训练，提升口语流利度和准确性。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (28, 14, 5, 'PET商务英语班', 'PET水平的商务英语应用训练。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (29, 15, 5, 'PET英语文学班', 'PET水平的英语文学鉴赏和分析。', 'one_on_one', NULL, NULL, NULL, NULL, 90, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (30, 15, 5, 'PET批判性思维班', '通过英语培养批判性思维能力。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (31, 16, 6, '中学数学竞赛班', '中学数学竞赛训练，培养高级数学思维。', 'one_on_one', NULL, NULL, NULL, NULL, 90, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (32, 16, 6, '高等数学预备班', '为学习高等数学打下坚实基础。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (33, 17, 6, '中学函数专题班', '中学数学函数专题深度学习。', 'one_on_one', NULL, NULL, NULL, NULL, 90, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (34, 17, 6, '中学几何证明班', '中学几何证明专项训练。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (35, 18, 6, '数学建模班', '中学数学建模训练，培养应用能力。', 'one_on_one', NULL, NULL, NULL, NULL, 90, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (36, 18, 6, '创新数学思维班', '培养创新数学思维和解题能力。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:40:53', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (37, 19, 7, '中学物理实验班', '中学物理实验课程，培养实验技能和科学思维。', 'one_on_one', NULL, NULL, NULL, NULL, 90, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (38, 19, 7, '科学研究方法班', '教授科学研究的基本方法和实验设计。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (39, 20, 7, '中学化学实验班', '中学化学实验课程，深入理解化学原理。', 'one_on_one', NULL, NULL, NULL, NULL, 90, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (40, 20, 7, '有机化学专题班', '有机化学专题学习，掌握有机反应机理。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (41, 21, 7, '中学生物实验班', '中学生物实验课程，探索生命科学奥秘。', 'one_on_one', NULL, NULL, NULL, NULL, 90, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (42, 21, 7, '分子生物学班', '分子生物学基础，了解生命的分子机制。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (43, 22, 8, '中学古代文学班', '中学古代文学学习，深入理解经典作品。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (44, 22, 8, '文言文精读班', '文言文精读训练，提升古文理解能力。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (45, 23, 8, '中学现代文学班', '中学现代文学鉴赏，培养文学素养。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (46, 23, 8, '中学写作指导班', '中学写作技巧指导，提升表达能力。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (47, 24, 8, '语言学基础班', '语言学基础知识，了解语言的科学原理。', 'one_on_one', NULL, NULL, NULL, NULL, 75, 'active', 0, '2025-07-28 21:44:29', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (48, 24, 8, '修辞学应用班', '修辞学应用训练，掌握高级语言技巧。', 'one_on_one', NULL, NULL, NULL, NULL, 60, 'active', 0, '2025-07-28 21:44:29', 0, NULL);

-- 大班课示例数据
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (49, 1, 1, '小学数学思维训练营', '为期8周的小学数学思维训练大班课，培养逻辑思维和解题能力。每周2次课，系统性提升数学素养。', 'large_class', 299.00, '2025-09-01', '2025-10-26', 25, 90, 'active', 1, '2025-07-28 22:00:00', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (50, 7, 3, '小学语文阅读写作提升班', '专注提升小学生阅读理解和写作能力的大班课程。通过经典文本阅读和写作练习，全面提升语文素养。', 'large_class', 399.00, '2025-09-02', '2025-11-25', 30, 120, 'active', 1, '2025-07-28 22:00:00', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (51, 10, 4, 'KET考试冲刺大班课', '针对KET考试的集中冲刺训练，12周系统复习，模拟考试练习。适合有一定英语基础的学生。', 'large_class', 599.00, '2025-09-03', '2025-11-26', 20, 120, 'active', 1, '2025-07-28 22:00:00', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (52, 4, 2, '小学科学实验探索营', '动手实验为主的科学启蒙大班课，每周进行有趣的科学实验，培养科学思维和动手能力。', 'large_class', NULL, '2025-09-05', '2025-12-05', NULL, 90, 'active', 0, '2025-07-28 22:00:00', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (53, 16, 6, '中学数学竞赛训练班', '面向有数学天赋学生的竞赛训练大班课，涵盖代数、几何、数论等竞赛专题。', 'large_class', 899.00, '2025-09-07', '2025-12-07', 15, 150, 'active', 1, '2025-07-28 22:00:00', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (54, 13, 5, 'PET英语能力提升班', '系统提升PET水平英语能力的大班课程，注重听说读写全面发展，为PET考试做准备。', 'large_class', 699.00, '2025-09-09', '2025-12-09', 18, 120, 'pending', 0, '2025-07-28 22:00:00', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `price`, `start_date`, `end_date`, `person_limit`, `duration_minutes`, `status`, `is_featured`, `created_at`, `is_deleted`, `deleted_at`) VALUES (55, 19, 7, '中学物理实验精品班', '中学物理实验大班课，通过系统的实验操作，深入理解物理原理，培养实验技能。', 'large_class', NULL, '2025-09-10', '2025-12-10', 12, 120, 'active', 0, '2025-07-28 22:00:00', 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for grades
-- ----------------------------
DROP TABLE IF EXISTS `grades`;
CREATE TABLE `grades` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '年级ID，主键自增',
  `grade_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '年级名称，如：小学一年级、初中二年级',
  `description` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '年级描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grade_name` (`grade_name`),
  KEY `idx_grade_name` (`grade_name`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='年级信息表，存储系统中的年级数据';

-- ----------------------------
-- Records of grades
-- ----------------------------
BEGIN;
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (14, '小学', '小学阶段，适合6-12岁儿童', '2025-07-28 21:55:14', '2025-07-28 21:55:14', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (15, '中学', '中学阶段，适合12-18岁学生', '2025-07-28 21:55:14', '2025-07-28 21:55:14', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (16, '高中', '高中阶段，适合15-18岁学生', '2025-07-28 21:55:14', '2025-07-28 21:55:14', 0, NULL);

COMMIT;

-- ----------------------------
-- Table structure for reschedule_requests
-- ----------------------------
DROP TABLE IF EXISTS `reschedule_requests`;
CREATE TABLE `reschedule_requests` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '调课申请ID',
  `schedule_id` bigint(20) NOT NULL COMMENT '原课程安排ID',
  `applicant_id` bigint(20) NOT NULL COMMENT '申请人ID(学生或教师)',
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
  `advance_notice_hours` int(11) DEFAULT NULL COMMENT '提前通知小时数',
  `status` enum('pending','approved','rejected','cancelled') DEFAULT 'pending' COMMENT '申请状态',
  `reviewer_id` bigint(20) DEFAULT NULL COMMENT '审核人ID',
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='调课申请表，记录课程时间调整申请';

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
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '排课ID，主键自增',
  `teacher_id` bigint(20) NOT NULL COMMENT '授课教师ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生ID',
  `course_id` bigint(20) DEFAULT NULL COMMENT '课程ID',
  `scheduled_date` date NOT NULL COMMENT '上课日期',
  `start_time` time NOT NULL COMMENT '开始时间',
  `end_time` time NOT NULL COMMENT '结束时间',
  `total_times` int(11) DEFAULT NULL COMMENT '总课程次数',
  `status` enum('progressing','completed','cancelled') DEFAULT 'progressing' COMMENT '课程状态：progressing-进行中，confirmed-已确认，cancelled-已取消',
  `teacher_notes` text COMMENT '教师课后备注和反馈',
  `student_feedback` text COMMENT '学生课后反馈',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '排课创建时间',
  `booking_request_id` bigint(20) DEFAULT NULL COMMENT '关联预约申请ID',
  `booking_source` enum('request','admin') DEFAULT 'request' COMMENT '预约来源：request-申请预约，admin-管理员安排',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `recurring_weekdays` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '周期性预约的星期几，逗号分隔：1,3,5',
  `recurring_time_slots` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '周期性预约的时间段，逗号分隔：14:00-16:00,18:00-20:00',
  `is_trial` tinyint(1) DEFAULT '0' COMMENT '是否为试听课：true-是，false-否',
  `session_number` int(11) DEFAULT NULL COMMENT '课程序号（在周期性课程中的第几次课）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='课程安排表，记录具体的上课时间';

-- ----------------------------
-- Records of schedules
-- ----------------------------
BEGIN;
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (1, 7, 6, NULL, '2025-07-22', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 1);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (2, 7, 6, NULL, '2025-07-23', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 2);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (3, 7, 6, NULL, '2025-07-29', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 3);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (4, 7, 6, NULL, '2025-07-30', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 4);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (5, 7, 6, NULL, '2025-08-05', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 5);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (6, 7, 6, NULL, '2025-08-06', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 6);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (7, 7, 6, NULL, '2025-08-12', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 7);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (8, 7, 6, NULL, '2025-08-13', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 8);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (9, 7, 6, NULL, '2025-08-19', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 9);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (10, 7, 6, NULL, '2025-08-20', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 10);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (11, 7, 6, NULL, '2025-08-26', '17:00:00', '18:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:12:05', 3, 'request', 0, NULL, '2,3', '17:00-18:00', 0, 11);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (12, 7, 6, 3, '2025-07-25', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 1);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (13, 7, 6, 3, '2025-07-29', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 2);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (14, 7, 6, 3, '2025-08-01', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 3);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (15, 7, 6, 3, '2025-08-05', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 4);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (16, 7, 6, 3, '2025-08-08', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 5);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (17, 7, 6, 3, '2025-08-12', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 6);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (18, 7, 6, 3, '2025-08-15', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 7);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (19, 7, 6, 3, '2025-08-19', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 8);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (20, 7, 6, 3, '2025-08-22', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 9);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (21, 7, 6, 3, '2025-08-26', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 1, '2025-07-28 15:23:31', '2,5', '14:00-15:00', 0, 10);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (22, 7, 6, 1, '2025-08-04', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 1);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (23, 7, 6, 1, '2025-08-11', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 2);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (24, 7, 6, 1, '2025-08-18', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 3);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (25, 7, 6, 1, '2025-08-25', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 4);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (26, 7, 6, 1, '2025-09-01', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 5);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (27, 7, 6, 1, '2025-09-08', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 6);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (28, 7, 6, 1, '2025-09-15', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 7);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (29, 7, 6, 1, '2025-09-22', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 8);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (30, 7, 6, 1, '2025-09-29', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 9);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (31, 7, 6, 1, '2025-10-06', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 10);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (32, 7, 6, 1, '2025-10-13', '15:00:00', '16:00:00', 12, 'progressing', NULL, NULL, '2025-07-28 16:14:46', 9, 'request', 0, NULL, '1', '15:00-16:00', 0, 11);
COMMIT;

-- ----------------------------
-- Table structure for student_subjects
-- ----------------------------
DROP TABLE IF EXISTS `student_subjects`;
CREATE TABLE `student_subjects` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `student_id` bigint(20) NOT NULL COMMENT '学生ID，关联students表',
  `subject_id` bigint(20) NOT NULL COMMENT '科目ID，关联subjects表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_student_subject` (`student_id`,`subject_id`),
  KEY `idx_student_id` (`student_id`),
  KEY `idx_subject_id` (`subject_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生感兴趣科目关联表';

-- ----------------------------
-- Records of student_subjects
-- ----------------------------
BEGIN;
COMMIT;

-- ----------------------------
-- Table structure for students
-- ----------------------------
DROP TABLE IF EXISTS `students`;
CREATE TABLE `students` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '学生ID，主键自增',
  `user_id` bigint(20) NOT NULL COMMENT '关联用户表的用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '学生真实姓名',
  `grade_level` varchar(20) DEFAULT NULL COMMENT '年级水平，如：小学三年级、初中一年级、高中二年级',
  `subjects_interested` varchar(50) DEFAULT NULL COMMENT '感兴趣的科目列表如：[数学,英语,物理]',
  `learning_goals` text COMMENT '学习目标和需求描述',
  `preferred_teaching_style` varchar(100) DEFAULT NULL COMMENT '偏好的教学风格，如：严格型、温和型、互动型',
  `balance` decimal(10,2) DEFAULT '0.00' COMMENT '学生余额，单位：M豆',
  `budget_range` varchar(50) DEFAULT NULL COMMENT '预算范围，如：100-200元/小时',
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
INSERT INTO `students` (`id`, `user_id`, `real_name`, `grade_level`, `subjects_interested`, `learning_goals`, `preferred_teaching_style`, `budget_range`, `is_deleted`, `deleted_at`, `gender`) VALUES (6, 10, 'student23', '小学', '', '123131123', '实践型教学', '100-200', 0, NULL, '不愿透露');
COMMIT;

-- ----------------------------
-- Table structure for subjects
-- ----------------------------
DROP TABLE IF EXISTS `subjects`;
CREATE TABLE `subjects` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '科目ID，主键自增',
  `name` varchar(50) NOT NULL COMMENT '科目名称，如：数学、语文、英语',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '科目图标URL地址',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：true-启用，false-禁用',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目分类表，定义平台支持的所有教学科目';

-- ----------------------------
-- Records of subjects
-- ----------------------------
BEGIN;
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (1, '小学数学', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (2, '小学科学', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (3, '小学华文', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (4, '英文(KET)', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (5, '英文(PET)', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (6, '中学数学', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (7, '中学科学', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (8, '中学华文', '', 1, 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for teacher_subjects
-- ----------------------------
DROP TABLE IF EXISTS `teacher_subjects`;
CREATE TABLE `teacher_subjects` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `teacher_id` bigint(20) NOT NULL COMMENT '教师ID，关联teachers表',
  `subject_id` bigint(20) NOT NULL COMMENT '科目ID，关联subjects表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_teacher_subject` (`teacher_id`,`subject_id`),
  KEY `idx_teacher_id` (`teacher_id`),
  KEY `idx_subject_id` (`subject_id`),
  CONSTRAINT `fk_teacher_subjects_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_teacher_subjects_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师科目关联表';

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
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (7, 7, 3, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (8, 8, 3, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (9, 9, 3, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (10, 10, 4, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (11, 11, 4, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (12, 12, 4, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (13, 13, 5, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (14, 14, 5, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (15, 15, 5, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (16, 16, 6, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (17, 17, 6, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (18, 18, 6, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (19, 19, 7, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (20, 20, 7, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (21, 21, 7, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (22, 22, 8, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (23, 23, 8, '2025-07-28 21:36:22');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (24, 24, 8, '2025-07-28 21:36:22');
COMMIT;

-- ----------------------------
-- Table structure for teachers
-- ----------------------------
DROP TABLE IF EXISTS `teachers`;
CREATE TABLE `teachers` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '教师ID，主键自增',
  `user_id` bigint(20) NOT NULL COMMENT '关联用户表的用户ID',
  `real_name` varchar(50) NOT NULL COMMENT '教师真实姓名',
  `education_background` text COMMENT '教育背景描述，包括学历、毕业院校等',
  `teaching_experience` int(11) DEFAULT NULL COMMENT '教学经验年数',
  `specialties` varchar(50) DEFAULT NULL COMMENT '专业领域如：[高考数学,竞赛辅导,基础提升]',
  `hourly_rate` decimal(10,2) DEFAULT NULL COMMENT '每小时收费标准，单位：元',
  `introduction` text COMMENT '个人介绍和教学理念',
  `video_intro_url` varchar(255) DEFAULT NULL COMMENT '个人介绍视频URL地址',
  `photo_url` varchar(255)  DEFAULT NULL COMMENT '个人照片',
  `gender` enum('男','女','不愿透露') DEFAULT '不愿透露' COMMENT '性别：男、女、不愿透露',
  `is_verified` tinyint(1) DEFAULT '0' COMMENT '是否已认证：true-已认证，false-未认证',
  `is_featured` tinyint(1) DEFAULT '0' COMMENT '是否展示到首页：true-是，false-否',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `available_time_slots` text COMMENT '可上课时间安排，JSON格式存储：[{"weekday":1,"timeSlots":["08:00-09:00","10:00-11:00"]},{"weekday":2,"timeSlots":["14:00-15:00"]}]，weekday: 1=周一,2=周二...7=周日',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`),
  KEY `idx_is_featured` (`is_featured`)
) ENGINE=InnoDB AUTO_INCREMENT=38 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师详细信息表，存储教师的专业资料和教学信息';

-- ----------------------------
-- Records of teachers
-- ----------------------------
BEGIN;
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (1, 143, '李明华', '北京师范大学数学教育硕士', 10, '小学数学基础,思维训练,奥数启蒙', 150.00, '师范大学数学教育专业，10年小学数学教学经验。擅长培养学生数学思维，让孩子在游戏中学会数学。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"09:00-10:00\", \"10:00-11:00\", \"18:00-19:00\", \"19:00-20:00\"]}, {\"weekday\": 2, \"timeSlots\": [\"09:00-10:00\", \"17:00-18:00\", \"18:00-19:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"09:00-10:00\", \"10:00-11:00\", \"18:00-19:00\", \"19:00-20:00\"]}, {\"weekday\": 4, \"timeSlots\": [\"09:00-10:00\", \"17:00-18:00\", \"18:00-19:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"09:00-10:00\", \"10:00-11:00\", \"18:00-19:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"09:00-10:00\", \"14:00-15:00\", \"15:00-16:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"09:00-10:00\", \"14:00-15:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (2, 144, '王雅琳', '华东师范大学数学系硕士', 8, '小学数学,应用题专项,计算能力', 140.00, '华师大数学硕士，8年小学数学教学经验。特别擅长应用题教学，帮助学生建立数学思维模式。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 2, \"timeSlots\": [\"09:00-10:00\", \"17:00-18:00\"]}, {\"weekday\": 4, \"timeSlots\": [\"09:00-10:00\", \"17:00-18:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"15:00-16:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"09:00-10:00\", \"14:00-15:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (3, 145, '张志强', '南京师范大学数学教育硕士', 12, '小学数学,几何启蒙,数学游戏', 160.00, '南师大数学教育专业，12年教学经验。善于用生动有趣的方式教授数学，让学生爱上数学学习。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"15:00-16:00\", \"18:00-19:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"15:00-16:00\", \"18:00-19:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"15:00-16:00\", \"18:00-19:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"10:00-11:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (4, 146, '陈博士', '中科院生物学博士', 15, '小学科学,自然观察,科学实验', 180.00, '中科院博士，15年科学教育经验。擅长通过实验和观察培养学生科学思维，让抽象的科学概念变得生动有趣。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"10:00-11:00\", \"17:00-18:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"10:00-11:00\", \"17:00-18:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"10:00-11:00\", \"17:00-18:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (5, 147, '刘晓敏', '北京师范大学科学教育硕士', 9, '小学科学,科学启蒙,动手实验', 160.00, '师范大学科学教育专业，专注小学科学教育9年。善于用生活中的例子解释科学原理，激发学生对科学的兴趣。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 2, \"timeSlots\": [\"11:00-12:00\", \"16:00-17:00\"]}, {\"weekday\": 4, \"timeSlots\": [\"11:00-12:00\", \"16:00-17:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"09:00-10:00\", \"17:00-18:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"11:00-12:00\", \"14:00-15:00\", \"17:00-18:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (6, 148, '赵宇航', '华中师范大学科学教育硕士', 7, '小学科学,环境科学,科学探究', 150.00, '华师科学教育硕士，7年教学经验。特别擅长环境科学教学，通过户外观察让学生了解自然，培养环保意识。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"11:00-12:00\", \"19:00-20:00\"]}, {\"weekday\": 2, \"timeSlots\": [\"11:00-12:00\", \"19:00-20:00\"]}, {\"weekday\": 4, \"timeSlots\": [\"11:00-12:00\", \"19:00-20:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"12:00-13:00\", \"16:00-17:00\", \"18:00-19:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (7, 149, '林雅文', '台湾师范大学中文系硕士', 11, '小学华文,拼音教学,识字启蒙', 160.00, '台师大中文硕士，11年小学华文教学经验。擅长拼音和识字教学，让孩子轻松掌握华文基础。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"18:00-19:00\"]}, {\"weekday\": 2, \"timeSlots\": [\"14:00-15:00\", \"18:00-19:00\"]}, {\"weekday\": 4, \"timeSlots\": [\"14:00-15:00\", \"18:00-19:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"12:00-13:00\", \"15:00-16:00\", \"17:00-18:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (8, 150, '黄志华', '北京语言大学汉语国际教育硕士', 8, '小学华文,阅读理解,写作启蒙', 150.00, '北语汉教硕士，专注小学华文教育8年。特别擅长阅读理解和写作启蒙，培养学生的语言表达能力。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 2, \"timeSlots\": [\"15:00-16:00\", \"20:00-21:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"15:00-16:00\", \"20:00-21:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"15:00-16:00\", \"20:00-21:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"12:00-13:00\", \"18:00-19:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (9, 151, '郑美玲', '香港中文大学中国语言文学硕士', 6, '小学华文,古诗词,传统文化', 140.00, '港中大中文硕士，6年小学华文教学经验。专注传统文化教育，通过古诗词让孩子感受中华文化魅力。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"16:00-17:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"16:00-17:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"16:00-17:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"13:00-14:00\", \"18:00-19:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"13:00-14:00\", \"20:00-21:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (10, 152, 'Emma Wilson', '英国剑桥大学英语文学硕士', 8, 'KET考试辅导,少儿英语,口语训练', 200.00, '英国剑桥大学硕士，专注于KET考试辅导8年。帮助超过200名学生成功通过KET考试，教学风格生动有趣。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"09:00-10:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"09:00-10:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"09:00-10:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"09:00-10:00\", \"14:00-15:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (11, 153, 'David Smith', '美国哥伦比亚大学TESOL硕士', 6, 'KET考试,英语语法,阅读理解', 180.00, '美国外教，专业TESOL认证，擅长KET考试技巧指导。课堂氛围轻松愉快，让学生在快乐中学习英语。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (12, 154, 'Sarah Johnson', '澳大利亚悉尼大学教育学硕士', 5, 'KET考试,写作训练,听力提升', 170.00, '澳洲海归教师，专注KET考试培训5年。特别擅长写作和听力训练，学生通过率高达95%。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (13, 155, 'Michael Brown', '英国牛津大学英语语言学硕士', 10, 'PET考试辅导,高级英语,学术写作', 220.00, '牛津大学硕士，10年PET考试辅导经验。专注高级英语教学，帮助学生达到更高的英语水平。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (14, 156, 'Jennifer Lee', '加拿大多伦多大学应用语言学硕士', 7, 'PET考试,英语口语,商务英语', 200.00, '加拿大外教，应用语言学硕士，7年PET教学经验。擅长口语训练和商务英语，让学生自信开口说英语。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (15, 157, 'Robert Taylor', '美国加州大学英语教育硕士', 9, 'PET考试,英语文学,批判性思维', 210.00, '美国外教，英语教育硕士，9年教学经验。注重培养学生的批判性思维和英语文学鉴赏能力。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (16, 158, '李教授', '清华大学数学系博士', 15, '中学数学,高等数学,竞赛数学', 250.00, '清华数学博士，15年中学数学教学经验。擅长高等数学和竞赛数学，所教学生多次获得数学竞赛奖项。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (17, 159, '王博士', '北京大学应用数学博士', 12, '中学数学,函数专题,几何证明', 230.00, '北大数学博士，专注中学数学教学12年。特别擅长函数、几何等难点突破，帮助学生建立完整的数学知识体系。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (18, 160, '张院士', '中科院数学与系统科学研究院博士', 18, '中学数学,数学建模,创新思维', 280.00, '中科院博士，18年教学经验。擅长数学建模和创新思维培养，让学生学会用数学解决实际问题。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (19, 161, '陈院士', '中科院物理研究所博士', 20, '中学物理,实验物理,科学研究', 300.00, '中科院物理博士，20年科学教育经验。擅长实验物理教学，培养学生的科学研究能力和创新精神。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (20, 162, '刘博士', '北京理工大学化学博士', 14, '中学化学,有机化学,化学实验', 260.00, '北理工化学博士，14年中学化学教学经验。特别擅长有机化学和实验教学，让抽象的化学概念变得具体可感。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 5, \"timeSlots\": [\"14:00-15:00\", \"15:00-16:00\", \"19:00-20:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"10:00-11:00\", \"11:00-12:00\", \"16:00-17:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (21, 163, '赵教授', '华中科技大学生物学博士', 16, '中学生物,分子生物学,生命科学', 270.00, '华科生物博士，16年教学经验。专注分子生物学和生命科学教育，培养学生对生命科学的深度理解。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 2, \"timeSlots\": [\"10:00-11:00\", \"16:00-17:00\", \"18:00-19:00\"]}, {\"weekday\": 4, \"timeSlots\": [\"10:00-11:00\", \"16:00-17:00\", \"18:00-19:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"09:00-10:00\", \"15:00-16:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"10:00-11:00\", \"15:00-16:00\", \"17:00-18:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (22, 164, '林教授', '北京师范大学中文系博士', 17, '中学华文,古代文学,文言文', 240.00, '北师大中文博士，17年中学华文教学经验。专精古代文学和文言文，让学生深入理解中华文化精髓。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 2, \"timeSlots\": [\"10:00-11:00\", \"16:00-17:00\", \"18:00-19:00\"]}, {\"weekday\": 4, \"timeSlots\": [\"10:00-11:00\", \"16:00-17:00\", \"18:00-19:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"09:00-10:00\", \"15:00-16:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"10:00-11:00\", \"15:00-16:00\", \"17:00-18:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (23, 165, '黄博士', '复旦大学中国语言文学博士', 13, '中学华文,现代文学,写作指导', 220.00, '复旦中文博士，专注中学华文教育13年。擅长现代文学和写作指导，培养学生的文学素养和表达能力。', NULL, '男', 1, 0, NULL, '[{\"weekday\": 2, \"timeSlots\": [\"10:00-11:00\", \"16:00-17:00\", \"18:00-19:00\"]}, {\"weekday\": 4, \"timeSlots\": [\"10:00-11:00\", \"16:00-17:00\", \"18:00-19:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"09:00-10:00\", \"15:00-16:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"10:00-11:00\", \"15:00-16:00\", \"17:00-18:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (24, 166, '郑教授', '中山大学中文系博士', 14, '中学华文,语言学,修辞学', 230.00, '中大中文博士，14年教学经验。专精语言学和修辞学，帮助学生掌握高级语言运用技巧。', NULL, '女', 1, 0, NULL, '[{\"weekday\": 2, \"timeSlots\": [\"10:00-11:00\", \"16:00-17:00\", \"18:00-19:00\"]}, {\"weekday\": 4, \"timeSlots\": [\"10:00-11:00\", \"16:00-17:00\", \"18:00-19:00\"]}, {\"weekday\": 6, \"timeSlots\": [\"09:00-10:00\", \"15:00-16:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"10:00-11:00\", \"15:00-16:00\", \"17:00-18:00\"]}]');
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`, `available_time_slots`) VALUES (37, 183, '青', NULL, NULL, NULL, NULL, NULL, NULL, '男', 0, 0, NULL, '[{\"weekday\": 1, \"timeSlots\": [\"11:00-12:00\", \"17:00-18:00\", \"20:00-21:00\"]}, {\"weekday\": 2, \"timeSlots\": [\"11:00-12:00\", \"17:00-18:00\", \"20:00-21:00\"]}, {\"weekday\": 3, \"timeSlots\": [\"11:00-12:00\", \"17:00-18:00\", \"20:00-21:00\"]}, {\"weekday\": 7, \"timeSlots\": [\"09:00-10:00\", \"14:00-15:00\", \"18:00-19:00\"]}]');
COMMIT;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '用户ID，主键自增',
  `username` varchar(50) NOT NULL COMMENT '用户名，唯一标识',
  `email` varchar(100) NOT NULL COMMENT '邮箱地址，用于登录和通知',
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码（加密）Bcrypt',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号码',
  `birth_date` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '出生年月，格式：YYYY-MM',
  `avatar_url` varchar(255) DEFAULT NULL COMMENT '头像图片URL地址',
  `user_type` enum('student','teacher','admin') NOT NULL COMMENT '用户类型：student-学生，teacher-教师，admin-管理员',
  `status` enum('active','inactive','banned') DEFAULT 'active' COMMENT '账户状态：active-激活，inactive-未激活，banned-封禁',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  `has_used_trial` tinyint(1) DEFAULT '0' COMMENT '是否已使用免费试听：true-已使用，false-未使用',
  `trial_used_at` timestamp NULL DEFAULT NULL COMMENT '试听课使用时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=184 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户基础信息表，存储所有用户的通用信息';

-- ----------------------------
-- Records of users
-- ----------------------------
BEGIN;
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (10, 'student', 'qinghaoyang@foxmail.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '', NULL, NULL, 'student', 'active', '2025-07-19 13:21:54', '2025-07-29 09:31:13', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `birth_date`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (11, 'admin', 'admin@admin.com', '$2a$10$CLJSuGd2ptKI9VlCz3r4buGyY7HfKg1qivwbKEfkk8/6Pz57oKjWK', NULL, NULL, NULL, 'admin', 'active', '2025-07-19 16:23:39', '2025-07-29 13:24:12', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (143, 'teacher_primary_math_01', 'primary_math01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800001001', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (144, 'teacher_primary_math_02', 'primary_math02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800001002', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (145, 'teacher_primary_math_03', 'primary_math03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800001003', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (146, 'teacher_primary_science_01', 'primary_science01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800002001', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (147, 'teacher_primary_science_02', 'primary_science02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800002002', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (148, 'teacher_primary_science_03', 'primary_science03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800002003', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (149, 'teacher_primary_chinese_01', 'primary_chinese01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800003001', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (150, 'teacher_primary_chinese_02', 'primary_chinese02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800003002', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (151, 'teacher_primary_chinese_03', 'primary_chinese03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800003003', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (152, 'teacher_ket_01', 'ket01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800004001', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (153, 'teacher_ket_02', 'ket02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800004002', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (154, 'teacher_ket_03', 'ket03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800004003', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (155, 'teacher_pet_01', 'pet01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800005001', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (156, 'teacher_pet_02', 'pet02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800005002', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (157, 'teacher_pet_03', 'pet03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800005003', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (158, 'teacher_middle_math_01', 'middle_math01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800006001', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (159, 'teacher_middle_math_02', 'middle_math02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800006002', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (160, 'teacher_middle_math_03', 'middle_math03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800006003', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (161, 'teacher_middle_science_01', 'middle_science01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800007001', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (162, 'teacher_middle_science_02', 'middle_science02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800007002', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (163, 'teacher_middle_science_03', 'middle_science03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800007003', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (164, 'teacher_middle_chinese_01', 'middle_chinese01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800008001', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (165, 'teacher_middle_chinese_02', 'middle_chinese02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800008002', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (166, 'teacher_middle_chinese_03', 'middle_chinese03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800008003', NULL, 'teacher', 'active', '2025-07-28 21:36:22', '2025-07-28 21:36:22', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (183, 'teacher', 'qhycursor@126.com', '$2a$10$jlFOPIGNKGflN7bUxRAR2OZ9F8NdnZotJaRsZ/cZ.IBPFPz5Gc/da', '', NULL, 'teacher', 'active', '2025-07-29 13:07:29', '2025-07-29 13:07:29', 0, NULL, 0, NULL);
COMMIT;

-- ----------------------------
-- 设置部分教师为天下名师（精选教师）
-- ----------------------------
UPDATE `teachers` SET `is_featured` = 1 WHERE `id` IN (1, 2, 7, 10, 13, 16, 19, 22);

-- ----------------------------
-- Table structure for study_abroad_countries
-- ----------------------------
DROP TABLE IF EXISTS `study_abroad_countries`;
CREATE TABLE `study_abroad_countries` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '留学国家ID，主键自增',
  `country_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '国家名称',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序权重，数值越小越靠前',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：1-启用，0-禁用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：1-已删除，0-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_country_name_isdel` (`country_name`,`is_deleted`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='留学国家管理表';

-- ----------------------------
-- Table structure for study_abroad_stages
-- ----------------------------
DROP TABLE IF EXISTS `study_abroad_stages`;
CREATE TABLE `study_abroad_stages` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '留学阶段ID，主键自增',
  `stage_name` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '阶段名称，如：高中、本科、硕士',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序权重，数值越小越靠前',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：1-启用，0-禁用',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：1-已删除，0-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stage_name_isdel` (`stage_name`,`is_deleted`),
  KEY `idx_is_active` (`is_active`),
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='留学阶段管理表';


-- ----------------------------
-- Table structure for study_abroad_programs
-- ----------------------------
DROP TABLE IF EXISTS `study_abroad_programs`;
CREATE TABLE `study_abroad_programs` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '留学项目ID，主键自增',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '项目标题',
  `country_id` bigint(20) NOT NULL COMMENT '留学国家ID，关联study_abroad_countries表',
  `stage_id` bigint(20) NOT NULL COMMENT '留学阶段ID，关联study_abroad_stages表',
  `description` text COLLATE utf8mb4_general_ci COMMENT '项目详细描述',
  `image_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '项目图片URL',
  `tags` text COLLATE utf8mb4_general_ci COMMENT '项目标签，JSON格式存储',
  `is_hot` tinyint(1) DEFAULT '0' COMMENT '是否为热门项目：1-是，0-否',
  `sort_order` int(11) DEFAULT '0' COMMENT '排序权重，数值越小越靠前',
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
  KEY `idx_sort_order` (`sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='留学项目管理表';

SET FOREIGN_KEY_CHECKS = 1;


-- ----------------------------
-- Seed data for Study Abroad (countries, stages, programs)
-- 使用 INSERT IGNORE/NOT EXISTS 以避免重复导入
-- ----------------------------

-- 基础国家（若已存在会忽略）
INSERT IGNORE INTO `study_abroad_countries` (`country_name`, `sort_order`, `is_active`) VALUES
  ('中国', 0, 1),
  ('美国', 0, 1),
  ('英国', 0, 1),
  ('加拿大', 0, 1);

-- 基础阶段（若已存在会忽略）
INSERT IGNORE INTO `study_abroad_stages` (`stage_name`, `sort_order`, `is_active`) VALUES
  ('高中', 0, 1),
  ('本科', 0, 1),
  ('硕士', 0, 1);

-- 留学项目示例（依赖上方国家/阶段），避免重复插入
INSERT INTO `study_abroad_programs` (`title`,`country_id`,`stage_id`,`description`,`image_url`,`tags`,`is_hot`,`sort_order`,`is_active`)
SELECT '英国高中A-Level申请指导', c.id, s.id,
       'A-Level 选课、文书、面试全流程辅导', NULL,
       '["文书指导","面试培训","选课规划"]', 1, 10, 1
FROM study_abroad_countries c, study_abroad_stages s
WHERE c.country_name='英国' AND c.is_deleted=0
  AND s.stage_name='高中' AND s.is_deleted=0
  AND NOT EXISTS (
      SELECT 1 FROM study_abroad_programs p
      WHERE p.title='英国高中A-Level申请指导' AND p.country_id=c.id AND p.stage_id=s.id AND p.is_deleted=0
  );

INSERT INTO `study_abroad_programs` (`title`,`country_id`,`stage_id`,`description`,`image_url`,`tags`,`is_hot`,`sort_order`,`is_active`)
SELECT '美国本科通用申请（Common App）', c.id, s.id,
       'Common App 文书、活动梳理、网申提交', NULL,
       '["文书指导","活动背景","网申"]', 1, 20, 1
FROM study_abroad_countries c, study_abroad_stages s
WHERE c.country_name='美国' AND c.is_deleted=0
  AND s.stage_name='本科' AND s.is_deleted=0
  AND NOT EXISTS (
      SELECT 1 FROM study_abroad_programs p
      WHERE p.title='美国本科通用申请（Common App）' AND p.country_id=c.id AND p.stage_id=s.id AND p.is_deleted=0
  );

INSERT INTO `study_abroad_programs` (`title`,`country_id`,`stage_id`,`description`,`image_url`,`tags`,`is_hot`,`sort_order`,`is_active`)
SELECT '加拿大硕士工科申请', c.id, s.id,
       '套磁、简历优化、研究计划书', NULL,
       '["套磁","简历优化","研究计划"]', 0, 30, 1
FROM study_abroad_countries c, study_abroad_stages s
WHERE c.country_name='加拿大' AND c.is_deleted=0
  AND s.stage_name='硕士' AND s.is_deleted=0
  AND NOT EXISTS (
      SELECT 1 FROM study_abroad_programs p
      WHERE p.title='加拿大硕士工科申请' AND p.country_id=c.id AND p.stage_id=s.id AND p.is_deleted=0
  );

INSERT INTO `study_abroad_programs` (`title`,`country_id`,`stage_id`,`description`,`image_url`,`tags`,`is_hot`,`sort_order`,`is_active`)
SELECT '中国港澳本科申请', c.id, s.id,
       '港大/港中文等院校申请咨询', NULL,
       '["院校选择","文书指导"]', 0, 40, 1
FROM study_abroad_countries c, study_abroad_stages s
WHERE c.country_name='中国' AND c.is_deleted=0
  AND s.stage_name='本科' AND s.is_deleted=0
  AND NOT EXISTS (
      SELECT 1 FROM study_abroad_programs p
      WHERE p.title='中国港澳本科申请' AND p.country_id=c.id AND p.stage_id=s.id AND p.is_deleted=0
  );


-- -------------------------------------------------
-- Performance Indexes (added by optimization task)
-- -------------------------------------------------

-- Courses: common filter and sort patterns
ALTER TABLE `courses`
  ADD INDEX `idx_courses_status_deleted_created` (`status`, `is_deleted`, `created_at`),
  ADD INDEX `idx_courses_teacher_deleted_status` (`teacher_id`, `is_deleted`, `status`),
  ADD INDEX `idx_courses_subject_deleted_status` (`subject_id`, `is_deleted`, `status`),
  ADD INDEX `idx_courses_featured_status_deleted_created` (`is_featured`, `status`, `is_deleted`, `created_at`);

-- Teacher-Subjects mapping: speed up teacher list by subject and reverse
ALTER TABLE `teacher_subjects`
  ADD INDEX `idx_ts_teacher_subject` (`teacher_id`, `subject_id`),
  ADD INDEX `idx_ts_subject_teacher` (`subject_id`, `teacher_id`);

-- Study Abroad Programs: public list filters
ALTER TABLE `study_abroad_programs`
  ADD INDEX `idx_sap_active_deleted_sort` (`is_active`, `is_deleted`, `sort_order`, `id`),
  ADD INDEX `idx_sap_country_stage_active` (`country_id`, `stage_id`, `is_active`, `is_deleted`),
  ADD INDEX `idx_sap_country_active` (`country_id`, `is_active`, `is_deleted`),
  ADD INDEX `idx_sap_stage_active` (`stage_id`, `is_active`, `is_deleted`);

-- Study Abroad Countries/Stages: active lists ordering
ALTER TABLE `study_abroad_countries`
  ADD INDEX `idx_sac_active_deleted_sort` (`is_active`, `is_deleted`, `sort_order`, `id`);

ALTER TABLE `study_abroad_stages`
  ADD INDEX `idx_sas_active_deleted_sort` (`is_active`, `is_deleted`, `sort_order`, `id`);



-- ----------------------------
-- Table structure for job_posts (教师招聘信息) - 高性能版本
-- ----------------------------
DROP TABLE IF EXISTS `job_posts`;
CREATE TABLE `job_posts` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '招聘ID，主键自增',
  `title` varchar(200) COLLATE utf8mb4_general_ci NOT NULL COMMENT '招聘标题',
  `introduction` text COLLATE utf8mb4_general_ci COMMENT '岗位介绍/职位描述',
  `position_tags` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职位标签，JSON数组字符串，如：["兼职","线上"]',
  -- 性能优化：冗余存储年级和科目信息，避免JOIN查询
  `grade_ids` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联年级ID列表，逗号分隔，如：14,15',
  `grade_names` varchar(200) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '年级名称列表，逗号分隔，如：小学,中学',
  `subject_ids` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '关联科目ID列表，逗号分隔，如：1,3,6',
  `subject_names` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '科目名称列表，逗号分隔，如：小学数学,小学华文,中学数学',
  -- 业务字段
  `status` enum('active','expired') DEFAULT 'active' COMMENT '招聘状态：active-招聘中，expired-已过期',
  `priority` int(10) DEFAULT '0' COMMENT '优先级排序，值越小越靠前',

  -- 审计字段
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：1-已删除，0-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',

  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='教师招聘信息表，供前台招聘页面展示';

-- ----------------------------
-- Table structure for job_post_grades (招聘-年级 关联)
-- ----------------------------
DROP TABLE IF EXISTS `job_post_grades`;
CREATE TABLE `job_post_grades` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_post_id` bigint(20) NOT NULL COMMENT '招聘ID，关联job_posts表',
  `grade_id` bigint(20) NOT NULL COMMENT '年级ID，关联grades表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_grade` (`job_post_id`,`grade_id`),
  KEY `idx_job_post_id` (`job_post_id`),
  KEY `idx_grade_id` (`grade_id`),
  CONSTRAINT `fk_jpg_job` FOREIGN KEY (`job_post_id`) REFERENCES `job_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_jpg_grade` FOREIGN KEY (`grade_id`) REFERENCES `grades` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='招聘与年级的多对多关联表';

-- ----------------------------
-- Table structure for job_post_subjects (招聘-科目 关联)
-- ----------------------------
DROP TABLE IF EXISTS `job_post_subjects`;
CREATE TABLE `job_post_subjects` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `job_post_id` bigint(20) NOT NULL COMMENT '招聘ID，关联job_posts表',
  `subject_id` bigint(20) NOT NULL COMMENT '科目ID，关联subjects表',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',


  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_job_subject` (`job_post_id`,`subject_id`),
  KEY `idx_job_post_id` (`job_post_id`),
  KEY `idx_subject_id` (`subject_id`),
  CONSTRAINT `fk_jps_job` FOREIGN KEY (`job_post_id`) REFERENCES `job_posts` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_jps_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='招聘与科目的多对多关联表';

-- ----------------------------
-- Performance indexes for job posts
-- ----------------------------
ALTER TABLE `job_posts`
  ADD INDEX `idx_job_posts_deleted_created` (`is_deleted`, `created_at`),
  ADD INDEX `idx_job_posts_created` (`created_at`);


-- ----------------------------
-- Performance indexes for job_posts & mappings (方案1)
-- ----------------------------
ALTER TABLE `job_posts`
  ADD INDEX `idx_job_posts_list` (`is_deleted`,`status`,`priority`,`created_at`,`id`);

ALTER TABLE `job_post_grades`
  ADD INDEX `idx_grade_job` (`grade_id`,`job_post_id`);

ALTER TABLE `job_post_subjects`
  ADD INDEX `idx_subject_job` (`subject_id`,`job_post_id`);

-- ----------------------------
-- FULLTEXT index for title/introduction (方案2)
-- ----------------------------
ALTER TABLE `job_posts`
  ADD FULLTEXT `ft_title_intro` (`title`,`introduction`);


-- ----------------------------
-- Records of job_posts & mappings (示例数据)
-- ----------------------------
BEGIN;
-- 岗位1：小学数学兼职教师（线上）
INSERT INTO `job_posts` (`id`,`title`,`introduction`,`position_tags`,`grade_ids`,`grade_names`,`subject_ids`,`subject_names`,`status`,`priority`,`created_at`,`is_deleted`)
VALUES (1001,'小学数学兼职教师','负责小学数学线上授课，要求有耐心，有相关教学经验。','["兼职","线上"]','14','小学','1','小学数学','active',10,NOW(),0);
INSERT INTO `job_post_grades` (`job_post_id`,`grade_id`) VALUES (1001,14);
INSERT INTO `job_post_subjects` (`job_post_id`,`subject_id`) VALUES (1001,1);

-- 岗位2：初中英语教师（可远程）
INSERT INTO `job_posts` (`id`,`title`,`introduction`,`position_tags`,`grade_ids`,`grade_names`,`subject_ids`,`subject_names`,`status`,`priority`,`created_at`,`is_deleted`)
VALUES (1002,'初中英语教师','负责初中阶段英语课程，小班教学，注重口语与阅读能力提升。','["全职","远程"]','15','中学','4','英文(KET)','active',20,NOW(),0);
INSERT INTO `job_post_grades` (`job_post_id`,`grade_id`) VALUES (1002,15);
INSERT INTO `job_post_subjects` (`job_post_id`,`subject_id`) VALUES (1002,4);

-- 岗位3：高中物理精品课主讲
INSERT INTO `job_posts` (`id`,`title`,`introduction`,`position_tags`,`grade_ids`,`grade_names`,`subject_ids`,`subject_names`,`status`,`priority`,`created_at`,`is_deleted`)
VALUES (1003,'高中科学精品课主讲','主讲高中科学精品课程，擅长模型化与解题思维训练；可提供讲义。','["全职","线下优先"]','16','高中','7','中学科学','active',30,NOW(),0);
INSERT INTO `job_post_grades` (`job_post_id`,`grade_id`) VALUES (1003,16);
INSERT INTO `job_post_subjects` (`job_post_id`,`subject_id`) VALUES (1003,7);

-- 岗位4：语文阅读写作指导（小学/中学）
INSERT INTO `job_posts` (`id`,`title`,`introduction`,`position_tags`,`grade_ids`,`grade_names`,`subject_ids`,`subject_names`,`status`,`priority`,`created_at`,`is_deleted`)
VALUES (1004,'华文阅读写作指导','教授阅读理解与写作技巧，面向小学和中学段学生；支持晚间时段授课。','["兼职","晚间"]','14,15','小学,中学','3,8','小学华文,中学华文','active',40,NOW(),0);
INSERT INTO `job_post_grades` (`job_post_id`,`grade_id`) VALUES (1004,14),(1004,15);
INSERT INTO `job_post_subjects` (`job_post_id`,`subject_id`) VALUES (1004,3),(1004,8);
COMMIT;

-- ----------------------------
-- Table structure for balance_transactions
-- ----------------------------
DROP TABLE IF EXISTS `balance_transactions`;
CREATE TABLE `balance_transactions` (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '交易ID，主键自增',
  `user_id` bigint(20) NOT NULL COMMENT '学生用户ID，关联users表',
  `name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '学生姓名',
  `amount` decimal(10,2) NOT NULL COMMENT '变动金额，正数表示增加，负数表示减少',
  `balance_before` decimal(10,2) NOT NULL COMMENT '变动前余额',
  `balance_after` decimal(10,2) NOT NULL COMMENT '变动后余额',
  `transaction_type` enum('RECHARGE','DEDUCT','REFUND') COLLATE utf8mb4_general_ci NOT NULL COMMENT '交易类型：RECHARGE-充值, DEDUCT-扣费, REFUND-退费',
  `reason` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT '变动原因',
  `booking_id` bigint(20) DEFAULT NULL COMMENT '关联的预约ID，可为空',
  `operator_id` bigint(20) DEFAULT NULL COMMENT '操作员ID（如管理员），可为空',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_booking_id` (`booking_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='学生余额变动记录表';
