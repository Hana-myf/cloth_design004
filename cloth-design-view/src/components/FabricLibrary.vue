<template>
  <div class="fabric-library">
    <!-- 标题：与主页面标题风格对齐 -->
    <h4 class="library-title">
      <span class="title-dot"></span>面料库
    </h4>
    
    <!-- 搜索区域：与主页面输入组件样式统一 -->
    <div class="search-container">
      <el-input
        v-model="searchQuery.name"
        placeholder="搜索面料名称"
        clearable
        class="search-input"
        @clear="handleSearch"
        @input="handleSearchDebounced"
      ></el-input>
      
      <el-select
        v-model="searchQuery.type"
        placeholder="选择面料类型"
        clearable
        class="type-select"
        @change="handleSearch"
      >
        <el-option
          v-for="type in allTypes"
          :key="type"
          :label="type"
          :value="type"
        ></el-option>
      </el-select>
    </div>
    
    <!-- 面料展示区：撑满剩余高度 + 滚动适配 -->
    <div class="fabric-grid">
      <div v-for="(fabric, index) in fabrics" :key="fabric.id" class="fabric-item">
        <div 
          class="fabric-wrapper"
          @click="handleFabricClick(fabric)"
        >
          <img 
            :src="fabric.imageUrl" 
            alt="面料图片" 
            class="fabric-image"
            @error="handleImageError(index)" 
          >
          <div class="fabric-info">
            <div class="fabric-name">{{ fabric.name }}</div>
            <div class="fabric-type">{{ fabric.type }}</div>
          </div>
        </div>
      </div>
      
      <!-- 无数据提示 -->
      <div v-if="fabrics.length === 0 && !loading" class="no-data">
        <p>暂无面料数据</p>
      </div>
      
      <!-- 加载中提示：与主页面加载样式统一 -->
      <div v-if="loading" class="loading">
        <div class="custom-spinner"></div>
        <p>加载中...</p>
      </div>
    </div>

    <!-- 面料详情弹窗：与主页面弹窗风格统一 -->
    <el-dialog
      :visible.sync="previewVisible"
      append-to-body
      close-on-click-modal
      show-close
      class="fabric-preview-dialog"
      width="60%"
    >
      <div class="preview-content">
        <img 
          :src="currentFabric.imageUrl" 
          alt="面料预览" 
          class="preview-image"
          @error="handlePreviewImageError"
        >
        <div class="preview-details">
          <h3>{{ currentFabric.name }}</h3>
          <p><strong>类型：</strong>{{ currentFabric.type }}</p>
          <p><strong>描述：</strong>{{ currentFabric.description || '暂无描述' }}</p>
          <el-button 
            type="primary" 
            @click="handleSelectFabric"
            style="margin-top: 16px;"
          >
            选择该面料
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { debounce } from 'lodash';

export default {
  name: 'FabricLibrary',
  data() {
    return {
      fabrics: [],
      searchQuery: {
        name: '',
        type: ''
      },
      allTypes: [],
      previewVisible: false,
      currentFabric: {},
      loading: false
    };
  },
  created() {
    this.fetchAllTypes();
    this.fetchFabrics();
  },
  methods: {
    handleSearchDebounced: debounce(function() {
      this.handleSearch();
    }, 300),
    
    async fetchFabrics() {
      this.loading = true;
      try {
        const params = {
          ...this.searchQuery,
          current: 1,
          size: 20 // 增加每页加载数量，让内容更饱满
        };
        const response = await this.$axios.post('/fabric/query', params);
        if (response.data.code === 200) {
          this.fabrics = response.data.data.fabricList || [];
        } else {
          this.$message.error('获取面料失败：' + (response.data.msg || '未知错误'));
          this.fabrics = [];
        }
      } catch (error) {
        this.$message.error('获取面料列表失败，请重试');
        console.error(error);
        this.fabrics = [];
      } finally {
        this.loading = false;
      }
    },
    
    async fetchAllTypes() {
      try {
        const response = await this.$axios.get('/fabric/types');
        if (response.data.code === 200) {
          this.allTypes = response.data.data || [];
        } else {
          this.$message.error('获取面料类型失败：' + (response.data.msg || '未知错误'));
        }
      } catch (error) {
        this.$message.error('获取面料类型失败，请重试');
        console.error(error);
      }
    },
    
    handleSearch() {
      this.fetchFabrics();
    },
    
    handleFabricClick(fabric) {
      this.currentFabric = { ...fabric };
      this.previewVisible = true;
    },
    
    handleSelectFabric() {
      this.$emit('select', this.currentFabric);
      this.previewVisible = false;
      this.$message.success(`已选择面料：${this.currentFabric.name}`);
    },
    
    handleImageError(index) {
      this.$set(this.fabrics[index], 'imageUrl', require('@/assets/fabric-placeholder.png'));
    },
    
    handlePreviewImageError() {
      this.currentFabric.imageUrl = require('@/assets/fabric-placeholder.png');
    }
  }
};
</script>

