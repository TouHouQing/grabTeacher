<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch } from 'vue'
import ContactUs from '../components/ContactUs.vue'
import { useRoute, useRouter } from 'vue-router'
import { studyAbroadAPI } from '../utils/api'
import { Document, Check } from '@element-plus/icons-vue'
import { getImageUrl } from '../utils/imageLoader'
import defaultStudyAbroadImage from '../assets/pictures/studyAbroadBanner.jpeg'


// 定义组件名称
defineOptions({
  name: 'StudyAbroadView'
})

// 路由与状态
const route = useRoute()
const router = useRouter()

// 分页参数（从路由 query 初始化）
const currentPage = ref(Number(route.query.page || 1))
const pageSize = ref(Number(route.query.size || 6))
const total = ref(0)

// 图片兜底与解析
const handleImageError = (e: Event) => {
  const target = e.target as HTMLImageElement
  if (target && target.src !== defaultStudyAbroadImage) {
    target.src = defaultStudyAbroadImage
  }
}
const resolveImage = (path?: string) => {
  if (!path) return defaultStudyAbroadImage
  if (/^https?:\/\//.test(path)) return path
  return getImageUrl(path) || defaultStudyAbroadImage
}

// 过滤条件（后端筛选，仅国家）
const filter = reactive({
  countryId: route.query.countryId ? Number(route.query.countryId) : null as number | null
})

// 数据源
const countries = ref<any[]>([])
const programs = ref<any[]>([])

const loading = ref(false)

const fetchCountries = async () => {
  const res = await studyAbroadAPI.getCountries()
  if (res?.success) countries.value = res.data || []
}

const fetchPrograms = async () => {
  loading.value = true
  try {
    const res = await studyAbroadAPI.getPublicProgramsPaged({
      page: currentPage.value,
      size: pageSize.value,
      countryId: filter.countryId || undefined
    })
    if (res?.success && res?.data) {
      programs.value = res.data.records || []
      total.value = Number(res.data.total || 0)
    } else {
      programs.value = []
      total.value = 0
    }
  } finally {
    loading.value = false
  }
}


// 底部滚动条：获取全部留学咨询用于无缝滚动展示
const allPrograms = ref<any[]>([])
const fetchAllPrograms = async () => {
  try {
    const res = await studyAbroadAPI.getPrograms({})
    allPrograms.value = (res?.success && Array.isArray(res?.data)) ? res.data : []
  } catch (e) {
    allPrograms.value = []
  }
}

// 没有全部数据时回退到当前页数据，保证有内容可滚动
const marqueeItems = computed(() => (allPrograms.value?.length ? allPrograms.value : programs.value))

// 截断工具：中文友好前 len 个字符
const truncate = (text?: string, len = 50) => {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '...' : text
}

onMounted(async () => {
  await fetchCountries()
  await fetchPrograms()
  await fetchAllPrograms()
})

// 留学项目数据（来自后端）
// programs 由 onMounted + fetchPrograms 填充







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

// 服务端分页：直接展示当前页
const displayPrograms = computed(() => programs.value)
  // 详情弹窗状态
  const detailVisible = ref(false)
  const selectedProgram = ref<any | null>(null)
  const showDetail = (p: any) => {
    selectedProgram.value = p
    detailVisible.value = true
  }


// 同步路由 query
const syncQuery = () => {
  const q: any = {}
  if (currentPage.value && currentPage.value !== 1) q.page = currentPage.value
  if (pageSize.value && pageSize.value !== 6) q.size = pageSize.value
  if (filter.countryId) q.countryId = filter.countryId
  router.replace({ query: q })
}

// 筛选方法
const handleFilter = () => {
  currentPage.value = 1
  syncQuery()
  fetchPrograms()
}

// 重置筛选
const resetFilter = () => {
  filter.countryId = null
  currentPage.value = 1
  syncQuery()
  fetchPrograms()
}

// 监听分页与筛选，持久化到 query
watch([currentPage, pageSize, () => filter.countryId], () => {
  syncQuery()
})
</script>

<template>
  <div class="study-abroad">
    <div class="banner">
      <div class="banner-content">
        <h1>留学咨询</h1>
        <p>专业规划，助您开启全球名校之旅</p>
      </div>
    </div>

    <div class="container">
      <!-- 留学项目筛选 -->
      <div class="filter-section">
        <el-form :inline="true" class="filter-form">
          <el-form-item label="留学国家">
            <el-select v-model="filter.countryId" placeholder="选择国家" clearable filterable :loading="loading">
              <el-option v-for="c in countries" :key="c.id" :label="c.countryName" :value="c.id" />
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
              <img :src="resolveImage(program.imageUrl || program.image)" :alt="program.title" @error="handleImageError">
              <div class="program-badge" v-if="program.hot">热门</div>
            </div>
            <div class="program-info">
              <h3>{{ program.title }}</h3>
              <p class="program-description">{{ program.description }}</p>
              <div class="program-tags" v-if="program.tags">
                <el-tag v-for="(tag, i) in JSON.parse(program.tags || '[]')" :key="i" size="small" class="program-tag">{{ tag }}</el-tag>

              </div>
              <div class="program-actions">
                <el-button type="primary" size="small" @click="showDetail(program)">详细信息</el-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 分页 -->
        <div class="pagination">
          <el-pagination
            :current-page="currentPage"
            :page-size="pageSize"
            :page-sizes="[6, 12, 24]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            @current-change="async (p)=>{ currentPage = p; await fetchPrograms() }"
            @size-change="async (s)=>{ pageSize = s; currentPage = 1; await fetchPrograms() }"
          />
        </div>
      </div>
      <!-- 底部滚动播放：所有留学相关咨询（含图片/标题/前50字） -->
      <div class="marquee-section" v-if="marqueeItems && marqueeItems.length">
        <div class="marquee-viewport">
          <div class="marquee-track">
            <div class="marquee-item" v-for="(item, idx) in marqueeItems" :key="'marq1-' + idx">
              <img class="mi-image" :src="resolveImage(item.imageUrl || item.image)" :alt="item.title" @error="handleImageError" />
              <div class="mi-text">
                <div class="mi-title">{{ item.title }}</div>
                <div class="mi-desc">{{ truncate(item.description, 50) }}</div>
              </div>
            </div>
            <!-- 复制一份实现无缝滚动 -->
            <div class="marquee-item" v-for="(item, idx) in marqueeItems" :key="'marq2-' + idx">
              <img class="mi-image" :src="resolveImage(item.imageUrl || item.image)" :alt="item.title" @error="handleImageError" />
              <div class="mi-text">
                <div class="mi-title">{{ item.title }}</div>
                <div class="mi-desc">{{ truncate(item.description, 50) }}</div>
              </div>
            </div>
          </div>
        </div>
      </div>

    </div>
      <!-- 联系我们 -->
      <ContactUs />
      <!-- 项目详细信息弹窗（单实例） -->
      <el-dialog v-model="detailVisible" title="项目详细信息" width="600px">
<!--
        <div v-if="selectedProgram">
          <h3 style="margin:0 0 8px">{{ selectedProgram.title }}</h3>
          <img :src="resolveImage(selectedProgram.imageUrl || selectedProgram.image)" alt="" style="width:100%;height:200px;object-fit:cover;border-radius:6px;margin-bottom:12px" @error="handleImageError" />
          <p style="white-space:pre-wrap;line-height:1.6">{{ selectedProgram.description }}</p>
          <div v-if="selectedProgram.tags" style="margin-top:8px">
            <el-tag v-for="(tag, i) in JSON.parse(selectedProgram.tags || '[]')" :key="i" size="small" class="program-tag">{{ tag }}</el-tag>
          </div>
        </div>
        <template #footer>
          <span class="dialog-footer">
            <el-button type="primary" @click="detailVisible = false"> </el-button>
          </span>
        </template>
-->
      </el-dialog>


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

/* 底部无缝横向滚动展示（高性能：仅 transform 动画） */
.marquee-section { margin: 20px 0 10px; background: #fff; border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,0.04); padding: 10px 0; }
.marquee-viewport { overflow: hidden; width: 100%; }
.marquee-track { display: flex; gap: 16px; will-change: transform; animation: marquee-left 40s linear infinite; }
.marquee-item { flex: 0 0 auto; display: flex; align-items: center; gap: 12px; padding: 8px 12px; border: 1px solid #f0f0f0; border-radius: 8px; background: #fafafa; min-width: clamp(260px, 28vw, 420px); }
.mi-image { width: 56px; height: 56px; object-fit: cover; border-radius: 6px; }
.mi-text { display: flex; flex-direction: column; min-width: 0; }
.mi-title { font-size: 14px; color: #333; font-weight: 600; line-height: 1.2; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.mi-desc { font-size: 12px; color: #666; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
@keyframes marquee-left { 0% { transform: translateX(0); } 100% { transform: translateX(-50%); } }


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
