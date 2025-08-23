<template>
  <div class="teacher-messages">
    <div class="messages-header">
      <h2>消息中心</h2>
      <div class="message-stats">
        <span class="total-count">总计 {{ total }} 条消息</span>
      </div>
    </div>

    <!-- 消息列表 -->
    <div class="messages-content">
      <div v-if="loading" class="loading">
        <i class="fas fa-spinner fa-spin"></i>
        加载中...
      </div>

      <div v-else-if="messages.length === 0" class="no-messages">
        <i class="fas fa-inbox"></i>
        <p>暂无消息</p>
      </div>

      <div v-else class="message-list">
        <div
          v-for="message in messages"
          :key="message.id"
          class="message-item"
          @click="viewMessage(message)"
        >
          <div class="message-header">
            <h3 class="message-title">{{ message.title }}</h3>
            <div class="message-meta">
              <span class="target-badge" :class="getTargetTypeClass(message.targetType)">
                {{ message.targetTypeDescription }}
              </span>
              <span class="message-time">{{ formatDate(message.createdAt) }}</span>
            </div>
          </div>
          <div class="message-preview">
            {{ getContentPreview(message.content) }}
          </div>
          <div class="message-footer">
            <span class="admin-name">发布者：{{ message.adminName }}</span>
            <i class="fas fa-chevron-right read-more"></i>
          </div>
        </div>
      </div>

      <!-- 分页 -->
      <div v-if="total > 0" class="pagination">
        <button
          @click="changePage(currentPage - 1)"
          :disabled="currentPage <= 1"
          class="btn-page"
        >
          <i class="fas fa-chevron-left"></i>
          上一页
        </button>
        <span class="page-info">
          第 {{ currentPage }} 页，共 {{ totalPages }} 页
        </span>
        <button
          @click="changePage(currentPage + 1)"
          :disabled="currentPage >= totalPages"
          class="btn-page"
        >
          下一页
          <i class="fas fa-chevron-right"></i>
        </button>
      </div>
    </div>

    <!-- 消息详情对话框 -->
    <div v-if="showDetailDialog" class="modal-overlay" @click="showDetailDialog = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>{{ currentMessage?.title }}</h3>
          <button @click="showDetailDialog = false" class="btn-close">
            <i class="fas fa-times"></i>
          </button>
        </div>
        <div class="modal-body">
          <div v-if="currentMessage" class="message-detail">
            <div class="detail-meta">
              <div class="meta-item">
                <span class="target-badge" :class="getTargetTypeClass(currentMessage.targetType)">
                  {{ currentMessage.targetTypeDescription }}
                </span>
              </div>
              <div class="meta-item">
                <span class="admin-name">发布者：{{ currentMessage.adminName }}</span>
              </div>
              <div class="meta-item">
                <span class="publish-time">发布时间：{{ formatDate(currentMessage.createdAt) }}</span>
              </div>
            </div>
            <div class="detail-content">
              <div class="content-text">{{ currentMessage.content }}</div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button @click="showDetailDialog = false" class="btn-close-modal">
            关闭
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { apiRequest } from '../../../utils/api'

export default {
  name: 'TeacherMessages',
  data() {
    return {
      loading: false,
      messages: [],
      total: 0,
      currentPage: 1,
      pageSize: 10,
      showDetailDialog: false,
      currentMessage: null
    }
  },
  computed: {
    totalPages() {
      return Math.ceil(this.total / this.pageSize)
    }
  },
  created() {
    this.loadMessages()
  },
  methods: {
    async loadMessages() {
      this.loading = true
      try {
        const response = await apiRequest('/api/teacher/messages', {
          method: 'GET',
          params: {
            pageNum: this.currentPage,
            pageSize: this.pageSize
          }
        })

        if (response.success) {
          this.messages = response.data.records || []
          this.total = response.data.total || 0
        } else {
          this.$message.error(response.message || '获取消息列表失败')
        }
      } catch (error) {
        console.error('获取消息列表失败:', error)
        this.$message.error('获取消息列表失败')
      } finally {
        this.loading = false
      }
    },

    changePage(page) {
      if (page >= 1 && page <= this.totalPages) {
        this.currentPage = page
        this.loadMessages()
      }
    },

    viewMessage(message) {
      this.currentMessage = message
      this.showDetailDialog = true
    },

    getContentPreview(content) {
      if (!content) return ''
      return content.length > 100 ? content.substring(0, 100) + '...' : content
    },

    getTargetTypeClass(targetType) {
      const classMap = {
        'STUDENT': 'student',
        'TEACHER': 'teacher',
        'ALL': 'all'
      }
      return classMap[targetType] || ''
    },

    formatDate(dateString) {
      if (!dateString) return ''
      const date = new Date(dateString)
      const now = new Date()
      const diff = now - date

      // 小于1小时显示分钟
      if (diff < 3600000) {
        const minutes = Math.floor(diff / 60000)
        return minutes <= 0 ? '刚刚' : `${minutes}分钟前`
      }

      // 小于24小时显示小时
      if (diff < 86400000) {
        const hours = Math.floor(diff / 3600000)
        return `${hours}小时前`
      }

      // 小于7天显示天数
      if (diff < 604800000) {
        const days = Math.floor(diff / 86400000)
        return `${days}天前`
      }

      // 超过7天显示具体日期
      return date.toLocaleDateString('zh-CN')
    }
  }
}
</script>

