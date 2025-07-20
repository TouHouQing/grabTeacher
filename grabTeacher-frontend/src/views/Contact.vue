<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'

// 定义组件名称
defineOptions({
  name: 'ContactView'
})

// 留言表单
const contactForm = reactive({
  name: '',
  email: '',
  phone: '',
  subject: '',
  message: ''
})

// 表单提交状态
const submitLoading = ref(false)

// 提交表单
const submitForm = () => {
  submitLoading.value = true

  // 模拟API请求
  setTimeout(() => {
    ElMessage.success('您的留言已成功提交，我们会尽快回复您！')
    resetForm()
    submitLoading.value = false
  }, 1500)
}

// 重置表单
const resetForm = () => {
  contactForm.name = ''
  contactForm.email = ''
  contactForm.phone = ''
  contactForm.subject = ''
  contactForm.message = ''
}

// FAQ数据
const faqs = ref([
  {
    question: '如何选择适合自己的老师？',
    answer: '您可以通过我们的智能匹配系统，根据您的学习需求和偏好，系统会自动为您推荐最适合的教师。您也可以在"天下名师"页面浏览教师资料，根据自己的需求选择合适的教师。'
  },
  {
    question: '课程价格如何计算？',
    answer: '课程价格根据教师资质、课程难度和市场行情等因素综合计算。我们提供多种课程套餐，您可以在"金牌课程"页面查看详细的课程价格。'
  },
  {
    question: '如何预约试听课？',
    answer: '您可以在找到心仪的老师后，点击"免费试听"按钮预约试听课。我们会安排老师与您联系，确认具体的试听时间。'
  },
  {
    question: '课程可以退款吗？',
    answer: '我们提供7天内无条件退款服务。如果您在购买课程后7天内对课程不满意，可以申请全额退款。超过7天的课程退款需要根据具体情况评估。'
  },
  {
    question: '如何成为平台上的教师？',
    answer: '您可以在"教师招聘"页面提交教师申请，我们会对您的资质进行评估，符合条件的教师将收到邀请加入我们的平台。'
  },
  {
    question: '平台如何保证教学质量？',
    answer: '我们有严格的教师筛选机制，所有教师都经过专业能力和教学经验的评估。同时，我们会收集学生的反馈，不断优化教学服务。'
  }
])
</script>

<template>
  <div class="contact-page">
    <div class="banner">
      <div class="banner-content">
        <h1>联系我们</h1>
        <p>有任何问题或建议，请随时与我们联系</p>
      </div>
    </div>

    <div class="container">
      <div class="contact-info-section">
        <div class="contact-info-grid">
          <div class="contact-info-card">
            <div class="icon">
              <el-icon><Location /></el-icon>
            </div>
            <h3>公司地址</h3>
            <p>北京市海淀区中关村科技园区8号楼</p>
          </div>
          <div class="contact-info-card">
            <div class="icon">
              <el-icon><Phone /></el-icon>
            </div>
            <h3>联系电话</h3>
            <p>400-888-8888</p>
            <p>010-66668888</p>
          </div>
          <div class="contact-info-card">
            <div class="icon">
              <el-icon><Message /></el-icon>
            </div>
            <h3>电子邮箱</h3>
            <p>contact@grabteacher.com</p>
            <p>support@grabteacher.com</p>
          </div>
          <div class="contact-info-card">
            <div class="icon">
              <el-icon><Clock /></el-icon>
            </div>
            <h3>工作时间</h3>
            <p>周一至周五: 9:00 - 18:00</p>
            <p>周六至周日: 10:00 - 16:00</p>
          </div>
        </div>
      </div>

      <div class="contact-content">
        <div class="contact-form-section">
          <h2>给我们留言</h2>
          <div class="form-description">
            <p>如果您有任何问题、建议或合作意向，请填写以下表单，我们会尽快回复您。</p>
          </div>
          <div class="contact-form">
            <el-form :model="contactForm" label-position="top">
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="姓名" required>
                    <el-input v-model="contactForm.name" placeholder="请输入您的姓名" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="电子邮箱" required>
                    <el-input v-model="contactForm.email" placeholder="请输入您的电子邮箱" />
                  </el-form-item>
                </el-col>
              </el-row>
              <el-row :gutter="20">
                <el-col :span="12">
                  <el-form-item label="联系电话">
                    <el-input v-model="contactForm.phone" placeholder="请输入您的联系电话" />
                  </el-form-item>
                </el-col>
                <el-col :span="12">
                  <el-form-item label="主题" required>
                    <el-select v-model="contactForm.subject" placeholder="请选择留言主题" style="width: 100%">
                      <el-option label="课程咨询" value="课程咨询" />
                      <el-option label="教师咨询" value="教师咨询" />
                      <el-option label="技术支持" value="技术支持" />
                      <el-option label="投诉建议" value="投诉建议" />
                      <el-option label="合作洽谈" value="合作洽谈" />
                      <el-option label="其他" value="其他" />
                    </el-select>
                  </el-form-item>
                </el-col>
              </el-row>
              <el-form-item label="留言内容" required>
                <el-input
                  v-model="contactForm.message"
                  type="textarea"
                  rows="5"
                  placeholder="请输入您的留言内容"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="submitForm" :loading="submitLoading">提交留言</el-button>
                <el-button @click="resetForm">重置</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>

        <div class="faq-section">
          <h2>常见问题</h2>
          <el-collapse>
            <el-collapse-item v-for="(faq, index) in faqs" :key="index" :title="faq.question">
              <p>{{ faq.answer }}</p>
            </el-collapse-item>
          </el-collapse>
        </div>
      </div>

      <div class="map-section">
        <h2>位置地图</h2>
        <div class="map-container">
          <img :src="$getImageUrl('@/assets/pictures/campusFB.jpeg')" alt="地图">
        </div>
      </div>

      <div class="cta-section">
        <h2>成为我们的合作伙伴</h2>
        <p>我们欢迎各类教育机构、学校或相关企业与我们进行合作，共同推动教育事业的发展。</p>
        <el-button type="primary" size="large">联系商务</el-button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.contact-page {
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

.contact-info-section {
  margin-bottom: 50px;
}

.contact-info-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
}

