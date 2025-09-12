<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import ContactUs from '../components/ContactUs.vue'
import { useRouter } from 'vue-router'
import { subjectAPI, teacherAPI } from '../utils/api'
import { ElMessage } from 'element-plus'
import { Loading, View, Calendar, Male, Female, UserFilled } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'


// 定义组件名称
defineOptions({
  name: 'FamousTeachersView'
})

// 路由实例
const router = useRouter()

const userStore = useUserStore()

// 导入图片资源
import teacherBoy1 from '@/assets/pictures/teacherBoy1.jpeg'
import teacherBoy2 from '@/assets/pictures/teacherBoy2.jpeg'
import teacherBoy3 from '@/assets/pictures/teacherBoy3.jpeg'
import teacherGirl1 from '@/assets/pictures/teacherGirl1.jpeg'
import teacherGirl2 from '@/assets/pictures/teacherGirl2.jpeg'
import teacherGirl3 from '@/assets/pictures/teacherGirl3.jpeg'
import teacherGirl4 from '@/assets/pictures/teacherGirl4.jpeg'
import studentGirl2 from '@/assets/pictures/studentGirl2.jpeg'

// 分页参数
const currentPage = ref(1)
const pageSize = ref(6)

// 过滤条件
const filter = reactive({
  subject: '',
  experience: '', keyword: ''
})


// 科目相关数据
const availableSubjects = ref([])
const loadingSubjects = ref(false)

// 教师相关数据
const loadingTeachers = ref(false)

// 教师数据（服务端分页）
const teachers = ref([])
const total = ref(0)


// 默认头像数组，用于随机分配给教师
const defaultAvatars = [
  teacherBoy1, teacherBoy2, teacherBoy3,
  teacherGirl1, teacherGirl2, teacherGirl3, teacherGirl4,
  studentGirl2
]

// 获取随机头像
const getRandomAvatar = () => {
  return defaultAvatars[Math.floor(Math.random() * defaultAvatars.length)]
}

// 筛选教师
const filteredTeachers = computed(() => {
  return teachers.value.filter(teacher => {
    let match = true
    if (filter.subject && teacher.subject !== filter.subject) {
      match = false
    }
    if (filter.experience) {
      const exp = parseInt(teacher.experience.toString())
      if (filter.experience === '0-5' && exp >= 5) {
        match = false
      } else if (filter.experience === '5-10' && (exp < 5 || exp > 10)) {
        match = false
      } else if (filter.experience === '10+' && exp < 10) {
        match = false
      }
    }
    return match
  })
})

// 分页显示（当前页记录+本地教龄过滤）
const displayTeachers = computed(() => {
  let list = teachers.value
  // 本地经验过滤
  if (filter.experience) {
    list = list.filter((teacher: any) => {
      const exp = parseInt((teacher.experience ?? 0).toString())
      if (filter.experience === '0-5') return exp < 5
      if (filter.experience === '5-10') return exp >= 5 && exp <= 10
      if (filter.experience === '10+') return exp >= 10
      return true
    })
  }
  // 服务器已按 page/size 返回当前页，直接返回
  return list
})

// 筛选方法
const handleFilter = async () => {
  currentPage.value = 1
  await loadTeachersWithFilter()
}

// 转换教师数据格式
const transformTeacherData = (teacherList: any[]) => {
  return teacherList.map((teacher: any, index: number) => {
    // 处理科目信息 - 现在subjects是数组
    const subjects = Array.isArray(teacher.subjects) ? teacher.subjects : []
    const primarySubject = subjects.length > 0 ? subjects[0] : '未设置'

    // 处理特长标签
    const specialties = teacher.specialties ? teacher.specialties.split(',').map((s: string) => s.trim()).slice(0, 3) : []
    const defaultTags = ['专业教学', '经验丰富', '认真负责']
    const tags = specialties.length > 0 ? specialties : defaultTags.slice(0, 2)

    // 生成合理的评分
    const baseRating = 4.5
    const experienceBonus = Math.min(teacher.teachingExperience * 0.02, 0.4) // 经验越多评分越高
    const randomFactor = Math.random() * 0.1
    const rating = Math.min(baseRating + experienceBonus + randomFactor, 5.0)

    // 使用用户头像或根据性别选择合适的默认头像
    let avatar = teacher.avatarUrl || defaultAvatars[index % defaultAvatars.length]
    if (!teacher.avatarUrl) {
      if (teacher.gender === 'Male') {
        const maleAvatars = [teacherBoy1, teacherBoy2, teacherBoy3]
        avatar = maleAvatars[index % maleAvatars.length]
      } else if (teacher.gender === 'Female') {
        const femaleAvatars = [teacherGirl1, teacherGirl2, teacherGirl3, teacherGirl4, studentGirl2]
        avatar = femaleAvatars[index % femaleAvatars.length]
      }
    }

    return {
      id: teacher.id,
      name: teacher.realName,
      subject: primarySubject,
      subjects: subjects,
      experience: teacher.teachingExperience || 0,
      rating: Math.round(rating * 10) / 10, // 保留一位小数
      description: teacher.introduction || `${teacher.realName}是一位优秀的${primarySubject}教师，教学经验丰富，深受学生喜爱。`,
      avatar: avatar,
      tags: tags,
      hourlyRate: teacher.hourlyRate || 0,
      education: teacher.educationBackground || '暂无信息',
      specialties: teacher.specialties || '',
      isVerified: teacher.isVerified || false,
      level: teacher.level || '未设置',
      gender: ((teacher.gender === 'Male' || teacher.gender === '男') ? '男' : ((teacher.gender === 'Female' || teacher.gender === '女') ? '女' : '不愿透露'))
    }
  })
}

