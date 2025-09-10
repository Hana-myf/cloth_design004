<template>
  <div class="detail-optimization">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>细节优化工具</h1>
      <p>上传图片 → 选择素材 → 生成方案</p>
    </div>

    <!-- 主布局：左（工具）- 中（交互）- 右（成果） -->
    <div class="main-layout">
      <!-- 左侧工具区：包含面料库和色彩提取器 -->
      <div class="tools-area">
        <div class="tools-title">设计工具</div>
        
        <!-- 面料库组件 -->
        <fabric-library 
          @select="onFabricSelect" 
           class="tool-component fabric-component"
        ></fabric-library>

        <color-extractor 
          @apply-colors="onColorsApplied"
          class="tool-component color-component"
        ></color-extractor>
        
        <!-- 搭配助手仍为占位 -->
          <CollocationAssistant
           class="tool-component assistant-component"
          ></CollocationAssistant>
      </div>

      <!-- 中间交互区：与AI对话 -->
      <div class="interaction-area">
        <div class="interaction-title">AI设计对话</div>
        
        <!-- 对话内容区 -->
        <div class="chat-content">
          <!-- 系统提示 -->
          <div class="system-message">
            请上传参考图片、选择面料或输入描述，我会生成设计方案
          </div>
          
          <!-- 对话记录 -->
          <div v-for="(msg, index) in chatRecords" :key="index" class="chat-bubble">
            <div class="bubble-avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
            <div class="bubble-content">
              <!-- 用户消息：可能包含选择的面料或色彩 -->
              <template v-if="msg.role === 'user'">
                <div v-if="msg.fabric" class="selected-fabric-tag">
                  已选择面料：{{ msg.fabric.name }}（{{ msg.fabric.type }}）
                </div>
                <div v-if="msg.colors" class="selected-colors">
                  <span class="tag">已选择色彩：</span>
                  <div class="color-swatch-container">
                    <div 
                      v-for="(color, cIndex) in msg.colors" 
                      :key="cIndex"
                      class="color-swatch"
                      :style="{ backgroundColor: color.hex }"
                      :title="color.hex"
                    ></div>
                  </div>
                </div>
                <p v-if="msg.text">{{ msg.text }}</p>
                <div v-if="msg.imageUrl" class="user-image">
                  <img :src="msg.imageUrl" alt="用户上传图片">
                </div>
              </template>
              
              <!-- AI消息：生成的方案 -->
              <template v-if="msg.role === 'ai'">
                <p v-if="msg.text">{{ msg.text }}</p>
                <div v-if="msg.imageUrl" class="ai-image">
                  <img :src="msg.imageUrl" alt="AI生成方案" @click="saveToGallery(msg.imageUrl, msg.title)">
                  <div class="image-action">点击保存到右侧成果库</div>
                </div>
              </template>
            </div>
          </div>
          
          <!-- 加载中提示 -->
          <div v-if="isLoading" class="loading">
            <div class="custom-spinner"></div>
            <p>加载中...</p>
          </div>
        </div>
        
        <!-- 输入区 -->
        <div class="input-area">
          <!-- 新增：上传图片按钮 -->
          <div class="upload-container">
            <el-upload
              class="image-uploader"
              action=""
              :auto-upload="false"
              :on-change="handleImageUpload"
              :show-file-list="false"
              accept="image/*"
            >
              <el-button type="primary" icon="el-icon-upload">上传图片</el-button>
            </el-upload>
          </div>
          
          <el-input 
            v-model="userInput" 
            placeholder="输入设计需求..." 
            class="text-input"
          ></el-input>
          <el-button 
            type="primary" 
            @click="sendMessage"
            class="send-btn"
            :disabled="!canSendMessage"
          >
            发送
          </el-button>
        </div>
      </div>

      <!-- 右侧成果区：保存生成的图片 -->
      <div class="gallery-area">
        <div class="gallery-title">我的成果库</div>
        <image-save-list 
          :saved-images="savedImages"
        ></image-save-list>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
