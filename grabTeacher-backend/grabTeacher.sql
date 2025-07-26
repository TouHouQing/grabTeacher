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

 Date: 26/07/2025 20:35:49
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
  `is_trial` tinyint(1) DEFAULT '0' COMMENT '是否为免费试听课：true-是，false-否',
  `trial_duration_minutes` int DEFAULT NULL COMMENT '试听课时长（分钟）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='预约申请表，记录学生的课程预约申请';

-- ----------------------------
-- Records of booking_requests
-- ----------------------------
BEGIN;
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (3, 6, 7, NULL, 'recurring', NULL, NULL, NULL, '2,3', '17:00-18:00', '2025-07-22', '2025-08-26', 12, '希望预约12355老师的语文课程', 'approved', '可以', NULL, '2025-07-21 13:07:49', '2025-07-21 13:12:05', '2025-07-21 13:12:05', 0, NULL, 0, NULL);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (4, 6, 7, 3, 'recurring', NULL, NULL, NULL, '2,5', '14:00-15:00', '2025-07-23', '2025-08-27', 12, '希望预约12355老师的《语文》课程', 'approved', '可以', NULL, '2025-07-21 13:47:39', '2025-07-21 13:47:58', '2025-07-21 13:47:58', 0, NULL, 0, NULL);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (5, 6, 7, 3, 'single', '2025-08-16', '11:00:00', '11:30:00', NULL, NULL, NULL, NULL, NULL, '希望预约12355老师的《语文》课程', 'pending', NULL, NULL, '2025-07-21 22:03:14', '2025-07-21 22:03:14', NULL, 0, NULL, 1, 30);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (6, 6, 7, 3, 'single', '2025-07-30', '10:30:00', '11:00:00', NULL, NULL, NULL, NULL, NULL, '希望预约12355老师的《语文》课程', 'pending', NULL, NULL, '2025-07-21 22:03:24', '2025-07-21 22:03:24', NULL, 0, NULL, 1, 30);
INSERT INTO `booking_requests` (`id`, `student_id`, `teacher_id`, `course_id`, `booking_type`, `requested_date`, `requested_start_time`, `requested_end_time`, `recurring_weekdays`, `recurring_time_slots`, `start_date`, `end_date`, `total_times`, `student_requirements`, `status`, `teacher_reply`, `admin_notes`, `created_at`, `updated_at`, `approved_at`, `is_deleted`, `deleted_at`, `is_trial`, `trial_duration_minutes`) VALUES (7, 6, 115, 5001, 'single', '2025-08-08', '10:30:00', '11:00:00', NULL, NULL, NULL, NULL, NULL, '希望预约门捷列夫老师的《初中化学基础课程》课程', 'pending', NULL, NULL, '2025-07-26 18:41:46', '2025-07-26 18:41:46', NULL, 0, NULL, 1, 30);
COMMIT;

