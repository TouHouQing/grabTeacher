<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'

interface Message {
  id: number;
  sender: string;
  senderAvatar: string;
  senderType: 'system' | 'student' | 'admin';
  content: string;
  time: string;
  isRead: boolean;
}

// 模拟消息数据
const messages = ref<Message[]>([
  {
    id: 1,
    sender: '系统通知',
    senderAvatar: '',
    senderType: 'system',
    content: '欢迎使用GrabTeacher教师平台，您的账号已激活。请完善您的个人资料，以便学生能更好地了解您。',
    time: '2023-07-01 08:30',
    isRead: true
  },
  {
    id: 2,
    sender: '张明',
    senderAvatar: 'https://img1.baidu.com/it/u=1817951587,699502146&fm=253&fmt=auto&app=138&f=JPEG?w=500&h=500',
    senderType: 'student',
    content: '老师您好，我想请问下周三的课程是否可以调整时间？我有个学校活动需要参加。',
    time: '2023-07-08 15:45',
    isRead: false
  },
  {
    id: 3,
    sender: '李华',
    senderAvatar: 'https://img1.baidu.com/it/u=2496571732,442429293&fm=253&fmt=auto&app=138&f=JPEG?w=400&h=400',
    senderType: 'student',
    content: '老师，我已经完成了您布置的所有作业，请查收。',
    time: '2023-07-07 20:12',
    isRead: true
  },
  {
    id: 4,
    sender: '王芳',
    senderAvatar: 'https://img0.baidu.com/it/u=1944204113,1917062272&fm=253&fmt=auto&app=138&f=JPEG?w=400&h=400',
    senderType: 'student',
    content: '老师，我遇到了一个关于函数的问题，可以请您帮忙解答一下吗？',
    time: '2023-07-09 10:05',
    isRead: false
  },
  {
    id: 5,
    sender: '平台管理员',
    senderAvatar: '',
    senderType: 'admin',
    content: '您的教师资格认证已通过审核，现在可以接单授课了。',
    time: '2023-07-05 14:30',
    isRead: true
  }
])

// 当前筛选类型
const currentFilter = ref('all') // all, unread, system, student, admin

// 根据筛选条件过滤消息
const filteredMessages = computed(() => {
  if (currentFilter.value === 'all') {
    return messages.value;
  } else if (currentFilter.value === 'unread') {
    return messages.value.filter(msg => !msg.isRead);
  } else {
    return messages.value.filter(msg => msg.senderType === currentFilter.value);
  }
})

// 未读消息数量
const unreadCount = computed(() => {
  return messages.value.filter(msg => !msg.isRead).length;
})

// 当前选中的消息
const selectedMessage = ref<Message | null>(null)

// 标记消息为已读
const markAsRead = (message: Message) => {
  if (!message.isRead) {
    message.isRead = true
  }
  selectedMessage.value = message
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
    if (selectedMessage.value && selectedMessage.value.id === id) {
      selectedMessage.value = null
    }
  }
}

// 回复消息
const replyContent = ref('')
const sendReply = () => {
  if (!selectedMessage.value || !replyContent.value.trim()) {
    return
  }

  ElMessage.success(`回复已发送给 ${selectedMessage.value.sender}`)
  replyContent.value = ''
}
</script>

<script lang="ts">
export default {
  name: 'TeacherMessages'
}
</script>

