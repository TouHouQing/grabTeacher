<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage } from 'element-plus'

interface Student {
  id: number;
  name: string;
  avatar: string;
  grade: string;
  subject: string;
  progress: number;
  enrollDate: string;
  nextClass: string;
  contactPhone: string;
  contactEmail: string;
  status: string;
}

// 模拟学生数据
const students = ref<Student[]>([
  {
    id: 1,
    name: '张明',
    avatar: '/src/assets/pictures/studentBoy1.jpeg',
    grade: 'Secondary 3',
    subject: 'Mathematics',
    progress: 60,
    enrollDate: '2023-05-15',
    nextClass: '2023-07-15 10:00',
    contactPhone: '138****1234',
    contactEmail: 'zhangming@example.com',
    status: '正常'
  },
  {
    id: 2,
    name: '李华',
    avatar: '/src/assets/pictures/studentBoy2.jpeg',
    grade: 'Secondary 2',
    subject: 'Mathematics',
    progress: 45,
    enrollDate: '2023-06-01',
    nextClass: '2023-07-12 16:00',
    contactPhone: '139****5678',
    contactEmail: 'lihua@example.com',
    status: '正常'
  },
  {
    id: 3,
    name: '王芳',
    avatar: '/src/assets/pictures/studentGirl1.jpeg',
    grade: 'Primary 6',
    subject: 'English',
    progress: 30,
    enrollDate: '2023-06-20',
    nextClass: '2023-07-18 14:00',
    contactPhone: '136****9012',
    contactEmail: 'wangfang@example.com',
    status: '请假'
  }
])

// 搜索条件
const searchForm = reactive({
  keyword: '',
  grade: '',
  subject: '',
  status: ''
})

// 筛选学生列表
const filteredStudents = computed(() => {
  return students.value.filter(student => {
    const keywordMatch = student.name.includes(searchForm.keyword) ||
                         student.contactPhone.includes(searchForm.keyword) ||
                         student.contactEmail.includes(searchForm.keyword);
    const gradeMatch = !searchForm.grade || student.grade === searchForm.grade;
    const subjectMatch = !searchForm.subject || student.subject === searchForm.subject;
    const statusMatch = !searchForm.status || student.status === searchForm.status;

    return keywordMatch && gradeMatch && subjectMatch && statusMatch;
  });
})

// 学生详情
const studentDetail = ref<Student | null>(null)
const detailDialogVisible = ref(false)

const showStudentDetail = (student: Student) => {
  studentDetail.value = student
  detailDialogVisible.value = true
}

// 发送消息
const sendMessageDialogVisible = ref(false)
const messageForm = reactive({
  student: null as Student | null,
  content: ''
})

const openSendMessage = (student: Student) => {
  messageForm.student = student
  messageForm.content = ''
  sendMessageDialogVisible.value = true
}

const sendMessage = () => {
  ElMessage.success(`消息已发送给 ${messageForm.student?.name}`)
  sendMessageDialogVisible.value = false
}

// 安排课程
const scheduleClassDialogVisible = ref(false)
const scheduleForm = reactive({
  student: null as Student | null,
  date: '',
  startTime: '',
  endTime: '',
  content: ''
})

const openScheduleClass = (student: Student) => {
  scheduleForm.student = student
  scheduleForm.date = ''
  scheduleForm.startTime = ''
  scheduleForm.endTime = ''
  scheduleForm.content = ''
  scheduleClassDialogVisible.value = true
}

const scheduleClass = () => {
  ElMessage.success(`已为 ${scheduleForm.student?.name} 安排课程`)
  scheduleClassDialogVisible.value = false
}
</script>

<script lang="ts">
export default {
  name: 'TeacherStudents'
}
</script>

