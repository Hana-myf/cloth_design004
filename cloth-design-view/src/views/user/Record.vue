<template>
  <div class="style-transfer-page">
    <!-- 页面标题 -->
    <div class="section-header page-header">
      <h2 class="page-title">
        <i class="el-icon-picture"></i>
        图片风格转换工具
      </h2>
      <p class="page-subtitle">通过文字描述修改您的图片</p>
      <div class="divider"></div>
    </div>

    <div class="main-content-wrapper">
      <!-- 登录状态提示 -->
      <div v-if="!isAuthenticated" class="info-card mb-6">
        <div class="info-icon">
          <i class="el-icon-info"></i>
        </div>
        <div class="info-content">
          <p class="info-text">检测到未登录状态，尝试使用默认认证信息...</p>
        </div>
      </div>

      <!-- 输入表单 -->
      <div class="input-form-card section-card mb-8">
        <div class="card-title">
          <i class="el-icon-edit"></i>
          <span>输入参数</span>
        </div>
        
        <div class="form-content">
          <!-- 图片URL输入 -->
          <div class="form-group">
            <label for="imageUrl" class="form-label">图片URL</label>
            <el-input
              type="url"
              id="imageUrl"
              v-model="imageUrl"
              placeholder="https://example.com/image.jpg"
              required
            ></el-input>
            <p class="input-tip">支持jpeg、png格式的可访问URL</p>
          </div>

          <!-- 提示词输入 -->
          <div class="form-group">
            <label for="prompt" class="form-label">描述文字</label>
            <el-input
              type="textarea"
              id="prompt"
              v-model="prompt"
              rows="3"
              placeholder="例如：改成爱心形状的泡泡"
              required
            ></el-input>
          </div>

          <!-- 认证Token输入（可选，用于测试） -->
          <div class="form-group">
            <label for="authToken" class="form-label">认证Token（可选）</label>
            <el-input
              type="text"
              id="authToken"
              v-model="authToken"
              placeholder="输入认证Token（如Bearer Token）"
            ></el-input>
          </div>

          <!-- 生成按钮 -->
          <el-button
            type="primary"
            size="medium"
            @click="generateImage"
            :disabled="isLoading || !isFormValid"
            :loading="isLoading"
            class="generate-btn"
          >
            <i class="el-icon-s-promotion"></i>
            <span>{{ isLoading ? '生成中...' : '生成图片' }}</span>
          </el-button>
        </div>
      </div>

      <!-- 结果展示 -->
      <div v-if="resultUrl" class="result-display-card section-card">
        <div class="card-title">
          <i class="el-icon-success"></i>
          <span>生成结果</span>
        </div>
        <div class="result-content">
          <div class="result-image-wrapper">
            <img
              :src="resultUrl"
              :alt="prompt"
              class="result-image"
              @load="imageLoaded = true"
            >
            <div v-if="!imageLoaded" class="image-placeholder">
              <i class="el-icon-picture-outline"></i>
            </div>
          </div>
          <a
            :href="resultUrl"
            target="_blank"
            class="view-original-link"
          >
            <span>查看原图</span>
            <i class="el-icon-link"></i>
          </a>
        </div>
      </div>

      <!-- 错误提示 -->
      <div v-if="errorMessage" class="error-card section-card">
        <div class="error-icon">
          <i class="el-icon-warning"></i>
        </div>
        <div class="error-details">
          <p class="error-message">{{ errorMessage }}</p>
          <p v-if="errorDetails" class="error-sub-message">{{ errorDetails }}</p>
          <p v-if="requestUrl" class="error-sub-message">请求地址: {{ requestUrl }}</p>
          <p v-if="authTip" class="error-sub-message">{{ authTip }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// 这里假设你的项目已经配置了Element UI，并且全局引入了Message组件
// 如果没有，你需要手动引入：
// import { Message } from 'element-ui';

export default {
  data() {
    return {
      // 表单数据
      imageUrl: '',
      prompt: '',
      authToken: '', // 用于输入认证Token
      
      // 状态管理
      isLoading: false,
      resultUrl: '',
      errorMessage: '',
      errorDetails: '',
      imageLoaded: false,
      requestUrl: '',
      authTip: '', // 认证相关提示
      
      // 后端接口完整地址
      apiEndpoint: 'http://localhost:21090/api/personal-heath/v1.0/api/proxy/generate-image'
    };
  },
  computed: {
    // 表单验证
    isFormValid() {
      return this.imageUrl.trim() !== '' && 
             this.prompt.trim() !== '' &&
             this.isValidImageUrl(this.imageUrl);
    },
    // 检查是否有认证信息
    isAuthenticated() {
      return !!this.authToken.trim();
    }
  },
  methods: {
    // 验证图片URL格式
    isValidImageUrl(url) {
      const regex = /^(https?:\/\/).*\.(jpeg|jpg|png)$/i;
      return regex.test(url);
    },
    
    async generateImage() {
      // 重置状态
      this.resultUrl = '';
      this.errorMessage = '';
      this.errorDetails = '';
      this.authTip = '';
      this.imageLoaded = false;
      this.requestUrl = this.apiEndpoint;
      this.isLoading = true;

      try {
        // 构建请求参数
        const requestData = {
          model: 'doubao-seededit-3-0-i2i-250628',
          prompt: this.prompt.trim(),
          image: this.imageUrl.trim(),
          seed: 123,
          guidance_scale: 5.5
        };

        // 构建请求头（包含认证信息）
        const headers = {
          'Content-Type': 'application/json',
          'X-Requested-With': 'XMLHttpRequest'
        };

        // 如果有认证Token，添加到请求头
        if (this.authToken.trim()) {
          // 自动补充Bearer前缀（如果用户没输入）
          headers['Authorization'] = this.authToken.startsWith('Bearer ') 
            ? this.authToken 
            : `Bearer ${this.authToken}`;
        } else {
          this.authTip = '未提供认证Token，可能导致请求失败。请输入有效的登录Token后重试。';
        }

        // 设置请求超时
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), 30000);

        // 发送请求
        const response = await fetch(this.apiEndpoint, {
          method: 'POST',
          headers: headers,
          body: JSON.stringify(requestData),
          signal: controller.signal,
          credentials: 'include' // 允许携带Cookie（如果后端用Session认证）
        });

        clearTimeout(timeoutId);

        // 读取响应内容
        const responseText = await response.text();
        let responseData;

        try {
          responseData = JSON.parse(responseText);
        } catch (e) {
          throw new Error(`后端返回无效格式: ${responseText.substring(0, 200)}`);
        }

        // 处理后端响应
        if (responseData.code === 200) {
          if (responseData.data && responseData.data.length > 0 && responseData.data[0].url) {
            this.resultUrl = responseData.data[0].url;
            this.$message.success('图片生成成功！'); // 使用Element UI的Message
          } else {
            throw new Error('后端未返回有效图片URL');
          }
        } else if (responseData.code === 400) {
          // 专门处理身份认证异常
          if (responseData.msg.includes('身份认证异常') || responseData.msg.includes('登录')) {
            this.authTip = '请先登录系统获取有效Token，或联系管理员开放接口访问权限。';
            this.$message.warning('认证失败，请检查Token。'); // 使用Element UI的Message
          }
          throw new Error(`请求错误: ${responseData.msg}`);
        } else {
          throw new Error(`请求失败: ${responseData.msg || '未知错误'}`);
        }

      } catch (error) {
        console.error('生成失败:', error);
        
        // 错误分类处理
        if (error.name === 'AbortError') {
          this.errorMessage = '请求超时';
          this.errorDetails = '服务器响应时间超过30秒，请重试';
          this.$message.error('请求超时，请重试。'); // 使用Element UI的Message
        } else if (error.message.includes('Failed to fetch')) {
          this.errorMessage = '无法连接到后端服务器';
          this.errorDetails = '请检查后端服务是否已启动，或网络是否正常';
          this.$message.error('无法连接到后端服务器。'); // 使用Element UI的Message
        } else {
          this.errorMessage = '生成图片失败';
          this.errorDetails = error.message;
          this.$message.error('生成图片失败：' + error.message); // 使用Element UI的Message
        }
      } finally {
        this.isLoading = false;
      }
    }
  }
};
</script>

