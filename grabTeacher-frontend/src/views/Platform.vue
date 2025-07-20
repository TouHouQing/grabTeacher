<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

// 组件名称
const name = 'PlatformView'
defineOptions({ name })

// 职位类型
const positions = ref([
  { label: '数学教师', value: '数学教师' },
  { label: '英语教师', value: '英语教师' },
  { label: '物理教师', value: '物理教师' },
  { label: '化学教师', value: '化学教师' },
  { label: '生物教师', value: '生物教师' },
  { label: '历史教师', value: '历史教师' },
  { label: '地理教师', value: '地理教师' },
  { label: '政治教师', value: '政治教师' }
])

// 招聘表单
const applicationForm = reactive({
  name: '',
  gender: '',
  age: undefined,
  phone: '',
  email: '',
  position: '',
  experience: '',
  education: '',
  introduction: '',
  resume: null
})

// 提交表单
const submitLoading = ref(false)
const submitForm = () => {
  submitLoading.value = true
  // 模拟提交
  setTimeout(() => {
    ElMessage.success('申请提交成功，我们将尽快与您联系!')
    resetForm()
    submitLoading.value = false
  }, 1500)
}

// 重置表单
const resetForm = () => {
  applicationForm.name = ''
  applicationForm.gender = ''
  applicationForm.age = undefined
  applicationForm.phone = ''
  applicationForm.email = ''
  applicationForm.position = ''
  applicationForm.experience = ''
  applicationForm.education = ''
  applicationForm.introduction = ''
  applicationForm.resume = null
}
</script>