<template>
  <div class="teacher-students">
    <h2>学生管理</h2>

    <!-- 搜索栏 -->
    <div class="search-bar">
      <el-form :inline="true" :model="searchForm">
        <el-form-item>
          <el-input v-model="searchForm.keyword" placeholder="学生姓名/联系方式" clearable></el-input>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.grade" placeholder="年级" clearable>
            <el-option label="Primary 6" value="Primary 6"></el-option>
            <el-option label="Secondary 2" value="Secondary 2"></el-option>
            <el-option label="Secondary 3" value="Secondary 3"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.subject" placeholder="科目" clearable>
            <el-option label="Mathematics" value="Mathematics"></el-option>
            <el-option label="English" value="English"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-select v-model="searchForm.status" placeholder="状态" clearable>
            <el-option label="正常" value="正常"></el-option>
            <el-option label="请假" value="请假"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="searchForm.keyword = ''">重置</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 学生列表 -->
    <div class="students-list">
      <el-card v-for="student in filteredStudents" :key="student.id" class="student-card">
        <div class="student-info">
          <div class="student-avatar">
            <img :src="$getImageUrl(student.avatar)" :alt="student.name">
          </div>
          <div class="student-details">
            <h3>{{ student.name }}</h3>
            <p>{{ student.grade }} | {{ student.subject }}</p>
            <div class="progress-info">
              <span>学习进度:</span>
              <el-progress :percentage="student.progress"></el-progress>
            </div>
            <div class="student-tags">
              <el-tag size="small" type="success">{{ student.status }}</el-tag>
              <el-tag size="small" type="info">{{ student.enrollDate }} 加入</el-tag>
            </div>
          </div>
          <div class="student-actions">
            <div class="next-class">
              <span>下节课: {{ student.nextClass }}</span>
            </div>
            <div class="action-buttons">
              <el-button size="small" type="primary" @click="openScheduleClass(student)">安排课程</el-button>
              <el-button size="small" type="success" @click="openSendMessage(student)">发送消息</el-button>
              <el-button size="small" type="info" @click="showStudentDetail(student)">查看详情</el-button>
            </div>
          </div>
        </div>
      </el-card>

      <div v-if="filteredStudents.length === 0" class="no-data">
        <el-empty description="暂无学生数据"></el-empty>
      </div>
    </div>

    <!-- 学生详情对话框 -->
    <el-dialog
      v-model="detailDialogVisible"
      title="学生详情"
      width="50%"
    >
      <div v-if="studentDetail" class="student-detail-dialog">
        <div class="detail-header">
          <div class="detail-avatar">
            <img :src="$getImageUrl(studentDetail.avatar)" :alt="studentDetail.name">
          </div>
          <div class="detail-basic-info">
            <h3>{{ studentDetail.name }}</h3>
            <p>{{ studentDetail.grade }} | {{ studentDetail.subject }}</p>
            <p><i class="el-icon-phone"></i> {{ studentDetail.contactPhone }}</p>
            <p><i class="el-icon-message"></i> {{ studentDetail.contactEmail }}</p>
          </div>
        </div>

        <div class="detail-body">
          <div class="detail-item">
            <span class="item-label">加入日期:</span>
            <span>{{ studentDetail.enrollDate }}</span>
          </div>
          <div class="detail-item">
            <span class="item-label">下节课时间:</span>
            <span>{{ studentDetail.nextClass }}</span>
          </div>
          <div class="detail-item">
            <span class="item-label">学习进度:</span>
            <el-progress :percentage="studentDetail.progress"></el-progress>
          </div>
          <div class="detail-item">
            <span class="item-label">状态:</span>
            <el-tag size="small" type="success">{{ studentDetail.status }}</el-tag>
          </div>
        </div>
      </div>
    </el-dialog>

    <!-- 发送消息对话框 -->
    <el-dialog
      v-model="sendMessageDialogVisible"
      title="发送消息"
      width="40%"
    >
      <div v-if="messageForm.student" class="message-form">
        <p class="message-to">发送给: {{ messageForm.student.name }}</p>
        <el-form :model="messageForm">
          <el-form-item>
            <el-input
              v-model="messageForm.content"
              type="textarea"
              :rows="5"
              placeholder="请输入消息内容..."
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="sendMessage">发送</el-button>
            <el-button @click="sendMessageDialogVisible = false">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>

    <!-- 安排课程对话框 -->
    <el-dialog
      v-model="scheduleClassDialogVisible"
      title="安排课程"
      width="40%"
    >
      <div v-if="scheduleForm.student" class="schedule-form">
        <p class="schedule-for">为 {{ scheduleForm.student.name }} 安排课程</p>
        <el-form :model="scheduleForm" label-width="100px">
          <el-form-item label="日期">
            <el-date-picker
              v-model="scheduleForm.date"
              type="date"
              placeholder="选择日期"
              style="width: 100%"
            ></el-date-picker>
          </el-form-item>
          <el-form-item label="开始时间">
            <el-time-picker
              v-model="scheduleForm.startTime"
              placeholder="选择时间"
              format="HH:mm"
              style="width: 100%"
            ></el-time-picker>
          </el-form-item>
          <el-form-item label="结束时间">
            <el-time-picker
              v-model="scheduleForm.endTime"
              placeholder="选择时间"
              format="HH:mm"
              style="width: 100%"
            ></el-time-picker>
          </el-form-item>
          <el-form-item label="课程内容">
            <el-input
              v-model="scheduleForm.content"
              type="textarea"
              :rows="3"
              placeholder="请输入课程内容..."
            ></el-input>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="scheduleClass">确认安排</el-button>
            <el-button @click="scheduleClassDialogVisible = false">取消</el-button>
          </el-form-item>
        </el-form>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.teacher-students {
  width: 100%;
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.search-bar {
  margin-bottom: 20px;
  background-color: #fff;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.students-list {
  display: flex;
  flex-direction: column;
  gap: 15px;
}

.student-card {
  transition: transform 0.3s;
}

.student-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.student-info {
  display: flex;
  align-items: center;
}

.student-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 20px;
}

.student-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.student-details {
  flex: 1;
}

.student-details h3 {
  margin: 0 0 5px;
  font-size: 18px;
  color: #333;
}

.student-details p {
  margin: 0 0 10px;
  color: #666;
}

.progress-info {
  margin-bottom: 10px;
}

.progress-info span {
  margin-right: 10px;
  color: #666;
}

.student-tags {
  display: flex;
  gap: 10px;
}

.student-actions {
  margin-left: 20px;
  text-align: right;
}

.next-class {
  margin-bottom: 10px;
  color: #666;
}

.action-buttons {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.no-data {
  margin-top: 40px;
}

/* 详情对话框样式 */
.student-detail-dialog {
  padding: 10px;
}

.detail-header {
  display: flex;
  margin-bottom: 20px;
}

.detail-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 20px;
}

.detail-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.detail-basic-info h3 {
  margin: 0 0 10px;
  font-size: 20px;
  color: #333;
}

.detail-basic-info p {
  margin: 5px 0;
  color: #666;
}

.detail-body {
  background-color: #f9f9f9;
  padding: 15px;
  border-radius: 4px;
}

.detail-item {
  margin-bottom: 15px;
}

.item-label {
  font-weight: bold;
  color: #333;
  margin-right: 10px;
}

/* 消息表单样式 */
.message-form, .schedule-form {
  padding: 10px;
}

.message-to, .schedule-for {
  margin-bottom: 15px;
  font-weight: bold;
  color: #333;
}

@media (max-width: 768px) {
  .student-info {
    flex-direction: column;
    align-items: flex-start;
  }

  .student-avatar {
    margin-bottom: 15px;
  }

  .student-actions {
    margin-left: 0;
    margin-top: 15px;
    width: 100%;
    text-align: center;
  }
}
</style>