<style scoped lang="scss">
.style-transfer-page {
  padding: 30px;
  max-width: 1000px; // 调整最大宽度，使其更紧凑
  margin: 0 auto;
  background-color: #fcfcfa; // 统一背景色
  min-height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
  gap: 30px;
}

// 统一的 Section Header 样式
.section-header {
  margin-bottom: 20px;
  position: relative;

  .page-title {
    font-size: 28px;
    color: #2d2d2a;
    margin: 0 0 8px 0;
    font-weight: 600;
    letter-spacing: 1px;
    display: flex;
    align-items: center;
    gap: 10px;
  }

  .page-subtitle {
    font-size: 16px;
    color: #6a6a66;
    margin: 0 0 15px 0;
  }

  .divider {
    height: 3px;
    width: 80px;
    background-color: #d4af37;
    border-radius: 2px;
    transition: width 0.3s ease;

    &:hover {
      width: 120px;
    }
  }
}

.main-content-wrapper {
  display: flex;
  flex-direction: column;
  gap: 24px; // 主要内容区块间的间距
}

// 统一的卡片样式
.section-card {
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.card-title {
  font-size: 20px;
  color: #2d2d2a;
  font-weight: 500;
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 15px;
  padding-bottom: 10px;
  border-bottom: 1px dashed #eee;
}

// 信息提示卡片
.info-card {
  background-color: #fff9e6; // 浅黄色背景
  border: 1px solid #ffe799; // 边框
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03); // 浅阴影

  .info-icon {
    color: #d4af37; // 金色图标
    font-size: 24px;
    margin-top: 2px;
  }
  .info-content {
    flex-grow: 1;
    .info-text {
      font-size: 14px;
      color: #7b6d4b; // 深黄色文字
      margin: 0;
    }
  }
}

