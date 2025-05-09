<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import math1 from '@/assets/pictures/math1.jpeg'
import english1 from '@/assets/pictures/english1.jpeg'
import physics1 from '@/assets/pictures/physics1.jpeg'

interface Material {
  id: number;
  title: string;
  type: string;
  subject: string;
  grade: string;
  size: string;
  uploadDate: string;
  downloads: number;
  path: string;
  preview?: string;
}

// 模拟教学资料数据
const materials = ref<Material[]>([
  {
    id: 1,
    title: '高中数学函数讲义',
    type: 'PDF',
    subject: 'Mathematics',
    grade: 'Secondary 3',
    size: '2.5MB',
    uploadDate: '2023-05-10',
    downloads: 56,
    path: '/materials/math-functions.pdf',
    preview: math1
  },
  {
    id: 2,
    title: '英语语法练习题',
    type: 'DOCX',
    subject: 'English',
    grade: 'Primary 6',
    size: '1.8MB',
    uploadDate: '2023-06-05',
    downloads: 32,
    path: '/materials/english-grammar.docx',
    preview: english1
  },
  {
    id: 3,
    title: '物理力学实验演示',
    type: 'PPT',
    subject: 'Science',
    grade: 'Secondary 2',
    size: '5.2MB',
    uploadDate: '2023-06-15',
    downloads: 28,
    path: '/materials/physics-mechanics.pptx',
    preview: physics1
  },
  {
    id: 4,
    title: '数学思维训练题集',
    type: 'PDF',
    subject: 'Mathematics',
    grade: 'Primary 6',
    size: '3.4MB',
    uploadDate: '2023-06-20',
    downloads: 45,
    path: '/materials/math-thinking.pdf',
    preview: math1
  }
])

// 搜索条件
const searchForm = reactive({
  keyword: '',
  subject: '',
  grade: '',
  type: ''
})

// 筛选资料列表
const filteredMaterials = computed(() => {
  return materials.value.filter(material => {
    const keywordMatch = material.title.toLowerCase().includes(searchForm.keyword.toLowerCase());
    const subjectMatch = !searchForm.subject || material.subject === searchForm.subject;
    const gradeMatch = !searchForm.grade || material.grade === searchForm.grade;
    const typeMatch = !searchForm.type || material.type === searchForm.type;

    return keywordMatch && subjectMatch && gradeMatch && typeMatch;
  });
})

// 上传资料相关
const uploadDialogVisible = ref(false)
const uploadForm = reactive({
  title: '',
  subject: '',
  grade: '',
  type: '',
  file: null
})

const showUploadDialog = () => {
  uploadForm.title = ''
  uploadForm.subject = ''
  uploadForm.grade = ''
  uploadForm.type = ''
  uploadForm.file = null
  uploadDialogVisible.value = true
}

const uploadMaterial = () => {
  ElMessage.success('资料上传成功')
  uploadDialogVisible.value = false
}

// 删除资料
const deleteMaterial = (id: number) => {
  ElMessageBox.confirm('确定要删除这个资料吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    const index = materials.value.findIndex(m => m.id === id)
    if (index !== -1) {
      materials.value.splice(index, 1)
      ElMessage.success('删除成功')
    }
  }).catch(() => {
    // 取消删除
  })
}

// 下载资料
const downloadMaterial = (material: Material) => {
  ElMessage.success(`开始下载: ${material.title}`)
  // 在真实环境中，这里应该是实际的下载逻辑
  material.downloads++
}
</script>

<script lang="ts">
export default {
  name: 'TeacherMaterials'
}
</script>

