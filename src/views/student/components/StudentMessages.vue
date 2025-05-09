<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

import teacherBoy1 from '@/assets/pictures/teacherBoy1.jpeg'
import teacherBoy2 from '@/assets/pictures/teacherBoy2.jpeg'


interface Message {
  id: number;
  title: string;
  content: string;
  sender: string;
  senderAvatar?: string;
  type: 'system' | 'teacher' | 'course' | 'notification';
  time: string;
  isRead: boolean;
  importance: 'normal' | 'important' | 'urgent';
}

// 模拟消息数据
const messages = ref<Message[]>([
  {
    id: 1,
    title: '课程调整通知',
    content: '尊敬的学生，您好！您报名的"高中数学 - 函数与导数"课程时间调整为每周一、三的18:00-20:00，请您合理安排时间，准时参加课程。',
    sender: '系统通知',
    type: 'system',
    time: '2023-07-09 14:30',
    isRead: false,
    importance: 'important'
  },
  {
    id: 2,
    title: '作业已批改',
    content: '您的数学作业已经批改完成，得分90分。总体完成得不错，但在第三题的导数应用上有一些小错误，建议您再复习一下导数的几何意义。',
    sender: '张老师',
    senderAvatar: teacherBoy1,
    type: 'teacher',
    time: '2023-07-08 20:15',
    isRead: false,
    importance: 'normal'
  },
  {
    id: 3,
    title: '课程即将开始提醒',
    content: '您的"高中物理 - 力学与电学"课程将于今天16:00开始，请提前做好准备，进入线上课堂。',
    sender: '课程提醒',
    type: 'course',
    time: '2023-07-12 15:00',
    isRead: true,
    importance: 'urgent'
  },
  {
    id: 4,
    title: '系统升级通知',
    content: 'GrabTeacher平台将于本周六凌晨2:00-4:00进行系统升级，届时将暂停服务。给您带来的不便，敬请谅解。',
    sender: '系统通知',
    type: 'notification',
    time: '2023-07-07 10:00',
    isRead: true,
    importance: 'normal'
  },
  {
    id: 5,
    title: '关于物理实验的问题',
    content: '同学你好，关于上节课讲的电磁感应实验，你还有疑问可以再问我，也推荐你看一下课本P78-80的内容，对理解这部分知识很有帮助。',
    sender: '王老师',
    senderAvatar: teacherBoy2,
    type: 'teacher',
    time: '2023-07-06 18:40',
    isRead: true,
    importance: 'normal'
  }
])

// 当前选中的消息
const selectedMessage = ref<Message | null>(null)

// 当前筛选类型
const currentFilter = ref('all')

// 未读消息数量
const unreadCount = computed(() => {
  return messages.value.filter(msg => !msg.isRead).length
})

// 筛选后的消息列表
const filteredMessages = computed(() => {
  if (currentFilter.value === 'all') {
    return messages.value
  } else if (currentFilter.value === 'unread') {
    return messages.value.filter(msg => !msg.isRead)
  } else {
    return messages.value.filter(msg => msg.type === currentFilter.value)
  }
})

// 显示消息详情
const showMessageDetail = (message: Message) => {
  selectedMessage.value = message

  // 如果消息未读，标记为已读
  if (!message.isRead) {
    message.isRead = true
  }
}

// 标记所有消息为已读
const markAllAsRead = () => {
  messages.value.forEach(msg => {
    msg.isRead = true
  })
  ElMessage.success('所有消息已标记为已读')
}

// 删除消息
const deleteMessage = (id: number) => {
  const index = messages.value.findIndex(msg => msg.id === id)
  if (index !== -1) {
    messages.value.splice(index, 1)
    ElMessage.success('消息已删除')

    // 如果删除的是当前选中的消息，清空选中状态
    if (selectedMessage.value && selectedMessage.value.id === id) {
      selectedMessage.value = null
    }
  }
}

// 回复消息相关
const replyVisible = ref(false)
const replyContent = ref('')

const openReply = () => {
  if (selectedMessage.value && selectedMessage.value.type === 'teacher') {
    replyVisible.value = true
  } else {
    ElMessage.warning('只能回复教师消息')
  }
}