<template>
  <div class="teacher-messages">
    <h2>消息中心 <el-badge v-if="unreadCount > 0" :value="unreadCount" type="danger" /></h2>

    <div class="messages-container">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <el-radio-group v-model="currentFilter" size="large">
          <el-radio-button label="all">全部消息</el-radio-button>
          <el-radio-button label="unread">
            未读消息
            <el-badge v-if="unreadCount > 0" :value="unreadCount" class="unread-badge" />
          </el-radio-button>
          <el-radio-button label="system">系统通知</el-radio-button>
          <el-radio-button label="student">学生消息</el-radio-button>
          <el-radio-button label="admin">管理员消息</el-radio-button>
        </el-radio-group>
        <el-button type="info" @click="markAllAsRead" :disabled="unreadCount === 0">
          全部标为已读
        </el-button>
      </div>

      <!-- 消息列表区域 -->
      <div class="messages-content">
        <!-- 消息列表 -->
        <div class="messages-list">
          <div
            v-for="message in filteredMessages"
            :key="message.id"
            class="message-item"
            :class="{ 'message-unread': !message.isRead, 'message-selected': selectedMessage && selectedMessage.id === message.id }"
            @click="markAsRead(message)"
          >
            <div class="message-avatar">
              <el-avatar v-if="message.senderAvatar" :src="message.senderAvatar" :size="40" />
              <el-avatar v-else :size="40" :icon="message.senderType === 'system' ? 'Bell' : 'User'" />
            </div>
            <div class="message-info">
              <div class="message-header">
                <span class="sender-name">{{ message.sender }}</span>
                <span class="message-time">{{ message.time }}</span>
              </div>
              <div class="message-preview">
                {{ message.content.substring(0, 50) }}{{ message.content.length > 50 ? '...' : '' }}
              </div>
            </div>
            <div class="message-status" v-if="!message.isRead"></div>
          </div>

          <el-empty v-if="filteredMessages.length === 0" description="暂无消息" />
        </div>

        <!-- 消息详情 -->
        <div class="message-detail">
          <template v-if="selectedMessage">
            <div class="detail-header">
              <div class="sender-info">
                <el-avatar v-if="selectedMessage.senderAvatar" :src="selectedMessage.senderAvatar" :size="50" />
                <el-avatar v-else :size="50" :icon="selectedMessage.senderType === 'system' ? 'Bell' : 'User'" />
                <div class="sender-details">
                  <div class="sender-name">{{ selectedMessage.sender }}</div>
                  <div class="sender-type">
                    <el-tag
                      size="small"
                      :type="selectedMessage.senderType === 'system' ? 'info' :
                            selectedMessage.senderType === 'admin' ? 'danger' : 'success'"
                    >
                      {{ selectedMessage.senderType === 'system' ? '系统通知' :
                         selectedMessage.senderType === 'admin' ? '管理员' : '学生' }}
                    </el-tag>
                  </div>
                </div>
              </div>
              <div class="detail-actions">
                <el-button
                  type="danger"
                  size="small"
                  @click="deleteMessage(selectedMessage.id)"
                  icon="Delete"
                  circle
                />
              </div>
            </div>

            <div class="detail-time">{{ selectedMessage.time }}</div>

            <div class="detail-content">
              {{ selectedMessage.content }}
            </div>

            <div class="reply-box" v-if="selectedMessage.senderType === 'student'">
              <div class="reply-header">回复 {{ selectedMessage.sender }}</div>
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="4"
                placeholder="输入回复内容..."
              />
              <div class="reply-actions">
                <el-button type="primary" @click="sendReply" :disabled="!replyContent.trim()">
                  发送回复
                </el-button>
              </div>
            </div>
          </template>

          <div v-else class="no-message-selected">
            <el-empty description="请选择一条消息查看详情" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.teacher-messages {
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
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  overflow: hidden;
}

.filter-bar {
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
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

.messages-list {
  width: 350px;
  border-right: 1px solid #ebeef5;
  overflow-y: auto;
  height: 100%;
}

.message-item {
  padding: 15px;
  border-bottom: 1px solid #ebeef5;
  cursor: pointer;
  display: flex;
  align-items: flex-start;
  transition: background-color 0.3s;
  position: relative;
}

.message-item:hover {
  background-color: #f5f7fa;
}

.message-selected {
  background-color: #ecf5ff;
}

.message-unread {
  background-color: #f0f9eb;
}

.message-avatar {
  margin-right: 12px;
}

.message-info {
  flex: 1;
}

.message-header {
  display: flex;
  justify-content: space-between;
  margin-bottom: 5px;
}

.sender-name {
  font-weight: bold;
  color: #333;
}

.message-time {
  font-size: 12px;
  color: #909399;
}

.message-preview {
  color: #606266;
  font-size: 13px;
  line-height: 1.4;
}

.message-status {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #f56c6c;
  position: absolute;
  top: 15px;
  right: 15px;
}

.message-detail {
  flex: 1;
  padding: 20px;
  overflow-y: auto;
  height: 100%;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.sender-info {
  display: flex;
  align-items: center;
}

.sender-details {
  margin-left: 15px;
}

.sender-details .sender-name {
  font-size: 18px;
  font-weight: bold;
  margin-bottom: 5px;
}

.detail-time {
  margin-bottom: 20px;
  color: #909399;
  font-size: 13px;
}

.detail-content {
  margin-bottom: 30px;
  line-height: 1.6;
  color: #333;
  white-space: pre-line;
}

.reply-box {
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.reply-header {
  margin-bottom: 10px;
  font-weight: bold;
  color: #333;
}

.reply-actions {
  margin-top: 15px;
  text-align: right;
}

.no-message-selected {
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

  .messages-list {
    width: 100%;
    max-height: 300px;
    border-right: none;
    border-bottom: 1px solid #ebeef5;
  }

  .message-detail {
    padding: 15px;
  }
}
</style>
