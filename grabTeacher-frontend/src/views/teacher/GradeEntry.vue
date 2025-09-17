<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { bookingAPI } from '../../utils/api'
import { lessonGradeAPI } from '../../utils/api'

interface ScheduleRow {
  id: number
  scheduledDate: string
  startTime: string
  endTime: string
  courseTitle?: string
  subjectName?: string
  studentId?: number
  studentName?: string
  status?: string
}

// 过滤与分页
const dateRange = ref<[string, string] | null>(null)
const allTime = ref(false)
const subjectFilter = ref<string>('')
const studentFilter = ref<string>('')

const loading = ref(false)
const schedules = ref<ScheduleRow[]>([])

// 成绩录入表单缓存（按 scheduleId）
const formMap = reactive<Record<number, { score: number | null; comment: string; testName?: string }>>({})

// 图表相关
const chartVisible = ref(false)
const chartStudentId = ref<number | null>(null)
const chartSubjectName = ref<string>('')
const chartType = ref<'line' | 'bar'>('line')
const chartPage = ref(1)
const chartPageSize = ref(10)
const chartData = ref<{ lesson: string; date: string; score: number; comment: string }[]>([])
const hasEcharts = computed(() => Boolean((window as any).echarts))

const filteredRows = computed(() => {
  return schedules.value.filter(r => {
    const okSubject = subjectFilter.value ? (r.subjectName || '').includes(subjectFilter.value) : true
    const okStudent = studentFilter.value ? (r.studentName || '').includes(studentFilter.value) : true
    return okSubject && okStudent
  })
})

const initDefaultDateRange = () => {
  const today = new Date()
  const start = new Date(today.getTime() - 30 * 24 * 60 * 60 * 1000)
  const end = today
  const fmt = (d: Date) => d.toISOString().split('T')[0]
  dateRange.value = [fmt(start), fmt(end)]
}

const loadSchedules = async () => {
  try {
    loading.value = true
    if (!dateRange.value) initDefaultDateRange()
    const [defaultStart, defaultEnd] = dateRange.value!
    const startDate = allTime.value ? '1970-01-01' : defaultStart
    const endDate = allTime.value ? '2099-12-31' : defaultEnd
    const res = await bookingAPI.getTeacherSchedules({ startDate, endDate })
    if (res.success && Array.isArray(res.data)) {
      schedules.value = res.data
      // 初始化表单缓存
      for (const s of schedules.value) {
        if (!formMap[s.id]) formMap[s.id] = { score: null, comment: '', testName: '' }
      }
    } else {
      schedules.value = []
      ElMessage.error(res.message || '获取课程安排失败')
    }
  } catch (e) {
    ElMessage.error('获取课程安排失败，请稍后重试')
  } finally {
    loading.value = false
  }
}

const submitGrade = async (row: ScheduleRow) => {
  try {
    if (!row.studentId) {
      ElMessage.warning('缺少学生ID，无法录入成绩')
      return
    }
    const f = formMap[row.id]
    if (f.score == null) {
      ElMessage.warning('请填写成绩（0-100）')
      return
    }

    // 二次确认，避免误操作
    await ElMessageBox.confirm(`确认为学生【${row.studentName}】录入成绩 ${f.score} 分？`, '确认', { type: 'warning' })

    // 注意：后端暂不支持“测试名称”字段，当前仅提交成绩与教师评语
    const res = await lessonGradeAPI.create({
      scheduleId: row.id,
      studentId: row.studentId!,
      score: Number(f.score),
      teacherComment: f.comment || ''
    })
    if (res.success) {
      ElMessage.success('成绩录入成功')
      // 刷新图表
      if (row.subjectName) openChart(row.studentId!, row.subjectName)
    } else {
      ElMessage.error(res.message || '成绩录入失败')
    }
  } catch (e: any) {
    if (e === 'cancel') return
    ElMessage.error(e?.message || '成绩录入失败')
  }
}