// 根据筛选条件加载教师数据（服务端分页）
const loadTeachersWithFilter = async () => {
  try {
    loadingTeachers.value = true
    const params: any = {
      page: currentPage.value,
      size: pageSize.value, realName: filter.keyword ? filter.keyword.trim() : undefined
    }

    if (filter.subject) {
      params.subject = filter.subject
    }

    const response = await teacherAPI.getPublicList(params)
    if (response.success && response.data) {
      const records = Array.isArray(response.data.records) ? response.data.records : []
      teachers.value = transformTeacherData(records)
      total.value = Number(response.data.total || 0)
    }
  } catch (error) {
    console.error('获取教师列表失败:', error)
    ElMessage.error('获取教师列表失败')
  } finally {
    loadingTeachers.value = false
  }
}

// 重置筛选
const resetFilter = async () => {
  filter.subject = ''
  filter.experience = ''; filter.keyword = ''
  currentPage.value = 1
  await loadTeachers() // 重新加载所有教师数据
}

// 刷新数据
const refreshData = async () => {
  await Promise.all([
    loadSubjects(),
    loadTeachers()
  ])
  ElMessage.success('数据刷新成功')
}

// 查看教师详情
const viewTeacherDetail = (teacherId: number) => {
  router.push(`/teacher-detail/${teacherId}`)
}

// 立即预约（未登录先去登录，已登录则跳转到详情并自动触发预约流程）
const bookTeacherNow = (teacherId: number) => {
  if (!userStore.isLoggedIn) {
    ElMessage.warning('请先登录后再预约')
    router.push('/login')
    return
  }
  router.push({ path: `/teacher-detail/${teacherId}`, query: { autoBooking: '1' } })
}


// 获取科目列表
const loadSubjects = async () => {
  try {
    loadingSubjects.value = true
    const response = await subjectAPI.getActiveSubjects()
    if (response.success && response.data) {
      availableSubjects.value = response.data
    }
  } catch (error) {
    console.error('获取科目列表失败:', error)
    ElMessage.error('获取科目列表失败')
  } finally {
    loadingSubjects.value = false
  }
}

// 获取教师列表（服务端分页）
const loadTeachers = async () => {
  try {
    loadingTeachers.value = true
    const requiredSize = Math.max(pageSize.value, currentPage.value * pageSize.value)
    const response = await teacherAPI.getPublicList({
      page: currentPage.value,
      size: pageSize.value,
      subject: filter.subject || undefined, realName: filter.keyword ? filter.keyword.trim() : undefined,
    })
    if (response.success && response.data) {
      const records = Array.isArray(response.data.records) ? response.data.records : []
      teachers.value = transformTeacherData(records)
      total.value = Number(response.data.total || 0)
    }
  } catch (error) {
    console.error('获取教师列表失败:', error)
    ElMessage.error('获取教师列表失败')
  } finally {
    loadingTeachers.value = false
  }
}

// 分页变化事件（服务端分页）
const handlePageChange = () => {
  if (filter.subject) {
    loadTeachersWithFilter()
  } else {
    loadTeachers()
  }
}

// 页面大小变化事件
const handleSizeChange = () => {
  currentPage.value = 1
  handlePageChange()
}

// 组件挂载时加载数据
onMounted(() => {
  loadSubjects()
  loadTeachers()
})
</script>

