-- ===========================
-- 用户基础表
-- ===========================
CREATE TABLE users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID，主键自增',
    username VARCHAR(50) UNIQUE NOT NULL COMMENT '用户名，唯一标识',
    email VARCHAR(100) UNIQUE NOT NULL COMMENT '邮箱地址，用于登录和通知',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密）',
    phone VARCHAR(20) COMMENT '手机号码',
    avatar_url VARCHAR(255) COMMENT '头像图片URL地址',
    user_type ENUM('student', 'teacher', 'admin') NOT NULL COMMENT '用户类型：student-学生，teacher-教师，admin-管理员',
    status ENUM('active', 'inactive', 'banned') DEFAULT 'active' COMMENT '账户状态：active-激活，inactive-未激活，banned-封禁',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间'
) COMMENT='用户基础信息表，存储所有用户的通用信息';

-- ===========================
-- 管理员信息表
-- ===========================
CREATE TABLE admins (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '管理员ID，主键自增',
    user_id BIGINT UNIQUE NOT NULL COMMENT '关联用户表的用户ID',
    real_name VARCHAR(50) NOT NULL COMMENT '管理员真实姓名',
    notes TEXT COMMENT '管理员备注信息'
) COMMENT='管理员详细信息表，存储管理员的职务信息和权限';

-- ===========================
-- 学生信息表
-- ===========================
CREATE TABLE students (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '学生ID，主键自增',
    user_id BIGINT UNIQUE NOT NULL COMMENT '关联用户表的用户ID',
    real_name VARCHAR(50) NOT NULL COMMENT '学生真实姓名',
    grade_level VARCHAR(20) COMMENT '年级水平，如：小学三年级、初中一年级、高中二年级',
    subjects_interested VARCHAR(50) COMMENT '感兴趣的科目列表如：[数学,英语,物理]',
    learning_goals TEXT COMMENT '学习目标和需求描述',
    preferred_teaching_style VARCHAR(100) COMMENT '偏好的教学风格，如：严格型、温和型、互动型',
    budget_range VARCHAR(50) COMMENT '预算范围，如：100-200元/小时'  -- 移除了此处的逗号
) COMMENT='学生详细信息表，存储学生的个人资料和学习偏好';

-- ===========================
-- 教师信息表
-- ===========================
CREATE TABLE teachers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '教师ID，主键自增',
    user_id BIGINT UNIQUE NOT NULL COMMENT '关联用户表的用户ID',
    real_name VARCHAR(50) NOT NULL COMMENT '教师真实姓名',
    education_background TEXT COMMENT '教育背景描述，包括学历、毕业院校等',
    teaching_experience INT COMMENT '教学经验年数',
    specialties VARCHAR(50) COMMENT '专业领域如：[高考数学,竞赛辅导,基础提升]',
    subjects VARCHAR(50) COMMENT '可教授科目列表如：[数学,物理,化学]',
    hourly_rate DECIMAL(10,2) COMMENT '每小时收费标准，单位：元',
    introduction TEXT COMMENT '个人介绍和教学理念',
    video_intro_url VARCHAR(255) COMMENT '个人介绍视频URL地址',
    is_verified BOOLEAN DEFAULT FALSE COMMENT '是否已认证：true-已认证，false-未认证'
) COMMENT='教师详细信息表，存储教师的专业资料和教学信息';

-- ===========================
-- 科目分类表
-- ===========================
CREATE TABLE subjects (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '科目ID，主键自增',
    name VARCHAR(50) NOT NULL COMMENT '科目名称，如：数学、语文、英语',
    grade_levels VARCHAR(20) COMMENT '适用年级范围如：[小学,初中,高中]',
    icon_url VARCHAR(255) COMMENT '科目图标URL地址',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用：true-启用，false-禁用'
) COMMENT='科目分类表，定义平台支持的所有教学科目';

-- ===========================
-- 课程表
-- ===========================
CREATE TABLE courses (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '课程ID，主键自增',
    teacher_id BIGINT NOT NULL COMMENT '授课教师ID，关联teachers表',
    subject_id BIGINT NOT NULL COMMENT '课程科目ID，关联subjects表',
    title VARCHAR(200) NOT NULL COMMENT '课程标题',
    description TEXT COMMENT '课程详细描述，包括内容大纲、适合人群等',	
    course_type ENUM('one_on_one','large_class') NOT NULL COMMENT '课程类型：one_on_one-一对一,large_class-大班课',
    duration_minutes INT NOT NULL COMMENT '单次课程时长，单位：分钟',
    status ENUM('active', 'inactive', 'full') DEFAULT 'active' COMMENT '课程状态：active-可报名，inactive-已下架，full-已满员',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '课程创建时间'
) COMMENT='课程信息表，存储教师发布的课程详情';