-- ----------------------------
-- Table structure for course_grades
-- ----------------------------
DROP TABLE IF EXISTS `course_grades`;
CREATE TABLE `course_grades` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `course_id` bigint NOT NULL COMMENT '课程ID，关联courses表',
  `grade` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '适用年级',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_course_id` (`course_id`),
  KEY `idx_grade` (`grade`)
) ENGINE=InnoDB AUTO_INCREMENT=270 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程年级关联表';

-- ----------------------------
-- Records of course_grades
-- ----------------------------
BEGIN;
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (1, 1, '小学一年级', '2025-07-26 17:41:16');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (2, 1, '小学二年级', '2025-07-26 17:41:16');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (3, 1, '小学三年级', '2025-07-26 17:41:16');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (4, 2, '初中一年级', '2025-07-26 17:41:16');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (5, 2, '初中二年级', '2025-07-26 17:41:16');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (6, 3, '高中一年级', '2025-07-26 17:41:16');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (7, 3, '高中二年级', '2025-07-26 17:41:16');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (8, 3, '高中三年级', '2025-07-26 17:41:16');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (9, 1001, '小学一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (10, 1001, '小学二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (11, 1001, '小学三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (12, 1002, '小学一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (13, 1002, '小学二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (14, 1002, '小学三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (15, 1003, '小学一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (16, 1003, '小学二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (17, 1003, '小学三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (18, 1004, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (19, 1004, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (20, 1004, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (21, 1005, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (22, 1005, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (23, 1005, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (24, 1006, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (25, 1006, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (26, 1006, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (27, 1007, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (28, 1007, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (29, 1007, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (30, 1008, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (31, 1008, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (32, 1008, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (33, 1009, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (34, 1009, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (35, 1009, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (36, 2001, '小学一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (37, 2001, '小学二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (38, 2001, '小学三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (39, 2002, '小学一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (40, 2002, '小学二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (41, 2002, '小学三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (42, 2003, '小学一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (43, 2003, '小学二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (44, 2003, '小学三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (45, 2004, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (46, 2004, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (47, 2004, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (48, 2005, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (49, 2005, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (50, 2005, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (51, 2006, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (52, 2006, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (53, 2006, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (54, 2007, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (55, 2007, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (56, 2007, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (57, 2008, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (58, 2008, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (59, 2008, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (60, 2009, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (61, 2009, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (62, 2009, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (63, 3001, '小学一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (64, 3001, '小学二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (65, 3001, '小学三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (66, 3002, '小学一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (67, 3002, '小学二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (68, 3002, '小学三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (69, 3003, '小学一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (70, 3003, '小学二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (71, 3003, '小学三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (72, 3004, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (73, 3004, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (74, 3004, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (75, 3005, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (76, 3005, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (77, 3005, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (78, 3006, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (79, 3006, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (80, 3006, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (81, 3007, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (82, 3007, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (83, 3007, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (84, 3008, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (85, 3008, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (86, 3008, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (87, 3009, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (88, 3009, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (89, 3009, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (90, 4001, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (91, 4001, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (92, 4001, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (93, 4002, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (94, 4002, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (95, 4002, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (96, 4003, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (97, 4003, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (98, 4003, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (99, 4004, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (100, 4004, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (101, 4004, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (102, 4005, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (103, 4005, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (104, 4005, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (105, 4006, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (106, 4006, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (107, 4006, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (108, 5001, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (109, 5001, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (110, 5001, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (111, 5002, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (112, 5002, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (113, 5002, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (114, 5003, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (115, 5003, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (116, 5003, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (117, 5004, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (118, 5004, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (119, 5004, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (120, 5005, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (121, 5005, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (122, 5005, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (123, 5006, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (124, 5006, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (125, 5006, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (126, 6001, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (127, 6001, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (128, 6001, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (129, 6002, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (130, 6002, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (131, 6002, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (132, 6003, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (133, 6003, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (134, 6003, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (135, 6004, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (136, 6004, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (137, 6004, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (138, 6005, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (139, 6005, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (140, 6005, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (141, 6006, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (142, 6006, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (143, 6006, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (144, 7001, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (145, 7001, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (146, 7001, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (147, 7002, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (148, 7002, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (149, 7002, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (150, 7003, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (151, 7003, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (152, 7003, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (153, 7004, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (154, 7004, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (155, 7004, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (156, 7005, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (157, 7005, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (158, 7005, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (159, 7006, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (160, 7006, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (161, 7006, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (162, 8001, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (163, 8001, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (164, 8001, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (165, 8002, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (166, 8002, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (167, 8002, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (168, 8003, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (169, 8003, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (170, 8003, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (171, 8004, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (172, 8004, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (173, 8004, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (174, 8005, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (175, 8005, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (176, 8005, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (177, 8006, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (178, 8006, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (179, 8006, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (180, 9001, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (181, 9001, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (182, 9001, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (183, 9002, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (184, 9002, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (185, 9002, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (186, 9003, '初中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (187, 9003, '初中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (188, 9003, '初中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (189, 9004, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (190, 9004, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (191, 9004, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (192, 9005, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (193, 9005, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (194, 9005, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (195, 9006, '高中一年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (196, 9006, '高中二年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (197, 9006, '高中三年级', '2025-07-26 18:39:24');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (198, 2001, '小学一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (199, 2001, '小学二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (200, 2001, '小学三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (201, 2001, '小学四年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (202, 2001, '小学五年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (203, 2001, '小学六年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (204, 2002, '小学一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (205, 2002, '小学二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (206, 2002, '小学三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (207, 2002, '小学四年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (208, 2002, '小学五年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (209, 2002, '小学六年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (210, 2003, '小学一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (211, 2003, '小学二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (212, 2003, '小学三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (213, 2003, '小学四年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (214, 2003, '小学五年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (215, 2003, '小学六年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (216, 2004, '初中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (217, 2004, '初中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (218, 2004, '初中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (219, 2005, '初中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (220, 2005, '初中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (221, 2005, '初中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (222, 2006, '初中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (223, 2006, '初中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (224, 2006, '初中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (225, 2007, '高中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (226, 2007, '高中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (227, 2007, '高中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (228, 2008, '高中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (229, 2008, '高中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (230, 2008, '高中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (231, 2009, '高中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (232, 2009, '高中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (233, 2009, '高中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (234, 3001, '小学一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (235, 3001, '小学二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (236, 3001, '小学三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (237, 3001, '小学四年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (238, 3001, '小学五年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (239, 3001, '小学六年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (240, 3002, '小学一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (241, 3002, '小学二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (242, 3002, '小学三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (243, 3002, '小学四年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (244, 3002, '小学五年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (245, 3002, '小学六年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (246, 3003, '小学一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (247, 3003, '小学二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (248, 3003, '小学三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (249, 3003, '小学四年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (250, 3003, '小学五年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (251, 3003, '小学六年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (252, 3004, '初中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (253, 3004, '初中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (254, 3004, '初中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (255, 3005, '初中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (256, 3005, '初中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (257, 3005, '初中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (258, 3006, '初中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (259, 3006, '初中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (260, 3006, '初中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (261, 3007, '高中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (262, 3007, '高中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (263, 3007, '高中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (264, 3008, '高中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (265, 3008, '高中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (266, 3008, '高中三年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (267, 3009, '高中一年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (268, 3009, '高中二年级', '2025-07-26 18:51:17');
INSERT INTO `course_grades` (`id`, `course_id`, `grade`, `created_at`) VALUES (269, 3009, '高中三年级', '2025-07-26 18:51:17');
COMMIT;

-- ----------------------------
-- Table structure for courses
-- ----------------------------
DROP TABLE IF EXISTS `courses`;
CREATE TABLE `courses` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '课程ID，主键自增',
  `teacher_id` bigint NOT NULL COMMENT '授课教师ID，关联teachers表',
  `subject_id` bigint NOT NULL COMMENT '课程科目ID，关联subjects表',
  `title` varchar(200) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程标题',
  `description` text COLLATE utf8mb4_unicode_ci COMMENT '课程详细描述，包括内容大纲、适合人群等',
  `course_type` enum('one_on_one','large_class') COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '课程类型：one_on_one-一对一,large_class-大班课',
  `duration_minutes` int NOT NULL COMMENT '单次课程时长，单位：分钟',
  `status` enum('active','inactive','full') COLLATE utf8mb4_unicode_ci DEFAULT 'active' COMMENT '课程状态：active-可报名，inactive-已下架，full-已满员',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '课程创建时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=9007 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='课程信息表，存储教师发布的课程详情';

-- ----------------------------
-- Records of courses
-- ----------------------------
BEGIN;
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (1, 7, 1, '小学语文基础班', '针对小学生的语文基础知识教学，包括拼音、汉字、阅读理解等', 'one_on_one', 120, 'active', '2025-07-20 14:05:18', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2, 7, 4, '初中数学提高班', '初中数学重点难点突破，提高解题能力', 'one_on_one', 120, 'active', '2025-07-20 14:05:40', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3, 7, 1, '高中语文冲刺班', '高考语文专项训练，作文写作技巧提升', 'large_class', 90, 'active', '2025-07-20 18:45:57', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2001, 104, 5, '小学数学基础课程', '小学数学基础知识，数的认识、四则运算等', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2002, 105, 5, '小学数学提高课程', '小学数学进阶课程，应用题和几何初步', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2003, 106, 5, '小学数学精品课程', '小学数学精品课程，培养数学思维', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2004, 104, 5, '初中数学基础课程', '初中数学基础教学，代数和几何并重', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2005, 105, 5, '初中数学提高课程', '初中数学进阶课程，函数和方程重点', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2006, 106, 5, '初中数学冲刺课程', '初中数学冲刺课程，针对中考重点', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2007, 107, 5, '高中数学基础课程', '高中数学基础教学，函数、导数、几何', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2008, 104, 5, '高中数学提高课程', '高中数学进阶课程，解析几何和立体几何', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (2009, 105, 5, '高中数学冲刺课程', '高中数学冲刺课程，针对高考重点难点', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3001, 108, 6, '小学英语基础课程', '小学英语基础教学，字母、单词、简单对话', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3002, 109, 6, '小学英语提高课程', '小学英语进阶课程，语法和阅读入门', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3003, 110, 6, '小学英语精品课程', '小学英语精品课程，培养英语兴趣', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3004, 108, 6, '初中英语基础课程', '初中英语基础教学，语法和词汇并重', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3005, 109, 6, '初中英语提高课程', '初中英语进阶课程，阅读理解和写作', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3006, 111, 6, '初中英语冲刺课程', '初中英语冲刺课程，针对中考重点', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3007, 110, 6, '高中英语基础课程', '高中英语基础教学，语法、词汇、阅读', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3008, 108, 6, '高中英语提高课程', '高中英语进阶课程，写作和听力强化', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (3009, 109, 6, '高中英语冲刺课程', '高中英语冲刺课程，针对高考重点', 'one_on_one', 45, 'active', '2025-07-26 18:51:17', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (4001, 112, 7, '初中物理基础课程', '初中物理基础教学，力学、光学、电学入门', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (4002, 113, 7, '初中物理提高课程', '初中物理进阶课程，实验和理论并重', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (4003, 114, 7, '初中物理冲刺课程', '初中物理冲刺课程，针对中考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (4004, 112, 7, '高中物理基础课程', '高中物理基础教学，力学、电磁学', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (4005, 113, 7, '高中物理提高课程', '高中物理进阶课程，光学、原子物理', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (4006, 114, 7, '高中物理冲刺课程', '高中物理冲刺课程，针对高考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (5001, 115, 8, '初中化学基础课程', '初中化学基础教学，化学反应、元素化合物', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (5002, 116, 8, '初中化学提高课程', '初中化学进阶课程，化学计算和实验', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (5003, 117, 8, '初中化学冲刺课程', '初中化学冲刺课程，针对中考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (5004, 115, 8, '高中化学基础课程', '高中化学基础教学，有机化学、无机化学', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (5005, 116, 8, '高中化学提高课程', '高中化学进阶课程，化学平衡、电化学', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (5006, 117, 8, '高中化学冲刺课程', '高中化学冲刺课程，针对高考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (6001, 118, 9, '初中生物基础课程', '初中生物基础教学，细胞、遗传、进化', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (6002, 119, 9, '初中生物提高课程', '初中生物进阶课程，生态系统、生物多样性', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (6003, 120, 9, '初中生物冲刺课程', '初中生物冲刺课程，针对中考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (6004, 118, 9, '高中生物基础课程', '高中生物基础教学，分子生物学、遗传学', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (6005, 119, 9, '高中生物提高课程', '高中生物进阶课程，生态学、进化论', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (6006, 120, 9, '高中生物冲刺课程', '高中生物冲刺课程，针对高考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (7001, 121, 10, '初中历史基础课程', '初中历史基础教学，中国古代史、近现代史', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (7002, 122, 10, '初中历史提高课程', '初中历史进阶课程，世界史、史学方法', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (7003, 123, 10, '初中历史冲刺课程', '初中历史冲刺课程，针对中考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (7004, 121, 10, '高中历史基础课程', '高中历史基础教学，中国史、世界史', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (7005, 122, 10, '高中历史提高课程', '高中历史进阶课程，史学理论、史料分析', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (7006, 123, 10, '高中历史冲刺课程', '高中历史冲刺课程，针对高考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (8001, 124, 11, '初中地理基础课程', '初中地理基础教学，自然地理、人文地理', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (8002, 125, 11, '初中地理提高课程', '初中地理进阶课程，区域地理、地理技能', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (8003, 126, 11, '初中地理冲刺课程', '初中地理冲刺课程，针对中考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (8004, 124, 11, '高中地理基础课程', '高中地理基础教学，自然地理原理', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (8005, 125, 11, '高中地理提高课程', '高中地理进阶课程，人文地理、区域发展', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (8006, 126, 11, '高中地理冲刺课程', '高中地理冲刺课程，针对高考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (9001, 127, 12, '初中政治基础课程', '初中政治基础教学，思想品德、法律常识', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (9002, 128, 12, '初中政治提高课程', '初中政治进阶课程，政治理论、时事政治', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (9003, 129, 12, '初中政治冲刺课程', '初中政治冲刺课程，针对中考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (9004, 127, 12, '高中政治基础课程', '高中政治基础教学，马克思主义基本原理', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (9005, 128, 12, '高中政治提高课程', '高中政治进阶课程，政治经济学、哲学', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
INSERT INTO `courses` (`id`, `teacher_id`, `subject_id`, `title`, `description`, `course_type`, `duration_minutes`, `status`, `created_at`, `is_deleted`, `deleted_at`) VALUES (9006, 130, 12, '高中政治冲刺课程', '高中政治冲刺课程，针对高考重点', 'one_on_one', 45, 'active', '2025-07-26 18:39:24', 0, NULL);
COMMIT;

-- ----------------------------
-- Table structure for grades
-- ----------------------------
DROP TABLE IF EXISTS `grades`;
CREATE TABLE `grades` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '年级ID，主键自增',
  `grade_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '年级名称，如：小学一年级、初中二年级',
  `description` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '年级描述',
  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_grade_name` (`grade_name`),
  KEY `idx_grade_name` (`grade_name`)
) ENGINE=InnoDB AUTO_INCREMENT=14 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='年级信息表，存储系统中的年级数据';

-- ----------------------------
-- Records of grades
-- ----------------------------
BEGIN;
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (1, '小学一年级', '适合6-7岁儿童', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (2, '小学二年级', '适合7-8岁儿童', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (3, '小学三年级', '适合8-9岁儿童', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (4, '小学四年级', '适合9-10岁儿童', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (5, '小学五年级', '适合10-11岁儿童', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (6, '小学六年级', '适合11-12岁儿童', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (7, '初中一年级', '适合12-13岁学生', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (8, '初中二年级', '适合13-14岁学生', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (9, '初中三年级', '适合14-15岁学生', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (10, '高中一年级', '适合15-16岁学生', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (11, '高中二年级', '适合16-17岁学生', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
INSERT INTO `grades` (`id`, `grade_name`, `description`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`) VALUES (12, '高中三年级', '适合17-18岁学生', '2025-07-26 15:15:30', '2025-07-26 15:15:30', 0, NULL);
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
  `course_id` bigint DEFAULT NULL COMMENT '课程ID',
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
  `is_trial` tinyint(1) DEFAULT '0' COMMENT '是否为试听课：true-是，false-否',
  `session_number` int DEFAULT NULL COMMENT '课程序号（在周期性课程中的第几次课）',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=22 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='课程安排表，记录具体的上课时间';

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
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (12, 7, 6, 3, '2025-07-25', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 1);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (13, 7, 6, 3, '2025-07-29', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 2);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (14, 7, 6, 3, '2025-08-01', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 3);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (15, 7, 6, 3, '2025-08-05', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 4);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (16, 7, 6, 3, '2025-08-08', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 5);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (17, 7, 6, 3, '2025-08-12', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 6);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (18, 7, 6, 3, '2025-08-15', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 7);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (19, 7, 6, 3, '2025-08-19', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 8);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (20, 7, 6, 3, '2025-08-22', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 9);
INSERT INTO `schedules` (`id`, `teacher_id`, `student_id`, `course_id`, `scheduled_date`, `start_time`, `end_time`, `total_times`, `status`, `teacher_notes`, `student_feedback`, `created_at`, `booking_request_id`, `booking_source`, `is_deleted`, `deleted_at`, `recurring_weekdays`, `recurring_time_slots`, `is_trial`, `session_number`) VALUES (21, 7, 6, 3, '2025-08-26', '14:00:00', '15:00:00', 12, 'progressing', NULL, NULL, '2025-07-21 13:47:58', 4, 'request', 0, NULL, '2,5', '14:00-15:00', 0, 10);
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
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='学生感兴趣科目关联表';

