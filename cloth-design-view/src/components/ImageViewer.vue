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
      
      <button 
        class="save-btn" 
        @click="saveImage" 
        :disabled="isLoading || loadError || saving"
      >
        {{ saving ? '保存中...' : '保存图片到我的创作' }}
      </button>
    </div>

    <!-- 无图片时的提示 -->
    <div v-else class="no-image">
      {{ hasChecked? '未从回复中提取到图片链接' : '等待AI生成图片...' }}
    </div>
  </div>
</template>

<script>
// 引入项目配置的request实例（确保带token）
import request from '@/utils/request';

export default {
  name: 'ImageViewer',
  props: {
    aiReply: {
      type: String,
      default: ''
    },
    currentUserId: {
      type: [String, Number],
      required: false,
      default: ''
    },
  },
  data() {
    return {
      imageUrl: '',
      isLoading: false,
      loadError: false,
      hasChecked: false,
      saving: false,
      saveSuccess: false,
    };
  },
  watch: {
    aiReply(newReply) {
      this.extractImageUrl(newReply);
    }
  },
  methods: {
    extractImageUrl(content) {
      if (!content) {
        this.resetState();
        return;
      }

      // 匹配Coze图片链接
      const imgRegex = /https?:\/\/s\.coze\.cn\/t\/[a-zA-Z0-9]+\/?/g;
      const matches = content.match(imgRegex);

      if (matches && matches.length > 0) {
        this.imageUrl = matches[0];
        this.isLoading = true;
        this.loadError = false;
        this.hasChecked = true;
      } else {
        this.resetState();
        this.hasChecked = true;
      }
    },

    handleImageError() {
      this.isLoading = false;
      this.loadError = true;
    },

    async saveImage() {
      // 验证用户登录状态
      if (!this.currentUserId) {
        alert('请先登录再保存图片');
        return;
      }
      // 验证图片链接
      if (!this.imageUrl || this.saving) return;

      this.saving = true;
      this.saveSuccess = false;

      try {
        const fileName = this.imageUrl.split('/').pop() || 'generated-image';
        
        // 生成符合后端要求的时间格式（不含毫秒）
        const date = new Date();
        // 转换为 ISO 格式并去除毫秒部分（例如 "2025-07-29T10:22:21Z"）
        const generateTime = date.toISOString().split('.')[0] + 'Z';
        // 打印请求信息（用于调试）
        console.log('保存请求参数:', {
          userId: this.currentUserId,
          imageName: fileName,
          imageUrl: this.imageUrl,
          generateTime: new Date().toISOString()
        });

        // 使用项目配置的request实例发送请求（自动带token）
        const response = await request.post('/generatedImage/save', {
          userId: this.currentUserId,
          imageName: fileName,
          imageUrl: this.imageUrl,
          generateTime: generateTime
        });

        // 处理后端响应（根据实际响应格式调整）
        if (response.data && (response.data.code === 200 || response.data.success)) {
          this.$emit('save-success');
          this.saveSuccess = true;
          alert('图片保存成功');
          setTimeout(() => this.saveSuccess = false, 2000);
        } else {
          throw new Error(response.data.msg || '保存失败（无详细信息）');
        }
      } catch (error) {
        console.error('保存失败详情:', error);
        console.error('响应数据:', error.response?.data || '无响应');
        alert('保存失败: ' + (error.message || '未知错误'));
      } finally {
        this.saving = false;
      }
    },

    resetState() {
      this.imageUrl = '';
      this.isLoading = false;
      this.loadError = false;
    }
  },
  created() {
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