<template>
  <div class="teacher-materials">
    <h2>教学资料管理</h2>

    <!-- 工具栏 -->
    <div class="toolbar">
      <div class="search-area">
        <el-form :inline="true" :model="searchForm">
          <el-form-item>
            <el-input
              v-model="searchForm.keyword"
              placeholder="搜索资料名称"
              clearable
              prefix-icon="Search"
            />
          </el-form-item>
          <el-form-item>
            <el-select v-model="searchForm.subject" placeholder="科目" clearable>
              <el-option label="Mathematics" value="Mathematics" />
              <el-option label="English" value="English" />
              <el-option label="Science" value="Science" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="searchForm.grade" placeholder="年级" clearable>
              <el-option label="Primary 6" value="Primary 6" />
              <el-option label="Secondary 2" value="Secondary 2" />
              <el-option label="Secondary 3" value="Secondary 3" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-select v-model="searchForm.type" placeholder="类型" clearable>
              <el-option label="PDF" value="PDF" />
              <el-option label="DOCX" value="DOCX" />
              <el-option label="PPT" value="PPT" />
            </el-select>
          </el-form-item>
        </el-form>
      </div>
      <div class="action-area">
        <el-button type="primary" @click="showUploadDialog">
          <el-icon><Upload /></el-icon>
          上传资料
        </el-button>
      </div>
    </div>

    <!-- 资料列表 -->
    <div class="materials-container">
      <el-empty v-if="filteredMaterials.length === 0" description="暂无教学资料" />

      <div v-else class="materials-grid">
        <el-card v-for="material in filteredMaterials" :key="material.id" class="material-card">
          <div class="material-preview" v-if="material.preview">
            <img :src="$getImageUrl(material.preview)" :alt="material.title">
          </div>
          <div class="material-preview" v-else>
            <div class="file-icon">
              <el-icon v-if="material.type === 'PDF'"><Document /></el-icon>
              <el-icon v-else-if="material.type === 'DOCX'"><Document /></el-icon>
              <el-icon v-else-if="material.type === 'PPT'"><PictureFilled /></el-icon>
              <el-icon v-else><Files /></el-icon>
            </div>
          </div>
          <div class="material-info">
            <h3 class="material-title">{{ material.title }}</h3>
            <div class="material-meta">
              <span class="material-type">{{ material.type }}</span>
              <span class="material-size">{{ material.size }}</span>
            </div>
            <div class="material-details">
              <div class="detail-item">
                <el-icon><User /></el-icon>
                <span>{{ material.grade }}</span>
              </div>
              <div class="detail-item">
                <el-icon><Reading /></el-icon>
                <span>{{ material.subject }}</span>
              </div>
              <div class="detail-item">
                <el-icon><Calendar /></el-icon>
                <span>{{ material.uploadDate }}</span>
              </div>
              <div class="detail-item">
                <el-icon><Download /></el-icon>
                <span>{{ material.downloads }} 次下载</span>
              </div>
            </div>
            <div class="material-actions">
              <el-button type="primary" size="small" @click="downloadMaterial(material)">
                <el-icon><Download /></el-icon>
                下载
              </el-button>
              <el-button type="danger" size="small" @click="deleteMaterial(material.id)">
                <el-icon><Delete /></el-icon>
                删除
              </el-button>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- 上传对话框 -->
    <el-dialog
      v-model="uploadDialogVisible"
      title="上传教学资料"
      width="40%"
    >
      <el-form :model="uploadForm" label-width="80px">
        <el-form-item label="资料名称" required>
          <el-input v-model="uploadForm.title" placeholder="请输入资料名称"></el-input>
        </el-form-item>
        <el-form-item label="科目" required>
          <el-select v-model="uploadForm.subject" placeholder="请选择科目" style="width: 100%">
            <el-option label="Mathematics" value="Mathematics"></el-option>
            <el-option label="English" value="English"></el-option>
            <el-option label="Science" value="Science"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="年级" required>
          <el-select v-model="uploadForm.grade" placeholder="请选择年级" style="width: 100%">
            <el-option label="Primary 6" value="Primary 6"></el-option>
            <el-option label="Secondary 2" value="Secondary 2"></el-option>
            <el-option label="Secondary 3" value="Secondary 3"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="资料类型" required>
          <el-select v-model="uploadForm.type" placeholder="请选择资料类型" style="width: 100%">
            <el-option label="PDF" value="PDF"></el-option>
            <el-option label="DOCX" value="DOCX"></el-option>
            <el-option label="PPT" value="PPT"></el-option>
          </el-select>
        </el-form-item>
        <el-form-item label="上传文件" required>
          <el-upload
            action="#"
            :auto-upload="false"
            :limit="1"
            accept=".pdf,.doc,.docx,.ppt,.pptx"
            drag
          >
            <el-icon class="el-icon--upload"><upload-filled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处或 <em>点击上传</em></div>
            <template #tip>
              <div class="el-upload__tip">
                支持PDF、Word、PowerPoint文件，大小不超过20MB
              </div>
            </template>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="uploadDialogVisible = false">取消</el-button>
          <el-button type="primary" @click="uploadMaterial">上传</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.teacher-materials {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  background-color: #fff;
  padding: 15px;
  border-radius: 4px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.search-area {
  flex: 1;
}

.materials-container {
  margin-top: 20px;
}

.materials-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 20px;
}

.material-card {
  transition: transform 0.3s;
  display: flex;
  flex-direction: column;
  height: 100%;
}

.material-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 5px 15px rgba(0, 0, 0, 0.1);
}

.material-preview {
  height: 150px;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f5f7fa;
  margin-bottom: 15px;
}

.material-preview img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.file-icon {
  font-size: 50px;
  color: #909399;
}

.material-info {
  padding: 0 10px;
  flex: 1;
  display: flex;
  flex-direction: column;
}

.material-title {
  margin: 0 0 10px;
  font-size: 16px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.material-meta {
  margin-bottom: 10px;
  display: flex;
  align-items: center;
}

.material-type {
  background-color: #ecf5ff;
  color: #409eff;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  margin-right: 10px;
}

.material-size {
  color: #909399;
  font-size: 12px;
}

.material-details {
  margin-bottom: 15px;
  flex: 1;
}

.detail-item {
  display: flex;
  align-items: center;
  margin-bottom: 5px;
  color: #606266;
  font-size: 13px;
}

.detail-item .el-icon {
  margin-right: 5px;
  font-size: 14px;
}

.material-actions {
  display: flex;
  justify-content: space-between;
  margin-top: auto;
}

.el-upload__tip {
  font-size: 12px;
  color: #909399;
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .action-area {
    margin-top: 15px;
    display: flex;
    justify-content: flex-end;
  }

  .materials-grid {
    grid-template-columns: 1fr;
  }
}
</style>