const sendReply = () => {
  if (!replyContent.value.trim()) {
    ElMessage.warning('回复内容不能为空')
    return
  }

  ElMessage.success(`回复已发送给 ${selectedMessage.value?.sender}`)
  replyContent.value = ''
  replyVisible.value = false
}
</script>

<script lang="ts">
export default {
  name: 'StudentMessages'
}
</script>

<template>
  <div class="student-messages">
    <h2>消息中心 <el-badge v-if="unreadCount > 0" :value="unreadCount" type="danger" /></h2>

    <div class="messages-container">
      <!-- 消息筛选栏 -->
      <div class="filter-bar">
        <el-radio-group v-model="currentFilter" size="large">
          <el-radio-button label="all">全部消息</el-radio-button>
          <el-radio-button label="unread">
            未读消息
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="unread-badge" />
          </el-radio-button>
          <el-radio-button label="system">系统通知</el-radio-button>
          <el-radio-button label="teacher">教师消息</el-radio-button>
          <el-radio-button label="course">课程提醒</el-radio-button>
        </el-radio-group>

        <el-button
          type="info"
          @click="markAllAsRead"
          :disabled="unreadCount === 0"
        >
          全部标为已读
        </el-button>
      </div>

      <!-- 消息内容区域 -->
      <div class="messages-content">
        <!-- 消息列表 -->
        <div class="message-list">
          <div v-if="filteredMessages.length === 0" class="no-messages">
            <el-empty description="暂无消息" />
          </div>

          <div
            v-for="message in filteredMessages"
            :key="message.id"
            class="message-item"
            :class="{
              'unread': !message.isRead,
              'selected': selectedMessage && selectedMessage.id === message.id,
              'important': message.importance === 'important',
              'urgent': message.importance === 'urgent'
            }"
            @click="showMessageDetail(message)"
          >
            <div class="message-icon">
              <el-avatar v-if="message.senderAvatar" :src="message.senderAvatar" :size="40" />
              <el-avatar v-else :size="40" :class="`message-type-${message.type}`">
                <template #default>
                  <el-icon v-if="message.type === 'system'"><Bell /></el-icon>
                  <el-icon v-else-if="message.type === 'teacher'"><User /></el-icon>
                  <el-icon v-else-if="message.type === 'course'"><Reading /></el-icon>
                  <el-icon v-else><Notification /></el-icon>
                </template>
              </el-avatar>
            </div>

            <div class="message-info">
              <div class="message-header">
                <span class="message-title" :class="{ 'unread': !message.isRead }">
                  {{ message.title }}
                </span>
                <div class="message-meta">
                  <span class="message-sender">{{ message.sender }}</span>
                  <span class="message-time">{{ message.time }}</span>
                </div>
              </div>

              <div class="message-preview">
                {{ message.content.length > 80 ? message.content.substring(0, 80) + '...' : message.content }}
              </div>
            </div>

            <div v-if="!message.isRead" class="unread-indicator"></div>
          </div>
        </div>

        <!-- 消息详情 -->
        <div class="message-detail">
          <div v-if="selectedMessage" class="detail-content">
            <div class="detail-header">
              <h3 class="detail-title">{{ selectedMessage.title }}</h3>
              <div class="detail-actions">
                <el-button
                  v-if="selectedMessage.type === 'teacher'"
                  type="primary"
                  size="small"
                  @click="openReply"
                >
                  <el-icon><ChatDotRound /></el-icon> 回复
                </el-button>
                <el-button
                  type="danger"
                  size="small"
                  @click="deleteMessage(selectedMessage.id)"
                >
                  <el-icon><Delete /></el-icon> 删除
                </el-button>
              </div>
            </div>

            <div class="detail-meta">
              <div class="sender-info">
                <el-avatar v-if="selectedMessage.senderAvatar" :src="selectedMessage.senderAvatar" :size="30" />
                <el-avatar v-else :size="30" :class="`message-type-${selectedMessage.type}`">
                  <template #default>
                    <el-icon v-if="selectedMessage.type === 'system'"><Bell /></el-icon>
                    <el-icon v-else-if="selectedMessage.type === 'teacher'"><User /></el-icon>
                    <el-icon v-else-if="selectedMessage.type === 'course'"><Reading /></el-icon>
                    <el-icon v-else><Notification /></el-icon>
                  </template>
                </el-avatar>
                <span>{{ selectedMessage.sender }}</span>
              </div>
              <div class="message-tags">
                <el-tag
                  size="small"
                  :type="selectedMessage.type === 'system' ? 'info' :
                        selectedMessage.type === 'teacher' ? 'success' :
                        selectedMessage.type === 'course' ? 'warning' : 'danger'"
                >
                  {{ selectedMessage.type === 'system' ? '系统通知' :
                     selectedMessage.type === 'teacher' ? '教师消息' :
                     selectedMessage.type === 'course' ? '课程提醒' : '平台公告' }}
                </el-tag>
                <el-tag
                  v-if="selectedMessage.importance !== 'normal'"
                  size="small"
                  :type="selectedMessage.importance === 'important' ? 'warning' : 'danger'"
                >
                  {{ selectedMessage.importance === 'important' ? '重要' : '紧急' }}
                </el-tag>
              </div>
              <div class="message-time">
                <el-icon><Timer /></el-icon>
                <span>{{ selectedMessage.time }}</span>
              </div>
            </div>

            <div class="detail-body">
              {{ selectedMessage.content }}
            </div>

            <!-- 回复区域 -->
            <div v-if="replyVisible" class="reply-area">
              <h4>回复 {{ selectedMessage.sender }}</h4>
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="4"
                placeholder="请输入回复内容..."
              />
              <div class="reply-actions">
                <el-button @click="replyVisible = false">取消</el-button>
                <el-button type="primary" @click="sendReply">发送</el-button>
              </div>
            </div>
          </div>

          <div v-else class="empty-detail">
            <el-empty description="请选择消息查看详情" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.student-messages {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
  display: flex;
  align-items: center;
  gap: 10px;
}