<style scoped>
.teacher-messages {
  padding: 20px;
  max-width: 800px;
  margin: 0 auto;
}

.messages-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 2px solid #e9ecef;
}

.messages-header h2 {
  margin: 0;
  color: #333;
  font-size: 24px;
}

.message-stats {
  color: #666;
  font-size: 14px;
}

.messages-content {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: hidden;
}

.loading,
.no-messages {
  padding: 60px 20px;
  text-align: center;
  color: #999;
}

.loading i,
.no-messages i {
  font-size: 48px;
  margin-bottom: 15px;
  display: block;
}

.no-messages p {
  font-size: 16px;
  margin: 0;
}

.message-list {
  padding: 0;
}

.message-item {
  padding: 20px;
  border-bottom: 1px solid #eee;
  cursor: pointer;
  transition: all 0.2s ease;
}

.message-item:hover {
  background: #f8f9fa;
}

.message-item:last-child {
  border-bottom: none;
}

.message-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 10px;
}

.message-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #333;
  line-height: 1.4;
  flex: 1;
  margin-right: 15px;
}

.message-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 5px;
}

.target-badge {
  padding: 3px 8px;
  border-radius: 10px;
  font-size: 11px;
  font-weight: 500;
  text-transform: uppercase;
}

.target-badge.student {
  background: #e3f2fd;
  color: #1976d2;
}

.target-badge.teacher {
  background: #f3e5f5;
  color: #7b1fa2;
}

.target-badge.all {
  background: #e8f5e8;
  color: #388e3c;
}

.message-time {
  font-size: 12px;
  color: #999;
}

.message-preview {
  color: #666;
  line-height: 1.5;
  margin-bottom: 10px;
}

.message-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: #999;
}

.admin-name {
  color: #666;
}

.read-more {
  color: #007bff;
}

.pagination {
  padding: 20px;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 15px;
  background: #f8f9fa;
}

.btn-page {
  padding: 8px 16px;
  border: 1px solid #ddd;
  background: white;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 5px;
}

.btn-page:hover {
  background: #007bff;
  color: white;
  border-color: #007bff;
}

.btn-page:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.btn-page:disabled:hover {
  background: white;
  color: #333;
  border-color: #ddd;
}

.page-info {
  color: #666;
  font-size: 14px;
}

/* 模态框样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  border-radius: 12px;
  max-width: 600px;
  width: 90%;
  max-height: 80vh;
  overflow-y: auto;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
}

.modal-header {
  padding: 20px 25px;
  border-bottom: 1px solid #eee;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f8f9fa;
  border-radius: 12px 12px 0 0;
}

.modal-header h3 {
  margin: 0;
  font-size: 20px;
  color: #333;
  flex: 1;
  margin-right: 15px;
}

.btn-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: #999;
  padding: 5px;
  border-radius: 50%;
  transition: all 0.2s;
}

.btn-close:hover {
  background: #e9ecef;
  color: #333;
}

.modal-body {
  padding: 25px;
}

.detail-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 15px;
  margin-bottom: 20px;
  padding-bottom: 15px;
  border-bottom: 1px solid #eee;
}

.meta-item {
  font-size: 14px;
}

.detail-content {
  line-height: 1.6;
}

.content-text {
  font-size: 16px;
  color: #333;
  white-space: pre-wrap;
  word-break: break-word;
}

.modal-footer {
  padding: 20px 25px;
  border-top: 1px solid #eee;
  display: flex;
  justify-content: flex-end;
  background: #f8f9fa;
  border-radius: 0 0 12px 12px;
}

.btn-close-modal {
  background: #6c757d;
  color: white;
  border: none;
  padding: 10px 20px;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-close-modal:hover {
  background: #5a6268;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .teacher-messages {
    padding: 15px;
  }

  .messages-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .message-header {
    flex-direction: column;
    align-items: flex-start;
  }

  .message-meta {
    align-items: flex-start;
    margin-top: 10px;
  }

  .modal-content {
    width: 95%;
    margin: 10px;
  }

  .modal-header,
  .modal-body,
  .modal-footer {
    padding: 15px;
  }
}
</style>