// 表单部分
.input-form-card {
  .form-content {
    display: flex;
    flex-direction: column;
    gap: 18px; // 表单组之间的间距
  }
  .form-group {
    .form-label {
      display: block;
      font-size: 14px;
      font-weight: 500;
      color: #4a4a46;
      margin-bottom: 8px;
    }
    .el-input, .el-textarea {
      width: 100%;
      ::v-deep .el-input__inner, ::v-deep .el-textarea__inner {
        border-radius: 6px;
        border-color: #e0e0e0;
        &:focus {
          border-color: #d4af37;
          box-shadow: 0 0 0 2px rgba(212, 175, 55, 0.2); // 金色光晕
        }
      }
    }
    .input-tip {
      font-size: 12px;
      color: #99958c;
      margin-top: 6px;
    }
  }
  .generate-btn {
    width: 100%;
    padding: 12px 20px;
    font-size: 18px;
    background-color: #d4af37;
    border-color: #d4af37;
    color: #fff;
    &:hover {
      background-color: darken(#d4af37, 8%);
      border-color: darken(#d4af37, 8%);
    }
    &:disabled {
      background-color: #e0e0e0;
      border-color: #e0e0e0;
      cursor: not-allowed;
    }
    i {
      margin-right: 8px;
    }
  }
}

// 结果展示部分
.result-display-card {
  .result-content {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 20px;

    .result-image-wrapper {
      width: 100%;
      max-width: 500px; // 控制结果图片的最大宽度
      aspect-ratio: 16 / 9; // 保持宽高比
      background-color: #f7f7f5;
      border: 1px solid #e0e0e0;
      border-radius: 8px;
      overflow: hidden;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;

      .result-image {
        width: 100%;
        height: 100%;
        object-fit: contain; // 包含整个图像，可能留白
      }
      .image-placeholder {
        position: absolute;
        inset: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        i {
          font-size: 60px;
          color: #d0d0cc;
        }
      }
    }
    .view-original-link {
      display: inline-flex;
      align-items: center;
      gap: 5px;
      color: #d4af37; // 金色链接
      font-weight: 500;
      font-size: 15px;
      text-decoration: none;
      &:hover {
        color: darken(#d4af37, 10%);
        text-decoration: underline;
      }
    }
  }
}

// 错误提示部分
.error-card {
  background-color: #ffe6e6; // 浅红色背景
  border: 1px solid #ffcccc; // 边框
  border-radius: 8px;
  padding: 16px;
  display: flex;
  align-items: flex-start;
  gap: 12px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.03); // 浅阴影

  .error-icon {
    color: #ff6666; // 红色图标
    font-size: 24px;
    margin-top: 2px;
  }
  .error-details {
    flex-grow: 1;
    .error-message {
      font-size: 14px;
      color: #cc0000; // 深红色文字
      font-weight: 500;
      margin: 0 0 8px 0;
    }
    .error-sub-message {
      font-size: 12px;
      color: #8b0000; // 更深的红色文字
      margin-bottom: 4px;
      background-color: #ffebeb;
      padding: 6px 10px;
      border-radius: 4px;
      word-break: break-all; // 防止长URL溢出
    }
  }
}

// 响应式调整
@media (max-width: 768px) {
  .style-transfer-page {
    padding: 15px;
    gap: 20px;
  }
  .section-header {
    .page-title {
      font-size: 24px;
    }
    .page-subtitle {
      font-size: 14px;
    }
    .divider {
      width: 60px;
      &:hover {
        width: 90px;
      }
    }
  }
  .section-card {
    padding: 18px;
    gap: 15px;
  }
  .card-title {
    font-size: 18px;
    margin-bottom: 10px;
    padding-bottom: 8px;
  }
  .info-card, .error-card {
    padding: 12px;
    gap: 10px;
    .info-icon, .error-icon {
      font-size: 20px;
    }
    .info-text, .error-message {
      font-size: 13px;
    }
    .error-sub-message {
      font-size: 11px;
      padding: 5px 8px;
    }
  }
  .input-form-card {
    .form-content {
      gap: 15px;
    }
    .generate-btn {
      font-size: 16px;
      padding: 10px 15px;
    }
  }
  .result-display-card {
    .result-content {
      gap: 15px;
    }
    .result-image-wrapper {
        max-width: 100%; // 小屏幕上占满宽度
    }
    .view-original-link {
      font-size: 14px;
    }
  }
}
</style>