<template>
  <div class="famous-teachers">
    <div class="banner">
      <div class="banner-content">
        <h1>教师推荐</h1>
        <p>专业优质的教师团队，助您学习成长</p>
      </div>
    </div>

    <div class="container">
      <!-- 教师筛选 -->
      <div class="filter-section">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="科目">
            <el-select v-model="filter.subject" placeholder="选择科目" clearable :loading="loadingSubjects">
              <el-option
                v-for="subject in availableSubjects"
                :key="subject.id"
                :label="subject.name"
                :value="subject.name"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="姓名">
            <el-input v-model="filter.keyword" placeholder="输入姓名（严格匹配）" clearable />
          </el-form-item>

          <el-form-item label="教龄">
            <el-select v-model="filter.experience" placeholder="教学经验" clearable>
              <el-option label="5年以下" value="0-5" />
              <el-option label="5-10年" value="5-10" />
              <el-option label="10年以上" value="10+" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleFilter">筛选</el-button>
            <el-button @click="resetFilter">重置</el-button>
            <el-button @click="refreshData" :loading="loadingTeachers || loadingSubjects">刷新</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 教师列表 -->
      <div class="teachers-section">
        <div v-if="loadingTeachers" class="loading-container">
          <el-icon class="loading-icon"><Loading /></el-icon>
          <p>正在加载教师信息...</p>
        </div>
        <div v-else-if="displayTeachers.length === 0" class="empty-container">
          <p>暂无符合条件的教师</p>
        </div>
        <div v-else class="teachers-grid">
          <div class="teacher-card" v-for="teacher in displayTeachers" :key="teacher.id">
            <div class="teacher-avatar">
              <img :src="teacher.avatar" :alt="teacher.name">
              <div class="teacher-rating">
                <el-rate v-model="teacher.rating" disabled text-color="#ff9900"></el-rate>
              </div>
            </div>
            <div class="teacher-info">
              <h3>{{ teacher.name }}</h3>
              <div class="teacher-tags">
                <el-tag v-for="(s, i) in teacher.subjects.slice(0, 3)" :key="i" size="small" class="subject-tag">{{ s }}</el-tag>
                <el-tag v-if="teacher.subjects.length > 3" size="small" class="subject-tag" type="info">+{{ teacher.subjects.length - 3 }}</el-tag>
              </div>
              <p>{{ teacher.experience }}年教龄 · {{ teacher.level || '未设置' }}</p>
              <div class="teacher-tags">
                <el-tag v-if="teacher.gender === '男'" size="small" class="teacher-tag" type="primary">
                  <el-icon style="margin-right: 4px;"><Male /></el-icon>男
                </el-tag>
                <el-tag v-else-if="teacher.gender === '女'" size="small" class="teacher-tag" type="danger">
                  <el-icon style="margin-right: 4px;"><Female /></el-icon>女
                </el-tag>
                <el-tag v-else size="small" class="teacher-tag" type="info">
                  <el-icon style="margin-right: 4px;"><UserFilled /></el-icon>不愿透露
                </el-tag>
                <el-tag v-for="(tag, i) in teacher.tags" :key="i" size="small" class="teacher-tag">{{ tag }}</el-tag>
              </div>
              <div class="teacher-actions">
                <el-button type="primary" @click="bookTeacherNow(teacher.id)" size="large">
                  <el-icon><Calendar /></el-icon> 立即预约
                </el-button>
                <el-button type="primary" @click="viewTeacherDetail(teacher.id)" size="large">
                  <el-icon><View /></el-icon> 查看详情
                </el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页（服务端） -->
        <div class="pagination">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[6, 12, 24]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @current-change="(p)=>{ currentPage = p; handlePageChange() }"
            @size-change="(s)=>{ pageSize = s; handleSizeChange() }"
          />
        </div>
      </div>
    </div>
      <!-- 联系我们 -->
      <ContactUs />
    </div>
</template>

<style scoped>
.famous-teachers {
  width: 100%;
}

.banner {
  height: 300px;
  background-image: url('@/assets/pictures/teacherBackground.jpeg');
  background-size: cover;
  background-position: center;
  display: flex;
  align-items: center;
  justify-content: center;
  color: white;
  text-align: center;
  position: relative;
}

.banner::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.6);
}

.banner-content {
  position: relative;
  z-index: 10;
}

.banner h1 {
  font-size: 48px;
  margin-bottom: 15px;
}

.banner p {
  font-size: 20px;
}

.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 40px 20px;
}

.filter-form .el-select {
  min-width: 140px;
  width: 180px;
}

.filter-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
}