.messages-container {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.filter-bar {
  padding: 15px;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e4e7ed;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.unread-badge {
  margin-left: 5px;
}

.messages-content {
  display: flex;
  height: calc(100vh - 220px);
  min-height: 500px;
}

.message-list {
  width: 350px;
  border-right: 1px solid #e4e7ed;
  overflow-y: auto;
  height: 100%;
}

.message-item {
  display: flex;
  padding: 15px;
  border-bottom: 1px solid #e4e7ed;
  cursor: pointer;
  position: relative;
  transition: background-color 0.3s;
}

.message-item:hover {
  background-color: #f5f7fa;
}

.message-item.selected {
  background-color: #ecf5ff;
}

.message-item.unread {
  background-color: #f0f9eb;
}

.message-item.important {
  border-left: 3px solid #e6a23c;
}

.message-item.urgent {
  border-left: 3px solid #f56c6c;
}

.message-icon {
  margin-right: 10px;
  flex-shrink: 0;
}

.message-type-system {
  background-color: #909399;
}

.message-type-teacher {
  background-color: #67c23a;
}

.message-type-course {
  background-color: #e6a23c;
}

.message-type-notification {
  background-color: #f56c6c;
}

.message-info {
  flex: 1;
  min-width: 0;
}

.message-header {
  margin-bottom: 5px;
}

.message-title {
  display: block;
  font-size: 15px;
  color: #333;
  margin-bottom: 5px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.message-title.unread {
  font-weight: bold;
}

.message-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
}

.message-preview {
  font-size: 13px;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.unread-indicator {
  position: absolute;
  top: 15px;
  right: 15px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
}

.message-detail {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
}

.detail-content {
  height: 100%;
  display: flex;
  flex-direction: column;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 15px;
}

.detail-title {
  margin: 0;
  font-size: 20px;
  color: #333;
}

.detail-actions {
  display: flex;
  gap: 10px;
}

.detail-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 15px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e4e7ed;
}

.sender-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.message-tags {
  display: flex;
  gap: 10px;
}

.message-time {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #909399;
  font-size: 14px;
}

.detail-body {
  flex: 1;
  padding: 10px 0;
  line-height: 1.6;
  color: #333;
  white-space: pre-line;
}

.reply-area {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #e4e7ed;
}

.reply-area h4 {
  margin: 0 0 15px;
  font-size: 16px;
  color: #333;
}

.reply-actions {
  margin-top: 15px;
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.empty-detail {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-messages {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 768px) {
  .messages-content {
    flex-direction: column;
    height: auto;
  }

  .message-list {
    width: 100%;
    height: 300px;
  }

  .message-detail {
    padding: 15px;
  }

  .detail-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }
}
</style>