.contact-info-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 30px 20px;
  text-align: center;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.contact-info-card:hover {
  transform: translateY(-10px);
}

.icon {
  width: 60px;
  height: 60px;
  background-color: #ecf5ff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 20px;
}

.icon .el-icon {
  font-size: 30px;
  color: #409EFF;
}

.contact-info-card h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.contact-info-card p {
  color: #666;
  margin-bottom: 5px;
}

.contact-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 40px;
  margin-bottom: 50px;
}

.contact-form-section,
.faq-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 30px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
}

h2 {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
  position: relative;
  padding-bottom: 10px;
}

h2::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  width: 50px;
  height: 3px;
  background-color: #409EFF;
}

.form-description {
  margin-bottom: 20px;
  color: #666;
}

.map-section {
  margin-bottom: 50px;
}

.map-container {
  height: 400px;
  background-color: #f5f5f5;
  border-radius: 8px;
  overflow: hidden;
}

.map-container img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cta-section {
  text-align: center;
  background-color: #ecf5ff;
  padding: 50px 30px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.cta-section h2 {
  font-size: 28px;
  margin-bottom: 15px;
  color: #333;
}

.cta-section h2::after {
  left: 50%;
  transform: translateX(-50%);
}

.cta-section p {
  color: #666;
  margin-bottom: 30px;
  max-width: 600px;
  margin-left: auto;
  margin-right: auto;
}

@media (max-width: 992px) {
  .contact-info-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .contact-content {
    grid-template-columns: 1fr;
    gap: 30px;
  }
}

@media (max-width: 768px) {
  .contact-page {
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

  .contact-info-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }

  .contact-info-card {
    padding: 20px 16px;
  }

  .icon {
    width: 50px;
    height: 50px;
    margin-bottom: 16px;
  }

  .icon .el-icon {
    font-size: 24px;
  }

  .contact-info-card h3 {
    font-size: 16px;
    margin-bottom: 8px;
  }

  .contact-info-card p {
    font-size: 14px;
    margin-bottom: 4px;
  }

  .contact-content {
    grid-template-columns: 1fr;
    gap: 24px;
  }

  .contact-form {
    padding: 20px;
  }

  .contact-form h2 {
    font-size: 20px;
    margin-bottom: 16px;
  }

  .form-row {
    flex-direction: column;
    gap: 16px;
  }

  .form-group {
    margin-bottom: 0;
  }

  .form-group label {
    font-size: 14px;
    margin-bottom: 8px;
  }

  .form-group input,
  .form-group select,
  .form-group textarea {
    padding: 12px 16px;
    font-size: 16px; /* 防止iOS缩放 */
    border-radius: 6px;
  }

  .form-group textarea {
    min-height: 100px;
  }

  .form-actions {
    margin-top: 20px;
    flex-direction: column;
    gap: 12px;
  }

  .form-actions .el-button {
    width: 100%;
    padding: 12px;
    font-size: 16px;
  }

  .map-section {
    padding: 20px;
  }

  .map-section h2 {
    font-size: 20px;
    margin-bottom: 16px;
  }

  .map-container {
    height: 250px;
    border-radius: 6px;
  }

  .cta-section {
    padding: 30px 20px;
    margin-bottom: 16px;
  }

  .cta-section h2 {
    font-size: 22px;
    margin-bottom: 12px;
  }

  .cta-section p {
    font-size: 14px;
    margin-bottom: 20px;
    line-height: 1.5;
  }
}

@media (max-width: 480px) {
  .contact-page {
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

  .contact-info-card {
    padding: 16px 12px;
  }

  .icon {
    width: 40px;
    height: 40px;
    margin-bottom: 12px;
  }

  .icon .el-icon {
    font-size: 20px;
  }

  .contact-form,
  .map-section {
    padding: 16px;
  }

  .form-group input,
  .form-group select,
  .form-group textarea {
    padding: 14px 16px;
  }

  .map-container {
    height: 200px;
  }

  .cta-section {
    padding: 24px 16px;
  }

  .cta-section h2 {
    font-size: 20px;
  }

  .cta-section p {
    font-size: 13px;
  }
}
</style>
