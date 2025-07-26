<template>
  <div class="image-viewer">
    <!-- 图片查看区域 -->
    <div v-if="imageUrl" class="image-container">
      <h3>图片预览</h3>
      <div class="image-wrapper">
        <img 
          :src="imageUrl" 
          alt="提取的图片" 
          class="preview-image"
          @load="isLoading = false"
          @error="handleImageError"
        >
        <div v-if="isLoading" class="image-loading">加载中...</div>
        <div v-if="loadError" class="image-error">图片加载失败，请检查链接</div>
      </div>
      
      <!-- 保存按钮 -->
      <button 
        class="save-btn" 
        @click="saveImage" 
        :disabled="isLoading || loadError"
      >
        保存图片到本地
      </button>
    </div>

    <!-- 无图片时的提示 -->
    <div v-else class="no-image">
      {{ hasChecked? '未从回复中提取到图片链接' : '等待AI生成图片...' }}
    </div>
  </div>
</template>

<script>
export default {
  name: 'ImageViewer',
  props: {
    // 接收AI的回复内容，用于提取图片链接
    aiReply: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      imageUrl: '', // 提取到的图片链接
      isLoading: false, // 图片加载状态
      loadError: false, // 加载错误状态
      hasChecked: false // 是否已检查过回复内容
    };
  },
  watch: {
    // 当AI回复变化时，自动提取图片链接
    aiReply(newReply) {
      this.extractImageUrl(newReply);
    }
  },
  methods: {
    // 从AI回复中提取图片链接（支持Coze图片链接格式）
    extractImageUrl(content) {
      if (!content) {
        this.resetState();
        return;
      }

      // 正则匹配Coze图片链接（https://s.coze.cn/t/xxx/格式）
      const imgRegex = /https?:\/\/s\.coze\.cn\/t\/[a-zA-Z0-9]+\/?/g;
      const matches = content.match(imgRegex);

      if (matches && matches.length > 0) {
        // 提取第一个有效链接
        this.imageUrl = matches[0];
        this.isLoading = true;
        this.loadError = false;
        this.hasChecked = true;
      } else {
        this.resetState();
        this.hasChecked = true;
      }
    },

    // 处理图片加载错误
    handleImageError() {
      this.isLoading = false;
      this.loadError = true;
    },

    // 保存图片到本地
    saveImage() {
      if (!this.imageUrl) return;

      // 创建一个隐藏的a标签用于下载
      const link = document.createElement('a');
      link.href = this.imageUrl;
      // 生成文件名（以链接中的ID为基础）
      const fileName = `coze-image-${this.imageUrl.split('/').pop() || 'unknown'}.png`;
      link.download = fileName; // 触发下载
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    },

    // 重置状态
    resetState() {
      this.imageUrl = '';
      this.isLoading = false;
      this.loadError = false;
    }
  },
  created() {
    // 初始化时检查已有回复
    if (this.aiReply) {
      this.extractImageUrl(this.aiReply);
    }
  }
};
</script>

<style scoped>
.image-viewer {
  width: 350px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  padding: 15px;
  margin: 10px 0;
  background-color: white;
}

.image-container h3 {
  margin: 0 0 15px 0;
  color: #333;
  font-size: 16px;
}

.image-wrapper {
  position: relative;
  width: 100%;
  height: 250px;
  border: 1px dashed #ccc;
  border-radius: 4px;
  overflow: hidden;
  margin-bottom: 15px;
}

.preview-image {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 保持图片比例，不拉伸 */
}

.image-loading {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255,255,255,0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
}

.image-error {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: #fff5f5;
  color: #f56c6c;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
}

.save-btn {
  background-color: #165DFF;
  color: white;
  border: none;
  padding: 8px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.save-btn:disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.save-btn:hover:not(:disabled) {
  background-color: #0e47d7;
}

.no-image {
  height: 250px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #666;
  border: 1px dashed #ccc;
  border-radius: 4px;
}
</style>