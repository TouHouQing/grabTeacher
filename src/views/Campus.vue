<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

const activeCity = ref('beijing')
const searchKeyword = ref('')

interface Campus {
  id: number;
  name: string;
  address: string;
  phone: string;
  hours: string;
  image: string;
}

interface CampusData {
  [key: string]: Campus[];
}

const campuses = reactive<CampusData>({
  beijing: [
    {
      id: 1,
      name: '中关村校区',
      address: '北京市海淀区中关村大街11号',
      phone: '010-88886666',
      hours: '周一至周日 8:00-22:00',
      image: '@/assets/pictures/campusZGC.jpg'
    },
    {
      id: 2,
      name: '五道口校区',
      address: '北京市海淀区五道口华清嘉园12号楼',
      phone: '010-88887777',
      hours: '周一至周日 9:00-21:00',
      image: '@/assets/pictures/campusWDK.jpeg'
    },
    {
      id: 3,
      name: '金融街校区',
      address: '北京市西城区金融街22号',
      phone: '010-88888888',
      hours: '周一至周日 9:00-21:00',
      image: '@/assets/pictures/campusJRJ.jpeg'
    }
  ],
  shanghai: [
    {
      id: 4,
      name: '陆家嘴校区',
      address: '上海市浦东新区陆家嘴环路88号',
      phone: '021-66665555',
      hours: '周一至周日 8:00-22:00',
      image: '@/assets/pictures/campusLJZ.jpeg'
    },
    {
      id: 5,
      name: '徐家汇校区',
      address: '上海市徐汇区肇嘉浜路8号',
      phone: '021-66667777',
      hours: '周一至周日 9:00-21:00',
      image: '@/assets/pictures/campusXJH.jpeg'
    }
  ],
  guangzhou: [
    {
      id: 6,
      name: '天河城校区',
      address: '广州市天河区天河路228号',
      phone: '020-55556666',
      hours: '周一至周日 9:00-22:00',
      image: '@/assets/pictures/campusTHC.jpg'
    }
  ],
  shenzhen: [
    {
      id: 7,
      name: '南山校区',
      address: '深圳市南山区科技园南区12栋',
      phone: '0755-88889999',
      hours: '周一至周日 9:00-22:00',
      image: '@/assets/pictures/campusNS.jpeg'
    }
  ]
})

const filteredCampuses = computed(() => {
  if (!searchKeyword.value) {
    return campuses[activeCity.value]
  }

  const keyword = searchKeyword.value.toLowerCase()
  return campuses[activeCity.value].filter(campus =>
    campus.name.toLowerCase().includes(keyword) ||
    campus.address.toLowerCase().includes(keyword)
  )
})
</script>

<template>
  <div class="campus-page">
    <div class="banner">
      <div class="banner-content">
        <h1>校区查询</h1>
        <p>已在中国北京,上海,天津,江苏,广州,以及新加坡开设100+线下教学点</p>
      </div>
    </div>

    <div class="container">
      <!-- 校区搜索 -->
      <div class="search-section">
        <div class="search-box">
          <el-input
            v-model="searchKeyword"
            placeholder="输入校区名称或地址关键词"
            prefix-icon="Search"
            clearable
          />
        </div>
        <div class="city-tabs">
          <el-tabs v-model="activeCity">
            <el-tab-pane label="北京" name="beijing"></el-tab-pane>
            <el-tab-pane label="上海" name="shanghai"></el-tab-pane>
            <el-tab-pane label="广州" name="guangzhou"></el-tab-pane>
            <el-tab-pane label="深圳" name="shenzhen"></el-tab-pane>
          </el-tabs>
        </div>
      </div>

      <!-- 校区列表 -->
      <div class="campus-list">
        <div v-if="filteredCampuses.length === 0" class="no-result">
          <el-empty description="没有找到符合条件的校区" />
        </div>
        <div v-else class="campus-grid">
          <el-card v-for="campus in filteredCampuses" :key="campus.id" class="campus-card">
            <div class="campus-image">
              <img :src="$getImageUrl(campus.image)" :alt="campus.name">
            </div>
            <div class="campus-info">
              <h3>{{ campus.name }}</h3>
              <div class="info-item">
                <el-icon><Location /></el-icon>
                <span>{{ campus.address }}</span>
              </div>
              <div class="info-item">
                <el-icon><Phone /></el-icon>
                <span>{{ campus.phone }}</span>
              </div>
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <span>{{ campus.hours }}</span>
              </div>
              <div class="campus-actions">
                <el-button type="primary" size="small">在线咨询</el-button>
                <el-button type="success" size="small">预约参观</el-button>
              </div>
            </div>
          </el-card>
        </div>
      </div>

      <!-- 校区地图 -->
      <div class="campus-map">
        <h2>校区分布地图</h2>
        <div class="map-container">
          <img :src="$getImageUrl('@/assets/pictures/campusFB.jpeg')" alt="校区地图">
        </div>
      </div>

      <!-- 预约参观 -->
      <div class="visit-appointment">
        <h2>预约参观</h2>
        <p class="section-desc">欢迎预约参观我们的校区，了解更多关于我们的教学环境和课程</p>
        <el-form label-position="top" class="appointment-form">
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="姓名">
                <el-input placeholder="请输入您的姓名" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="联系电话">
                <el-input placeholder="请输入您的联系电话" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="预约校区">
                <el-select placeholder="请选择校区" style="width: 100%">
                  <el-option
                    v-for="campus in campuses[activeCity]"
                    :key="campus.id"
                    :label="campus.name"
                    :value="campus.id"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="预约时间">
                <el-date-picker
                  type="datetime"
                  placeholder="选择日期和时间"
                  style="width: 100%"
                />
              </el-form-item>
            </el-col>
          </el-row>
          <el-form-item label="留言">
            <el-input type="textarea" rows="4" placeholder="请输入您的要求或问题" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary">提交预约</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.campus-page {
  width: 100%;
}

.banner {
  height: 300px;
  background-image: v-bind('`url(${$getImageUrl("@/assets/pictures/campusBackground.jpg")})`');
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

.search-section {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 30px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.search-box {
  margin-bottom: 20px;
}

.campus-list {
  margin-bottom: 40px;
}

.campus-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

.campus-card {
  transition: transform 0.3s ease;
  height: 100%;
}

.campus-card:hover {
  transform: translateY(-5px);
}

.campus-image {
  height: 200px;
  overflow: hidden;
  margin-bottom: 15px;
}

.campus-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.campus-info h3 {
  font-size: 18px;
  margin-bottom: 15px;
  color: #333;
}

.info-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 10px;
  color: #666;
}

.info-item .el-icon {
  margin-right: 8px;
  color: #409EFF;
}

.campus-actions {
  display: flex;
  gap: 10px;
  margin-top: 15px;
}

.campus-map {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  margin-bottom: 40px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.campus-map h2 {
  font-size: 24px;
  margin-bottom: 20px;
  color: #333;
  text-align: center;
}

.map-container {
  width: 100%;
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

.visit-appointment {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.05);
}

.visit-appointment h2 {
  font-size: 24px;
  margin-bottom: 10px;
  color: #333;
  text-align: center;
}

.section-desc {
  text-align: center;
  color: #666;
  margin-bottom: 30px;
}

.appointment-form {
  max-width: 800px;
  margin: 0 auto;
}

@media (max-width: 992px) {
  .campus-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .campus-grid {
    grid-template-columns: 1fr;
  }
}
</style>