const openChart = async (studentId: number, subjectName: string) => {
  chartStudentId.value = studentId
  chartSubjectName.value = subjectName
  chartVisible.value = true
  chartPage.value = 1
  await loadChartData()
  await nextTick()
  initEcharts()
}

const handleViewChart = async (row: ScheduleRow) => {
  if (!row.studentId) {
    ElMessage.warning('该课程缺少学生信息，无法展示图表')
    return
  }
  if (!row.subjectName || row.subjectName.trim() === '') {
    ElMessage.warning('该课程缺少科目信息，无法展示图表')
    return
  }
  await openChart(row.studentId, row.subjectName)
}

const handleTypeChange = () => {
  nextTick(() => initEcharts())
}

const loadChartData = async () => {
  if (!chartStudentId.value || !chartSubjectName.value) return
  const res = await lessonGradeAPI.getStudentSubjectChartData(chartStudentId.value, chartSubjectName.value)
  if (res.success && Array.isArray(res.data)) {
    // 排序并映射
    const sorted = res.data.sort((a: any, b: any) => {
      const da = new Date(`${a.scheduledDate} ${a.startTime}`)
      const db = new Date(`${b.scheduledDate} ${b.startTime}`)
      return da.getTime() - db.getTime()
    })
    chartData.value = sorted.map((it: any, idx: number) => ({
      lesson: `第${idx + 1}课`,
      date: it.scheduledDate,
      score: Number(it.score || 0),
      comment: it.teacherComment || ''
    }))
  } else {
    chartData.value = []
  }
}

const pagedChartData = computed(() => {
  const start = (chartPage.value - 1) * chartPageSize.value
  return chartData.value.slice(start, start + chartPageSize.value)
})

const initEcharts = () => {
  const dom = document.getElementById('teacherGradeChart') as HTMLElement | null
  if (!dom || pagedChartData.value.length === 0) return
  const echarts = (window as any).echarts
  if (!echarts) return
  const chart = echarts.init(dom)
  const option = {
    title: { text: `${chartSubjectName.value} 成绩趋势`, left: 'center', textStyle: { fontSize: 16 } },
    tooltip: {
      trigger: 'axis',
      formatter: (params: any[]) => {
        const p = params[0]
        const d = pagedChartData.value[p.dataIndex]
        return `<div><strong>${d.lesson}</strong><br/>日期: ${d.date}<br/>成绩: ${d.score}分${d.comment ? `<br/>评价: ${d.comment}` : ''}</div>`
      }
    },
    grid: { left: 40, right: 20, bottom: 60, top: 50 },
    xAxis: { type: 'category', data: pagedChartData.value.map(d => d.lesson), axisLabel: { rotate: 40, fontSize: 12 } },
    yAxis: { type: 'value', min: 0, max: 100, axisLabel: { formatter: '{value}分', fontSize: 12 } },
    series: [
      chartType.value === 'line'
        ? { type: 'line', name: '成绩', data: pagedChartData.value.map(d => d.score), smooth: true, lineStyle: { width: 3 }, itemStyle: { borderWidth: 2 } }
        : { type: 'bar', name: '成绩', data: pagedChartData.value.map(d => d.score), itemStyle: { color: '#409eff' } }
    ]
  }
  chart.setOption(option)
  window.addEventListener('resize', () => chart.resize(), { passive: true })
}

onMounted(() => {
  initDefaultDateRange()
  loadSchedules()
})
</script>

