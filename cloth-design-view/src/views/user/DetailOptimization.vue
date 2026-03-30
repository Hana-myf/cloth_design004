<template>
  <div class="detail-optimization">


    <!-- 主布局：左(自定义工具区)-中(对话区)-右(预览+成果库) -->
    <div class="main-layout">
      <!-- 左侧：自定义工具区（彻底替换Tabs） -->
      <div class="tools-area">
        <!-- 自定义标签栏 -->
        <div class="custom-tab-header">
          <div 
            class="custom-tab-item"
            :class="{ active: activeToolTab === 'fabric' }"
            @click="activeToolTab = 'fabric'"
          >
            面料库
          </div>
          <div 
            class="custom-tab-item"
            :class="{ active: activeToolTab === 'color' }"
            @click="activeToolTab = 'color'"
          >
            色彩提取器
          </div>
          <div 
            class="custom-tab-item"
            :class="{ active: activeToolTab === 'assistant' }"
            @click="activeToolTab = 'assistant'"
          >
            搭配助手
          </div>
        </div>

        <!-- 工具内容区 -->
        <div class="custom-tab-content">
          <fabric-library 
            @select="onFabricSelect" 
            v-if="activeToolTab === 'fabric'"
            class="tool-component"
          ></fabric-library>
          <color-extractor 
            @apply-colors="onColorsApplied"
            v-if="activeToolTab === 'color'"
            class="tool-component"
          ></color-extractor>
          <CollocationAssistant
            v-if="activeToolTab === 'assistant'"
            class="tool-component"
            @select-recommend="onRecommendSelect"
          ></CollocationAssistant>
        </div>
      </div>

      <!-- 中间：对话交互区（保留原有逻辑） -->
      <div class="interaction-area">
        <div class="interaction-title">AI设计对话</div>
        <div class="chat-content">
          <div class="system-message">
            请上传参考图片、输入公网链接或描述，我会生成设计方案
          </div>
          <div v-for="(msg, index) in chatRecords" :key="index" class="chat-bubble">
            <div class="bubble-avatar" :class="msg.role === 'user'? 'user-avatar' : 'ai-avatar'">
              {{ msg.role === 'user'? '我' : 'AI' }}
            </div>
            <div class="bubble-content" :class="msg.role === 'user'? 'user-content' : 'ai-content'">
              <template v-if="msg.role === 'user'">
                <div v-if="msg.fabric" class="selected-fabric-tag">已选面料：{{ msg.fabric.name }}</div>
                <div v-if="msg.colors" class="selected-colors">
                  <span class="tag">已选色彩：</span>
                  <div class="color-swatch-container">
                    <div v-for="(color, cIndex) in msg.colors" :key="cIndex" class="color-swatch" :style="{ backgroundColor: color.hex }" :title="color.hex"></div>
                  </div>
                </div>
                <p v-if="msg.text">{{ msg.text }}</p>
                <div v-if="msg.imageUrl" class="user-image">
                  <img :src="msg.imageUrl" alt="用户参考图片" @click="previewImage(msg.imageUrl)">
                  <div class="image-action">右侧预览</div>
                </div>
              </template>
              <template v-if="msg.role === 'ai'">
                <p v-if="msg.text">{{ msg.text }}</p>
                <div v-if="msg.imageUrl" class="ai-image">
                  <img :src="msg.imageUrl" alt="AI生成方案" @click="previewImage(msg.imageUrl)">
                  <div class="image-action">点击预览 | 右键保存到成果库</div>
                </div>
              </template>
            </div>
          </div>
          <div v-if="isLoading" class="loading">
            <div class="custom-spinner"></div>
            <p>{{ isUploading ? '图片上传中...' : '方案生成中...' }}</p>
          </div>
        </div>
        
        <div class="input-area">
          <div class="input-controls">
            <el-input 
              v-model="imageUrlInput" 
              placeholder="粘贴公共图片URL" 
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
              <el-button type="primary" icon="el-icon-upload" :disabled="!!imageUrlInput" class="upload-btn">上传本地图片</el-button>
            </el-upload>
          </div>
          
          <div class="main-input-row">
            <el-input 
              v-model="userInput" 
              placeholder="输入设计需求（例：改成赛博朋克风格）..." 
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

      <!-- 右侧：预览区 + 成果库（统一样式） -->
      <div class="gallery-area">
        <div class="gallery-title">图片预览 & 成果库</div>
        
        <!-- 上半部分：高清预览区 -->
        <div class="preview-section">
          <div class="img-placeholder-container">
            <img 
              :src="currentPreviewImage" 
              alt="预览图片"
              class="preview-img"
              v-if="currentPreviewImage"
            >
            <div class="img-placeholder" v-else>
              <div class="placeholder-content">
                <i class="el-icon-picture-outline"></i>
                <p>右侧预览</p>
              </div>
            </div>
          </div>
          <!-- 预览区保存按钮 -->
          <button 
            @click="saveCurrentPreviewImage" 
            class="save-img-btn"
            v-if="currentPreviewImage"
          >
            保存到成果库
          </button>
        </div>

        <!-- 下半部分：成果库列表 -->
        <div class="list-section">
          <image-save-list 
            :saved-images="savedImages"
            class="save-list"
            @click-image="previewImage"
          ></image-save-list>
        </div>
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
    // 登录状态判断
    if (!this.currentUserId) {
      this.$router.push('/login');
    } else {
      this.loadUserImages();
    }
  },
  data() {
    return {
      chatRecords: [],
      userInput: '',
      savedImages: [],
      isLoading: false,
      isUploading: false,
      currentFabric: null,
      currentColors: null,
      currentUploadedImageUrl: null,
      imageUrlInput: '',
      imgbbApiKey: 'ddfd3ce61ba74d50a1d1167d498e4c0f',
      cozeImageEditorConfig: {
        apiKey: 'pat_oNhoddgk898CKMqK8avnbPzZt7UlcXoiWxHW3QXanyIdTX3YExf0JHVKfHdXDgYH',
        botId: '7533205966545272882',
        userId: 'user_' + Math.random().toString(36).substr(2, 9),
        defaultImageUrl: "https://ark-project.tos-cn-beijing.volces.com/doc_image/seededit_i2i.jpeg"
      },
      currentUserId: this.$store.state.user.userInfo?.id || '',
      activeToolTab: 'fabric', // 保留原有激活状态逻辑
      currentPreviewImage: '' // 当前预览的图片URL
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
    // 预览图片方法
    previewImage(imageUrl) {
      this.currentPreviewImage = imageUrl;
      // 滚动到右侧预览区
      const galleryEl = this.$el.querySelector('.gallery-area');
      if (galleryEl) galleryEl.scrollTop = 0;
    },
    // 保存当前预览的图片
    saveCurrentPreviewImage() {
      if (!this.currentPreviewImage) return;
      const imageName = `设计方案_${new Date().getTime()}`;
      this.saveImageToBackend({ 
        imageUrl: this.currentPreviewImage, 
        imageName 
      });
    },

    // 搭配助手选择推荐
    onRecommendSelect(recommend) {
      this.userInput = `按【${recommend.style}】风格设计：${recommend.desc}`;
      this.$message.success(`已为你填充${recommend.style}搭配需求`);
    },
    // 选择面料
    onFabricSelect(fabric) {
      this.currentFabric = fabric;
      this.$message.success(`已选择面料：${fabric.name}`);
    },
    // 应用提取的色彩
    onColorsApplied(colors) {
      this.currentColors = colors.slice(0, 4);
      this.$message.success('已选择色彩方案');
    },
    // 上传本地图片
    async handleImageUpload(file) {
      const imageFile = file.raw;
      if (!imageFile || this.isLoading) return;
      this.isLoading = true;
      this.isUploading = true;
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
        this.isUploading = false;
      }
    },
    // 发送设计需求
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
    // 保存到成果库（兼容原有逻辑）
    saveToGallery(imageUrl, title) {
      this.previewImage(imageUrl);
      const imageName = title || `设计方案_${new Date().getTime()}`; 
      this.saveImageToBackend({ imageUrl, imageName });
    },
    // 保存图片到后端
    async saveImageToBackend(imageInfo) {
      try {
        const now = new Date();
        const generateTimeFormatted = now.toISOString().slice(0, 19) + 'Z'; 
        const imageData = {
          userId: this.currentUserId,
          imageName: imageInfo.imageName,
          imageUrl: imageInfo.imageUrl,
          generateTime: generateTimeFormatted
        };
        const response = await request.post('/generatedImage/save', imageData);
        if (response.data && response.data.code === 200) {
          this.$message.success('图片已保存到成果库！');
          this.loadUserImages(); // 重新加载成果库
        } else {
          this.$message.error(response.data.msg || '保存图片失败');
        }
      } catch (error) {
        console.error('保存图片失败:', error);
        this.$message.error('保存图片异常');
      }
    },
    // 生成AI设计方案
    async generateAIResponse(imageUrl) {
      this.isLoading = true;
      this.isUploading = false;
      const aiMsgPlaceholder = {
        role: 'ai',
        text: '已接单，AI正在全力创作中，请耐心等待...',
        imageUrl: '',
        title: `AI设计方案`
      };
      this.chatRecords.push(aiMsgPlaceholder);
      const feedbackMessageIndex = this.chatRecords.length - 1;

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
          throw new Error(`API 请求失败: ${response.status} ${response.statusText}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let fullReplyContent = '';
        let finalImageUrlFromAI = '';
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

                  if (data.type === 'tool_response' && data.content) {
                    try {
                      const toolResponse = JSON.parse(data.content);
                      if (toolResponse.msg === 'success' && typeof toolResponse.data === 'string' && toolResponse.data.startsWith('https://')) {
                        finalImageUrlFromAI = toolResponse.data;
                      }
                    } catch (e) {
                      console.warn("解析 tool_response 失败:", e);
                    }
                  } else if (data.type === 'answer' && data.content) {
                    fullReplyContent += data.content;
                    this.$set(this.chatRecords[feedbackMessageIndex], 'text', `AI正在生成... ${fullReplyContent}`);
                    const match = data.content.match(/\[.*?\]\((https?:\/\/[^\s)]+\.(?:jpg|png|jpeg|gif|webp|bmp))\)/);
                    if (match && match[1]) {
                      finalImageUrlFromAI = match[1];
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
          let displayText = fullReplyContent.replace(/\[.*?\]\((https?:\/\/[^\s)]+\.(?:jpg|png|jpeg|gif|webp|bmp))\)/g, '').trim();
          // 对回复内容进行去重处理
          displayText = this.removeDuplicateContent(displayText);
          this.$set(this.chatRecords[feedbackMessageIndex], 'text', displayText);
          this.$set(this.chatRecords[feedbackMessageIndex], 'imageUrl', finalImageUrlFromAI);
          this.lastAiReply = fullReplyContent;
        } else {
          if (fullReplyContent.trim()) {
            // 对回复内容进行去重处理
            const processedText = this.removeDuplicateContent(fullReplyContent.trim());
            this.$set(this.chatRecords[feedbackMessageIndex], 'text', processedText);
            this.lastAiReply = fullReplyContent;
          } else {
            throw new Error('AI 生成成功，但未找到图片 URL 或有效文本回复');
          }
        }

      } catch (error) {
        console.error('Coze任务失败：', error);
        const errorMsg = error.message || '未知网络错误';
        if (this.chatRecords[feedbackMessageIndex]) {
            this.$set(this.chatRecords[feedbackMessageIndex], 'text', `❌ 生成失败：${errorMsg}`);
            this.$set(this.chatRecords[feedbackMessageIndex], 'imageUrl', '');
        }
      } finally {
        this.isLoading = false;
        this.$nextTick(() => {
            const container = this.$el.querySelector('.chat-content');
            if (container) {
                container.scrollTop = container.scrollHeight;
            }
        });
      }
    },
    // 加载用户成果库图片
    async loadUserImages() {
      try {
        const queryDto = {
          userId: this.currentUserId,
          pageNum: 1,
          pageSize: 4
        };
        console.log('查询成果库参数：', queryDto); // 调试用
        const response = await request.post('/generatedImage/query', queryDto);
        console.log('成果库返回数据：', response.data); // 调试用
        if (response.data && response.data.code === 200) { 
          this.savedImages = response.data.data.slice(0, 4);
          console.log('渲染成果库数据：', this.savedImages); // 调试用
        } else {
          console.warn('后端返回非成功状态：', response.data);
          this.$message.error(response.data.msg || '加载成果库失败');
        }
      } catch (error) {
        console.error('加载成果库失败：', error);
        this.$message.error('加载成果库异常');
      }
    },
    // 轮询获取生成结果
    async pollForResult(conversationId) {
      const maxAttempts = 20;
      const delay = 3000;
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
            return data;
          } else if (data?.status === 'failed') {
            throw new Error(`图片生成任务失败: ${data.last_error?.msg || '未知原因'}`);
          }
        } catch (error) {
           throw new Error(`取件API请求失败: ${error.message}`);
        }
        await new Promise(resolve => setTimeout(resolve, delay));
      }
      throw new Error('图片生成超时，请稍后再试');
    },

    /* 移除重复内容 */
    removeDuplicateContent(content) {
      if (!content) return content;
      
      // 首先尝试检测完全重复的内容
      const contentLength = content.length;
      if (contentLength > 10) { // 只有当内容足够长时才进行去重
        // 检查是否有重复的一半内容
        const halfLength = Math.floor(contentLength / 2);
        const firstHalf = content.substring(0, halfLength);
        const secondHalf = content.substring(halfLength);
        
        // 如果前后两半内容相似，只保留前半部分
        if (firstHalf.trim() === secondHalf.trim()) {
          return firstHalf.trim() + (content.endsWith('。') ? '。' : '');
        }
      }
      
      // 然后检测重复的句子
      const sentences = content.split('。').filter(s => s.trim());
      const uniqueSentences = [];
      const seen = new Set();
      
      for (const sentence of sentences) {
        const trimmed = sentence.trim();
        if (trimmed && !seen.has(trimmed)) {
          seen.add(trimmed);
          uniqueSentences.push(trimmed);
        }
      }
      
      return uniqueSentences.join('。') + (content.endsWith('。') ? '。' : '');
    }
  }
};
</script>

<style scoped>
/* 全局手绘风基底 */
.detail-optimization {
  padding: 20px;
  background: url("https://pic1.imgdb.cn/item/6958e1eeda3df73ea1bc5a2c.png") center/cover no-repeat;
  background-color: rgba(255, 255, 250, 0.95);
  background-blend-mode: overlay;
  height: 100vh;
  box-sizing: border-box;
  font-family: "Microsoft YaHei", Arial, sans-serif;
  overflow: hidden; /* 防止页面整体滚动 */
}

/* 页面标题 */
.page-header {
  text-align: center;
  margin-bottom: 20px;
  color: #8a7d5e;
}
.page-header h1 {
  font-size: 28px;
  margin: 0 0 8px 0;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}
.page-header h1::before {
  content: "";
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #f0c880;
  box-shadow: 0 0 4px rgba(240, 200, 128, 0.5);
}
.page-header p {
  color: #a09880;
  margin: 0;
}

/* 主布局 - 核心修复：调整高度计算+最小高度 */
.main-layout {
  display: grid;
  grid-template-columns: 280px 1fr 320px; 
  gap: 20px;
  height: calc(100vh - 80px); /* 适配删除页面标题后的高度 */
  min-height: 600px; /* 小屏幕最小高度，防止布局坍塌 */
}

/* 左侧工具区 - 完全自定义，无ElementUI Tabs干扰 */
.tools-area {
  background-color: rgba(255, 255, 250, 0.98);
  border-radius: 16px;
  box-shadow: 0 6px 18px rgba(240, 224, 180, 0.15);
  overflow: hidden;
  display: flex;
  flex-direction: column; /* 纵向布局 */
}

/* 自定义标签栏 - 替代ElementUI Tabs */
.custom-tab-header {
  display: flex;
  background-color: #fff8e8;
  border-bottom: 1px solid #f0e8d0;
  padding: 0 10px;
}
.custom-tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  color: #8a7d5e;
  cursor: pointer;
  font-size: 14px;
  position: relative;
  transition: color 0.2s ease;
}
.custom-tab-item:hover {
  color: #f0c880;
}
.custom-tab-item.active {
  color: #f0c880;
  font-weight: 500;
}
/* 激活标签下划线 */
.custom-tab-item.active::after {
  content: "";
  position: absolute;
  bottom: -1px;
  left: 15%;
  width: 70%;
  height: 2px;
  background-color: #f0c880;
  border-radius: 1px;
}

/* 自定义标签内容区 */
.custom-tab-content {
  flex: 1;
  overflow: hidden;
}
/* 工具组件容器 - 撑满内容区，无白色背景 */
.tool-component {
  padding: 15px;
  background-color: transparent;
  height: 100%;
  box-sizing: border-box;
}

/* 中间对话区 - 微调样式，与左右侧统一 */
.interaction-area {
  background-color: rgba(255, 255, 250, 0.98);
  border-radius: 16px;
  box-shadow: 0 6px 18px rgba(240, 224, 180, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.interaction-title {
  padding: 12px 18px;
  font-weight: 500;
  color: #8a7d5e;
  border-bottom: 1px solid #f0e8d0;
  background-color: #fff8e8;
}

.chat-content {
  flex-grow: 1;
  overflow-y: auto;
  padding: 18px;
  background-color: #fffdf8;
  background-image: linear-gradient(90deg, transparent 0%, transparent 98%, rgba(245, 220, 168, 0.2) 98%, rgba(245, 220, 168, 0.2) 100%);
  background-size: 25px 100%;
}
.chat-content::-webkit-scrollbar {
  width: 8px;
}
.chat-content::-webkit-scrollbar-track {
  background-color: #fff8e8;
  border-radius: 4px;
}
.chat-content::-webkit-scrollbar-thumb {
  background-color: #f5dca8;
  border-radius: 4px;
  border: 1px solid #fff8e8;
}

.system-message {
  text-align: center;
  color: #a09880;
  font-size: 12px;
  margin-bottom: 15px;
}

.chat-bubble {
  display: flex;
  margin-bottom: 15px;
  max-width: 90%;
}
.bubble-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: bold;
  margin-right: 10px;
  flex-shrink: 0;
  border: 2px solid;
}
.user-avatar {
  background-color: #f8fff0;
  color: #7d8a5e;
  border-color: #e0f0d8;
}
.ai-avatar {
  background-color: #fff8e8;
  color: #8a7d5e;
  border-color: #f5dca8;
}
.bubble-content {
  padding: 12px 16px;
  border-radius: 20px;
  word-break: break-word;
  border: 1px solid;
  box-shadow: 0 3px 8px rgba(0,0,0,0.05);
  position: relative;
}
.user-content {
  background-color: #f8fff0;
  border-color: #e0f0d8;
  margin-left: auto;
}
.ai-content {
  background-color: #fff8e8;
  border-color: #f0e8d0;
}
.ai-content::after {
  content: "";
  position: absolute;
  left: -8px;
  top: 15px;
  width: 12px;
  height: 12px;
  background-color: #fff8e8;
  border: 1px solid #f0e8d0;
  border-right: none;
  border-bottom: none;
  transform: rotate(45deg);
  box-shadow: -2px -2px 2px rgba(240, 224, 180, 0.08);
}
.user-content::after {
  content: "";
  position: absolute;
  right: -8px;
  top: 15px;
  width: 12px;
  height: 12px;
  background-color: #f8fff0;
  border: 1px solid #e0f0d8;
  border-left: none;
  border-top: none;
  transform: rotate(45deg);
  box-shadow: 2px 2px 2px rgba(224, 240, 216, 0.08);
}

.selected-fabric-tag, .selected-colors {
  background-color: #fff8e8;
  border: 1px solid #f0e8d0;
  border-radius: 4px;
  padding: 5px 8px;
  margin-bottom: 5px;
  font-size: 13px;
  color: #8a7d5e;
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
  border: 1px solid #f0e8d0;
}

.user-image, .ai-image {
  margin-top: 8px;
}
.user-image img, .ai-image img {
  max-width: 200px;
  max-height: 200px;
  border-radius: 8px;
  border: 1px solid #f0e8d0;
  box-shadow: 0 3px 8px rgba(240, 224, 180, 0.15);
  cursor: pointer;
}
.image-action {
  font-size: 12px;
  color: #a09880;
  margin-top: 4px;
  text-align: center;
}

.loading {
  text-align: center;
  color: #8a7d5e;
  padding: 10px 0;
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

.input-area {
  padding: 12px;
  border-top: 1px solid #f0e8d0;
  background-color: #fffdf8;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.input-controls {
  display: flex;
  gap: 8px;
  align-items: center;
}
.url-input {
  flex-grow: 1;
  --el-input-border-color: #f0e8d0;
  --el-input-hover-border-color: #f0c880;
  border-radius: 22px;
}
/* 统一按钮样式 - 无蓝色 */
.upload-btn, .send-btn {
  --el-button-bg-color: #f5dca8 !important;
  --el-button-border-color: #f0c880 !important;
  --el-button-text-color: #8a7d5e !important;
  --el-button-hover-bg-color: #f0c880 !important;
  --el-button-hover-text-color: #7a6d4e !important;
  --el-button-disabled-bg-color: #f9eeda !important;
  --el-button-disabled-border-color: #f5dca8 !important;
  --el-button-disabled-text-color: #a09880 !important;
  border-radius: 22px !important;
}
.main-input-row {
  display: flex;
  gap: 8px;
}
.text-input {
  flex-grow: 1;
  --el-input-border-color: #f0e8d0;
  --el-input-hover-border-color: #f0c880;
  border-radius: 22px;
}

/* 右侧：预览+成果库 - 与左侧视觉完全统一 */
.gallery-area {
  background-color: rgba(255, 255, 250, 0.98);
  border-radius: 16px;
  box-shadow: 0 6px 18px rgba(240, 224, 180, 0.15);
  display: flex;
  flex-direction: column;
  overflow: hidden;
  height: 100%; /* 强制继承父容器高度 */
}
.gallery-title {
  padding: 12px 18px;
  font-weight: 500;
  color: #8a7d5e;
  border-bottom: 1px solid #f0e8d0;
  background-color: #fff8e8;
  flex-shrink: 0; /* 禁止标题被压缩 */
}

/* 上半部分：预览区 - 限制最大高度，不再占满 */
.preview-section {
  padding: 15px;
  border-bottom: 1px solid #f0e8d0;
  flex-shrink: 0; /* 禁止预览区被压缩 */
}
.img-placeholder-container {
  width: 100%;
  max-height: 286px; /* 核心：限制预览区最大高度，扩大1.3倍 */
  aspect-ratio: 1/1; /* 保持正方形，但不超过max-height */
  margin: 0 auto 10px;
  position: relative;
}
.img-placeholder {
  width: 100%;
  height: 100%;
  background-color: #fff8e8;
  border: 1px dashed #f0e8d0;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: #8a7d5e;
}
.placeholder-content i {
  font-size: 48px;
  margin-bottom: 10px;
  color: #f0c880;
}
.placeholder-content p {
  font-size: 14px;
  margin: 0;
}
.preview-img {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background-color: #fffdf8;
  border: 1px solid #f0e8d0;
  border-radius: 8px;
}
/* 预览区保存按钮 */
.save-img-btn {
  width: 100%;
  padding: 8px 0;
  background-color: #f5dca8;
  color: #8a7d5e;
  border: 1px solid #f0c880;
  border-radius: 20px;
  cursor: pointer;
  font-size: 14px;
  transition: all 0.2s ease;
}
.save-img-btn:hover {
  background-color: #f0c880;
  color: #7a6d4e;
  box-shadow: 0 2px 6px rgba(240, 200, 128, 0.15);
}

/* 下半部分：成果库列表 - 强制预留最小高度 */
.list-section {
  flex-grow: 1; /* 占满剩余所有高度 */
  overflow-y: auto;
  padding: 12px;
  min-height: 180px; /* 核心：给列表设置最小高度，确保可见 */
}
.save-list {
  width: 100%;
}
/* 成果库列表样式优化 */
:deep(.save-list .history-item) {
  border: 1px solid #f0e8d0;
  border-radius: 10px;
  box-shadow: 0 3px 8px rgba(240, 224, 180, 0.12);
  overflow: hidden;
  transition: all 0.2s ease;
  margin-bottom: 10px;
  cursor: pointer;
}
:deep(.save-list .history-item:hover) {
  transform: translateY(-2px);
  box-shadow: 0 5px 12px rgba(240, 224, 180, 0.18);
  border-color: #f0c880;
}

/* 滚动条全局统一 - 左右侧保持一致 */
::-webkit-scrollbar {
  width: 8px;
  height: 8px;
}
::-webkit-scrollbar-track {
  background-color: #fff8e8;
  border-radius: 4px;
}
::-webkit-scrollbar-thumb {
  background-color: #f5dca8;
  border-radius: 4px;
  border: 1px solid #fff8e8;
}
</style>