<template>
  <div class="message-management">
    <div class="header">
      <h3>消息管理</h3>
    </div>

    <!-- 搜索区域 -->
    <el-card class="search-card" shadow="never">
      <el-form :model="searchParams" inline>
        <el-form-item label="消息标题">
          <el-input
            v-model="searchParams.title"
            placeholder="搜索消息标题"
            :prefix-icon="Search"
            clearable
            style="width: 200px"
            @keyup.enter="searchMessages"
          />
        </el-form-item>
        <el-form-item label="目标对象">
          <el-select v-model="searchParams.targetType" placeholder="选择目标对象" clearable style="width: 150px">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="全体" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchParams.isActive" placeholder="选择状态" clearable style="width: 120px">
            <el-option label="激活" :value="true" />
            <el-option label="停用" :value="false" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="searchMessages">搜索</el-button>
          <el-button :icon="Refresh" @click="resetSearch">重置</el-button>
          <el-button type="primary" :icon="Plus" @click="showCreateDialog = true">
            发布消息
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

        <!-- 消息列表 -->
    <div class="table-wrap">
      <el-table :data="messages" v-loading="loading" stripe style="width: 100%">
        <el-table-column prop="id" label="ID" width="80" align="center" />
        <el-table-column label="标题" min-width="200">
          <template #default="scope">
            <el-link type="primary" @click="viewMessage(scope.row)">
              {{ scope.row.title }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="目标对象" width="120" align="center">
          <template #default="scope">
            <el-tag
              :type="getTargetTypeTagType(scope.row.targetType)"
              size="small"
            >
              {{ scope.row.targetTypeDescription }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="adminName" label="发布者" width="120" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="scope">
            <el-switch
              v-model="scope.row.isActive"
              @change="toggleMessageStatus(scope.row)"
              :loading="scope.row.statusLoading || false"
            />
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="180" align="center">
          <template #default="scope">
            {{ formatDate(scope.row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="scope">
            <div class="operation-buttons">
              <el-button
                size="small"
                :icon="View"
                @click="viewMessage(scope.row)"
              >
                查看
              </el-button>
              <el-button
                size="small"
                :icon="Edit"
                @click="editMessage(scope.row)"
              >
                编辑
              </el-button>
              <el-button
                size="small"
                type="danger"
                :icon="Delete"
                @click="deleteMessage(scope.row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <!-- 分页 -->
    <div class="pagination-wrapper" v-if="total > 0">
      <el-pagination
        :current-page="currentPage"
        :page-size="pageSize"
        :page-sizes="[10, 20, 50, 100]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
      />
    </div>

        <!-- 创建/编辑消息对话框 -->
    <el-dialog
      v-model="showCreateDialog"
      :title="'发布消息'"
      width="600px"
      @close="closeDialogs"
    >
      <el-form :model="messageForm" label-width="100px">
        <el-form-item label="消息标题" required>
          <el-input
            v-model="messageForm.title"
            placeholder="请输入消息标题"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="目标对象" required>
          <el-select v-model="messageForm.targetType" placeholder="请选择目标对象" style="width: 100%">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="全体" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息内容" required>
          <el-input
            v-model="messageForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入消息内容"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeDialogs">取消</el-button>
          <el-button type="primary" @click="submitMessage" :loading="submitting">
            发布
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 编辑消息对话框 -->
    <el-dialog
      v-model="showEditDialog"
      :title="'编辑消息'"
      width="600px"
      @close="closeDialogs"
    >
      <el-form :model="messageForm" label-width="100px">
        <el-form-item label="消息标题" required>
          <el-input
            v-model="messageForm.title"
            placeholder="请输入消息标题"
            maxlength="255"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="目标对象" required>
          <el-select v-model="messageForm.targetType" placeholder="请选择目标对象" style="width: 100%">
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
            <el-option label="全体" value="ALL" />
          </el-select>
        </el-form-item>
        <el-form-item label="消息内容" required>
          <el-input
            v-model="messageForm.content"
            type="textarea"
            :rows="8"
            placeholder="请输入消息内容"
            maxlength="5000"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="激活状态">
          <el-switch v-model="messageForm.isActive" />
        </el-form-item>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="closeDialogs">取消</el-button>
          <el-button type="primary" @click="submitMessage" :loading="submitting">
            更新
          </el-button>
        </span>
      </template>
    </el-dialog>

    <!-- 查看消息详情对话框 -->
    <el-dialog
      v-model="showViewDialog"
      :title="currentMessage?.title || '消息详情'"
      width="600px"
    >
      <div v-if="currentMessage">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="消息标题">
            {{ currentMessage.title }}
          </el-descriptions-item>
          <el-descriptions-item label="目标对象">
            <el-tag
              :type="getTargetTypeTagType(currentMessage.targetType)"
              size="small"
            >
              {{ currentMessage.targetTypeDescription }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发布者">
            {{ currentMessage.adminName }}
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag
              :type="currentMessage.isActive ? 'success' : 'danger'"
              size="small"
            >
              {{ currentMessage.isActive ? '激活' : '停用' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="发布时间">
            {{ formatDate(currentMessage.createdAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="更新时间">
            {{ formatDate(currentMessage.updatedAt) }}
          </el-descriptions-item>
          <el-descriptions-item label="消息内容">
            <div style="white-space: pre-wrap; line-height: 1.6;">
              {{ currentMessage.content }}
            </div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="showViewDialog = false">关闭</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { apiRequest } from '../../../utils/api'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Refresh, Plus, View, Edit, Delete } from '@element-plus/icons-vue'

// 响应式数据
const loading = ref(false)
const submitting = ref(false)
const messages = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const searchParams = ref({
  title: '',
  targetType: '',
  adminName: '',
  isActive: ''
})
const showCreateDialog = ref(false)
const showEditDialog = ref(false)
const showViewDialog = ref(false)
const currentMessage = ref(null)
const messageForm = ref({
  title: '',
  content: '',
  targetType: '',
  isActive: true
})

// 计算属性
const totalPages = computed(() => {
  return Math.ceil(total.value / pageSize.value)
})

// 方法
const loadMessages = async () => {
  loading.value = true
  try {
    const response = await apiRequest('/api/admin/messages', {
      method: 'GET',
      params: {
        pageNum: currentPage.value,
        pageSize: pageSize.value,
        ...searchParams.value
      }
    })

    if (response.success) {
      messages.value = response.data.records || []
      total.value = response.data.total || 0
    } else {
      ElMessage.error(response.message || '获取消息列表失败')
    }
  } catch (error) {
    console.error('获取消息列表失败:', error)
    ElMessage.error('获取消息列表失败')
  } finally {
    loading.value = false
  }
}

const searchMessages = () => {
  currentPage.value = 1
  loadMessages()
}

const resetSearch = () => {
  searchParams.value = {
    title: '',
    targetType: '',
    adminName: '',
    isActive: ''
  }
  currentPage.value = 1
  loadMessages()
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const changePage = (page: number) => {
  if (page >= 1 && page <= totalPages.value) {
    currentPage.value = page
    loadMessages()
  }
}

const handleSizeChange = (val: number) => {
  pageSize.value = val
  currentPage.value = 1
  loadMessages()
}

const handleCurrentChange = (val: number) => {
  currentPage.value = val
  loadMessages()
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const viewMessage = (message: Record<string, any>) => {
  currentMessage.value = message
  showViewDialog.value = true
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const editMessage = (message: Record<string, any>) => {
  currentMessage.value = message
  messageForm.value = {
    title: message.title,
    content: message.content,
    targetType: message.targetType,
    isActive: message.isActive
  }
  showEditDialog.value = true
}

const submitMessage = async () => {
  submitting.value = true
  try {
    let response
    if (showCreateDialog.value) {
      response = await apiRequest('/api/admin/messages', {
        method: 'POST',
        data: {
          title: messageForm.value.title,
          content: messageForm.value.content,
          targetType: messageForm.value.targetType
        }
      })
    } else {
      response = await apiRequest(`/api/admin/messages/${currentMessage.value.id}`, {
        method: 'PUT',
        data: messageForm.value
      })
    }

    if (response.success) {
      ElMessage.success(showCreateDialog.value ? '消息发布成功' : '消息更新成功')
      closeDialogs()
      loadMessages()
    } else {
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    console.error('提交消息失败:', error)
    ElMessage.error('操作失败')
  } finally {
    submitting.value = false
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const toggleMessageStatus = async (message: Record<string, any>) => {
  // 添加加载状态
  message.statusLoading = true
  const originalStatus = message.isActive

  try {
    const response = await apiRequest(`/api/admin/messages/${message.id}/toggle-status`, {
      method: 'PUT'
    })

    if (response.success) {
      ElMessage.success(`消息已${message.isActive ? '激活' : '停用'}`)
    } else {
      // 如果失败，恢复原状态
      message.isActive = originalStatus
      ElMessage.error(response.message || '操作失败')
    }
  } catch (error) {
    // 如果失败，恢复原状态
    message.isActive = originalStatus
    console.error('切换状态失败:', error)
    ElMessage.error('操作失败')
  } finally {
    message.statusLoading = false
  }
}

// eslint-disable-next-line @typescript-eslint/no-explicit-any
const deleteMessage = async (message: Record<string, any>) => {
  try {
    await ElMessageBox.confirm('确定要删除这条消息吗？删除后无法恢复！', '确认删除', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })

    const response = await apiRequest(`/api/admin/messages/${message.id}`, {
      method: 'DELETE'
    })

    if (response.success) {
      ElMessage.success('删除成功')
      loadMessages()
    } else {
      ElMessage.error(response.message || '删除失败')
    }
  } catch (error) {
    if (error !== 'cancel') {
      console.error('删除消息失败:', error)
      ElMessage.error('删除失败')
    }
  }
}

const closeDialogs = () => {
  showCreateDialog.value = false
  showEditDialog.value = false
  currentMessage.value = null
  messageForm.value = {
    title: '',
    content: '',
    targetType: '',
    isActive: true
  }
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const getTargetTypeClass = (targetType: string) => {
  const classMap = {
    'STUDENT': 'student',
    'TEACHER': 'teacher',
    'ALL': 'all'
  }
  return classMap[targetType] || ''
}

const getTargetTypeTagType = (targetType: string) => {
  const typeMap = {
    'STUDENT': 'primary',
    'TEACHER': 'warning',
    'ALL': 'success'
  }
  return typeMap[targetType] || ''
}

const formatDate = (dateString: string) => {
  if (!dateString) return ''
  return new Date(dateString).toLocaleString('zh-CN')
}

// 挂载时加载数据
onMounted(() => {
  loadMessages()
})
</script>

<style scoped>
.message-management {
  padding: 20px;
}

.header {
  margin-bottom: 20px;
}

.header h3 {
  margin: 0;
  color: #333;
  font-size: 18px;
  font-weight: 600;
}

.search-card {
  margin-bottom: 20px;
}

.operation-buttons {
  display: flex;
  gap: 8px;
  flex-wrap: nowrap;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
  width: 100%;
  padding: 0 8px;
}

.operation-buttons .el-button {
  margin: 0;
  min-width: 60px;
  text-align: center;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: center;
}

/* 确保操作列标题居中 */
:deep(.el-table__header-wrapper .el-table__header th:last-child .cell) {
  text-align: center;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .operation-buttons {
    flex-wrap: wrap;
    gap: 4px;
    justify-content: center;
  }

  .operation-buttons .el-button {
    font-size: 12px;
    padding: 4px 8px;
    min-width: 50px;
  }
}

/* 表格横向滚动兜底 */
.table-wrap { width: 100%; overflow-x: auto; }
.table-wrap :deep(table) { min-width: 900px; }

/* 小屏对话框与表单布局 */
@media (max-width: 768px) {
  :deep(.el-dialog) { width: 100vw !important; max-width: 100vw !important; margin: 0 !important; }
  :deep(.el-dialog__body) { padding: 12px; }
  :deep(.el-dialog .el-form .el-form-item__label) { float: none; display: block; padding-bottom: 4px; }
  :deep(.el-dialog .el-form .el-form-item__content) { margin-left: 0 !important; }
  :deep(.el-card .el-form) { display: grid; grid-template-columns: 1fr 1fr; gap: 8px 12px; }
}
@media (max-width: 480px) {
  :deep(.el-card .el-form) { grid-template-columns: 1fr; }
}

</style>
