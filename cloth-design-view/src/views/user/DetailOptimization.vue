<template>
  <div class="detail-optimization">
    <!-- 页面标题 -->
    <div class="page-header">
      <h1>细节优化工具</h1>
      <p>上传图片/输入公网链接 → 选择素材 → 生成方案</p>
    </div>

    <!-- 主布局：左（工具）- 中（交互）- 右（成果） -->
    <div class="main-layout">
      <!-- 左侧工具区 -->
      <div class="tools-area">
        <div class="tools-title">设计工具</div>
        <fabric-library 
          @select="onFabricSelect" 
          class="tool-component fabric-component"
        ></fabric-library>
        <color-extractor 
          @apply-colors="onColorsApplied"
          class="tool-component color-component"
        ></color-extractor>
        <CollocationAssistant
          class="tool-component assistant-component"
          @select-recommend="onRecommendSelect"
        ></CollocationAssistant>
      </div>

      <!-- 中间交互区 -->
      <div class="interaction-area">
        <div class="interaction-title">AI设计对话</div>
        <div class="chat-content">
          <div class="system-message">
            请上传参考图片、输入公网链接或描述，我会生成设计方案
          </div>
          <!-- 聊天记录渲染（保持不变） -->
          <div v-for="(msg, index) in chatRecords" :key="index" class="chat-bubble">
            <div class="bubble-avatar">{{ msg.role === 'user' ? '我' : 'AI' }}</div>
            <div class="bubble-content">
              <template v-if="msg.role === 'user'">
                <div v-if="msg.fabric" class="selected-fabric-tag">已选择面料：{{ msg.fabric.name }}</div>
                <div v-if="msg.colors" class="selected-colors">
                  <span class="tag">已选择色彩：</span>
                  <div class="color-swatch-container">
                    <div v-for="(color, cIndex) in msg.colors" :key="cIndex" class="color-swatch" :style="{ backgroundColor: color.hex }" :title="color.hex"></div>
                  </div>
                </div>
                <p v-if="msg.text">{{ msg.text }}</p>
                <div v-if="msg.imageUrl" class="user-image">
                  <img :src="msg.imageUrl" alt="用户参考图片">
                </div>
              </template>
              <template v-if="msg.role === 'ai'">
                <p v-if="msg.text">{{ msg.text }}</p>
                <div v-if="msg.imageUrl" class="ai-image">
                  <img :src="msg.imageUrl" alt="AI生成方案" @click="saveToGallery(msg.imageUrl, msg.title)">
                  <div class="image-action">点击保存到右侧成果库</div>
                </div>
              </template>
            </div>
          </div>
          <div v-if="isLoading" class="loading">
            <div class="custom-spinner"></div>
            <p>方案生成中...</p>
          </div>
        </div>
        
        <!-- 输入区 -->
        <div class="input-area">
          <div class="input-controls">
            <!-- 【新增】公网URL输入框 -->
            <el-input 
              v-model="imageUrlInput" 
              placeholder="在此粘贴公共图片URL" 
              class="url-input"
              clearable
            ></el-input>
            
            <el-upload
              class="image-uploader"
              action=""
              :auto-upload="false"
              :on-change="handleImageUpload"
              :show-file-list="false"
              accept="image/jpeg,image/png,image/webp"
              :disabled="isLoading"
            >
              <el-button type="primary" icon="el-icon-upload" :disabled="!!imageUrlInput">上传本地图片</el-button>
            </el-upload>
          </div>
          
          <div class="main-input-row">
            <el-input 
              v-model="userInput" 
              placeholder="输入设计需求，例如：改成赛博朋克风格..." 
              class="text-input"
              @keyup.enter.native="sendMessage"
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
      </div>

      <!-- 右侧成果区 -->
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
import request from '@/utils/request'; 
import FabricLibrary from '@/components/FabricLibrary.vue';
import ImageSaveList from '@/components/ImageSaveList.vue';
import ColorExtractor from '@/components/ColorExtractor.vue';
import CollocationAssistant from '@/components/CollocationAssistant.vue';

