<!-- ImageSaveList.vue 完整代码 -->
<template>
  <div class="image-save-list">
    <h4 class="list-title">我的创作</h4>
    <!-- 固定4个位置的网格 -->
    <div class="image-grid">
      <!-- 循环4次，每次取对应索引的图片 -->
      <div v-for="index in 4" :key="index" class="saved-image-item">
        <!-- 有图片时显示图片 -->
        <div v-if="savedImages[index - 1]" class="image-wrapper">
          <img 
            :src="savedImages[index - 1].imageUrl" 
            alt="保存的图片" 
            class="saved-image"
            @error="handleImageError(index - 1)" 
          >
          <!-- 显示图片名称（可选） -->
          <div class="image-name">{{ savedImages[index - 1].imageName }}</div>
        </div>
        
        <!-- 无图片时显示占位样式 -->
        <div v-else class="image-placeholder">
          <span>+</span>
          <p>暂无图片</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    savedImages: {
      type: Array,
      default: () => [] // 接收父组件传递的图片列表（最多4条，最新在前）
    }
  },
  methods: {
    // 处理图片加载失败（显示占位图）
    handleImageError(index) {
      // 这里可以替换为默认占位图URL
      this.$set(this.savedImages, index, {
        ...this.savedImages[index],
        imageUrl: require('@/assets/default-placeholder.png') // 需准备一张占位图
      });
    }
  }
};
</script>

<style scoped>
.image-save-list {
  padding: 16px;
  border-top: 1px solid #eee;
}

.list-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #333;
}

/* 固定4列网格 */
.image-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr); /* 4个等宽列 */
  gap: 12px;
}

.saved-image-item {
  width: 100%;
  height: 120px; /* 固定高度 */
  border-radius: 8px;
  overflow: hidden;
  position: relative;
}

/* 有图片时的样式 */
.image-wrapper {
  width: 100%;
  height: 100%;
}

.saved-image {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 保持比例填充容器 */
  cursor: pointer;
  transition: transform 0.2s;
}

.saved-image:hover {
  transform: scale(1.05);
}

.image-name {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 4px 8px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  font-size: 12px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 无图片时的占位样式 */
.image-placeholder {
  width: 100%;
  height: 100%;
  background: #f5f5f5;
  border: 1px dashed #ddd;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
}

.image-placeholder span {
  font-size: 24px;
  margin-bottom: 4px;
}

.image-placeholder p {
  font-size: 12px;
  margin: 0;
}
</style>