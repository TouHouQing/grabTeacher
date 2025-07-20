<script setup lang="ts">
import { ref, reactive, computed } from 'vue'

// 定义组件名称
defineOptions({
  name: 'StudyAbroadView'
})

// 导入图片资源
import usaFlag from '@/assets/pictures/usaFlag.jpeg'
import ukFlag from '@/assets/pictures/ukFlag.jpeg'
import canadaFlag from '@/assets/pictures/canadaFlag.jpeg'
import australiaFlag from '@/assets/pictures/australiaFlag.jpeg'
import japanFlag from '@/assets/pictures/japanFlag.jpeg'
import koreaFlag from '@/assets/pictures/koreaFlag.jpeg'
import germanyFlag from '@/assets/pictures/germanyFlag.jpeg'
import franceFlag from '@/assets/pictures/franceFlag.jpeg'
import consultant1 from '@/assets/pictures/consultant1.jpeg'
import consultant2 from '@/assets/pictures/consultant2.jpeg'
import consultant3 from '@/assets/pictures/consultant3.jpeg'
import consultant4 from '@/assets/pictures/consultant4.jpeg'
import consultant5 from '@/assets/pictures/consultant5.jpeg'

// 分页参数
const currentPage = ref(1)
const pageSize = ref(6)

// 过滤条件
const filter = reactive({
  country: '',
  level: '',
  type: ''
})

// 留学项目数据
const programs = ref([
  {
    title: '美国本科留学申请计划',
    consultant: '王顾问',
    consultantAvatar: consultant1,
    description: '提供美国TOP50大学申请指导，包括文书写作、选校定位、面试辅导等全方位服务，助您顺利进入理想大学。',
    image: usaFlag,
    duration: '12个月',
    students: 1280,
    rating: 4.8,
    price: 39800,
    country: '美国',
    level: '本科',
    type: '申请规划',
    isHot: true,
    tags: ['TOP50名校', '文书指导', '面试培训']
  },
  {
    title: '英国硕士留学全程服务',
    consultant: '李顾问',
    consultantAvatar: consultant2,
    description: '专注英国G5名校申请，提供个性化申请方案，包括选校、文书、签证等服务，成功率高达95%。',
    image: ukFlag,
    duration: '10个月',
    students: 958,
    rating: 4.7,
    price: 36800,
    country: '英国',
    level: '硕士',
    type: '全程服务',
    isHot: true,
    tags: ['G5名校', '高录取率', '签证保障']
  },
  {
    title: '加拿大高中留学项目',
    consultant: '张顾问',
    consultantAvatar: consultant3,
    description: '为计划赴加拿大读高中的学生提供全面指导，包括寄宿家庭安排、学校申请、监护人服务等，让孩子海外学习无忧。',
    image: canadaFlag,
    duration: '18个月',
    students: 876,
    rating: 4.9,
    price: 42800,
    country: '加拿大',
    level: '高中',
    type: '全程服务',
    isHot: false,
    tags: ['寄宿家庭', '监护人服务', '语言培训']
  },
  {
    title: '澳大利亚移民留学双规划',
    consultant: '陈顾问',
    consultantAvatar: consultant4,
    description: '结合澳大利亚留学与移民政策，为客户定制最佳留学方案，提高移民成功率，适合有移民意向的留学生。',
    image: australiaFlag,
    duration: '24个月',
    students: 1056,
    rating: 4.9,
    price: 58800,
    country: '澳大利亚',
    level: '硕士',
    type: '移民规划',
    isHot: true,
    tags: ['移民规划', '专业选择', '就业指导']
  },
  {
    title: '日本语言学校+升学方案',
    consultant: '刘顾问',
    consultantAvatar: consultant5,
    description: '提供日本语言学校申请及后续升学规划，包括语言培训、名校申请、生活安排等服务，帮助学生顺利适应日本学习生活。',
    image: japanFlag,
    duration: '15个月',
    students: 782,
    rating: 4.8,
    price: 29800,
    country: '日本',
    level: '语言学校',
    type: '语言+升学',
    isHot: false,
    tags: ['语言培训', '名校升学', '生活指导']
  },
  {
    title: '韩国艺术类专业申请',
    consultant: '周顾问',
    consultantAvatar: consultant1,
    description: '专为艺术生提供的韩国名校申请服务，包括作品集指导、专业选择、面试培训等，提高艺术类专业录取几率。',
    image: koreaFlag,
    duration: '12个月',
    students: 685,
    rating: 4.7,
    price: 33800,
    country: '韩国',
    level: '本科/硕士',
    type: '艺术留学',
    isHot: false,
    tags: ['艺术专业', '作品集指导', '名校申请']
  },
  {
    title: '德国大学申请计划',
    consultant: '赵顾问',
    consultantAvatar: consultant2,
    description: '提供德国公立大学申请服务，包括语言培训、材料准备、签证办理等，享受德国免学费高质量教育。',
    image: germanyFlag,
    duration: '14个月',
    students: 592,
    rating: 4.8,
    price: 31800,
    country: '德国',
    level: '本科/硕士',
    type: '申请规划',
    isHot: false,
    tags: ['公立大学', '免学费', '语言培训']
  },
  {
    title: '法国艺术留学精品项目',
    consultant: '杨顾问',
    consultantAvatar: consultant3,
    description: '为艺术生提供法国顶尖艺术院校申请服务，包括专业作品集指导、面试培训、语言准备等，打开世界艺术之都的大门。',
    image: franceFlag,
    duration: '16个月',
    students: 1123,
    rating: 4.9,
    price: 39800,
    country: '法国',
    level: '本科/硕士',
    type: '艺术留学',
    isHot: true,
    tags: ['艺术名校', '作品集指导', '语言培训']
  }
])

