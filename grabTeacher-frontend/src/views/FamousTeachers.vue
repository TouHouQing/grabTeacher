<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter } from 'vue-router'

// 定义组件名称
defineOptions({
  name: 'FamousTeachersView'
})

// 路由实例
const router = useRouter()

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
  grade: '',
  experience: ''
})

// 教师数据
const teachers = ref([
  {
    id: 1, // 添加唯一ID
    name: '张老师',
    subject: '数学',
    grade: '初中',
    experience: 10,
    rating: 4.8,
    description: '数学教育专家，专注于中小学数学教学，善于激发学生学习兴趣，使用多种教学方法帮助学生理解数学概念。',
    avatar: teacherBoy1,
    tags: ['趣味教学', '重点突破', '思维导图'],
    schedule: ['周一 18:00-20:00', '周三 18:00-20:00', '周六 10:00-12:00']
  },
  {
    id: 2, // 添加唯一ID
    name: '李老师',
    subject: '英语',
    grade: '初中',
    experience: 8,
    rating: 4.9,
    description: '毕业于英国剑桥大学，拥有TESOL证书，擅长英语口语教学，注重学生的语言应用能力培养。',
    avatar: teacherBoy2,
    tags: ['发音纠正', '口语强化', '语法精通'],
    schedule: ['周二 18:00-20:00', '周四 18:00-20:00', '周日 14:00-16:00']
  },
  {
    id: 3, // 添加唯一ID
    name: '王老师',
    subject: '物理',
    grade: '初中',
    experience: 12,
    rating: 4.7,
    description: '物理学博士，有丰富的教学经验，能将复杂概念简单化，善于通过实验和演示帮助学生理解物理原理。',
    avatar: teacherBoy3,
    tags: ['概念解析', '解题技巧', '高考冲刺'],
    schedule: ['周一 16:00-18:00', '周三 16:00-18:00', '周六 14:00-16:00']
  },
  {
    id: 4, // 添加唯一ID
    name: '刘老师',
    subject: '化学',
    grade: '初中',
    experience: 15,
    rating: 4.9,
    description: '化学教育硕士，从事一线教学工作15年，教学方法灵活多样，注重培养学生的实验能力和科学思维。',
    avatar: teacherGirl1,
    tags: ['实验教学', '概念讲解', '解题方法'],
    schedule: ['周二 16:00-18:00', '周五 18:00-20:00', '周日 10:00-12:00']
  },
  {
    id: 5, // 添加唯一ID
    name: '陈老师',
    subject: '数学',
    grade: '初中',
    experience: 7,
    rating: 4.6,
    description: '数学教育专业毕业，擅长启发式教学，能够根据学生的特点制定个性化的学习计划。',
    avatar: teacherGirl2,
    tags: ['基础夯实', '思维训练', '难题攻克'],
    schedule: ['周一 15:00-17:00', '周四 16:00-18:00', '周六 16:00-18:00']
  },
  {
    id: 6, // 添加唯一ID
    name: '赵老师',
    subject: '生物',
    grade: '初中',
    experience: 9,
    rating: 4.8,
    description: '生物学硕士，有丰富的教学经验，擅长将生物学知识与日常生活相结合，让学习更加生动有趣。',
    avatar: teacherGirl3,
    tags: ['实验演示', '概念讲解', '考点梳理'],
    schedule: ['周二 19:00-21:00', '周五 16:00-18:00', '周日 16:00-18:00']
  },
  {
    id: 7, // 添加唯一ID
    name: '杨老师',
    subject: '英语',
    grade: '小学',
    experience: 5,
    rating: 4.9,
    description: '英语专业毕业，有海外留学经验，擅长通过游戏、歌曲等形式激发孩子学习英语的兴趣。',
    avatar: teacherGirl4,
    tags: ['趣味教学', '语音纠正', '词汇积累'],
    schedule: ['周三 15:00-17:00', '周五 15:00-17:00', '周六 10:00-12:00']
  },
  {
    id: 8, // 添加唯一ID
    name: '周老师',
    subject: '物理',
    grade: '初中',
    experience: 8,
    rating: 4.7,
    description: '物理教育专业毕业，擅长实验教学，能够通过实验激发学生的学习兴趣，培养学生的动手能力。',
    avatar: studentGirl2,
    tags: ['实验教学', '概念讲解', '题型分析'],
    schedule: ['周一 17:00-19:00', '周四 17:00-19:00', '周日 14:00-16:00']
  }
])