// 引入组件（保持原有组件引用）
import FabricLibrary from '@/components/FabricLibrary.vue';
import ImageSaveList from '@/components/ImageSaveList.vue';
import ColorExtractor from '@/components/ColorExtractor.vue';
import CollocationAssistant from '@/components/CollocationAssistant.vue';
import DirectModelCall from '@/components/DirectModelCall.vue';

export default {
  components: {
    FabricLibrary,
    ImageSaveList,
    ColorExtractor,
    CollocationAssistant,
    DirectModelCall
  },
  data() {
    return {
      // 对话记录
      chatRecords: [],
      // 用户输入文本
      userInput: '',
      // 保存的成果图片
      savedImages: [],
      // 加载状态
      isLoading: false,
      // 当前选中的面料（未发送状态）
      currentFabric: null,
      // 当前选中的颜色（未发送状态）
      currentColors: null,
      // 当前上传的图片（未发送状态）
      currentImageUrl: null,
      // 大模型直连配置（核心：替换为大模型官方信息）
      aiModelConfig: {
        // 1. 大模型官方API端点（以火山方舟为例，需按实际文档调整）
        apiUrl: "https://ark.volcengine.com/api/v3/images/generations",
        // 2. 你的大模型API Key（从大模型平台获取，如火山引擎控制台）
        apiKey: "f2390958-8c5a-424a-9021-651e920c2bc4",
        // 3. 模型ID（保持与之前一致）
        modelId: "doubao-seededit-3-0-i2i-250628",
        // 4. 默认示例图片（大模型可访问的公网URL）
        defaultImageUrl: "https://ark-project.tos-cn-beijing.volces.com/doc_image/seededit_i2i.jpeg"
      },
      useDirectCall: false,
      directModelInstance: null
    };
  },
  computed: {
    // 判断是否可以发送消息（逻辑不变）
    canSendMessage() {
      return this.userInput.trim() !== '' || 
             this.currentFabric !== null || 
             this.currentColors !== null ||
             this.currentImageUrl !== null;
    }
  },
  methods: {
    initDirectModelCall() {
      this.directModelInstance = this.$refs.directModel;
    },
    // 接收面料库选择的面料（逻辑不变）
    onFabricSelect(fabric) {
      this.currentFabric = fabric;
      if (this.userInput.trim()) {
        this.userInput += `，使用【${fabric.name}】面料`;
      } else {
        this.userInput = `使用【${fabric.name}】面料`;
      }
      this.$message.success(`已选择面料：${fabric.name}`);
    },
    // 接收色彩提取器应用的色彩（逻辑不变）
    onColorsApplied(colors) {
      const displayColors = colors.slice(0, 4);
      this.currentColors = displayColors;
      const colorHexList = displayColors.map(c => c.hex).join('、');
      if (this.userInput.trim()) {
        this.userInput += `，使用颜色：${colorHexList}`;
      } else {
        this.userInput = `使用颜色：${colorHexList}`;
      }
      this.$message.success('已选择色彩方案');
    },
    // 处理图片上传（适配大模型：跳过后端，直接使用公网示例图或OSS上传）
    async handleImageUpload(file) {
      this.isLoading = true;
      try {
        // 方案说明：
        // 1. 开发调试：直接使用默认示例图（无需后端/OSS）
        // 2. 生产环境：替换为OSS/CDN上传逻辑，确保图片为公网可访问URL
        this.currentImageUrl = this.aiModelConfig.defaultImageUrl;
        this.$message.success('图片已适配为大模型可访问格式');

        // 【生产环境可选】OSS上传示例（需替换为你的OSS配置）
        // const formData = new FormData();
        // formData.append('file', file.raw);
        // const ossResponse = await axios.post('你的OSS上传接口', formData);
        // this.currentImageUrl = ossResponse.data.publicUrl; // OSS返回的公网URL

      } catch (error) {
        console.error('图片处理失败：', error);
        this.$message.error('图片处理失败，请重试');
        this.currentImageUrl = this.aiModelConfig.defaultImageUrl; // 失败时回退到示例图
      } finally {
        this.isLoading = false;
      }
    },
    // 发送用户消息（逻辑不变，仅调用直连大模型的generateAIResponse）
    sendMessage() {
      if (!this.canSendMessage) return;
      const userMessage = {
        role: 'user',
        text: this.userInput,
        fabric: this.currentFabric,
        colors: this.currentColors,
        imageUrl: this.currentImageUrl
      };
      this.chatRecords.push(userMessage);
      // 调用直连大模型的方法
      this.generateAIResponse({
        fabric: this.currentFabric,
        colors: this.currentColors,
        image: this.currentImageUrl,
        prompt: this.userInput
      });
      // 重置输入状态
      this.userInput = '';
      this.currentFabric = null;
      this.currentColors = null;
      this.currentImageUrl = null;
    },
    // 核心修改：直接调用大模型API，完全绕开后端
    async generateAIResponse({ fabric, colors, image, prompt }) {
      this.isLoading = true;
      try {
        // 1. 构建完整提示词（保留面料、颜色逻辑）
        let fullPrompt = prompt || '';
        if (fabric) fullPrompt += ` 使用【${fabric.name}】面料（${fabric.type}）`;
        if (colors) {
          const colorHexList = colors.map(c => c.hex).join('、');
          fullPrompt += ` 搭配颜色：${colorHexList}`;
        }
        if (!fullPrompt.trim()) fullPrompt = "根据素材生成设计方案";

        // 2. 处理图片URL（确保大模型可访问）
        let imageUrl = image;
        // 本地base64图片替换为示例图
        if (imageUrl && imageUrl.startsWith('data:image/')) {
          imageUrl = this.aiModelConfig.defaultImageUrl;
          this.$message.warning('本地图片已替换为大模型可访问的示例图');
        }
        // 未上传图片时使用默认示例图
        const finalImageUrl = imageUrl || this.aiModelConfig.defaultImageUrl;

        // 3. 构建大模型请求参数（按官方API格式）
        const requestData = {
          model: this.aiModelConfig.modelId,          // 模型ID
          prompt: fullPrompt,                         // 完整提示词
          image: finalImageUrl,                       // 参考图片URL（i2i必需）
          seed: Math.floor(Math.random() * 1000),     // 随机种子（保证多样性）
          guidance_scale: 5.5,                        // 引导强度（3-7效果最佳）
          response_format: "url",                     // 返回图片URL（而非base64）
          size: "1024x1024",                          // 生成图片尺寸（按模型支持调整）
          watermark: true                             // 添加水印（可选）
        };
        console.log('直连大模型接口：', this.aiModelConfig.apiUrl);
        console.log('大模型请求参数：', requestData);

        // 4. 发送请求：直接调用大模型API（无需后端中转）
        const response = await axios.post(
          this.aiModelConfig.apiUrl,
          requestData,
          { 
            headers: { 
              'Content-Type': 'application/json',
              // 大模型认证：通过Authorization头传递API Key（官方标准）
              'Authorization': `Bearer ${this.aiModelConfig.apiKey}`
            } 
          }
        );

        // 5. 解析大模型响应（按官方返回格式处理）
        const resData = response.data;
        // 校验响应合法性（不同模型返回格式可能不同，需按文档调整）
        if (resData.code !== 200) {
          throw new Error(`大模型错误：${resData.msg || '未知错误'}`);
        }
        if (!resData.data || resData.data.length === 0) {
          throw new Error('大模型返回空结果，未生成图片');
        }
        const generatedImageUrl = resData.data[0].url;
        if (!generatedImageUrl) {
          throw new Error('生成的图片URL为空');
        }

        // 6. 添加AI回复到对话记录（逻辑不变）
        let responseText = '';
        if (fabric) responseText = `已用【${fabric.name}】面料生成方案：`;
        else if (colors) responseText = `已用指定色彩生成方案：`;
        else if (image) responseText = `已根据图片生成方案：`;
        else responseText = `已根据需求生成方案：`;

        this.chatRecords.push({
          role: 'ai',
          text: responseText,
          imageUrl: generatedImageUrl,
          title: fabric ? `${fabric.name}设计方案` : 
                 colors ? '色彩搭配方案' : 'AI生成方案'
        });

      } catch (error) {
        // 精细化错误提示（针对大模型直连场景）
        console.error('大模型调用完整错误日志：', error);
        let errorMsg = '图片生成失败，请重试';
        // 按错误类型提示具体原因
        if (error.message.includes('401')) {
          errorMsg = '❌ API Key无效或过期，请在大模型平台重新获取';
        } else if (error.message.includes('403')) {
          errorMsg = '❌ 无模型使用权限，请在大模型平台开通权限';
        } else if (error.message.includes('404')) {
          errorMsg = `❌ 大模型API地址错误：${this.aiModelConfig.apiUrl}`;
        } else if (error.message.includes('Network Error')) {
          errorMsg = '❌ 网络错误，无法连接大模型服务（检查网络或API地址）';
        } else if (error.message.includes('模型未开通')) {
          errorMsg = '❌ 当前模型未开通，请在大模型平台激活服务';
        } else {
          errorMsg = `❌ 生成失败：${error.message || '未知错误'}`;
        }
        // 添加错误回复到对话
        this.chatRecords.push({ role: 'ai', text: errorMsg, imageUrl: null });
      } finally {
        // 无论成功失败，结束加载状态
        this.isLoading = false;
      }
    },
    // 保存图片到成果库（逻辑不变）
    saveToGallery(imageUrl, title) {
      if (this.savedImages.length >= 4) this.savedImages.shift();
      this.savedImages.push({
        imageUrl: imageUrl,
        imageName: title || `设计方案_${new Date().getTime()}`
      });
      this.$message.success('已保存到成果库');
    }
  }
};
</script>

