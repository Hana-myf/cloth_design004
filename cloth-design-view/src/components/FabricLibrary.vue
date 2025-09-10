<template>
  <div class="fabric-library">
    <h4 class="library-title">面料库</h4>
    
    <!-- 搜索区域 -->
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
    
    <!-- 面料网格展示 -->
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
      
      <!-- 加载中提示 -->
      <div v-if="loading" class="loading">
      <!-- 纯CSS实现的加载动画 -->
     <div class="custom-spinner"></div>
     <p>加载中...</p>
     </div>
    </div>

    <!-- 面料详情预览弹窗 -->
    <el-dialog
      :visible.sync="previewVisible"
      :append-to-body="true"
      :close-on-click-modal="true"
      :show-close="true"
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
import { debounce } from 'lodash'; // 假设使用lodash的防抖函数

export default {
  name: 'FabricLibrary',
  data() {
    return {
      // 面料列表数据
      fabrics: [],
      // 搜索条件
      searchQuery: {
        name: '',
        type: ''
      },
      // 所有面料类型
      allTypes: [],
      // 预览相关
      previewVisible: false,
      currentFabric: {},
      // 加载状态
      loading: false
    };
  },
  created() {
    // 初始化时获取所有面料类型和面料列表
    this.fetchAllTypes();
    this.fetchFabrics();
  },
  methods: {
    // 防抖处理搜索，避免频繁请求
    handleSearchDebounced: debounce(function() {
      this.handleSearch();
    }, 300),
    
    // 获取面料列表
    // 获取面料列表（修改后）
async fetchFabrics() {
  this.loading = true;
  try {
    // 补充分页参数（和管理员一致）
    const params = {
      ...this.searchQuery,
      current: 1, // 至少传递第1页
      size: 10    // 每页10条（可根据需要调整）
    };
    const response = await this.$axios.post('/fabric/query', params);
    // 用 code === 200 判断成功（和管理员一致）
    if (response.data.code === 200) {
      // 注意：管理员界面接收的是 response.data.data.fabricList，这里可能需要对应调整
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
    
    // 获取所有面料类型
    // 获取面料类型（修改后）
async fetchAllTypes() {
  try {
    const response = await this.$axios.get('/fabric/types');
    // 用 code === 200 判断成功
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
    
    // 处理搜索
    handleSearch() {
      this.fetchFabrics();
    },
    
    // 处理面料点击事件
    handleFabricClick(fabric) {
      this.currentFabric = { ...fabric };
      this.previewVisible = true;
    },
    
    // 处理选择面料
    handleSelectFabric() {
      // 向父组件发射选择事件，传递选中的面料信息
      this.$emit('select', this.currentFabric);
      this.previewVisible = false;
      this.$message.success(`已选择面料：${this.currentFabric.name}`);
    },
    
    // 处理面料图片加载失败
    handleImageError(index) {
      this.$set(this.fabrics[index], 'imageUrl', require('@/assets/fabric-placeholder.png'));
    },
    
    // 处理预览图片加载失败
    handlePreviewImageError() {
      this.currentFabric.imageUrl = require('@/assets/fabric-placeholder.png');
    }
  }
};
</script>

<style scoped>
.fabric-library {
  padding: 16px;
  border-top: 1px solid #eee;
  height: 100%;
  box-sizing: border-box;
  overflow-y: auto;
}

.library-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #333;
}

/* 搜索区域样式 */
.search-container {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
  width: auto;
}

.type-select {
  width: 160px;
}

/* 面料网格样式 */
.fabric-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.fabric-item {
  width: 100%;
  height: 140px; /* 比图片列表稍高，因为要显示更多信息 */
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  transition: box-shadow 0.2s;
}

.fabric-item:hover {
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.1);
}

.fabric-wrapper {
  width: 100%;
  height: 100%;
  cursor: pointer;
}

.fabric-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.2s;
}

.fabric-item:hover .fabric-image {
  transform: scale(1.05);
}

.fabric-info {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 4px 8px;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.7));
  color: white;
}

.fabric-name {
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.fabric-type {
  font-size: 11px;
  opacity: 0.9;
}

/* 无数据和加载样式 */
.no-data, .loading {
  grid-column: span 4;
  height: 140px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  background: #f5f5f5;
  border-radius: 8px;
}

.loading .el-loading-spinner {
  width: 30px;
  height: 30px;
  margin-bottom: 8px;
}

/* 预览弹窗样式 */
.preview-content {
  display: flex;
  gap: 20px;
  align-items: flex-start;
}

.preview-image {
  width: 300px;
  height: 300px;
  object-fit: cover;
  border-radius: 4px;
}

.preview-details {
  flex: 1;
  min-width: 0;
}

.preview-details h3 {
  margin-top: 0;
  margin-bottom: 16px;
  color: #333;
}

.preview-details p {
  margin: 8px 0;
  line-height: 1.6;
  color: #666;
}

.fabric-preview-dialog .el-dialog__body {
  padding: 20px;
  max-height: 70vh;
  overflow-y: auto;
}

/* 响应式调整 */
@media (max-width: 768px) {
  .fabric-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  
  .no-data, .loading {
    grid-column: span 2;
  }
  
  .preview-content {
    flex-direction: column;
  }
  /* 自定义加载动画 */
.custom-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #ddd;
  border-top-color: #1890ff; /* 蓝色顶部 */
  border-radius: 50%;
  animation: spin 1s linear infinite; /* 旋转动画 */
  margin-bottom: 8px;
}
  .preview-image {
    width: 100%;
    height: auto;
  }
  /* 旋转动画定义 */
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}
}
</style>