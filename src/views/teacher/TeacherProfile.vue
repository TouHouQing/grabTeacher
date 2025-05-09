<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

// 模拟教师资料数据
const profileForm = reactive({
  username: userStore.username || '教师用户',
  realName: '张老师',
  gender: '男',
  age: 35,
  phone: '13900139000',
  email: 'teacher@example.com',
  education: '研究生',
  major: '数学',
  teachingExperience: 10,
  introduction: '数学教育专家，专注于中小学数学教学，善于激发学生学习兴趣，使用多种教学方法帮助学生理解数学概念。',
  avatar: '@/assets/pictures/teacherBoy2.jpeg'
})

// 教学信息设置
const teachingForm = reactive({
  subjects: ['高中数学'],
  grades: ['高一', '高二', '高三'],
  teachingStyle: '启发式教学',
  specialties: ['函数', '解析几何', '概率统计'],
  hourlyRate: 200,
  availableDays: ['周一', '周三', '周五', '周六'],
  availableTimeStart: '14:00',
  availableTimeEnd: '21:00'
})

// 上传头像相关
const avatarUrl = ref(profileForm.avatar)

interface UploadResponse {
  raw: File;
}

const handleAvatarSuccess = (response: UploadResponse) => {
  // 实际应用中处理上传成功回调
  avatarUrl.value = URL.createObjectURL(response.raw)
}

// 修改密码表单
const passwordForm = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 提交按钮状态
const profileLoading = ref(false)
const teachingLoading = ref(false)
const passwordLoading = ref(false)

// 保存个人资料
const saveProfile = () => {
  profileLoading.value = true
  setTimeout(() => {
    ElMessage.success('个人资料保存成功')
    profileLoading.value = false
  }, 1000)
}

// 保存教学信息
const saveTeachingInfo = () => {
  teachingLoading.value = true
  setTimeout(() => {
    ElMessage.success('教学信息保存成功')
    teachingLoading.value = false
  }, 1000)
}

// 修改密码
const changePassword = () => {
  passwordLoading.value = true

  // 实际应用中需要验证两次密码是否一致
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的密码不一致')
    passwordLoading.value = false
    return
  }

  setTimeout(() => {
    ElMessage.success('密码修改成功')
    passwordForm.currentPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
    passwordLoading.value = false
  }, 1000)
}
</script>