// 筛选教师
const filteredTeachers = computed(() => {
  return teachers.value.filter(teacher => {
    let match = true
    if (filter.subject && teacher.subject !== filter.subject) {
      match = false
    }
    if (filter.grade && teacher.grade !== filter.grade) {
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

// 分页显示
const displayTeachers = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredTeachers.value.slice(start, end)
})

// 筛选方法
const handleFilter = () => {
  currentPage.value = 1
}

// 重置筛选
const resetFilter = () => {
  filter.subject = ''
  filter.grade = ''
  filter.experience = ''
  currentPage.value = 1
}

// 查看教师详情
const viewTeacherDetail = (teacherId: number) => {
  router.push(`/student/teacher-detail/${teacherId}`)
}
</script>

<template>
  <div class="famous-teachers">
    <div class="banner">
      <div class="banner-content">
        <h1>天下名师</h1>
        <p>专业优质的教师团队，助您学习成长</p>
      </div>
    </div>

    <div class="container">
      <!-- 教师筛选 -->
      <div class="filter-section">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="科目">
            <el-select v-model="filter.subject" placeholder="选择科目" clearable>
              <el-option label="数学" value="数学" />
              <el-option label="英语" value="英语" />
              <el-option label="物理" value="物理" />
              <el-option label="化学" value="化学" />
              <el-option label="生物" value="生物" />
            </el-select>
          </el-form-item>
          <el-form-item label="年级">
            <el-select v-model="filter.grade" placeholder="选择年级" clearable>
              <el-option label="小学" value="小学" />
              <el-option label="初中" value="初中" />
            </el-select>
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
          </el-form-item>
        </el-form>
      </div>

      <!-- 教师列表 -->
      <div class="teachers-section">
        <div class="teachers-grid">
          <div class="teacher-card" v-for="(teacher, index) in displayTeachers" :key="index">
            <div class="teacher-avatar">
              <img :src="teacher.avatar" :alt="teacher.name">
              <div class="teacher-rating">
                <el-rate v-model="teacher.rating" disabled text-color="#ff9900"></el-rate>
              </div>
            </div>
            <div class="teacher-info">
              <h3>{{ teacher.name }}</h3>
              <p>{{ teacher.subject }} | {{ teacher.grade }} | {{ teacher.experience }}年教龄</p>
              <p class="teacher-description">{{ teacher.description }}</p>
              <div class="teacher-tags">
                <el-tag v-for="(tag, i) in teacher.tags" :key="i" size="small" class="teacher-tag">{{ tag }}</el-tag>
              </div>
              <div class="teacher-schedule">
                <div class="schedule-title">可授课时间：</div>
                <div class="schedule-times">
                  <span v-for="(time, i) in teacher.schedule" :key="i" class="schedule-tag">{{ time }}</span>
                </div>
              </div>
              <div class="teacher-actions">
                <el-button type="primary" size="small">预约课程</el-button>
                <el-button type="info" size="small" @click="viewTeacherDetail(teacher.id)">查看详情</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[6, 12, 24]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="filteredTeachers.length"
          />
        </div>
      </div>
    </div>
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

.teacher-schedule {
  margin-bottom: 15px;
}

.schedule-title {
  font-weight: bold;
  margin-bottom: 8px;
  color: #333;
}

.schedule-times {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.schedule-tag {
  font-size: 12px;
  color: #409EFF;
  background-color: #ecf5ff;
  padding: 4px 8px;
  border-radius: 4px;
}

.teacher-actions {
  display: flex;
  gap: 10px;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 40px;
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

  .teacher-schedule {
    margin-bottom: 16px;
  }

  .schedule-title {
    font-size: 13px;
    margin-bottom: 8px;
  }

  .schedule-times {
    flex-direction: column;
    gap: 6px;
  }

  .schedule-tag {
    font-size: 12px;
    padding: 4px 8px;
    margin-right: 0;
    margin-bottom: 0;
  }

  .teacher-actions {
    flex-direction: column;
    gap: 8px;
  }

  .teacher-actions .el-button {
    width: 100%;
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
