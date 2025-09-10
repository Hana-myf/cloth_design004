<template>
  <div class="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex flex-col">
    <!-- 顶部导航 -->
    <header class="bg-white shadow-md py-4 px-6">
      <div class="container mx-auto">
        <h1 class="text-2xl font-bold text-gray-800">图片风格转换工具</h1>
        <p class="text-gray-600 mt-1">通过文字描述修改您的图片</p>
      </div>
    </header>

    <!-- 主要内容区 -->
    <main class="flex-grow container mx-auto px-4 py-8">
      <div class="max-w-4xl mx-auto">
        <!-- 登录状态提示 -->
        <div v-if="!isAuthenticated" class="bg-yellow-50 border border-yellow-200 rounded-lg p-4 mb-6">
          <div class="flex items-start">
            <i class="fa fa-info-circle mt-1 mr-3 text-yellow-500"></i>
            <p class="text-sm text-yellow-700">检测到未登录状态，尝试使用默认认证信息...</p>
          </div>
        </div>

        <!-- 输入表单 -->
        <div class="bg-white rounded-xl shadow-lg p-6 mb-8 transform transition-all hover:shadow-xl">
          <h2 class="text-xl font-semibold text-gray-800 mb-4">输入参数</h2>
          
          <div class="space-y-4">
            <!-- 图片URL输入 -->
            <div>
              <label for="imageUrl" class="block text-sm font-medium text-gray-700 mb-1">图片URL</label>
              <input
                type="url"
                id="imageUrl"
                v-model="imageUrl"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                placeholder="https://example.com/image.jpg"
                required
              >
              <p class="text-xs text-gray-500 mt-1">支持jpeg、png格式的可访问URL</p>
            </div>

            <!-- 提示词输入 -->
            <div>
              <label for="prompt" class="block text-sm font-medium text-gray-700 mb-1">描述文字</label>
              <textarea
                id="prompt"
                v-model="prompt"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                rows="3"
                placeholder="例如：改成爱心形状的泡泡"
                required
              ></textarea>
            </div>

            <!-- 认证Token输入（可选，用于测试） -->
            <div>
              <label for="authToken" class="block text-sm font-medium text-gray-700 mb-1">认证Token（可选）</label>
              <input
                type="text"
                id="authToken"
                v-model="authToken"
                class="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-blue-500 transition-colors"
                placeholder="输入认证Token（如Bearer Token）"
              >
            </div>

            <!-- 生成按钮 -->
            <button
              @click="generateImage"
              :disabled="isLoading || !isFormValid"
              class="w-full bg-blue-600 hover:bg-blue-700 text-white font-medium py-2 px-4 rounded-lg transition-colors disabled:bg-gray-400 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              <span>生成图片</span>
              <i v-if="isLoading" class="fa fa-spinner fa-spin"></i>
            </button>
          </div>
        </div>

        <!-- 结果展示 -->
        <div v-if="resultUrl" class="bg-white rounded-xl shadow-lg p-6 transform transition-all hover:shadow-xl">
          <h2 class="text-xl font-semibold text-gray-800 mb-4">生成结果</h2>
          <div class="flex flex-col items-center">
            <div class="relative w-full max-w-2xl aspect-video bg-gray-100 rounded-lg overflow-hidden mb-4">
              <img
                :src="resultUrl"
                :alt="prompt"
                class="w-full h-full object-contain"
                @load="imageLoaded = true"
              >
              <div v-if="!imageLoaded" class="absolute inset-0 flex items-center justify-center">
                <i class="fa fa-image text-gray-300 text-5xl"></i>
              </div>
            </div>
            <a
              :href="resultUrl"
              target="_blank"
              class="inline-flex items-center text-blue-600 hover:text-blue-800 font-medium"
            >
              <span>查看原图</span>
              <i class="fa fa-external-link ml-1"></i>
            </a>
          </div>
        </div>

        <!-- 错误提示 -->
        <div v-if="errorMessage" class="bg-red-50 border border-red-200 rounded-lg p-4 text-red-700">
          <div class="flex items-start">
            <i class="fa fa-exclamation-circle mt-1 mr-3 text-red-500"></i>
            <div>
              <p>{{ errorMessage }}</p>
              <p v-if="errorDetails" class="text-xs mt-2 bg-red-100 p-2 rounded">{{ errorDetails }}</p>
              <p v-if="requestUrl" class="text-xs mt-2 bg-red-100 p-2 rounded">请求地址: {{ requestUrl }}</p>
              <p v-if="authTip" class="text-xs mt-2 bg-red-100 p-2 rounded">{{ authTip }}</p>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 页脚 -->
    <footer class="bg-white py-4 px-6 border-t">
      <div class="container mx-auto text-center text-gray-600 text-sm">
        <p>图片生成工具 &copy; {{ new Date().getFullYear() }}</p>
      </div>
    </footer>
  </div>
</template>

<script>
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
          } else {
            throw new Error('后端未返回有效图片URL');
          }
        } else if (responseData.code === 400) {
          // 专门处理身份认证异常
          if (responseData.msg.includes('身份认证异常') || responseData.msg.includes('登录')) {
            this.authTip = '请先登录系统获取有效Token，或联系管理员开放接口访问权限。';
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
        } else if (error.message.includes('Failed to fetch')) {
          this.errorMessage = '无法连接到后端服务器';
          this.errorDetails = '请检查后端服务是否已启动，或网络是否正常';
        } else {
          this.errorMessage = '生成图片失败';
          this.errorDetails = error.message;
        }
      } finally {
        this.isLoading = false;
      }
    }
  }
};
</script>