// 留学套餐数据
const studyPackages = ref([
  {
    title: '美国名校保录取套餐',
    originalPrice: 59800,
    currentPrice: 49800,
    programs: [
      { title: '美国大学申请规划', consultant: '王顾问', duration: '12个月' },
      { title: 'SAT/托福考试培训', consultant: '李顾问', duration: '6个月' },
      { title: '文书写作与面试指导', consultant: '张顾问', duration: '3个月' }
    ],
    features: [
      'TOP30名校申请指导',
      '标化考试全程培训',
      '名校校友1对1指导',
      '定制化申请方案'
    ]
  },
  {
    title: '英国硕士黄金申请套餐',
    originalPrice: 46800,
    currentPrice: 39800,
    programs: [
      { title: '英国硕士申请规划', consultant: '陈顾问', duration: '10个月' },
      { title: '雅思考试强化培训', consultant: '刘顾问', duration: '4个月' }
    ],
    features: [
      'G5名校申请指导',
      '雅思考试培训保分',
      '签证办理全程指导',
      '海外生活适应培训'
    ]
  },
  {
    title: '日韩留学经济套餐',
    originalPrice: 38800,
    currentPrice: 32800,
    programs: [
      { title: '日韩留学申请规划', consultant: '周顾问', duration: '12个月' },
      { title: '日语/韩语强化培训', consultant: '赵顾问', duration: '6个月' },
      { title: '海外生活适应指导', consultant: '杨顾问', duration: '3个月' }
    ],
    features: [
      '语言培训+留学规划',
      '性价比最高留学方案',
      '海外生活全面指导',
      '就业规划与指导'
    ]
  }
])

// 筛选项目
const filteredPrograms = computed(() => {
  return programs.value.filter(program => {
    let match = true
    if (filter.country && program.country !== filter.country) {
      match = false
    }
    if (filter.level && program.level !== filter.level) {
      match = false
    }
    if (filter.type && program.type !== filter.type) {
      match = false
    }
    return match
  })
})

// 分页显示
const displayPrograms = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return filteredPrograms.value.slice(start, end)
})

// 筛选方法
const handleFilter = () => {
  currentPage.value = 1
}

// 重置筛选
const resetFilter = () => {
  filter.country = ''
  filter.level = ''
  filter.type = ''
  currentPage.value = 1
}
</script>