<template>
  <div class="grade-entry">
    <h2>成绩录入</h2>

    <div class="filters">
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
        :disabled="allTime"
        @change="loadSchedules"
      />
      <el-checkbox v-model="allTime" @change="loadSchedules">全部时间</el-checkbox>
      <el-input v-model="subjectFilter" placeholder="按科目筛选" clearable style="max-width: 200px" />
      <el-input v-model="studentFilter" placeholder="按学生筛选" clearable style="max-width: 200px" />
      <el-button type="primary" @click="loadSchedules" :loading="loading">刷新</el-button>
    </div>

    <div class="table-wrap">
      <el-table :data="filteredRows" v-loading="loading" style="width: 100%">
        <el-table-column prop="scheduledDate" label="日期" width="120" />
        <el-table-column label="时间" width="140">
          <template #default="scope">
            {{ scope.row.startTime }}-{{ scope.row.endTime }}
          </template>
        </el-table-column>
        <el-table-column prop="courseTitle" label="课程" />
        <el-table-column prop="subjectName" label="科目" width="120" />
        <el-table-column prop="studentName" label="学生" width="140" />
        <el-table-column label="成绩(0-100)" width="160">
          <template #default="{ row }">
            <el-input-number v-model="formMap[row.id].score" :min="0" :max="100" :step="1" :precision="0" />
          </template>
        </el-table-column>
        <el-table-column label="教师评语">
          <template #default="{ row }">
            <el-input v-model="formMap[row.id].comment" placeholder="请输入评语，选填" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="submitGrade(row)">保存成绩</el-button>
            <el-button size="small" @click="handleViewChart(row)">查看图表</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-drawer v-model="chartVisible" title="成绩趋势" size="70%">
      <template #header>
        <div class="chart-header">
          <div>
            <strong>学生ID：</strong>{{ chartStudentId }}
            <span style="margin-left: 12px"><strong>科目：</strong>{{ chartSubjectName }}</span>
          </div>
          <div>
            <el-radio-group v-model="chartType" size="small" @change="handleTypeChange">
              <el-radio-button label="line">折线</el-radio-button>
              <el-radio-button label="bar">柱状</el-radio-button>
            </el-radio-group>
          </div>
        </div>
      </template>
      <div v-if="chartData.length === 0" class="empty-chart">
        <el-empty description="暂无成绩数据" />
      </div>
      <div v-else>
        <template v-if="hasEcharts">
          <div id="teacherGradeChart" style="width: 100%; height: 420px;"></div>
          <div class="chart-pagination">
            <el-pagination
              :current-page="chartPage"
              :page-size="chartPageSize"
              layout="total, prev, pager, next"
              :total="chartData.length"
              @current-change="(p)=>{ chartPage = p; nextTick(()=>initEcharts()) }"
            />
          </div>
        </template>
        <template v-else>
          <el-alert title="未检测到 ECharts，以下为表格展示" type="warning" show-icon style="margin-bottom: 8px;" />
          <el-table :data="pagedChartData" size="small">
            <el-table-column prop="lesson" label="课次" width="100" />
            <el-table-column prop="date" label="日期" width="140" />
            <el-table-column prop="score" label="成绩" width="100" />
            <el-table-column prop="comment" label="评语" />
          </el-table>
          <div class="chart-pagination">
            <el-pagination
              :current-page="chartPage"
              :page-size="chartPageSize"
              layout="total, prev, pager, next"
              :total="chartData.length"
              @current-change="(p)=>{ chartPage = p }"
            />
          </div>
        </template>
      </div>
    </el-drawer>
  </div>
</template>

<style scoped>
.grade-entry { background: #fff; padding: 16px; border-radius: 8px; }
.filters { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; flex-wrap: wrap; }
.chart-header { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.chart-pagination { display: flex; justify-content: center; margin-top: 8px; }

/* 表格横向滚动兜底与小屏适配 */
.table-wrap { width: 100%; overflow-x: auto; }
.table-wrap :deep(table) { min-width: 860px; }

@media (max-width: 768px) {
  .grade-entry { padding: 12px; }
  .filters { gap: 8px; }
  .filters :deep(.el-input),
  .filters :deep(.el-select),
  .filters :deep(.el-date-editor) { width: 100% !important; max-width: 100%; }
  :deep(.el-drawer) { width: 100vw !important; }
  .chart-header { flex-direction: column; gap: 8px; align-items: flex-start; }
}
</style>

