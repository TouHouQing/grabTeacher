<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useUserStore } from '../../stores/user'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

// 模拟用户资料数据
const userForm = reactive({
  username: userStore.username || '学生用户',
  realName: '张三',
  gender: '男',
  age: 16,
  grade: '初一',
  school: '实验中学',
  phone: '13800138000',
  email: 'student@example.com',
  avatar: '@/assets/pictures/studentBoy2.jpeg'
})

// 学习偏好设置
const preferences = reactive({
  learningStyle: '视觉学习',
  subjects: ['数学', '物理'],
  weakPoints: ['函数', '微积分'],
  strongPoints: ['代数', '概率'],
  preferredDays: ['周一', '周三', '周六'],
  preferredTime: '晚上'
})

// 模拟提交操作
const loading = ref(false)
const saveProfile = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('个人资料保存成功')
  }, 1000)
}

const savePreferences = () => {
  loading.value = true
  setTimeout(() => {
    loading.value = false
    ElMessage.success('学习偏好保存成功')
  }, 1000)
}
</script>

<template>
  <div class="student-profile">
    <h2>个人资料</h2>

    <el-tabs>
      <el-tab-pane label="基本信息">
        <div class="profile-container">
          <div class="avatar-container">
            <div class="avatar">
              <img :src="$getImageUrl(userForm.avatar)" alt="头像">
            </div>
            <el-button size="small" type="primary">更换头像</el-button>
          </div>

          <div class="form-container">
            <el-form :model="userForm" label-width="100px">
              <el-form-item label="用户名">
                <el-input v-model="userForm.username" disabled></el-input>
              </el-form-item>
              <el-form-item label="真实姓名">
                <el-input v-model="userForm.realName"></el-input>
              </el-form-item>
              <el-form-item label="性别">
                <el-radio-group v-model="userForm.gender">
                  <el-radio label="男">男</el-radio>
                  <el-radio label="女">女</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="年龄">
                <el-input-number v-model="userForm.age" :min="6" :max="25"></el-input-number>
              </el-form-item>
              <el-form-item label="年级">
                <el-select v-model="userForm.grade">
                  <el-option label="初一" value="初一"></el-option>
                  <el-option label="初二" value="初二"></el-option>
                  <el-option label="初三" value="初三"></el-option>
                </el-select>
              </el-form-item>
              <el-form-item label="学校">
                <el-input v-model="userForm.school"></el-input>
              </el-form-item>
              <el-form-item label="手机号码">
                <el-input v-model="userForm.phone"></el-input>
              </el-form-item>
              <el-form-item label="邮箱">
                <el-input v-model="userForm.email"></el-input>
              </el-form-item>
              <el-form-item>
                <el-button type="primary" @click="saveProfile" :loading="loading">保存信息</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane label="学习偏好">
        <div class="preferences-container">
          <el-form :model="preferences" label-width="120px">
            <el-form-item label="学习风格">
              <el-select v-model="preferences.learningStyle">
                <el-option label="视觉学习" value="视觉学习"></el-option>
                <el-option label="听觉学习" value="听觉学习"></el-option>
                <el-option label="动手学习" value="动手学习"></el-option>
                <el-option label="阅读学习" value="阅读学习"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="关注科目">
              <el-select v-model="preferences.subjects" multiple>
                <el-option label="数学" value="数学"></el-option>
                <el-option label="语文" value="语文"></el-option>
                <el-option label="英语" value="英语"></el-option>
                <el-option label="物理" value="物理"></el-option>
                <el-option label="化学" value="化学"></el-option>
                <el-option label="生物" value="生物"></el-option>
                <el-option label="历史" value="历史"></el-option>
                <el-option label="地理" value="地理"></el-option>
                <el-option label="政治" value="政治"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="薄弱环节">
              <el-select v-model="preferences.weakPoints" multiple>
                <el-option label="代数" value="代数"></el-option>
                <el-option label="几何" value="几何"></el-option>
                <el-option label="函数" value="函数"></el-option>
                <el-option label="微积分" value="微积分"></el-option>
                <el-option label="概率" value="概率"></el-option>
                <el-option label="统计" value="统计"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="擅长环节">
              <el-select v-model="preferences.strongPoints" multiple>
                <el-option label="代数" value="代数"></el-option>
                <el-option label="几何" value="几何"></el-option>
                <el-option label="函数" value="函数"></el-option>
                <el-option label="微积分" value="微积分"></el-option>
                <el-option label="概率" value="概率"></el-option>
                <el-option label="统计" value="统计"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="偏好上课日期">
              <el-select v-model="preferences.preferredDays" multiple>
                <el-option label="周一" value="周一"></el-option>
                <el-option label="周二" value="周二"></el-option>
                <el-option label="周三" value="周三"></el-option>
                <el-option label="周四" value="周四"></el-option>
                <el-option label="周五" value="周五"></el-option>
                <el-option label="周六" value="周六"></el-option>
                <el-option label="周日" value="周日"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item label="偏好上课时间">
              <el-select v-model="preferences.preferredTime">
                <el-option label="上午" value="上午"></el-option>
                <el-option label="下午" value="下午"></el-option>
                <el-option label="晚上" value="晚上"></el-option>
                <el-option label="周末" value="周末"></el-option>
              </el-select>
            </el-form-item>

            <el-form-item>
              <el-button type="primary" @click="savePreferences" :loading="loading">保存偏好</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>

      <el-tab-pane label="修改密码">
        <div class="password-container">
          <el-form label-width="120px">
            <el-form-item label="当前密码">
              <el-input type="password" show-password></el-input>
            </el-form-item>
            <el-form-item label="新密码">
              <el-input type="password" show-password></el-input>
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input type="password" show-password></el-input>
            </el-form-item>
            <el-form-item>
              <el-button type="primary">修改密码</el-button>
            </el-form-item>
          </el-form>
        </div>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<style scoped>
.student-profile {
  padding: 20px;
}

.profile-container {
  display: flex;
  margin-top: 20px;
}

.avatar-container {
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
  margin-bottom: 15px;
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-container {
  flex: 1;
}

.preferences-container,
.password-container {
  margin-top: 20px;
  max-width: 600px;
}
</style>