<template>
  <div class="study-abroad">
    <div class="banner">
      <div class="banner-content">
        <h1>海外留学咨询</h1>
        <p>专业规划，助您开启全球名校之旅</p>
      </div>
    </div>

    <div class="container">
      <!-- 留学项目筛选 -->
      <div class="filter-section">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="留学国家">
            <el-select v-model="filter.country" placeholder="选择国家" clearable>
              <el-option label="美国" value="美国" />
              <el-option label="英国" value="英国" />
              <el-option label="加拿大" value="加拿大" />
              <el-option label="澳大利亚" value="澳大利亚" />
              <el-option label="日本" value="日本" />
              <el-option label="韩国" value="韩国" />
              <el-option label="德国" value="德国" />
              <el-option label="法国" value="法国" />
            </el-select>
          </el-form-item>
          <el-form-item label="留学阶段">
            <el-select v-model="filter.level" placeholder="选择阶段" clearable>
              <el-option label="高中" value="高中" />
              <el-option label="本科" value="本科" />
              <el-option label="硕士" value="硕士" />
              <el-option label="语言学校" value="语言学校" />
              <el-option label="本科/硕士" value="本科/硕士" />
            </el-select>
          </el-form-item>
          <el-form-item label="服务类型">
            <el-select v-model="filter.type" placeholder="服务类型" clearable>
              <el-option label="申请规划" value="申请规划" />
              <el-option label="全程服务" value="全程服务" />
              <el-option label="语言+升学" value="语言+升学" />
              <el-option label="移民规划" value="移民规划" />
              <el-option label="艺术留学" value="艺术留学" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleFilter">筛选</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </el-form-item>
        </el-form>
      </div>

      <!-- 留学项目列表 -->
      <div class="programs-section">
        <div class="programs-grid">
          <div class="program-card" v-for="(program, index) in displayPrograms" :key="index">
            <div class="program-image">
              <img :src="program.image" :alt="program.title">
              <div class="program-badge" v-if="program.isHot">热门</div>
              <div class="price-tag">¥{{ program.price }}</div>
            </div>
            <div class="program-info">
              <h3>{{ program.title }}</h3>
              <div class="program-consultant">
                <img :src="program.consultantAvatar" :alt="program.consultant">
                <span>{{ program.consultant }}</span>
              </div>
              <p class="program-description">{{ program.description }}</p>
              <div class="program-meta">
                <div class="meta-item">
                  <el-icon><Timer /></el-icon>
                  <span>{{ program.duration }}</span>
                </div>
                <div class="meta-item">
                  <el-icon><User /></el-icon>
                  <span>{{ program.students }}人已咨询</span>
                </div>
                <div class="meta-item">
                  <el-icon><Star /></el-icon>
                  <span>{{ program.rating }}分</span>
                </div>
              </div>
              <div class="program-tags">
                <el-tag v-for="(tag, i) in program.tags" :key="i" size="small" class="program-tag">{{ tag }}</el-tag>
              </div>
              <div class="program-actions">
                <span class="program-price">￥{{ program.price }}</span>
                <el-button type="primary" size="small">立即咨询</el-button>
                <el-button type="info" size="small" plain>免费评估</el-button>
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
            :total="filteredPrograms.length"
          />
        </div>
      </div>

      <!-- 热门留学套餐 -->
      <div class="study-packages">
        <h2 class="section-title">精选留学套餐</h2>
        <p class="section-subtitle">一站式留学解决方案，助您圆梦海外名校</p>
        <div class="packages-grid">
          <div class="package-card" v-for="(pkg, index) in studyPackages" :key="index">
            <div class="package-header">
              <h3>{{ pkg.title }}</h3>
              <div class="package-price">
                <span class="original-price">￥{{ pkg.originalPrice }}</span>
                <span class="current-price">￥{{ pkg.currentPrice }}</span>
              </div>
            </div>
            <div class="package-content">
              <div class="package-programs">
                <div class="package-program-item" v-for="(program, i) in pkg.programs" :key="i">
                  <div class="program-icon">
                    <el-icon><Document /></el-icon>
                  </div>
                  <div class="program-detail">
                    <h4>{{ program.title }}</h4>
                    <p>{{ program.consultant }} | {{ program.duration }}</p>
                  </div>
                </div>
              </div>
              <div class="package-features">
                <div class="feature-item" v-for="(feature, i) in pkg.features" :key="i">
                  <el-icon><Check /></el-icon>
                  <span>{{ feature }}</span>
                </div>
              </div>
              <el-button type="primary" class="package-btn">立即购买</el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.study-abroad {
  width: 100%;
}

.banner {
  height: 300px;
  background-image: url('@/assets/pictures/studyAbroadBanner.jpeg');
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

.programs-section {
  margin-bottom: 60px;
}

.programs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
  margin-bottom: 30px;
}