-- ----------------------------
-- Records of student_subjects
-- ----------------------------
BEGIN;
INSERT INTO `student_subjects` (`id`, `student_id`, `subject_id`, `created_at`) VALUES (1, 6, 12, '2025-07-26 19:11:51');
INSERT INTO `student_subjects` (`id`, `student_id`, `subject_id`, `created_at`) VALUES (2, 6, 9, '2025-07-26 19:11:51');
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
  `gender` enum('男','女','不愿透露') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT '不愿透露' COMMENT '性别：男、女、不愿透露',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='学生详细信息表，存储学生的个人资料和学习偏好';

-- ----------------------------
-- Records of students
-- ----------------------------
BEGIN;
INSERT INTO `students` (`id`, `user_id`, `real_name`, `grade_level`, `subjects_interested`, `learning_goals`, `preferred_teaching_style`, `budget_range`, `is_deleted`, `deleted_at`, `gender`) VALUES (6, 10, 'student23', '小学一年级', '政治,生物', '123131123', '实践型教学', '100-200', 0, NULL, '不愿透露');
INSERT INTO `students` (`id`, `user_id`, `real_name`, `grade_level`, `subjects_interested`, `learning_goals`, `preferred_teaching_style`, `budget_range`, `is_deleted`, `deleted_at`, `gender`) VALUES (7, 15, '青', '小学二年级', '数学', '666', '温和型', '200-300', 0, NULL, '不愿透露');
COMMIT;

