<template>
  <div class="teacher-income">
    <h2>收入管理</h2>

    <div class="income-overview">
      <div class="income-card">
        <div class="income-icon">
          <el-icon><Money /></el-icon>
        </div>
        <div class="income-info">
          <div class="income-title">本月收入</div>
          <div class="income-value">¥3600</div>
          <div class="income-compare">较上月 <span class="increase">+12%</span></div>
        </div>
      </div>
      <div class="income-card">
        <div class="income-icon purple">
          <el-icon><CalendarTime /></el-icon>
        </div>
        <div class="income-info">
          <div class="income-title">上月收入</div>
          <div class="income-value">¥3200</div>
          <div class="income-compare">较上上月 <span class="increase">+8%</span></div>
        </div>
      </div>
      <div class="income-card">
        <div class="income-icon green">
          <el-icon><Finished /></el-icon>
        </div>
        <div class="income-info">
          <div class="income-title">今年总收入</div>
          <div class="income-value">¥25600</div>
          <div class="income-compare">较去年同期 <span class="increase">+15%</span></div>
        </div>
      </div>
      <div class="income-card">
        <div class="income-icon orange">
          <el-icon><CircleCheck /></el-icon>
        </div>
        <div class="income-info">
          <div class="income-title">待结算</div>
          <div class="income-value">¥1200</div>
          <div class="income-compare">预计 <span>7月25日</span> 到账</div>
        </div>
      </div>
    </div>

    <!-- 收入图表 -->
    <div class="income-charts">
      <div class="chart-container">
        <div class="chart-header">
          <h3>月收入趋势</h3>
          <el-radio-group v-model="timeRange" size="small">
            <el-radio-button label="month">月度</el-radio-button>
            <el-radio-button label="quarter">季度</el-radio-button>
            <el-radio-button label="year">年度</el-radio-button>
          </el-radio-group>
        </div>
        <div class="chart-content">
          <div class="chart-placeholder">
            <div class="chart-line">
              <div class="chart-bar" style="height: 40%;"></div>
              <div class="chart-label">1月</div>
            </div>
            <div class="chart-line">
              <div class="chart-bar" style="height: 65%;"></div>
              <div class="chart-label">2月</div>
            </div>
            <div class="chart-line">
              <div class="chart-bar" style="height: 55%;"></div>
              <div class="chart-label">3月</div>
            </div>
            <div class="chart-line">
              <div class="chart-bar" style="height: 70%;"></div>
              <div class="chart-label">4月</div>
            </div>
            <div class="chart-line">
              <div class="chart-bar" style="height: 50%;"></div>
              <div class="chart-label">5月</div>
            </div>
            <div class="chart-line">
              <div class="chart-bar" style="height: 80%;"></div>
              <div class="chart-label">6月</div>
            </div>
            <div class="chart-line">
              <div class="chart-bar current" style="height: 90%;"></div>
              <div class="chart-label">7月</div>
            </div>
          </div>
        </div>
      </div>
      <div class="chart-container">
        <div class="chart-header">
          <h3>收入构成</h3>
        </div>
        <div class="chart-content">
          <div class="pie-chart">
            <div class="pie-segment" style="--percentage: 65%; --color: #409eff;"></div>
            <div class="pie-segment" style="--percentage: 25%; --color: #67c23a;"></div>
            <div class="pie-segment" style="--percentage: 10%; --color: #e6a23c;"></div>
            <div class="pie-center">总收入</div>
          </div>
          <div class="pie-legend">
            <div class="legend-item">
              <div class="legend-color" style="background-color: #409eff;"></div>
              <div class="legend-text">一对一课程 (65%)</div>
            </div>
            <div class="legend-item">
              <div class="legend-color" style="background-color: #67c23a;"></div>
              <div class="legend-text">小班课程 (25%)</div>
            </div>
            <div class="legend-item">
              <div class="legend-color" style="background-color: #e6a23c;"></div>
              <div class="legend-text">资料销售 (10%)</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 收入明细 -->
    <div class="income-details">
      <div class="details-header">
        <h3>收入明细</h3>
        <div class="filter-actions">
          <el-date-picker
            v-model="dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            size="small"
          />
          <el-select v-model="incomeType" placeholder="收入类型" size="small">
            <el-option label="全部类型" value="all" />
            <el-option label="一对一课程" value="oneToOne" />
            <el-option label="小班课程" value="smallClass" />
            <el-option label="资料销售" value="materials" />
          </el-select>
          <el-button type="primary" size="small">筛选</el-button>
        </div>
      </div>
      <div class="table-container">
        <el-table :data="incomeRecords" style="width: 100%">
          <el-table-column prop="date" label="日期" width="180" />
          <el-table-column prop="type" label="类型">
            <template #default="scope">
              <el-tag
                :type="scope.row.type === '一对一课程' ? 'primary' :
                      scope.row.type === '小班课程' ? 'success' : 'warning'"
                size="small"
              >
                {{ scope.row.type }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="course" label="课程名称" />
          <el-table-column prop="student" label="学生" />
          <el-table-column prop="amount" label="金额">
            <template #default="scope">
              <span class="income-amount">¥{{ scope.row.amount }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="status" label="状态">
            <template #default="scope">
              <el-tag
                :type="scope.row.status === '已到账' ? 'success' : 'warning'"
                size="small"
              >
                {{ scope.row.status }}
              </el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div class="pagination">
          <el-pagination
            layout="prev, pager, next"
            :total="100"
            :page-size="10"
            @current-change="handlePageChange"
          />
        </div>
      </div>
    </div>

    <!-- 收入设置 -->
    <div class="income-settings">
      <h3>收款设置</h3>
      <el-form label-position="top">
        <el-form-item label="收款方式">
          <el-radio-group v-model="paymentMethod">
            <el-radio label="alipay">支付宝</el-radio>
            <el-radio label="wechat">微信</el-radio>
            <el-radio label="bank">银行卡</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="收款账号">
          <el-input v-model="paymentAccount" placeholder="请输入收款账号" />
        </el-form-item>
        <el-form-item label="实名信息">
          <el-input v-model="realName" placeholder="请输入您的真实姓名" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary">保存设置</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

// 收入统计时间范围
const timeRange = ref('month')

// 收入明细筛选
const dateRange = ref([])
const incomeType = ref('all')

// 收款设置
const paymentMethod = ref('alipay')
const paymentAccount = ref('example@alipay.com')
const realName = ref('张老师')

// 模拟收入记录数据
const incomeRecords = ref([
  {
    date: '2023-07-15',
    type: '一对一课程',
    course: '高中数学 - 函数与导数',
    student: '张同学',
    amount: 350,
    status: '已到账'
  },
  {
    date: '2023-07-12',
    type: '一对一课程',
    course: '高中物理 - 力学与电学',
    student: '李同学',
    amount: 350,
    status: '已到账'
  },
  {
    date: '2023-07-10',
    type: '小班课程',
    course: '高中数学 - 三角函数',
    student: '4人小班',
    amount: 800,
    status: '已到账'
  },
  {
    date: '2023-07-08',
    type: '一对一课程',
    course: '高中数学 - 函数与导数',
    student: '张同学',
    amount: 350,
    status: '已到账'
  },
  {
    date: '2023-07-05',
    type: '一对一课程',
    course: '高中物理 - 力学与电学',
    student: '李同学',
    amount: 350,
    status: '已到账'
  },
  {
    date: '2023-07-03',
    type: '资料销售',
    course: '高中数学必刷题',
    student: '高中学生群',
    amount: 600,
    status: '已到账'
  },
  {
    date: '2023-07-21',
    type: '一对一课程',
    course: '高中生物 - 基因与遗传',
    student: '王同学',
    amount: 350,
    status: '待结算'
  },
  {
    date: '2023-07-20',
    type: '小班课程',
    course: '高中物理 - 电磁学基础',
    student: '3人小班',
    amount: 600,
    status: '待结算'
  },
  {
    date: '2023-07-18',
    type: '一对一课程',
    course: '高中英语 - 阅读理解技巧',
    student: '赵同学',
    amount: 350,
    status: '待结算'
  },
  {
    date: '2023-07-16',
    type: '一对一课程',
    course: '高中化学 - 有机化学',
    student: '钱同学',
    amount: 350,
    status: '待结算'
  }
])

// 分页处理
const handlePageChange = (page: number) => {
  console.log('当前页:', page)
}
</script>

<script lang="ts">
export default {
  name: 'TeacherIncome'
}
</script>

<style scoped>
.teacher-income {
  padding: 20px;
}

h2 {
  margin-bottom: 20px;
  font-size: 24px;
  color: #333;
}

h3 {
  margin-top: 0;
  font-size: 18px;
  color: #333;
}

/* 收入概览 */
.income-overview {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 30px;
}

.income-card {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  display: flex;
  align-items: center;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.income-icon {
  width: 60px;
  height: 60px;
  border-radius: 8px;
  background-color: #ecf5ff;
  color: #409EFF;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  margin-right: 15px;
}

.income-icon.purple {
  background-color: #f5f0ff;
  color: #8c6eff;
}

.income-icon.green {
  background-color: #f0f9eb;
  color: #67c23a;
}

.income-icon.orange {
  background-color: #fdf6ec;
  color: #e6a23c;
}

.income-info {
  flex: 1;
}

.income-title {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.income-value {
  font-size: 24px;
  font-weight: bold;
  color: #333;
  margin-bottom: 8px;
}

.income-compare {
  font-size: 12px;
  color: #666;
}

.increase {
  color: #67c23a;
}

.decrease {
  color: #f56c6c;
}

/* 收入图表 */
.income-charts {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 30px;
}

.chart-container {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.chart-content {
  height: 300px;
  position: relative;
}

/* 模拟柱状图 */
.chart-placeholder {
  height: 100%;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 30px;
}

.chart-line {
  width: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.chart-bar {
  width: 40px;
  background-color: #409eff;
  border-radius: 4px 4px 0 0;
  transition: height 0.3s ease;
}

.chart-bar.current {
  background-color: #67c23a;
}

.chart-label {
  margin-top: 8px;
  font-size: 12px;
  color: #666;
}

/* 模拟饼图 */
.pie-chart {
  width: 200px;
  height: 200px;
  position: relative;
  border-radius: 50%;
  background-color: #f5f7fa;
  margin: 0 auto;
  overflow: hidden;
}

.pie-segment {
  position: absolute;
  width: 100%;
  height: 100%;
  transform-origin: 50% 50%;
  background: conic-gradient(
    var(--color) 0% calc(var(--percentage) * 1%),
    transparent calc(var(--percentage) * 1%) 100%
  );
}

.pie-segment:nth-child(2) {
  transform: rotate(calc(var(--percentage, 0) * 3.6deg));
}

.pie-segment:nth-child(3) {
  transform: rotate(calc((var(--percentage, 0) + var(--percentage, 0)) * 3.6deg));
}

.pie-center {
  position: absolute;
  width: 100px;
  height: 100px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: white;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: bold;
  color: #333;
}

.pie-legend {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.legend-item {
  display: flex;
  align-items: center;
}

.legend-color {
  width: 12px;
  height: 12px;
  border-radius: 2px;
  margin-right: 8px;
}

.legend-text {
  font-size: 12px;
  color: #666;
}

/* 收入明细 */
.income-details {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
  margin-bottom: 30px;
}

.details-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.filter-actions {
  display: flex;
  gap: 10px;
}

.income-amount {
  font-weight: bold;
  color: #67c23a;
}

.table-container {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 收入设置 */
.income-settings {
  background-color: #fff;
  border-radius: 8px;
  padding: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.05);
}

/* 响应式布局 */
@media (max-width: 1200px) {
  .income-overview {
    grid-template-columns: repeat(2, 1fr);
  }

  .income-charts {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .income-overview {
    grid-template-columns: 1fr;
  }

  .details-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 10px;
  }

  .filter-actions {
    flex-direction: column;
    width: 100%;
  }
}
</style>