<template>
  <div class="platform-page">
    <div class="banner">
      <div class="banner-content">
        <h1>教师招聘</h1>
        <p>加入GrabTeacher，成为优秀教育者的一员</p>
      </div>
    </div>

    <div class="container">
      <div class="section intro">
        <h2>关于我们的教师平台</h2>
        <div class="intro-content">
          <div class="intro-text">
            <p>GrabTeacher教师平台提供灵活的工作机会和丰厚的薪酬待遇，让您能够充分发挥自己的教学才能，同时平衡个人生活和职业发展。</p>
            <p>我们寻找具有专业知识和教学热情的优秀教师，通过我们的平台，您可以接触到更多学生，提升您的教学影响力。</p>
            <div class="advantages">
              <div class="advantage-item">
                <el-icon><Money /></el-icon>
                <h3>丰厚薪酬</h3>
                <p>根据您的资质和评价，获得具有竞争力的课时费</p>
              </div>
              <div class="advantage-item">
                <el-icon><Timer /></el-icon>
                <h3>灵活时间</h3>
                <p>自主安排授课时间，兼顾个人生活与工作</p>
              </div>
              <div class="advantage-item">
                <el-icon><StarFilled /></el-icon>
                <h3>职业成长</h3>
                <p>提供专业培训和发展机会，提升教学技能</p>
              </div>
              <div class="advantage-item">
                <el-icon><User /></el-icon>
                <h3>优质学生</h3>
                <p>接触到真正需要指导的学生，提高教学满足感</p>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div class="section requirements">
        <h2>教师要求</h2>
        <div class="requirements-list">
          <div class="requirement-item">
            <div class="requirement-icon">
              <el-icon><Document /></el-icon>
            </div>
            <div class="requirement-content">
              <h3>学历要求</h3>
              <p>本科及以上学历，相关专业毕业，有教师资格证者优先</p>
            </div>
          </div>
          <div class="requirement-item">
            <div class="requirement-icon">
              <el-icon><Bell /></el-icon>
            </div>
            <div class="requirement-content">
              <h3>教学经验</h3>
              <p>至少1年相关教学经验，有辅导班或培训机构工作经验者优先</p>
            </div>
          </div>
          <div class="requirement-item">
            <div class="requirement-icon">
              <el-icon><Connection /></el-icon>
            </div>
            <div class="requirement-content">
              <h3>沟通能力</h3>
              <p>良好的沟通表达能力，能够清晰地讲解知识点，有耐心</p>
            </div>
          </div>
          <div class="requirement-item">
            <div class="requirement-icon">
              <el-icon><Cpu /></el-icon>
            </div>
            <div class="requirement-content">
              <h3>技术要求</h3>
              <p>能够熟练使用电脑和网络工具，适应在线教学环境</p>
            </div>
          </div>
        </div>
      </div>

      <div class="section positions">
        <h2>招聘职位</h2>
        <el-row :gutter="20">
          <el-col :xs="24" :sm="12" :md="8" v-for="(position, index) in positions" :key="index">
            <div class="position-card">
              <h3>{{ position.label }}</h3>
              <div class="position-tags">
                <el-tag size="small">全职/兼职</el-tag>
                <el-tag size="small" type="success">远程授课</el-tag>
              </div>
              <p>我们正在寻找优秀的{{ position.label }}加入我们的团队，为学生提供高质量的教学服务。</p>
              <el-button type="primary" size="small" @click="applicationForm.position = position.value">立即申请</el-button>
            </div>
          </el-col>
        </el-row>
      </div>

      <div class="section application">
        <h2>申请教师</h2>
        <div class="application-form">
          <el-form :model="applicationForm" label-position="top">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="姓名" required>
                  <el-input v-model="applicationForm.name" placeholder="请输入您的姓名" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="性别">
                  <el-select v-model="applicationForm.gender" placeholder="请选择" style="width: 100%">
                    <el-option label="男" value="男" />
                    <el-option label="女" value="女" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="年龄">
                  <el-input-number v-model="applicationForm.age" :min="18" :max="65" style="width: 100%" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号码" required>
                  <el-input v-model="applicationForm.phone" placeholder="请输入您的手机号码" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="电子邮箱" required>
                  <el-input v-model="applicationForm.email" placeholder="请输入您的电子邮箱" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="应聘职位" required>
                  <el-select v-model="applicationForm.position" placeholder="请选择应聘职位" style="width: 100%">
                    <el-option
                      v-for="item in positions"
                      :key="item.value"
                      :label="item.label"
                      :value="item.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="教学经验">
                  <el-select v-model="applicationForm.experience" placeholder="请选择教学经验" style="width: 100%">
                    <el-option label="1年以下" value="1年以下" />
                    <el-option label="1-3年" value="1-3年" />
                    <el-option label="3-5年" value="3-5年" />
                    <el-option label="5-10年" value="5-10年" />
                    <el-option label="10年以上" value="10年以上" />
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="最高学历">
                  <el-select v-model="applicationForm.education" placeholder="请选择最高学历" style="width: 100%">
                    <el-option label="专科" value="专科" />
                    <el-option label="本科" value="本科" />
                    <el-option label="硕士" value="硕士" />
                    <el-option label="博士" value="博士" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
            <el-form-item label="个人简介" required>
              <el-input
                v-model="applicationForm.introduction"
                type="textarea"
                rows="5"
                placeholder="请简要介绍您的教学经验、专业背景和教学风格"
              />
            </el-form-item>
            <el-form-item label="上传简历" required>
              <el-upload
                action="#"
                :auto-upload="false"
                :limit="1"
                accept=".pdf,.doc,.docx"
              >
                <template #trigger>
                  <el-button type="primary">选择文件</el-button>
                </template>
                <template #tip>
                  <div class="el-upload__tip">
                    请上传PDF或Word格式的个人简历，文件大小不超过10MB
                  </div>
                </template>
              </el-upload>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="submitForm" :loading="submitLoading">提交申请</el-button>
              <el-button @click="resetForm">重置</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.platform-page {
  width: 100%;
}