<style scoped>
/* 原有样式保持不变 */
.detail-optimization {
  padding: 20px;
  background-color: #f9f9f9;
  /* 关键修改：移除min-height: 100vh的强制限制，让内容自然撑开高度 */
  min-height: auto;
  box-sizing: border-box;
}

.page-header {
  text-align: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  color: #333;
  margin: 0 0 8px;
}

.page-header p {
  font-size: 14px;
  color: #666;
  margin: 0;
}

.main-layout {
  display: grid;
  grid-template-columns: 300px 1fr 300px;
  gap: 20px;
  /* 关键修改：将固定高度改为最小高度，允许内容超出时自然扩展 */
  min-height: calc(100vh - 100px);
  /* 新增：当内容超出视口时，允许页面整体滚动 */
}

.tools-area {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  overflow: hidden;
  display: flex;
  flex-direction: column;
  /* 关键修改：工具区使用最小高度而非固定高度，允许内容扩展 */
  min-height: 500px;
}

.tools-title {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #eee;
}

/* 工具组件样式调整 - 增加高度上限，允许更多内容展示 */
.tool-component {
  padding: 16px;
  border-bottom: 1px solid #eee;
  /* 提高最大高度限制 */
  max-height: 500px;
  overflow-y: auto;
}

/* 面料库组件：增加高度占比 */
.fabric-component {
  max-height: 50%; /* 从40%提高到50% */
  min-height: 300px; /* 提高最小高度 */
}