-- ===========================
-- 课程安排表
-- ===========================
CREATE TABLE schedules (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '排课ID，主键自增',
    teacher_id BIGINT NOT NULL COMMENT '授课教师ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    course_id BIGINT NOT NULL COMMENT '课程ID',
    scheduled_date DATE NOT NULL COMMENT '上课日期',
    start_time TIME NOT NULL COMMENT '开始时间',
    end_time TIME NOT NULL COMMENT '结束时间',
		total_times INT COMMENT '总课程次数',
    status ENUM('progressing', 'completed', 'cancelled') DEFAULT 'progressing' COMMENT '课程状态：progressing-进行中，confirmed-已确认，cancelled-已取消',
    teacher_notes TEXT COMMENT '教师课后备注和反馈',
    student_feedback TEXT COMMENT '学生课后反馈',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '排课创建时间'
) COMMENT='课程安排表，记录具体的上课时间';

CREATE TABLE booking_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '预约申请ID',
    student_id BIGINT NOT NULL COMMENT '学生ID',
    teacher_id BIGINT NOT NULL COMMENT '教师ID',
    course_id BIGINT COMMENT '课程ID，可为空(自定义预约)',
    booking_type ENUM('single', 'recurring') NOT NULL COMMENT '预约类型：single-单次，recurring-周期性',
    
    -- 单次预约字段
    requested_date DATE COMMENT '请求的上课日期(单次预约)',
    requested_start_time TIME COMMENT '请求的开始时间(单次预约)',
    requested_end_time TIME COMMENT '请求的结束时间(单次预约)',
    
    -- 周期性预约字段  
    recurring_weekdays VARCHAR(50) COMMENT '周期性预约的星期几，逗号分隔：1,3,5',
    recurring_time_slots VARCHAR(200) COMMENT '周期性预约的时间段，逗号分隔：14:00-16:00,18:00-20:00',
    start_date DATE COMMENT '周期性预约开始日期',
    end_date DATE COMMENT '周期性预约结束日期',
    total_times INT COMMENT '总课程次数',
    
    -- 通用字段
    duration_minutes INT NOT NULL COMMENT '课程时长(分钟)',
    subject_id BIGINT COMMENT '科目ID',
    student_requirements TEXT COMMENT '学生需求说明',
    
    status ENUM('pending', 'approved', 'rejected', 'cancelled') DEFAULT 'pending' COMMENT '申请状态pending-待定中，approved-已批准，rejected-已拒绝，cancelled-已取消',
    teacher_reply TEXT COMMENT '教师回复内容',
    admin_notes TEXT COMMENT '管理员备注',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    approved_at TIMESTAMP NULL COMMENT '批准时间'
) COMMENT='预约申请表，记录学生的课程预约申请';

CREATE TABLE reschedule_requests (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '调课申请ID',
    schedule_id BIGINT NOT NULL COMMENT '原课程安排ID',
    applicant_id BIGINT NOT NULL COMMENT '申请人ID(学生或教师)',
    applicant_type ENUM('student', 'teacher') NOT NULL COMMENT '申请人类型',
    
    request_type ENUM('single', 'recurring', 'cancel') NOT NULL COMMENT '申请类型：single-单次调课，recurring-周期性调课，cancel-取消课程',
    
    -- 原定安排信息
    original_date DATE NOT NULL COMMENT '原定日期',
    original_start_time TIME NOT NULL COMMENT '原定开始时间',
    original_end_time TIME NOT NULL COMMENT '原定结束时间',
    
    -- 新安排信息(调课用)
    new_date DATE COMMENT '新日期',
    new_start_time TIME COMMENT '新开始时间', 
    new_end_time TIME COMMENT '新结束时间',
    new_weekly_schedule VARCHAR(500) COMMENT '新的周期性安排，格式：周一,周三 14:00-16:00;周五 18:00-20:00',
    
    reason TEXT NOT NULL COMMENT '调课原因',
    urgency_level ENUM('low', 'medium', 'high') DEFAULT 'medium' COMMENT '紧急程度',
    advance_notice_hours INT COMMENT '提前通知小时数',
    
    status ENUM('pending', 'approved', 'rejected', 'cancelled') DEFAULT 'pending' COMMENT '申请状态',
    reviewer_id BIGINT COMMENT '审核人ID',
    reviewer_type ENUM('teacher', 'student', 'admin') COMMENT '审核人类型',
    review_notes TEXT COMMENT '审核备注',
    
    compensation_amount DECIMAL(10,2) DEFAULT 0 COMMENT '补偿金额(如需要)',
    affects_future_sessions BOOLEAN DEFAULT FALSE COMMENT '是否影响后续课程',
    
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    reviewed_at TIMESTAMP NULL COMMENT '审核时间'

) COMMENT='调课申请表，记录课程时间调整申请';

-- 在现有schedules表基础上增加字段
ALTER TABLE schedules ADD COLUMN booking_request_id BIGINT COMMENT '关联预约申请ID';
ALTER TABLE schedules ADD COLUMN booking_source ENUM('request', 'admin') DEFAULT 'request' COMMENT '预约来源：request-申请预约，admin-管理员安排';