-- ----------------------------
-- Table structure for subjects
-- ----------------------------
DROP TABLE IF EXISTS `subjects`;
CREATE TABLE `subjects` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '科目ID，主键自增',
  `name` varchar(50) NOT NULL COMMENT '科目名称，如：数学、语文、英语',
  `icon_url` varchar(255) DEFAULT NULL COMMENT '科目图标URL地址',
  `is_active` tinyint(1) DEFAULT '1' COMMENT '是否启用：true-启用，false-禁用',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=13 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='科目分类表，定义平台支持的所有教学科目';

-- ----------------------------
-- Records of subjects
-- ----------------------------
BEGIN;
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (1, '语文', '', 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (5, '数学', NULL, 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (6, '英语', NULL, 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (7, '物理', NULL, 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (8, '化学', NULL, 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (9, '生物', NULL, 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (10, '历史', NULL, 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (11, '地理', NULL, 1, 0, NULL);
INSERT INTO `subjects` (`id`, `name`, `icon_url`, `is_active`, `is_deleted`, `deleted_at`) VALUES (12, '政治', NULL, 1, 0, NULL);
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
  CONSTRAINT `fk_teacher_subjects_subject` FOREIGN KEY (`subject_id`) REFERENCES `subjects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_teacher_subjects_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `teachers` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=36 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师科目关联表';

-- ----------------------------
-- Records of teacher_subjects
-- ----------------------------
BEGIN;
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (1, 8, 1, '2025-07-26 17:46:47');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (2, 8, 5, '2025-07-26 18:25:41');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (3, 8, 6, '2025-07-26 18:25:41');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (4, 8, 9, '2025-07-26 18:25:41');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (5, 101, 1, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (6, 102, 1, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (7, 103, 1, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (8, 104, 5, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (9, 105, 5, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (10, 106, 5, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (11, 107, 5, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (12, 108, 6, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (13, 109, 6, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (14, 110, 6, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (15, 111, 6, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (16, 112, 7, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (17, 113, 7, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (18, 114, 7, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (19, 115, 8, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (20, 116, 8, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (21, 117, 8, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (22, 118, 9, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (23, 119, 9, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (24, 120, 9, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (25, 121, 10, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (26, 122, 10, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (27, 123, 10, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (28, 124, 11, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (29, 125, 11, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (30, 126, 11, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (31, 127, 12, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (32, 128, 12, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (33, 129, 12, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (34, 130, 12, '2025-07-26 18:38:07');
INSERT INTO `teacher_subjects` (`id`, `teacher_id`, `subject_id`, `created_at`) VALUES (35, 7, 1, '2025-07-26 18:44:21');
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
  `hourly_rate` decimal(10,2) DEFAULT NULL COMMENT '每小时收费标准，单位：元',
  `introduction` text COMMENT '个人介绍和教学理念',
  `video_intro_url` varchar(255) DEFAULT NULL COMMENT '个人介绍视频URL地址',
  `gender` enum('男','女','不愿透露') DEFAULT '不愿透露' COMMENT '性别：男、女、不愿透露',
  `is_verified` tinyint(1) DEFAULT '0' COMMENT '是否已认证：true-已认证，false-未认证',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '是否删除：true-已删除，false-未删除',
  `deleted_at` timestamp NULL DEFAULT NULL COMMENT '删除时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `user_id` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=131 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='教师详细信息表，存储教师的专业资料和教学信息';

-- ----------------------------
-- Records of teachers
-- ----------------------------
BEGIN;
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (7, 14, '12355', '硕士研究生', 1, '1231236', 50.00, '123123555', NULL, '不愿透露', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (8, 16, '青', '333', 2, '5555', NULL, '1123132312', NULL, '不愿透露', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (101, 101, '张语文', '北京师范大学中文系硕士', 8, '古代文学、现代文学、作文指导', 120.00, '专业语文教师，擅长古诗词教学和作文指导，有丰富的中高考辅导经验。', NULL, '女', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (102, 102, '李文华', '华东师范大学汉语言文学学士', 5, '阅读理解、文言文、写作技巧', 100.00, '年轻有活力的语文老师，善于激发学生对文学的兴趣，教学方法新颖。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (103, 103, '王诗雅', '南京师范大学中文系博士', 12, '诗词鉴赏、文学史、语言文字运用', 150.00, '资深语文教师，对古典文学有深入研究，教学经验丰富。', NULL, '女', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (104, 104, '陈数学', '清华大学数学系硕士', 10, '高等数学、解析几何、微积分', 140.00, '数学功底扎实，善于将复杂问题简单化，帮助学生建立数学思维。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (105, 105, '刘计算', '北京大学数学系学士', 6, '代数、几何、概率统计', 110.00, '耐心细致的数学老师，擅长基础知识巩固和解题技巧训练。', NULL, '女', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (106, 106, '赵函数', '复旦大学应用数学硕士', 7, '函数、导数、数列', 125.00, '思维敏捷，善于启发式教学，帮助学生理解数学本质。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (107, 107, '孙几何', '上海交通大学数学系博士', 15, '立体几何、解析几何、数学建模', 160.00, '资深数学教师，在几何教学方面有独特见解，培养了众多优秀学生。', NULL, '女', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (108, 108, 'Smith', '剑桥大学英语文学硕士', 9, '口语、听力、语法', 130.00, '外籍英语教师，纯正英式发音，擅长提高学生口语和听力水平。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (109, 109, '李英语', '北京外国语大学英语系硕士', 7, '阅读理解、写作、翻译', 115.00, '专业英语教师，对英语教学有深入研究，善于培养学生语感。', NULL, '女', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (110, 110, 'Johnson', '牛津大学语言学博士', 12, '语音学、语法、文学', 145.00, '资深外教，在语言学方面有深厚造诣，教学方法科学有效。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (111, 111, '张口语', '上海外国语大学英语系学士', 4, '口语交际、商务英语、雅思托福', 95.00, '年轻活泼的英语老师，善于营造轻松的学习氛围。', NULL, '女', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (112, 112, '牛物理', '中科院物理所博士', 11, '力学、电磁学、光学', 135.00, '物理功底深厚，善于用实验和生活实例解释物理现象。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (113, 113, '爱因斯坦', '北京理工大学物理系硕士', 8, '量子力学、相对论、热力学', 120.00, '理论物理专家，善于培养学生的物理思维和解题能力。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (114, 114, '居里夫人', '清华大学物理系学士', 6, '原子物理、核物理、实验物理', 105.00, '注重实验教学，帮助学生理解物理原理，培养动手能力。', NULL, '女', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (115, 115, '门捷列夫', '北京化工大学化学系博士', 13, '有机化学、无机化学、分析化学', 140.00, '化学专家，对化学反应机理有深入理解，教学严谨细致。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (116, 116, '居里化学', '华东理工大学化学系硕士', 9, '物理化学、化学平衡、电化学', 125.00, '善于将抽象的化学概念具体化，帮助学生理解化学本质。', NULL, '女', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (117, 117, '拉瓦锡', '南京大学化学系学士', 5, '化学实验、化学计算、化学工艺', 100.00, '注重实验安全和操作规范，培养学生的实验技能。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (118, 118, '达尔文', '北京大学生物系博士', 10, '进化论、遗传学、生态学', 130.00, '生物学专家，善于用进化的观点解释生物现象。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (119, 119, '孟德尔', '清华大学生物系硕士', 7, '遗传学、分子生物学、细胞生物学', 115.00, '遗传学专家，善于用实例解释遗传规律。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (120, 120, '袁隆平', '中国农业大学生物系学士', 8, '植物学、农业生物学、生物技术', 110.00, '植物生物学专家，注重理论与实践相结合。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (121, 121, '司马迁', '北京师范大学历史系博士', 14, '中国古代史、史学理论、文献学', 145.00, '历史学专家，对中国古代史有深入研究，善于史料分析。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (122, 122, '范文澜', '华东师范大学历史系硕士', 9, '中国近现代史、世界史、史学方法', 125.00, '善于将历史事件与现实联系，培养学生的历史思维。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (123, 123, '吕思勉', '南京大学历史系学士', 6, '中国通史、历史地理、史学概论', 105.00, '注重历史脉络梳理，帮助学生建立完整的历史知识体系。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (124, 124, '徐霞客', '北京大学地理系博士', 12, '自然地理、人文地理、地理信息系统', 135.00, '地理学专家，善于用地图和实例教学，培养学生的空间思维。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (125, 125, '竺可桢', '南京师范大学地理系硕士', 8, '气候学、地貌学、环境地理', 120.00, '气候地理专家，注重地理现象的成因分析。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (126, 126, '李四光', '华东师范大学地理系学士', 5, '地质地理、区域地理、地理教学法', 100.00, '年轻的地理老师，善于用现代技术辅助教学。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (127, 127, '马克思', '中国人民大学马克思主义学院博士', 15, '马克思主义基本原理、政治经济学、哲学', 150.00, '马克思主义理论专家，善于理论联系实际。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (128, 128, '恩格斯', '北京大学哲学系硕士', 10, '辩证唯物主义、历史唯物主义、政治学', 130.00, '哲学功底深厚，善于培养学生的辩证思维。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (129, 129, '列宁', '清华大学马克思主义学院学士', 7, '政治理论、思想政治教育、时事政治', 115.00, '政治理论扎实，善于结合时事进行教学。', NULL, '男', 1, 0, NULL);
INSERT INTO `teachers` (`id`, `user_id`, `real_name`, `education_background`, `teaching_experience`, `specialties`, `hourly_rate`, `introduction`, `video_intro_url`, `gender`, `is_verified`, `is_deleted`, `deleted_at`) VALUES (130, 130, '毛泽东', '中央党校政治学博士', 20, '毛泽东思想、中国特色社会主义理论、党史', 180.00, '资深政治理论专家，教学经验极其丰富。', NULL, '男', 1, 0, NULL);
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
  `has_used_trial` tinyint(1) DEFAULT '0' COMMENT '是否已使用免费试听：true-已使用，false-未使用',
  `trial_used_at` timestamp NULL DEFAULT NULL COMMENT '试听课使用时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=131 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户基础信息表，存储所有用户的通用信息';

-- ----------------------------
-- Records of users
-- ----------------------------
BEGIN;
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (10, 'student', 'qinghaoyang@foxmail.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '', NULL, 'student', 'active', '2025-07-19 13:21:54', '2025-07-19 13:21:54', 0, '2025-07-19 23:32:59', 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (11, 'admin', 'admin@admin.com', '$2a$10$CLJSuGd2ptKI9VlCz3r4buGyY7HfKg1qivwbKEfkk8/6Pz57oKjWK', NULL, NULL, 'admin', 'active', '2025-07-19 16:23:39', '2025-07-19 19:54:42', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (14, 'teacher', '123456@qq.com', '$2a$10$eNVMo.XWzg/OG1RFYwzX0etNFyhahK3Cx3qVmPWvXP2hlOVstrVPm', '', NULL, 'teacher', 'active', '2025-07-19 14:38:47', '2025-07-19 14:38:47', 0, '2025-07-19 23:26:32', 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (15, 'student2', 'jiang659585281@163.com', '$2a$10$Cktv7O3R9jT2UAdz15jyLO7lQ1UWcJwn/7v3gt9kV5XyWoJ6CDNiu', '', NULL, 'student', 'active', '2025-07-21 10:51:14', '2025-07-21 10:51:14', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (16, 'teacher2', 'PGS.000936581@med.suez.edu.eg', '$2a$10$Vu87ac.Pm3FAyPuco3A3W.W.ZtxM10pLyo1LM7wZuqX0sFdg/5g7y', '', NULL, 'teacher', 'active', '2025-07-21 10:51:59', '2025-07-21 10:51:59', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (101, 'teacher_chinese_01', 'chinese01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000101', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (102, 'teacher_chinese_02', 'chinese02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000102', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (103, 'teacher_chinese_03', 'chinese03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000103', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (104, 'teacher_math_01', 'math01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000104', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (105, 'teacher_math_02', 'math02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000105', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (106, 'teacher_math_03', 'math03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000106', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (107, 'teacher_math_04', 'math04@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000107', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (108, 'teacher_english_01', 'english01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000108', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (109, 'teacher_english_02', 'english02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000109', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (110, 'teacher_english_03', 'english03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000110', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (111, 'teacher_english_04', 'english04@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000111', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (112, 'teacher_physics_01', 'physics01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000112', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (113, 'teacher_physics_02', 'physics02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000113', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (114, 'teacher_physics_03', 'physics03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000114', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (115, 'teacher_chemistry_01', 'chemistry01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000115', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (116, 'teacher_chemistry_02', 'chemistry02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000116', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (117, 'teacher_chemistry_03', 'chemistry03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000117', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (118, 'teacher_biology_01', 'biology01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000118', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (119, 'teacher_biology_02', 'biology02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000119', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (120, 'teacher_biology_03', 'biology03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000120', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (121, 'teacher_history_01', 'history01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000121', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (122, 'teacher_history_02', 'history02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000122', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (123, 'teacher_history_03', 'history03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000123', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (124, 'teacher_geography_01', 'geography01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000124', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (125, 'teacher_geography_02', 'geography02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000125', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (126, 'teacher_geography_03', 'geography03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000126', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (127, 'teacher_politics_01', 'politics01@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000127', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (128, 'teacher_politics_02', 'politics02@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000128', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (129, 'teacher_politics_03', 'politics03@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000129', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
INSERT INTO `users` (`id`, `username`, `email`, `password`, `phone`, `avatar_url`, `user_type`, `status`, `created_at`, `updated_at`, `is_deleted`, `deleted_at`, `has_used_trial`, `trial_used_at`) VALUES (130, 'teacher_politics_04', 'politics04@teacher.com', '$2a$10$pd68PvpCnLxJEymLxawDz.HUvpXAh56NFRnhaOXhBe4z2sh2no8iW', '13800000130', NULL, 'teacher', 'active', '2025-07-26 18:38:07', '2025-07-26 18:38:07', 0, NULL, 0, NULL);
COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