<style scoped>
/* 面料库容器：撑满左侧工具区高度 + 主页面风格统一 */
.fabric-library {
  padding: 20px;
  background-color: rgba(253, 250, 245, 0.98);
  height: 100%; /* 强制占满父容器（左侧工具区）高度 */
  box-sizing: border-box;
  display: flex;
  flex-direction: column; /* 纵向布局，让内容撑满 */
  overflow: hidden;
  border-radius: 16px;
  box-shadow: 0 6px 18px rgba(240, 224, 180, 0.15);
  border: 1px solid #f0e8d0;
}

/* 标题：与主页面标题样式完全对齐 */
.library-title {
  margin: 0 0 20px;
  font-size: 18px;
  color: #3d2f24;
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
  letter-spacing: 1px;
}
.title-dot {
  display: inline-block;
  width: 10px;
  height: 20px;
  background-color: #e2b86e;
  border-radius: 5px;
  box-shadow: 0 2px 4px rgba(226, 184, 110, 0.3);
}

/* 搜索区域：与主页面输入组件样式统一 */
.search-container {
  display: flex;
  gap: 15px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}
.search-input {
  flex: 1;
  min-width: 200px;
  --el-input-border-color: #f0e8d0 !important;
  --el-input-hover-border-color: #e2b86e !important;
  --el-input-text-color: #3d2f24 !important;
  --el-input-placeholder-color: #8c7662 !important;
  border-radius: 25px !important; /* 与主页面输入框圆角一致 */
  box-shadow: 0 2px 8px rgba(240, 224, 180, 0.1) !important;
  transition: all 0.3s ease !important;
}
.type-select {
  width: 180px;
  --el-select-border-color: #f0e8d0 !important;
  --el-select-hover-border-color: #e2b86e !important;
  --el-select-text-color: #3d2f24 !important;
  --el-select-placeholder-color: #8c7662 !important;
  border-radius: 25px !important; /* 与主页面下拉框圆角一致 */
  box-shadow: 0 2px 8px rgba(240, 224, 180, 0.1) !important;
  transition: all 0.3s ease !important;
}

/* 面料展示区：撑满剩余高度 + 滚动适配 */
.fabric-grid {
  flex: 1; /* 撑满容器剩余高度 */
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 2列布局适配工具区宽度 */
  gap: 15px;
  overflow-y: auto; /* 内容超出时滚动 */
  padding-right: 5px;
  /* 滚动条样式：与主页面.chat-content滚动条完全一致 */
  &::-webkit-scrollbar {
    width: 8px;
  }
  &::-webkit-scrollbar-track {
    background-color: #fff8e8;
    border-radius: 4px;
  }
  &::-webkit-scrollbar-thumb {
    background-color: #f5dca8;
    border-radius: 4px;
    border: 1px solid #fff8e8;
    transition: background-color 0.2s ease;
  }
  &::-webkit-scrollbar-thumb:hover {
    background-color: #e2b86e;
  }
}

