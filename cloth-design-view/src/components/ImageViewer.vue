<template>
  <div class="image-viewer">
    <!-- 图片容器：统一正方形占位 + 图片自适应 -->
    <div class="img-placeholder-container">
      <!-- 已生成图片 -->
      <img 
        :src="proxyImageUrl" 
        alt="AI生成图"
        class="preview-img"
        v-if="proxyImageUrl"
      >
      <!-- 未生成时的正方形占位符 -->
      <div class="img-placeholder" v-else>
        <div class="placeholder-content">
          <i class="el-icon-picture-outline"></i>
          <p>等待AI生成图片...</p>
        </div>
      </div>
    </div>
    <!-- 保存按钮：匹配chatvue暖色调风格 -->
    <button 
      @click="saveImage" 
      class="save-img-btn"
      v-if="proxyImageUrl"
    >
      保存图片
    </button>
  </div>
</template>

<script>
export default {
  name: 'ImageViewer',
  props: {
    aiReply: {
      type: String,
      default: ''
    }
  },
  computed: {
    // 直接从AI回复中提取Coze链接 + 拼接代理URL
    proxyImageUrl() {
      if (!this.aiReply) return '';
      // 提取s.coze.cn的链接
      const match = this.aiReply.match(/https?:\/\/s\.coze\.cn\/t\/[a-zA-Z0-9_\-]+\/?/);
      if (!match) return '';
      // 拼接后端代理URL
      return `http://localhost:21090/api/personal-heath/v1.0/imageProxy?url=${encodeURIComponent(match[0])}`;
    }
  },
  methods: {
    saveImage() {
      // 保存逻辑保留原有代码
      fetch('http://localhost:21090/api/personal-heath/v1.0/generatedImage/save', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          imageUrl: this.proxyImageUrl,
          userId: 23,
          generateTime: new Date().toISOString()
        })
      }).then(res => alert('保存成功'))
        .catch(err => alert('保存失败'));
    }
  }
};
</script>

<style scoped>
/* 整体容器：匹配chatvue的image-viewer样式 */
.image-viewer {
  padding: 15px;
  font-family: Arial, sans-serif;
}

.image-viewer h3 {
  margin: 0 0 15px 0;
  font-size: 16px;
  color: #8a7d5e; /* 匹配chatvue暖色调文字 */
}

/* 图片容器：保证占位符和图片都是正方形 */
.img-placeholder-container {
  width: 100%;
  aspect-ratio: 1/1; /* 强制正方形 */
  margin: 10px 0;
  position: relative;
}

/* 未生成图片时的正方形占位符 */
.img-placeholder {
  width: 100%;
  height: 100%;
  background-color: #fff8e8; /* 匹配chatvue暖色调背景 */
  border: 1px dashed #f0e8d0; /* 暖色调虚线边框 */
  border-radius: 8px; /* 匹配chatvue圆角 */
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: #8a7d5e; /* 暖色调文字 */
}

.placeholder-content i {
  font-size: 48px;
  margin-bottom: 10px;
  color: #f0c880; /* 暖色调图标 */
}

.placeholder-content p {
  font-size: 14px;
  margin: 0;
}

/* 预览图片：适配正方形容器，保持风格统一 */
.preview-img {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 保持图片比例，不拉伸 */
  background-color: #fffdf8; /* 图片背景色匹配chatvue */
  border: 1px solid #f0e8d0; /* 暖色调边框 */
  border-radius: 8px; /* 匹配chatvue圆角 */
}

/* 保存按钮：完全匹配chatvue的按钮风格 */
.save-img-btn {
  padding: 8px 20px;
  background-color: #f5dca8; /* 匹配chatvue按钮底色 */
  color: #8a7d5e; /* 暖色调文字 */
  border: 1px solid #f0c880; /* 暖色调边框 */
  border-radius: 20px; /* 匹配chatvue圆角按钮 */
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}

.save-img-btn:hover {
  background-color: #f0c880; /* hover暖色调加深 */
  color: #7a6d4e; /* 文字加深 */
  box-shadow: 0 2px 6px rgba(240, 200, 128, 0.15); /* 匹配chatvue阴影 */
}

.save-img-btn:active {
  transform: translateY(1px);
}
</style>