.program-card {
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.program-card:hover {
  transform: translateY(-10px);
}

.program-image {
  position: relative;
}

.program-image img {
  width: 100%;
  height: 180px;
  object-fit: cover;
}

.program-badge {
  position: absolute;
  top: 10px;
  right: 10px;
  background-color: #f56c6c;
  color: white;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: bold;
}

.price-tag {
  position: absolute;
  bottom: 10px;
  right: 10px;
  background-color: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 10px;
  border-radius: 4px;
  font-size: 16px;
  font-weight: bold;
}

.program-info {
  padding: 20px;
}

.program-info h3 {
  font-size: 18px;
  margin-bottom: 10px;
  color: #333;
  height: 50px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.program-consultant {
  display: flex;
  align-items: center;
  margin-bottom: 10px;
}

.program-consultant img {
  width: 30px;
  height: 30px;
  border-radius: 50%;
  object-fit: cover;
  margin-right: 10px;
}

.program-consultant span {
  color: #409EFF;
  font-size: 14px;
}

.program-description {
  color: #666;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 15px;
  height: 90px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 4;
  -webkit-box-orient: vertical;
}

.program-meta {
  display: flex;
  justify-content: space-between;
  margin-bottom: 15px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 5px;
  color: #666;
  font-size: 14px;
}

.program-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 15px;
}

.program-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.program-price {
  font-size: 20px;
  font-weight: bold;
  color: #f56c6c;
  margin-right: auto;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 40px;
}

.section-title {
  font-size: 28px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 10px;
  color: #333;
}

.section-subtitle {
  font-size: 16px;
  text-align: center;
  margin-bottom: 30px;
  color: #666;
}

.packages-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 30px;
}

.package-card {
  background-color: #fff;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
  transition: transform 0.3s ease;
}

.package-card:hover {
  transform: translateY(-10px);
}

.package-header {
  background-color: #409EFF;
  color: white;
  padding: 20px;
  text-align: center;
}

.package-header h3 {
  font-size: 20px;
  margin-bottom: 10px;
}

.package-price {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 10px;
}

.original-price {
  font-size: 16px;
  text-decoration: line-through;
  opacity: 0.8;
}

.current-price {
  font-size: 26px;
  font-weight: bold;
}

.package-content {
  padding: 20px;
}

.package-programs {
  margin-bottom: 20px;
}

.package-program-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 0;
  border-bottom: 1px solid #eee;
}

.package-program-item:last-child {
  border-bottom: none;
}

.program-icon {
  font-size: 24px;
  color: #409EFF;
}

.program-detail {
  flex: 1;
}

.program-detail h4 {
  font-size: 16px;
  margin-bottom: 5px;
  color: #333;
}

.program-detail p {
  font-size: 14px;
  color: #666;
}

.package-features {
  margin-bottom: 20px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
  color: #666;
}

.feature-item .el-icon {
  color: #67c23a;
}

.package-btn {
  width: 100%;
}

@media (max-width: 1200px) {
  .programs-grid,
  .packages-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}

@media (max-width: 768px) {
  .programs-grid,
  .packages-grid {
    grid-template-columns: 1fr;
  }

  .filter-form {
    flex-direction: column;
    align-items: flex-start;
  }

  .study-abroad {
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

  .filter-form {
    flex-direction: column;
    gap: 16px;
  }

  .filter-form .el-form-item {
    margin-right: 0;
    width: 100%;
    margin-bottom: 0;
  }

  .filter-form .el-select {
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

  .programs-grid {
    grid-template-columns: 1fr;
    gap: 20px;
  }

  .program-card {
    padding: 16px;
  }

  .program-image {
    height: 180px;
    margin-bottom: 16px;
  }

  .program-info h3 {
    font-size: 18px;
    margin-bottom: 8px;
  }

  .program-meta {
    flex-direction: column;
    gap: 8px;
    align-items: flex-start;
    margin-bottom: 12px;
  }

  .meta-item {
    font-size: 13px;
  }

  .program-description {
    font-size: 13px;
    line-height: 1.5;
    margin-bottom: 12px;
  }

  .program-features {
    margin-bottom: 16px;
  }

  .feature-tag {
    font-size: 12px;
    margin-right: 6px;
    margin-bottom: 6px;
  }

  .program-actions {
    flex-direction: column;
    align-items: stretch;
    gap: 8px;
  }

  .program-actions .el-button {
    width: 100%;
  }

  .consultation-form {
    padding: 20px 16px;
  }

  .consultation-form h2 {
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
  .study-abroad {
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

  .program-card {
    padding: 12px;
  }

  .program-image {
    height: 160px;
    margin-bottom: 12px;
  }

  .program-info h3 {
    font-size: 16px;
  }

  .program-description {
    font-size: 12px;
  }

  .filter-actions {
    flex-direction: column;
  }

  .filter-actions .el-button {
    width: 100%;
    margin-bottom: 8px;
  }

  .consultation-form {
    padding: 16px 12px;
  }

  .consultation-form h2 {
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
