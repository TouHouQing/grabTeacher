<script setup lang="ts">
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'

// 定义组件名称
defineOptions({
  name: 'MobileSearch'
})

const router = useRouter()

// 搜索相关状态
const isSearchVisible = ref(false)
const searchQuery = ref('')
const searchHistory = ref<string[]>([])

// 热门搜索词
const hotSearches = ref([
  '数学', '英语', '物理', '化学', '语文',
  '初中', '高中', '小学', '一对一', '在线课程'
])

// 搜索建议
const searchSuggestions = computed(() => {
  if (!searchQuery.value) return []

  const suggestions = [
    '数学一对一辅导',
    '英语口语训练',
    '物理实验课程',
    '化学基础知识',
    '语文阅读理解',
    '初中数学函数',
    '高中英语语法',
    '小学语文作文'
  ]

  return suggestions.filter(item =>
    item.toLowerCase().includes(searchQuery.value.toLowerCase())
  ).slice(0, 5)
})

// 显示搜索界面
const showSearch = () => {
  isSearchVisible.value = true
}

// 隐藏搜索界面
const hideSearch = () => {
  isSearchVisible.value = false
  searchQuery.value = ''
}

// 执行搜索
const performSearch = (query?: string) => {
  const searchTerm = query || searchQuery.value
  if (!searchTerm.trim()) return

  // 添加到搜索历史
  if (!searchHistory.value.includes(searchTerm)) {
    searchHistory.value.unshift(searchTerm)
    if (searchHistory.value.length > 10) {
      searchHistory.value = searchHistory.value.slice(0, 10)
    }
  }

  // 执行搜索逻辑（这里可以根据实际需求调整）
  router.push(`/search?q=${encodeURIComponent(searchTerm)}`)
  hideSearch()
}

// 清除搜索历史
const clearHistory = () => {
  searchHistory.value = []
}

// 删除单个历史记录
const removeHistoryItem = (index: number) => {
  searchHistory.value.splice(index, 1)
}

// 暴露给父组件的方法
defineExpose({
  showSearch,
  hideSearch
})
</script>

<template>
  <div class="mobile-search-wrapper">
    <!-- 搜索按钮 -->
    <el-button
      size="small"
      text
      @click="showSearch"
      class="search-trigger mobile-only"
    >
      <el-icon :size="18">
        <Search />
      </el-icon>
    </el-button>

    <!-- 搜索遮罩层 -->
    <div
      v-if="isSearchVisible"
      class="search-overlay"
      @click="hideSearch"
    ></div>

    <!-- 搜索界面 -->
  <div class="mobile-search" :class="{ 'search-visible': isSearchVisible }">
    <div class="search-header">
      <div class="search-input-container">
        <el-input
          v-model="searchQuery"
          placeholder="搜索课程、教师..."
          size="large"
          @keyup.enter="performSearch()"
          class="search-input"
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <el-button
        text
        @click="hideSearch"
        class="cancel-btn"
      >
        取消
      </el-button>
    </div>

    <div class="search-content">
      <!-- 搜索建议 -->
      <div v-if="searchQuery && searchSuggestions.length" class="search-suggestions">
        <div class="section-title">搜索建议</div>
        <div class="suggestion-list">
          <div
            v-for="suggestion in searchSuggestions"
            :key="suggestion"
            class="suggestion-item"
            @click="performSearch(suggestion)"
          >
            <el-icon><Search /></el-icon>
            <span>{{ suggestion }}</span>
          </div>
        </div>
      </div>

      <!-- 搜索历史 -->
      <div v-if="!searchQuery && searchHistory.length" class="search-history">
        <div class="section-header">
          <div class="section-title">搜索历史</div>
          <el-button
            text
            size="small"
            @click="clearHistory"
            class="clear-btn"
          >
            清除
          </el-button>
        </div>
        <div class="history-list">
          <div
            v-for="(item, index) in searchHistory"
            :key="index"
            class="history-item"
          >
            <span @click="performSearch(item)">{{ item }}</span>
            <el-icon
              @click="removeHistoryItem(index)"
              class="remove-icon"
            >
              <Close />
            </el-icon>
          </div>
        </div>
      </div>

      <!-- 热门搜索 -->
      <div v-if="!searchQuery" class="hot-searches">
        <div class="section-title">热门搜索</div>
        <div class="hot-tags">
          <el-tag
            v-for="tag in hotSearches"
            :key="tag"
            @click="performSearch(tag)"
            class="hot-tag"
          >
            {{ tag }}
          </el-tag>
        </div>
      </div>
    </div>
  </div>
  </div>
</template>

<style scoped>
.search-trigger {
  padding: 8px;
}

.search-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 1998;
}

.mobile-search {
  position: fixed;
  top: -100%;
  left: 0;
  right: 0;
  height: 100vh;
  background-color: #fff;
  z-index: 1999;
  transition: top 0.3s ease;
  display: flex;
  flex-direction: column;
}

.mobile-search.search-visible {
  top: 0;
}

.search-header {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e6e6e6;
  background-color: #fff;
}

.search-input-container {
  flex: 1;
  margin-right: 12px;
}

.search-input {
  border-radius: 20px;
}

.cancel-btn {
  color: #409eff;
  font-size: 16px;
  padding: 0 8px;
}

.search-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
}

.section-title {
  font-size: 14px;
  font-weight: 500;
  color: #666;
  margin-bottom: 12px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.clear-btn {
  color: #999;
  font-size: 12px;
}

.search-suggestions {
  margin-bottom: 24px;
}

.suggestion-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.suggestion-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  border-radius: 6px;
  transition: background-color 0.2s;
}

.suggestion-item:hover {
  background-color: #f5f7fa;
}

.suggestion-item .el-icon {
  margin-right: 12px;
  color: #999;
  font-size: 16px;
}

.search-history {
  margin-bottom: 24px;
}

.history-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.history-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 12px;
  background-color: #f5f7fa;
  border-radius: 6px;
  margin-bottom: 6px;
}

.history-item span {
  flex: 1;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}

.remove-icon {
  color: #999;
  font-size: 14px;
  cursor: pointer;
  padding: 4px;
}

.remove-icon:hover {
  color: #f56c6c;
}

.hot-searches {
  margin-bottom: 24px;
}

.hot-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.hot-tag {
  cursor: pointer;
  border: 1px solid #e6e6e6;
  background-color: #fff;
  color: #666;
  transition: all 0.2s;
}

.hot-tag:hover {
  border-color: #409eff;
  color: #409eff;
}

@media (max-width: 768px) {
  .mobile-search {
    display: flex;
  }
}

@media (min-width: 769px) {
  .mobile-search {
    display: none;
  }
}
</style>