/* 面料卡片：与主页面成果库卡片样式统一 */
.fabric-item {
  background-color: rgba(255, 255, 255, 0.85);
  border: 1px solid #f0e8d0;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 5px 15px rgba(240, 224, 180, 0.1);
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
.fabric-item::before {
  content: '';
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  transition: left 0.6s ease;
}
.fabric-item:hover::before {
  left: 100%;
}
.fabric-item:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 25px rgba(240, 224, 180, 0.2);
  border-color: #e2b86e;
}
.fabric-wrapper {
  cursor: pointer;
  display: flex;
  flex-direction: column;
  height: 100%;
}
.fabric-image {
  width: 100%;
  height: 140px;
  object-fit: cover;
  transition: transform 0.3s ease;
}
.fabric-item:hover .fabric-image {
  transform: scale(1.05);
}
.fabric-info {
  padding: 12px 15px;
  color: #3d2f24;
  background: #fff8e8;
  flex-grow: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.fabric-name {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 6px;
  color: #3d2f24;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.fabric-type {
  font-size: 12px;
  opacity: 0.8;
  color: #8c7662;
  background: rgba(226, 184, 110, 0.1);
  padding: 2px 8px;
  border-radius: 12px;
  align-self: flex-start;
}

/* 无数据/加载提示：与主页面风格统一 */
.no-data, .loading {
  grid-column: span 2;
  height: 100%; /* 占满展示区高度 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8c7662;
  background-color: rgba(255, 255, 250, 0.8);
  border: 2px dashed #f0e8d0;
  border-radius: 16px;
  padding: 40px;
  transition: all 0.3s ease;
}
.no-data:hover, .loading:hover {
  border-color: #e2b86e;
  background-color: rgba(255, 255, 250, 0.9);
}
.custom-spinner {
  margin: 0 auto 12px;
  width: 40px;
  height: 40px;
  border: 3px solid rgba(240, 200, 128, 0.2);
  border-top: 3px solid #e2b86e;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  box-shadow: 0 0 10px rgba(226, 184, 110, 0.3);
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 弹窗：与主页面弹窗样式统一 */
.fabric-preview-dialog {
  --el-dialog-bg-color: rgba(255, 255, 250, 0.98) !important;
  --el-dialog-border-color: #f0e8d0 !important;
  --el-dialog-border-radius: 20px !important;
  box-shadow: 0 20px 60px rgba(74, 58, 42, 0.15) !important;
  overflow: hidden;
}
.fabric-preview-dialog .el-dialog__header {
  border-bottom: 1px solid #f0e8d0;
  color: #3d2f24;
  padding: 20px 24px;
  background: #fff8e8;
}
.fabric-preview-dialog .el-dialog__title {
  font-size: 18px;
  font-weight: 600;
  color: #3d2f24;
}
.preview-content {
  display: flex;
  gap: 30px;
  padding: 30px 24px;
  align-items: flex-start;
}
.preview-image {
  width: 300px;
  height: 300px;
  object-fit: cover;
  border: 2px solid #f0e8d0;
  border-radius: 16px;
  box-shadow: 0 8px 20px rgba(240, 224, 180, 0.15);
  transition: all 0.3s ease;
}
.preview-image:hover {
  transform: scale(1.02);
  box-shadow: 0 12px 25px rgba(240, 224, 180, 0.2);
  border-color: #e2b86e;
}
.preview-details {
  flex: 1;
  color: #3d2f24;
  background: rgba(255, 255, 250, 0.6);
  padding: 20px;
  border-radius: 16px;
  border: 1px solid #f0e8d0;
  box-shadow: 0 4px 12px rgba(240, 224, 180, 0.1);
}
.preview-details h3 {
  font-size: 20px;
  margin-bottom: 20px;
  color: #3d2f24;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 10px;
}
.preview-details h3::before {
  content: '';
  width: 8px;
  height: 20px;
  background-color: #e2b86e;
  border-radius: 4px;
}
.preview-details p {
  line-height: 1.7;
  margin-bottom: 12px;
  color: #8c7662;
  font-size: 15px;
}
.preview-details p strong {
  color: #3d2f24;
  font-weight: 600;
  margin-right: 8px;
}
.preview-details .el-button--primary {
  --el-button-bg-color: #e2b86e !important;
  --el-button-border-color: #e2b86e !important;
  --el-button-text-color: #fff !important;
  --el-button-hover-bg-color: #d1a55a !important;
  --el-button-hover-text-color: #fff !important;
  --el-button-hover-border-color: #d1a55a !important;
  border-radius: 25px !important; /* 与主页面按钮圆角一致 */
  padding: 10px 24px !important;
  font-size: 14px !important;
  font-weight: 500 !important;
  box-shadow: 0 4px 12px rgba(226, 184, 110, 0.3) !important;
  transition: all 0.3s ease !important;
  margin-top: 20px !important;
}
.preview-details .el-button--primary:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(226, 184, 110, 0.4) !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .fabric-library {
    padding: 15px;
  }
  .search-container {
    flex-direction: column;
  }
  .type-select {
    width: 100%;
  }
  .fabric-grid {
    grid-template-columns: 1fr;
  }
  .no-data, .loading {
    grid-column: span 1;
  }
  .preview-content {
    flex-direction: column;
    align-items: center;
  }
  .preview-image {
    width: 100%;
    max-width: 300px;
  }
}
</style>