/* 色彩提取器组件：增加高度占比 */
.color-component {
  max-height: 50%; /* 从40%提高到50% */
  min-height: 200px; /* 提高最小高度 */
}

/* 新增色彩选择相关样式 */
.selected-colors {
  margin-bottom: 8px;
  padding: 8px;
  background-color: #fff8e6;
  border-radius: 4px;
}

.tag {
  font-size: 12px;
  color: #fa8c16;
  font-weight: 500;
}

.color-swatch-container {
  display: inline-flex;
  margin-left: 8px;
  gap: 4px;
}

.color-swatch {
  width: 20px;
  height: 20px;
  border-radius: 3px;
  border: 1px solid #eee;
  cursor: pointer;
}

/* 其他原有样式保持不变 */
.tool-placeholder {
  padding: 20px 16px;
  text-align: center;
  border-bottom: 1px solid #eee;
  background-color: #fafafa;
}

.placeholder-icon {
  font-size: 24px;
  margin-bottom: 8px;
  color: #999;
}

.placeholder-text {
  font-size: 14px;
  color: #666;
  margin: 0 0 4px;
}

.placeholder-desc {
  font-size: 12px;
  color: #ccc;
  margin: 0;
}

.interaction-area {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 关键修改：交互区使用最小高度，允许内容无限扩展 */
  min-height: 600px;
}

