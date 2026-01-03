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
  padding: 15px;
  background-color: rgba(253, 253, 209, 0.98);
  height: 100%; /* 强制占满父容器（左侧工具区）高度 */
  box-sizing: border-box;
  display: flex;
  flex-direction: column; /* 纵向布局，让内容撑满 */
  overflow: hidden;
}

/* 标题：与主页面标题样式完全对齐 */
.library-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #8a7d5e;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-dot {
  display: inline-block;
  width: 8px;
  height: 16px;
  background-color: #f0c880;
  border-radius: 4px;
}

/* 搜索区域：与主页面输入组件样式统一 */
.search-container {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}
.search-input {
  flex: 1;
  --el-input-border-color: #f0e8d0 !important;
  --el-input-hover-border-color: #f0c880 !important;
  --el-input-text-color: #8a7d5e !important;
  --el-input-placeholder-color: #a09880 !important;
  border-radius: 22px !important; /* 与主页面输入框圆角一致 */
}
.type-select {
  width: 160px;
  --el-select-border-color: #f0e8d0 !important;
  --el-select-hover-border-color: #f0c880 !important;
  --el-select-text-color: #8a7d5e !important;
  --el-select-placeholder-color: #a09880 !important;
  border-radius: 22px !important; /* 与主页面下拉框圆角一致 */
}

/* 面料展示区：撑满剩余高度 + 滚动适配 */
.fabric-grid {
  flex: 1; /* 撑满容器剩余高度 */
  display: grid;
  grid-template-columns: repeat(2, 1fr); /* 2列布局适配工具区宽度 */
  gap: 12px;
  overflow-y: auto; /* 内容超出时滚动 */
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
  }
}

/* 面料卡片：与主页面成果库卡片样式统一 */
.fabric-item {
  background-color: #fff8e8;
  border: 1px solid #f0e8d0;
  border-radius: 10px;
  overflow: hidden;
  box-shadow: 0 3px 8px rgba(240, 224, 180, 0.12);
  transition: all 0.2s ease;
}
.fabric-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 5px 12px rgba(240, 224, 180, 0.18);
  border-color: #f0c880;
}
.fabric-wrapper {
  cursor: pointer;
}
.fabric-image {
  width: 100%;
  height: 120px;
  object-fit: cover;
}
.fabric-info {
  padding: 8px 10px;
  color: #8a7d5e;
}
.fabric-name {
  font-size: 14px;
  font-weight: 500;
  margin-bottom: 4px;
}
.fabric-type {
  font-size: 12px;
  opacity: 0.9;
}

/* 无数据/加载提示：与主页面风格统一 */
.no-data, .loading {
  grid-column: span 2;
  height: 100%; /* 占满展示区高度 */
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #8a7d5e;
  background-color: #fff8e8;
  border: 1px dashed #f0e8d0;
  border-radius: 10px;
}
.custom-spinner {
  margin: 0 auto 8px;
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #f0c880;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* 弹窗：与主页面弹窗样式统一 */
.fabric-preview-dialog {
  --el-dialog-bg-color: rgba(255, 255, 250, 0.98) !important;
  --el-dialog-border-color: #f0e8d0 !important;
}
.fabric-preview-dialog .el-dialog__header {
  border-bottom: 1px solid #f0e8d0;
  color: #8a7d5e;
}
.preview-content {
  display: flex;
  gap: 20px;
  padding: 10px 0;
}
.preview-image {
  width: 240px;
  height: 240px;
  object-fit: cover;
  border: 1px solid #f0e8d0;
  border-radius: 8px;
}
.preview-details {
  flex: 1;
  color: #8a7d5e;
}
.preview-details h3 {
  font-size: 18px;
  margin-bottom: 16px;
  color: #8a7d5e;
}
.preview-details p {
  line-height: 1.6;
  margin-bottom: 8px;
}
.preview-details .el-button--primary {
  --el-button-bg-color: #f5dca8 !important;
  --el-button-border-color: #f0c880 !important;
  --el-button-text-color: #8a7d5e !important;
  --el-button-hover-bg-color: #f0c880 !important;
  --el-button-hover-text-color: #7a6d4e !important;
  border-radius: 22px !important; /* 与主页面按钮圆角一致 */
}
</style>