export default {
  components: {
    FabricLibrary,
    ImageSaveList,
    ColorExtractor,
    CollocationAssistant
  },
  mounted() {
    // 登录状态判断（只执行一次）
    if (!this.currentUserId) {
      this.$router.push('/login'); // 未登录跳转到登录页
    } else {
      this.loadUserImages(); // 已登录才加载图片
    }
    console.log('Vuex用户信息:', this.$store.state.user.userInfo);
    console.log('当前用户ID:', this.currentUserId);
  },
  data() {
    return {
      chatRecords: [],
      userInput: '',
      savedImages: [],
      isLoading: false,
      currentFabric: null,
      currentColors: null,
      currentUploadedImageUrl: null,
      imageUrlInput: '',
      imgbbApiKey: 'ddfd3ce61ba74d50a1d1167d498e4c0f',

      // 【核心替换】将火山引擎配置替换为Coze图像编辑智能体配置
      cozeImageEditorConfig: {
        // 我们将通过代理调用，所以这里不需要apiUrl
        apiKey: 'pat_mY0OSWqTlyfxYW4cV8fu7vGY6jXupqFhQJ2FvyZfasve8SgKMGmFoFoS5Rshonq5',
        botId: '7533205966545272882',
        userId: 'user_' + Math.random().toString(36).substr(2, 9),
        // 默认图，当用户未提供任何图片时使用
        defaultImageUrl: "https://ark-project.tos-cn-beijing.volces.com/doc_image/seededit_i2i.jpeg"
      },
       currentUserId: this.$store.state.user.userInfo?.id || '',
    };
  },
  computed: {
    canSendMessage() {
      return !this.isLoading && (
             this.userInput.trim() !== '' || 
             this.currentFabric !== null || 
             this.currentColors !== null ||
             this.currentUploadedImageUrl !== null ||
             this.imageUrlInput.trim() !== ''
      );
    }
  },
  methods: {
    // onRecommendSelect, onFabricSelect, onColorsApplied, handleImageUpload, sendMessage, saveToGallery
    // 这些方法我们保持不变
    onRecommendSelect(recommend) {
      this.userInput = `按【${recommend.style}】风格设计：${recommend.desc}`;
      this.$message.success(`已为你填充${recommend.style}搭配需求`);
    },
    onFabricSelect(fabric) {
      this.currentFabric = fabric;
      this.$message.success(`已选择面料：${fabric.name}`);
    },
    onColorsApplied(colors) {
      this.currentColors = colors.slice(0, 4);
      this.$message.success('已选择色彩方案');
    },
    async handleImageUpload(file) {
      const imageFile = file.raw;
      if (!imageFile || this.isLoading) return;
      this.isLoading = true;
      const formData = new FormData();
      formData.append('image', imageFile);
      try {
        const response = await fetch(`https://api.imgbb.com/1/upload?key=${this.imgbbApiKey}`, {
          method: 'POST', body: formData,
        });
        const result = await response.json();
        if (result.success) {
          this.currentUploadedImageUrl = result.data.url;
          this.imageUrlInput = '';
          this.$message.success('图片上传成功！');
        } else {
          throw new Error(result.error?.message || 'ImgBB API 返回错误');
        }
      } catch (error) {
        this.$message.error(`上传失败：${error.message || '未知错误'}`);
      } finally {
        this.isLoading = false;
      }
    },
    sendMessage() {
      if (!this.canSendMessage) return;
      const finalImageUrl = this.imageUrlInput.trim() || this.currentUploadedImageUrl;
      const userMessage = {
        role: 'user',
        text: this.userInput,
        fabric: this.currentFabric,
        colors: this.currentColors,
        imageUrl: finalImageUrl
      };
      this.chatRecords.push(userMessage);
      this.generateAIResponse(finalImageUrl);
      this.userInput = '';
      this.currentFabric = null;
      this.currentColors = null;
      this.currentUploadedImageUrl = null;
      this.imageUrlInput = '';
    },
     saveToGallery(imageUrl, title) {
      // 这里的 title 可以根据你的需要调整，例如从 AI 的回复中提取
      const imageName = title || `设计方案_${new Date().getTime()}`; 
      this.saveImageToBackend({ imageUrl, imageName });
    },
     // DetailOptimization.vue - methods
    // DetailOptimization.vue - methods
    async saveImageToBackend(imageInfo) {
      try {
        // ⭐ 核心修改在这里：生成 ISO 字符串并去除毫秒部分 ⭐
        const now = new Date();
        // toISOString() 格式通常是 "YYYY-MM-DDTHH:mm:ss.sssZ" (24个字符 + 毫秒 + Z)
        // .slice(0, 19) 会截取到秒，去除毫秒和 Z
        // 然后我们再手动加上 'Z'
        const generateTimeFormatted = now.toISOString().slice(0, 19) + 'Z'; 

        const imageData = {
          userId: this.currentUserId,
          imageName: imageInfo.imageName, // 图片名称
          imageUrl: imageInfo.imageUrl,   // 图片URL
          generateTime: generateTimeFormatted // 使用处理后的日期字符串
        };
        console.log('保存图片到后端的数据:', imageData);

        const response = await request.post('/generatedImage/save', imageData);
        
        if (response.data && response.data.code === 200) {
          this.$message.success('图片已保存到成果库！');
          this.loadUserImages(); // 保存成功后，重新加载列表以更新显示
        } else {
          this.$message.error(response.data.msg || '保存图片失败');
        }
      } catch (error) {
        console.error('保存图片失败:', error);
        this.$message.error('保存图片异常');
      }
    },
async generateAIResponse(imageUrl) {
  console.log("🔴[DEBUG] 你即将发送 POST /__coze_proxy_submit__");
  this.isLoading = true;
  
  // 1. 先添加一个占位消息，用于实时更新
  const aiMsgPlaceholder = {
    role: 'ai',
    text: '已接单，AI正在全力创作中，请耐心等待...', // 初始文本
    imageUrl: '', // 初始为空
    title: `AI设计方案`
  };
  this.chatRecords.push(aiMsgPlaceholder);
  const feedbackMessageIndex = this.chatRecords.length - 1; // 获取这个占位消息的索引

  let fullPrompt = `编辑需求：${this.userInput.trim() || '请根据素材进行优化'}`;
  if (this.currentFabric) fullPrompt += `，使用【${this.currentFabric.name}】面料`;
  if (this.currentColors) {
    const colorHexList = this.currentColors.map(c => c.hex).join('、');
    fullPrompt += `，搭配颜色：${colorHexList}`;
  }
  const finalApiImageUrl = imageUrl || this.cozeImageEditorConfig.defaultImageUrl;
  fullPrompt += `，参考图片URL：${finalApiImageUrl}`;

  try {
    const response = await fetch('/__coze_proxy_submit__', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${this.cozeImageEditorConfig.apiKey}`
      },
      body: JSON.stringify({
        bot_id: this.cozeImageEditorConfig.botId,
        user_id: this.cozeImageEditorConfig.userId,
        stream: true,
        auto_save_history: true,
        additional_messages: [
          {
            role: "user",
            content: fullPrompt,
            content_type: "text"
          }
        ]
      })
    });

    if (!response.ok) {
      const errorText = await response.text();
      console.error(`请求失败: ${response.status}, 响应内容: ${errorText}`);
      throw new Error(`API 请求失败: ${response.status} ${response.statusText}`);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let fullReplyContent = ''; // 用于拼接 AI 完整的文本回复（包括 Markdown 链接）
    let finalImageUrlFromAI = ''; // 用于存储最终提取到的图片 URL
    let currentEvent = null;

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;

      const chunk = decoder.decode(value, { stream: true });
      const lines = chunk.split('\n').filter(line => line.trim());

      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent = line.replace('event:', '').trim();
          continue;
        }

        if (line.startsWith('data:')) {
          const dataStr = line.replace('data:', '').trim();
          if (dataStr) {
            try {
              const data = JSON.parse(dataStr);

              // 优先从 tool_response 中提取 URL（如果它先出现且可靠）
              if (data.type === 'tool_response' && data.content) {
                try {
                  const toolResponse = JSON.parse(data.content);
                  if (toolResponse.msg === 'success' && typeof toolResponse.data === 'string' && toolResponse.data.startsWith('https://')) {
                    finalImageUrlFromAI = toolResponse.data;
                    console.log("从 tool_response 提取到图片 URL:", finalImageUrlFromAI);
                  }
                } catch (e) {
                  console.warn("解析 tool_response 的 content 失败:", e);
                }
              }
              // 从 answer 类型中拼接文本，并尝试提取 Markdown 链接
              else if (data.type === 'answer' && data.content) {
                fullReplyContent += data.content; // 拼接完整的文本内容
                
                // 实时更新占位消息的文本
                // 注意：这里只更新文本，图片会在流结束后统一设置
                this.$set(this.chatRecords[feedbackMessageIndex], 'text', `AI正在生成... ${fullReplyContent}`);

                const match = data.content.match(/\[.*?\]\((https?:\/\/[^\s)]+\.(?:jpg|png|jpeg|gif|webp|bmp))\)/);
                if (match && match[1]) {
                  finalImageUrlFromAI = match[1]; // 提取到 URL
                  console.log("从AI文字回复 (answer) 中提取到图片 URL:", finalImageUrlFromAI);
                }
              }

            } catch (e) {
              console.warn('解析流数据片段失败:', e);
            }
          }
        }
      }
    }
    if (finalImageUrlFromAI) {
      // 移除 fullReplyContent 中的 Markdown 链接文本，只显示纯文本部分
      const displayText = fullReplyContent.replace(/\[.*?\]\((https?:\/\/[^\s)]+\.(?:jpg|png|jpeg|gif|webp|bmp))\)/g, '').trim();
      
      // 更新占位消息的所有属性
      this.$set(this.chatRecords[feedbackMessageIndex], 'text', displayText);
      this.$set(this.chatRecords[feedbackMessageIndex], 'imageUrl', finalImageUrlFromAI);
      // this.$set(this.chatRecords[feedbackMessageIndex], 'title', `AI设计方案`); // 标题可以保持不变

      // 将完整的回复（包含 Markdown）保存到 lastAiReply，以便 ImageViewer 处理
      this.lastAiReply = fullReplyContent; 

    } else {
      // 如果没有提取到图片 URL，但 AI 仍然返回了文字，则显示文字
      if (fullReplyContent.trim()) {
        this.$set(this.chatRecords[feedbackMessageIndex], 'text', fullReplyContent.trim());
        this.lastAiReply = fullReplyContent;
      } else {
        // 如果既没有图片 URL，也没有文字回复，才抛出错误
        throw new Error('AI 生成成功，但未找到图片 URL 或有效文本回复');
      }
    }

  } catch (error) {
    console.error('Coze任务全流程失败：', error);
    const errorMsg = error.message || '未知网络错误';
    
    // 更新占位消息为错误信息
    if (this.chatRecords[feedbackMessageIndex]) {
        this.$set(this.chatRecords[feedbackMessageIndex], 'text', `❌ 生成失败：${errorMsg}`);
        this.$set(this.chatRecords[feedbackMessageIndex], 'imageUrl', ''); // 清空图片
    }
  } finally {
    this.isLoading = false;
    this.$nextTick(() => {
        const container = this.$el.querySelector('.chat-messages');
        if (container) {
            container.scrollTop = container.scrollHeight;
        }
    });
  }
},
 async loadUserImages() {
      try {
        const queryDto = {
          userId: this.currentUserId,
          pageNum: 1, // 假设你需要分页，或者只获取最新几张
          pageSize: 4 // 只显示最新的4张，与 CozeChat 保持一致
        };
        console.log('前端查询参数：', queryDto);

        // 假设你的后端查询接口是 /generatedImage/query
        const response = await request.post('/generatedImage/query', queryDto);
        
        console.log('后端响应完整数据：', response); 
        console.log('后端响应data字段：', response.data);

        if (response.data && response.data.code === 200) { 
          console.log('后端返回的图片列表：', response.data.data);

          // CozeChat 示例中后端已排序，直接取 slice
          this.savedImages = response.data.data.slice(0, 4);
          
          console.log('前端savedImages赋值后：', this.savedImages); 
        } else {
          console.warn('后端返回非成功状态：', response.data);
          this.$message.error(response.data.msg || '加载成果库失败');
        }
      } catch (error) {
        console.error('加载成果库失败：', error);
        this.$message.error('加载成果库异常');
      }
    },
    /**
     * 【新增】轮询辅助函数
     * @param {string} conversationId “取件凭证”
     * @returns {Promise<object>} 包含最终结果的完整对象
     */
    async pollForResult(conversationId) {
      const maxAttempts = 20; // 最多轮询20次
      const delay = 3000;     // 每次间隔3秒

      for (let i = 0; i < maxAttempts; i++) {
        console.log(`第 ${i + 1} 次尝试取件...`);
        try {
          const retrieveResponse = await axios.get(
            '/__coze_proxy_retrieve__',
            {
              params: {
                conversation_id: conversationId,
                bot_id: this.cozeImageEditorConfig.botId
              },
              headers: { 'Authorization': `Bearer ${this.cozeImageEditorConfig.apiKey}` }
            }
          );
          
          const data = retrieveResponse.data?.data;
          
          if (data?.status === 'completed') {
            console.log('🎉 图片生成完成！', data);
            return data; // 返回成功的结果
          } else if (data?.status === 'failed') {
            throw new Error(`图片生成任务失败: ${data.last_error?.msg || '未知原因'}`);
          }
          // 如果是 in_progress，则继续等待
        } catch (error) {
           // 如果是取件API本身报错，直接抛出
           throw new Error(`取件API请求失败: ${error.message}`);
        }

        // 等待指定时间后进行下一次轮询
        await new Promise(resolve => setTimeout(resolve, delay));
      }

      // 如果轮询次数用尽还未成功
      throw new Error('图片生成超时，请稍后再试');
    }
}
}
</script>

<style scoped>
/* 为了新加的输入框，微调一下样式 */
.input-area {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 10px;
  border-top: 1px solid #e0e0e0;
}
.input-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}
.url-input {
  flex-grow: 1;
}
.main-input-row {
  display: flex;
  gap: 8px;
}
.text-input {
  flex-grow: 1;
}
/* 其他样式保持不变 */
.detail-optimization {
  padding: 20px;
  background-color: #f4f6f8;
  height: 100vh;
  box-sizing: border-box;
}
.page-header {
  text-align: center;
  margin-bottom: 20px;
}
.page-header h1 {
  font-size: 28px;
  color: #333;
}
.page-header p {
  color: #888;
}
.main-layout {
  display: grid;
  grid-template-columns: 280px 1fr 300px;
  gap: 20px;
  height: calc(100vh - 120px);
}
.tools-area, .interaction-area, .gallery-area {
  background-color: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.05);
  display: flex;
  flex-direction: column;
}
.tools-title, .interaction-title, .gallery-title {
  padding: 15px;
  font-weight: bold;
  color: #333;
  border-bottom: 1px solid #eee;
}
.tool-component {
  margin: 10px;
}
.interaction-area {
  height: 100%;
}
.chat-content {
  flex-grow: 1; /* 保持 flex-grow: 1 以占据可用空间 */
  overflow-y: auto; /* 启用垂直滚动条 */
  padding: 15px;
  /* ⭐ 新增以下两行 ⭐ */
  max-height: calc(100vh - 100px); /* 根据实际布局调整此值，这是一个示例 */
                                 /* 100vh - (页面顶部header + 输入区 + 其他固定高度) */
  min-height: 200px; /* 也可以设置一个最小高度 */
}
/* 确保父容器 interaction-area 也有确定的高度，才能让 flex-grow: 1 生效 */
.interaction-area {
  height: 100%; /* 保持这个，确保它有明确的高度 */
  display: flex; /* 确保是 flex 容器 */
  flex-direction: column; /* 垂直布局 */
}
.system-message {
  text-align: center;
  color: #aaa;
  font-size: 12px;
  margin-bottom: 15px;
}
.chat-bubble {
  display: flex;
  margin-bottom: 15px;
  max-width: 90%;
}
.chat-bubble .bubble-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background-color: #007bff;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 10px;
  flex-shrink: 0;
}
.chat-bubble .bubble-content {
  background-color: #f0f0f0;
  border-radius: 12px;
  padding: 10px 15px;
  word-break: break-word;
}
.chat-bubble .user-image, .chat-bubble .ai-image {
  margin-top: 8px;
}
.chat-bubble .user-image img, .chat-bubble .ai-image img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 6px;
}
.ai-image img {
  cursor: pointer;
}
.image-action {
  font-size: 12px;
  color: #888;
  margin-top: 4px;
}
.loading {
  text-align: center;
  color: #888;
}
.custom-spinner {
  margin: 10px auto;
  width: 30px;
  height: 30px;
  border: 3px solid #f3f3f3;
  border-top: 3px solid #007bff;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  0% { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}
.selected-fabric-tag, .selected-colors {
  background-color: #e9f5ff;
  border: 1px solid #d0ebff;
  border-radius: 4px;
  padding: 5px 8px;
  margin-bottom: 5px;
  font-size: 13px;
}
.color-swatch-container {
  display: flex;
  gap: 5px;
  margin-top: 5px;
}
.color-swatch {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  border: 1px solid #ccc;
}
</style>