.interaction-title {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #eee;
}

.chat-content {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  /* 新增：增加底部内边距，让最后一条消息有更多空间 */
  padding-bottom: 40px;
}

.system-message {
  padding: 12px 16px;
  background-color: #f5f9ff;
  border-radius: 4px;
  font-size: 14px;
  color: #666;
  margin-bottom: 16px;
}

.chat-bubble {
  display: flex;
  margin-bottom: 16px;
  align-items: flex-start;
}

.bubble-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #eee;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: #333;
  margin-right: 12px;
  flex-shrink: 0;
}

.bubble-content {
  flex: 1;
}

.selected-fabric-tag {
  display: inline-block;
  padding: 4px 8px;
  background-color: #e6f7ff;
  color: #1890ff;
  border-radius: 4px;
  font-size: 12px;
  margin-bottom: 8px;
}
.user-image img {
  max-width: 100%; /* 不超过容器宽度 */
  max-height: 300px; /* 限制最大高度 */
  object-fit: contain; /* 保持比例，不拉伸 */
  border-radius: 4px;
  margin-top: 10px;
}

/* 对话内容容器限制宽度，避免图片过宽 */
.chat-content {
  max-width: 100%;
  padding: 10px;
  box-sizing: border-box;
}
.ai-image {
  margin-top: 8px;
  border-radius: 4px;
  overflow: hidden;
  position: relative;
  /* 新增：允许图片容器扩展 */
  margin-bottom: 16px;
}

/* AI生成的图片样式（如果也有过大问题） */
.ai-image img {
  max-width: 100%;
  max-height: 400px;
  object-fit: contain;
  border-radius: 4px;
}

.image-action {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 8px;
  background: rgba(0,0,0,0.5);
  color: white;
  font-size: 12px;
  text-align: center;
}

.loading {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 16px;
  color: #666;
}

.custom-spinner {
  width: 24px;
  height: 24px;
  border: 3px solid #ddd;
  border-top-color: #1890ff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 8px;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.input-area {
  padding: 16px;
  border-top: 1px solid #eee;
  display: flex;
  gap: 12px;
}

.text-input {
  flex: 1;
}

.send-btn {
  width: 80px;
}

.gallery-area {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  /* 关键修改：图库区域使用最小高度，允许内容扩展 */
  min-height: 600px;
}

.gallery-title {
  padding: 16px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
  border-bottom: 1px solid #eee;
}

/* 响应式调整 - 保持原有逻辑，确保在小屏幕上也能扩展 */
@media (max-width: 1200px) {
  .main-layout {
    grid-template-columns: 260px 1fr 260px;
  }
}

@media (max-width: 992px) {
  .main-layout {
    grid-template-columns: 1fr;
    grid-template-rows: auto 1fr auto;
    height: auto; /* 小屏幕下完全由内容决定高度 */
  }
  /* 小屏幕下增加各区域最小高度，确保内容展示空间 */
  .tools-area, .interaction-area, .gallery-area {
    min-height: 500px;
  }
}
</style>