.teachers-section {
  margin-bottom: 40px;
}

.teachers-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30px;
  margin-bottom: 30px;
}

.teacher-card {
  display: flex;
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.teacher-card:hover {
  transform: translateY(-5px);
}

.teacher-avatar {
  width: 200px;
  flex-shrink: 0;
  position: relative;
}

.teacher-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.teacher-rating {
  position: absolute;
  bottom: 10px;
  left: 10px;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 20px;
  padding: 4px 10px;
}

.teacher-info {
  flex: 1;
  padding: 20px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.teacher-info h3 {
  font-size: 20px;
  margin-bottom: 10px;
  color: #333;
}

.teacher-info > p {
  color: #666;
  margin-bottom: 10px;
}

.teacher-description {
  color: #666;
  line-height: 1.6;
  margin-bottom: 15px;
  flex: 1;
  min-height: 60px;
}

.teacher-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
}

.teacher-tag {
  font-size: 12px;
}



.teacher-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
  padding-top: 15px;
  border-top: 1px solid #f0f0f0;
}

.teacher-actions .el-button {
  flex: 1;
  font-weight: 600;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #666;
}

.loading-icon {
  font-size: 48px;
  color: #409eff;
  animation: rotate 2s linear infinite;
  margin-bottom: 20px;
}

@keyframes rotate {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}

.empty-container {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  color: #999;
  font-size: 16px;
}

@media (max-width: 1200px) {
  .teachers-grid {
    grid-template-columns: repeat(1, 1fr);
  }
}

@media (max-width: 768px) {
  .famous-teachers {
    padding-top: 56px; /* 适配移动端导航高度 */
  }

  .banner {
    height: 200px;
    padding: 0 20px;
  }

  .banner h1 {
    font-size: 32px;
    margin-bottom: 10px;
  }

  .banner p {
    font-size: 16px;
  }

  .container {
    padding: 30px 16px;
  }

  .filter-section {
    margin-bottom: 30px;
  }

  .filter-form {
    flex-direction: column;
    align-items: stretch;
    gap: 16px;
  }

  .filter-form .el-form-item {
    margin-right: 0;
    margin-bottom: 0;
    width: 100%;
  }

  .filter-actions {
    margin-top: 16px;
    display: flex;
    gap: 12px;
  }

  .filter-actions .el-button {
    flex: 1;
  }

  .teachers-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .teacher-card {
    flex-direction: column;
    padding: 16px;
  }

  .teacher-avatar {
    width: 100%;
    height: 200px;
    margin-bottom: 16px;
  }

  .teacher-info {
    padding: 0;
  }

  .teacher-info h3 {
    font-size: 18px;
    margin-bottom: 8px;
  }

  .teacher-info p {
    font-size: 14px;
    margin-bottom: 12px;
  }

  .teacher-description {
    font-size: 13px !important;
    line-height: 1.5;
    margin-bottom: 12px !important;
  }

  .teacher-tags {
    margin-bottom: 12px;
  }

  .teacher-tag {
    font-size: 12px;
    margin-right: 6px;
    margin-bottom: 6px;
  }


  .teacher-actions {
    flex-direction: column;
    gap: 8px;
    margin-top: 12px;
    padding-top: 12px;
  }

  .teacher-actions .el-button {
    width: 100%;
    font-size: 14px;
  }

  .pagination {
    margin-top: 30px;
  }

  .el-pagination {
    justify-content: center;
  }

  .el-pagination .el-pager li {
    min-width: 32px;
    height: 32px;
    line-height: 32px;
  }
}

@media (max-width: 480px) {
  .famous-teachers {
    padding-top: 52px;
  }

  .banner {
    height: 180px;
    padding: 0 16px;
  }

  .banner h1 {
    font-size: 28px;
  }

  .banner p {
    font-size: 14px;
  }

  .container {
    padding: 24px 12px;
  }

  .teacher-card {
    padding: 12px;
  }

  .teacher-avatar {
    height: 180px;
    margin-bottom: 12px;
  }

  .teacher-info h3 {
    font-size: 16px;
  }

  .teacher-info p {
    font-size: 13px;
  }

  .teacher-description {
    font-size: 12px !important;
  }

  .filter-actions {
    flex-direction: column;
  }

  .filter-actions .el-button {
    width: 100%;
    margin-bottom: 8px;
  }

  .el-pagination {
    flex-wrap: wrap;
  }

  .el-pagination .el-pagination__sizes,
  .el-pagination .el-pagination__jump {
    order: 3;
    margin-top: 12px;
  }
}
</style>