<template>
  <div class="teacher-profile">
    <h2>个人设置</h2>

    <el-tabs>
      <el-tab-pane label="基本信息">
        <div class="profile-container">
          <div class="avatar-section">
            <div class="avatar">
              <img :src="$getImageUrl(avatarUrl)" alt="头像">
            </div>
            <el-upload
              class="avatar-uploader"
              action="#"
              :show-file-list="false"
              :on-success="handleAvatarSuccess"
            >
              <el-button size="small" type="primary">更换头像</el-button>
            </el-upload>
          </div>

          <div class="form-section">
            <el-form :model="profileForm" label-width="100px">
              <el-form-item label="用户名">
                <el-input v-model="profileForm.username" disabled></el-input>
              </el-form-item>
              <el-form-item label="真实姓名">
                <el-input v-model="profileForm.realName"></el-input>
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="profileForm.gender">
                  <el-radio label="男">男</el-radio>
                  <el-radio label="女">女</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="年龄">
                <el-input-number v-model="profileForm.age" :min="18" :max="80"></el-input-number>
              </el-form-item>
              <el-form-item label="联系电话">
                <el-input v-model="profileForm.phone"></el-input>
              </el-form-item>
              <el-form-item label="电子邮箱">
                <el-input v-model="profileForm.email"></el-input>
              </el-form-item>
              <el-form-item label="学历">
                <el-select v-model="profileForm.education" style="width: 100%">
                  <el-option label="专科" value="专科"></el-option>
                  <el-option label="本科" value="本科"></el-option>
                  <el-option label="研究生" value="研究生"></el-option>
                  <el-option label="博士" value="博士"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="专业">
                <el-input v-model="profileForm.major"></el-input>
              </el-form-item>
              <el-form-item label="教龄">
                <el-input-number v-model="profileForm.teachingExperience" :min="0" :max="50"></el-input-number>
              </el-form-item>
              <el-form-item label="自我介绍">
                <el-input
                  v-model="profileForm.introduction"
                  type="textarea"
                  :rows="4"
                  placeholder="请简要介绍您的教学风格、经验和专长"
                ></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProfile" :loading="profileLoading">保存信息</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="教学信息">
        <div class="teaching-container">
          <el-form :model="teachingForm" label-width="100px">
            <el-form-item label="教授科目">
              <el-select v-model="teachingForm.subjects" multiple style="width: 100%">
                <el-option label="小学数学" value="小学数学"></el-option>
                <el-option label="初中数学" value="初中数学"></el-option>
                <el-option label="高中数学" value="高中数学"></el-option>
                <el-option label="小学英语" value="小学英语"></el-option>
                <el-option label="初中英语" value="初中英语"></el-option>
                <el-option label="高中英语" value="高中英语"></el-option>
                <el-option label="高中物理" value="高中物理"></el-option>
                <el-option label="高中化学" value="高中化学"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="教授年级">
              <el-select v-model="teachingForm.grades" multiple style="width: 100%">
                <el-option label="小学一年级" value="小学一年级"></el-option>
                <el-option label="小学二年级" value="小学二年级"></el-option>
                <el-option label="小学三年级" value="小学三年级"></el-option>
                <el-option label="小学四年级" value="小学四年级"></el-option>
                <el-option label="小学五年级" value="小学五年级"></el-option>
                <el-option label="小学六年级" value="小学六年级"></el-option>
                <el-option label="初一" value="初一"></el-option>
                <el-option label="初二" value="初二"></el-option>
                <el-option label="初三" value="初三"></el-option>
                <el-option label="高一" value="高一"></el-option>
                <el-option label="高二" value="高二"></el-option>
                <el-option label="高三" value="高三"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="教学风格">
              <el-select v-model="teachingForm.teachingStyle" style="width: 100%">
                <el-option label="启发式教学" value="启发式教学"></el-option>
                <el-option label="严谨教学型" value="严谨教学型"></el-option>
                <el-option label="幽默风趣型" value="幽默风趣型"></el-option>
                <el-option label="严慈相济型" value="严慈相济型"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="教学专长">
              <el-select v-model="teachingForm.specialties" multiple style="width: 100%">
                <el-option label="函数" value="函数"></el-option>
                <el-option label="解析几何" value="解析几何"></el-option>
                <el-option label="概率统计" value="概率统计"></el-option>
                <el-option label="立体几何" value="立体几何"></el-option>
                <el-option label="三角函数" value="三角函数"></el-option>
                <el-option label="导数" value="导数"></el-option>
              </el-select>
            </el-form-item>
            <el-form-item label="小时收费(元)">
              <el-input-number v-model="teachingForm.hourlyRate" :min="50" :max="1000" :step="10"></el-input-number>
            </el-form-item>
            <el-form-item label="可授课日期">
              <el-checkbox-group v-model="teachingForm.availableDays">
                <el-checkbox label="周一">周一</el-checkbox>
                <el-checkbox label="周二">周二</el-checkbox>
                <el-checkbox label="周三">周三</el-checkbox>
                <el-checkbox label="周四">周四</el-checkbox>
                <el-checkbox label="周五">周五</el-checkbox>
                <el-checkbox label="周六">周六</el-checkbox>
                <el-checkbox label="周日">周日</el-checkbox>
              </el-checkbox-group>
            </el-form-item>
            <el-form-item label="授课时间">
              <div class="time-range">
                <el-time-picker
                  v-model="teachingForm.availableTimeStart"
                  format="HH:mm"
                  placeholder="开始时间"
                />
                <span class="separator">至</span>
                <el-time-picker
                  v-model="teachingForm.availableTimeEnd"
                  format="HH:mm"
                  placeholder="结束时间"
                />
              </div>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="saveTeachingInfo" :loading="teachingLoading">保存教学信息</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="修改密码">
        <div class="password-container">
          <el-form :model="passwordForm" label-width="120px">
            <el-form-item label="当前密码">
              <el-input v-model="passwordForm.currentPassword" type="password" placeholder="请输入当前密码" show-password></el-input>
            </el-form-item>
            <el-form-item label="新密码">
              <el-input v-model="passwordForm.newPassword" type="password" placeholder="请输入新密码" show-password></el-input>
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input v-model="passwordForm.confirmPassword" type="password" placeholder="请再次输入新密码" show-password></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="changePassword" :loading="passwordLoading">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="认证资料">
        <div class="certification-container">
          <div class="certification-intro">
            <h3>教师认证</h3>
            <p>提交您的教师资格证和相关证书，通过认证后可以提高接单率和课时费。</p>
          </div>

          <div class="certification-uploads">
            <div class="upload-item">
              <h4>教师资格证</h4>
              <p class="upload-desc">请上传教师资格证正反面照片</p>
              <el-upload
                action="#"
                list-type="picture-card"
                :limit="2"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
            </div>

            <div class="upload-item">
              <h4>学历证书</h4>
              <p class="upload-desc">请上传最高学历证书照片</p>
              <el-upload
                action="#"
                list-type="picture-card"
                :limit="1"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
            </div>

            <div class="upload-item">
              <h4>身份证明</h4>
              <p class="upload-desc">请上传身份证正反面照片</p>
              <el-upload
                action="#"
                list-type="picture-card"
                :limit="2"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
            </div>

            <div class="upload-item">
              <h4>其他证书</h4>
              <p class="upload-desc">可上传其他相关资格证书</p>
              <el-upload
                action="#"
                list-type="picture-card"
                :limit="3"
              >
                <el-icon><Plus /></el-icon>
              </el-upload>
            </div>
          </div>

          <div class="certification-submit">
            <el-button type="primary">提交认证</el-button>
            <p class="certification-tip">认证材料提交后，我们会在1-3个工作日内完成审核</p>
          </div>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.teacher-profile {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

.profile-container {
  display: flex;
}

.avatar-section {
  margin-right: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.avatar {
  width: 150px;
  height: 150px;
  border-radius: 50%;
  overflow: hidden;
  margin-bottom: 20px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-section {
  flex: 1;
}

.teaching-container,
.password-container {
  max-width: 600px;
}

.time-range {
  display: flex;
  align-items: center;
}

.separator {
  margin: 0 10px;
}

.certification-container {
  max-width: 800px;
}

.certification-intro {
  margin-bottom: 30px;
}

.certification-intro h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
}

.certification-intro p {
  color: #666;
}

.certification-uploads {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 30px;
  margin-bottom: 30px;
}

.upload-item h4 {
  font-size: 16px;
  margin-bottom: 10px;
  color: #333;
}

.upload-desc {
  color: #666;
  font-size: 14px;
  margin-bottom: 15px;
}

.certification-tip {
  margin-top: 15px;
  color: #909399;
  font-size: 14px;
}

@media (max-width: 768px) {
  .profile-container {
    flex-direction: column;
  }

  .avatar-section {
    margin-right: 0;
    margin-bottom: 30px;
  }

  .certification-uploads {
    grid-template-columns: 1fr;
  }
}
</style>