.banner {
  height: 300px;
  background-image: url('@/assets/pictures/aboutBackground.jpeg');
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

.section {
  margin-bottom: 60px;
}

.section h2 {
  font-size: 28px;
  text-align: center;
  margin-bottom: 30px;
  color: #333;
  position: relative;
}

.section h2::after {
  content: '';
  position: absolute;
  bottom: -10px;
  left: 50%;
  transform: translateX(-50%);
  width: 50px;
  height: 3px;
  background-color: #409EFF;
}

.intro-content {
  text-align: center;
}

.intro-text p {
  line-height: 1.8;
  margin-bottom: 20px;
  color: #666;
  max-width: 800px;
  margin-left: auto;
  margin-right: auto;
}

.advantages {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-top: 40px;
}

.advantage-item {
  background-color: #fff;
  border-radius: 8px;
  padding: 30px 20px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.advantage-item:hover {
  transform: translateY(-10px);
}

.advantage-item .el-icon {
  font-size: 36px;
  color: #409EFF;
  margin-bottom: 15px;
}

.advantage-item h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.advantage-item p {
  color: #666;
  font-size: 14px;
}

.requirements-list {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.requirement-item {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: flex-start;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

.requirement-icon {
  width: 50px;
  height: 50px;
  background-color: #ecf5ff;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #409EFF;
  font-size: 24px;
  margin-right: 15px;
}

.requirement-content h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.requirement-content p {
  color: #666;
  line-height: 1.6;
}

.position-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  height: 100%;
  display: flex;
  flex-direction: column;
}

.position-card h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.position-tags {
  display: flex;
  gap: 10px;
  margin-bottom: 15px;
}

.position-card p {
  color: #666;
  line-height: 1.6;
  margin-bottom: 20px;
  flex: 1;
}

.application-form {
  background-color: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

@media (max-width: 992px) {
  .advantages,
  .requirements-list {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .platform-page {
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

  .section {
    margin-bottom: 40px;
  }

  .section h2 {
    font-size: 24px;
    margin-bottom: 20px;
  }

  .advantages,
  .requirements-list {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .advantage-item,
  .requirement-item {
    padding: 20px 16px;
  }

  .advantage-item .el-icon,
  .requirement-item .el-icon {
    font-size: 32px;
    margin-bottom: 12px;
  }

  .advantage-item h3,
  .requirement-item h3 {
    font-size: 16px;
    margin-bottom: 8px;
  }

  .advantage-item p,
  .requirement-item p {
    font-size: 13px;
    line-height: 1.5;
  }

  .positions-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .position-card {
    padding: 20px 16px;
  }

  .position-header h3 {
    font-size: 16px;
  }

  .position-header .el-tag {
    font-size: 12px;
  }

  .position-card p {
    font-size: 13px;
    line-height: 1.5;
    margin-bottom: 16px;
  }

  .application-form {
    padding: 20px 16px;
  }

  .application-form h2 {
    font-size: 20px;
    margin-bottom: 16px;
  }

  .el-form-item {
    margin-bottom: 16px;
  }

  .el-form-item__label {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .el-input__inner,
  .el-select .el-input__inner,
  .el-textarea__inner {
    font-size: 16px; /* 防止iOS缩放 */
    padding: 12px 16px;
  }

  .el-textarea__inner {
    min-height: 80px;
  }

  .form-actions {
    flex-direction: column;
    gap: 12px;
  }

  .form-actions .el-button {
    width: 100%;
    padding: 12px;
    font-size: 16px;
  }
}

@media (max-width: 480px) {
  .platform-page {
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

  .section h2 {
    font-size: 20px;
  }

  .advantage-item,
  .requirement-item,
  .position-card {
    padding: 16px 12px;
  }

  .application-form {
    padding: 16px 12px;
  }

  .application-form h2 {
    font-size: 18px;
  }

  .el-input__inner,
  .el-select .el-input__inner,
  .el-textarea__inner {
    padding: 14px 16px;
  }

  .form-actions .el-button {
    padding: 14px;
  }
}
</style>
