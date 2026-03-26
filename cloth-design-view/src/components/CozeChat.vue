<template>
  <div class="chat-layout">
    <!-- 左侧：智能体对话框 -->
    <div class="chat-container">
    <div class="chat-header">
     <h3>AI 助手</h3>
     <div class="header-buttons">
      <!-- 清空按钮 -->
      <!-- <button @click="clearChatHistory" class="clear-btn">
        清空记录
      </button> -->
      <!-- 最小化按钮 -->
      <button @click="isMinimized =!isMinimized" class="minimize-btn">
        {{ isMinimized? '展开' : '收起' }}
      </button>
     </div>
    </div>

      <div v-if="!isMinimized" class="chat-messages">
        <div v-for="(msg, index) in messages" :key="index" class="message-item">
          <div :class="['message-bubble', msg.isUser? 'user' : 'ai']">
            <img :src="msg.avatar" class="avatar" alt="头像">
            <div class="content">
              <p class="sender">{{ msg.isUser? '我' : 'AI 助手' }}</p>
              <div class="text" v-html="parseContent(msg.content)"></div>
            </div>
          </div>
        </div>
        <div v-if="isLoading" class="loading">AI 正在思考...</div>
      </div>

      <div v-if="!isMinimized" class="chat-input">
        <label class="file-upload-btn" for="file-upload">
          <i class="el-icon-picture"></i> <!-- 假设使用Element UI图标，也可替换为文字 -->
        </label>
        <input 
          type="file" 
          id="file-upload" 
          class="file-upload-input"
          accept="image/*" 
          @change="handleFileUpload"
          :disabled="isLoading"
        >
        <input 
          type="text" 
          v-model="userInput" 
          @keyup.enter="sendMessage" 
          placeholder="输入消息...按回车发送"
          :disabled="isLoading"
        >
        <button @click="sendMessage" :disabled="isLoading">发送</button>
      </div>
    </div>

    <!-- 右侧：图片预览框（与左侧等高并排） -->
    <div class="image-container">
      <div class="image-header">
        <h3>图片预览</h3>
      </div>
      <ImageViewer 
       :aiReply="lastAiReply" 
       :currentUserId="realCurrentUserId" 
       class="image-viewer"
       @save-success="loadUserImages" 
      />
      <ImageSaveList 
        :savedImages="savedImages" 
        class="save-list"
      />
    </div>
  </div>
</template>

<script>
import ImageViewer from './ImageViewer.vue';
import ImageSaveList from './ImageSaveList.vue';
import { mapGetters } from 'vuex';
import request from '@/utils/request';
export default {
  name: 'CozeChat',
  components: {
    ImageViewer,
    ImageSaveList
  },
  // 新增：计算属性获取正确的用户ID（解决初始化拿不到store的问题）
  computed: {
    realCurrentUserId() {
      return this.$store.state.user.userInfo?.id || 23; // 兜底默认23，避免空值
    }
  },
  data() {
    return {
      messages: [],
      userInput: '',
      isLoading: false,
      isMinimized: false,
      apiKey: 'pat_l71GjeLrzmzEzh0d8R5saRWgJFrlhAL79YJbt1KJPLDSmHt2OuXaxom7A92dKjqG',
      robotId: '7475343940674371584',
      userId: 'user_' + Math.random().toString(36).substr(2, 9),
      apiUrl: 'https://api.coze.cn/v3/chat',
      lastAiReply: '',
      savedImages: [], // 存储后端返回的用户图片列表
      // 注释掉原错误赋值：避免初始化时拿不到store
      // currentUserId: this.$store.state.user.userInfo?.id || '', 
      uploadUrl: '/file/upload',
    };
  },
  mounted() {
    // 初始化欢迎消息
    this.messages.push({
      content: '你好！我是AI助手，有什么可以帮你的吗？',
      isUser: false,
      avatar: 'https://pic1.imgdb.cn/item/69c5501053701b6d63d9b415.png'
    });
    // 登录状态判断（只执行一次）
    if (!this.realCurrentUserId) { // 修复：使用计算属性的用户ID
      this.$router.push('/login'); // 未登录跳转
    } else {
      this.loadUserImages(); // 已登录才加载图片
    }
    console.log('Vuex用户信息:', this.$store.state.user.userInfo);
    console.log('当前用户ID:', this.realCurrentUserId); // 修复：打印正确的用户ID
  },
  methods: {
    // 调用后端接口，查询用户的图片列表
    async loadUserImages() {
      try {
        const queryDto = {
          userId: this.realCurrentUserId, // 修复：使用正确的用户ID
          pageNum: 1,
          pageSize: 4
        };
        console.log('前端查询参数：', queryDto);

        const response = await request.post('/generatedImage/query', queryDto);
        // 关键：打印完整响应，确认后端返回格式
        console.log('后端响应完整数据：', response); 
        console.log('后端响应data字段：', response.data);

        // 修正判断条件：根据后端实际返回的code判断（通常200代表成功）
        if (response.data && response.data.code === 200) { 
          // 确认后端返回的图片数组在response.data.data中
          console.log('后端返回的图片列表：', response.data.data);

          // 按时间倒序（后端已按generate_time DESC排序，可省略）
          const sortedImages = response.data.data.sort(
            (a, b) => new Date(b.generateTime) - new Date(a.generateTime)
          );
          this.savedImages = sortedImages.slice(0, 4);
          // 打印赋值后的savedImages，确认数据已接收
          console.log('前端savedImages赋值后：', this.savedImages); 
        } else {
          console.warn('后端返回非成功状态：', response.data);
        }
      } catch (error) {
        console.error('加载失败：', error);
      }
    },
    async handleFileUpload(e) {
      const file = e.target.files[0];
      if (!file) return;

      // 清空文件选择框（允许重复选择同一文件）
      e.target.value = '';

      // 验证文件类型和大小
      if (!file.type.startsWith('image/')) {
        this.$message.error('请上传图片文件（jpg、png等）');
        return;
      }
      if (file.size > 10 * 1024 * 1024) { // 10MB限制
        this.$message.error('文件大小不能超过10MB');
        return;
      }

      this.isLoading = true;
      try {
        // 构建FormData
        const formData = new FormData();
        formData.append('file', file);

        // 调用后端上传接口
        const response = await this.$axios.post(this.uploadUrl, formData, {
          headers: { 'Content-Type': 'multipart/form-data' }
        });

        if (response.data.code === 200) {
          const imageUrl = response.data.data; // 后端返回的图片URL
          // 将图片添加到聊天记录
          this.messages.push({
            content: `<img src="${imageUrl}" class="chat-image" alt="上传的图片">`,
            isUser: true,
            avatar: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/coze-logo.png'
          });
          this.scrollToBottom();

          // 自动发送图片给AI处理（如果需要）
          this.sendMessageWithImage(imageUrl);
        } else {
          this.$message.error(response.data.msg || '上传失败');
        }
      } catch (error) {
        console.error('文件上传失败:', error);
        this.$message.error('上传失败，请重试');
      } finally {
        this.isLoading = false;
      }
    },
    sendMessageWithImage(imageUrl) {
      // 可以添加提示文本，例如"请分析这张图片"
      const userMessage = `请分析这张图片: ${imageUrl}`;
      
      // 调用原有发送逻辑
      this.userInput = userMessage;
      this.sendMessage();
    },
    // 保存图片到后端（调用已有的save接口）
    async saveImageToBackend(imageInfo) {
      try {
        const imageData = {
          userId: this.realCurrentUserId, // 修复：使用正确的用户ID
          imageName: imageInfo.name, // 图片名称
          imageUrl: imageInfo.url,   // 图片URL
          generateTime: new Date()   // 生成时间
        };
        await this.$http.post('/generatedImage/save', imageData);
        // 保存成功后，重新加载列表（此时后端会返回最新的4张）
        this.loadUserImages();
      } catch (error) {
        console.error('保存图片失败:', error);
      }
    },
    parseContent(content) {
      return content;
    },
    async sendMessage() {
      // 原有发送消息逻辑不变...
      if (!this.userInput.trim() || this.isLoading) return;

      this.messages.push({
        content: this.userInput,
        isUser: true,
        avatar: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/coze-logo.png'
      });

      const userMessage = this.userInput;
      this.userInput = '';
      this.isLoading = true;

      const aiMsgIndex = this.messages.push({
        content: '',
        isUser: false,
        avatar: 'https://pic1.imgdb.cn/item/69c5501053701b6d63d9b415.png'
      }) - 1;

      try {
        const response = await fetch(this.apiUrl, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.apiKey}`
          },
          body: JSON.stringify({
            bot_id: this.robotId,
            user_id: this.userId,
            stream: true,
            auto_save_history: true,
            additional_messages: [
              {
                role: "user",
                content: userMessage,
                content_type: "text"
              }
            ]
          })
        });

        if (!response.ok) {
          throw new Error(`请求失败: ${response.status}`);
        }

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let fullReply = '';
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

            if (line.startsWith('data:') && currentEvent === 'conversation.message.delta') {
              const dataStr = line.replace('data:', '').trim();
              if (dataStr) {
                try {
                  const data = JSON.parse(dataStr);
                  if (data.content) {
                    fullReply += data.content;
                    this.messages[aiMsgIndex].content = fullReply;
                    this.scrollToBottom();
                  }
                } catch (e) {
                  console.warn('解析片段失败:', e);
                }
              }
            }
          }
        }

        this.lastAiReply = fullReply;
        console.log('AI回复已传递给ImageViewer:', this.lastAiReply); // 新增：验证回复传递
        this.isLoading = false;

      } catch (error) {
        console.error('API调用错误:', error);
        this.messages[aiMsgIndex].content = `出错了: ${error.message}`;
        this.isLoading = false;
      }
    },
    scrollToBottom() {
      const container = this.$el.querySelector('.chat-messages');
      if (container) {
        container.scrollTop = container.scrollHeight;
      }
    }
  },
   clearChatHistory() {
    // 确认弹窗，防止误操作
    if (confirm('确定要清空所有聊天记录吗？')) {
      this.messages = []; // 清空消息列表
      // 可选项：重新添加初始欢迎消息
      this.messages.push({
        content: '你好！我是AI助手，有什么可以帮你的吗？',
        isUser: false,
        avatar: 'https://pic1.imgdb.cn/item/69c5501053701b6d63d9b415.png'
      });
      this.scrollToBottom(); // 滚动到底部
    }
  }
};
</script>

<style scoped>
/* 横向布局容器 - 加背景图+暖色调基底 */
.chat-layout {
  display: flex;
  gap: 20px; /* 两框之间的间距 */
  padding: 20px;
  max-width: 1400px; /* 限制最大宽度，避免过宽 */
  margin: 0 auto; /* 居中显示 */
  align-items: flex-start; /* 顶部对齐，而不是拉伸对齐 */
  /* 背景图（替换成你的图片URL）+ 暖色调叠加 */
  background: url("https://pic1.imgdb.cn/item/6958e1eeda3df73ea1bc5a2c.png") center/cover no-repeat;
  background-color: rgba(255, 255, 250, 0.95);
  background-blend-mode: overlay;
  min-height: calc(100vh - 100px);
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(240, 224, 180, 0.12);
}

/* 左侧聊天框样式 - 暖色调边框/背景 */
.chat-container {
  width: 600px;
  border: 1px solid #f0e8d0; /* 暖色调边框 */
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(240, 224, 180, 0.15); /* 暖色调阴影 */
  font-family: Arial, sans-serif;
  display: flex;
  flex-direction: column; /* 纵向布局，内容区自适应高度 */
  max-height: 700px;
  background-color: rgba(255, 255, 250, 0.98); /* 暖色调背景 */
}

/* 右侧图片预览框样式 - 同步暖色调 */
.image-container {
  width: 400px; /* 与聊天框等宽 */
  border: 1px solid #f0e8d0; /* 暖色调边框 */
  border-radius: 8px; /* 相同圆角 */
  box-shadow: 0 2px 10px rgba(240, 224, 180, 0.15); /* 暖色调阴影 */
  font-family: Arial, sans-serif;
  display: flex;
  flex-direction: column;
  /* 新增：最大高度限制，超出则滚动 */
  max-height: 650px;
  background-color: rgba(255, 255, 250, 0.98); /* 暖色调背景 */
  /* 移除了flex:1，不再强制拉伸高度 */
}

/* 图片框头部（暖色调替换原蓝色） */
.image-header {
  background-color: #fff8e8; /* 暖色调头部背景 */
  color: #8a7d5e; /* 暖色调文字 */
  padding: 12px 15px;
  border-radius: 8px 8px 0 0;
  border-bottom: 1px solid #f0e8d0;
}

.image-header h3 {
  margin: 0;
  font-size: 16px;
}

/* 图片预览工具的容器 */
.image-viewer {
  padding: 15px;
  /* 让内容决定高度 */
  min-height: 100px; /* 最小高度，避免内容过少时框体过小 */
  max-height: calc(100% - 45px); /* 减去头部高度 */
  overflow-y: auto; /* 内容超出时显示滚动条 */
}
.save-list {
  flex: 1; /* 允许列表占据剩余空间 */
  overflow-y: auto; /* 列表内容超出时滚动 */
}
/* 聊天框头部（暖色调替换原蓝色） */
.chat-header {
  background-color: #fff8e8; /* 暖色调头部背景 */
  color: #8a7d5e; /* 暖色调文字 */
  /* 减小上下内边距（原5px调整为3px），左右内边距保留 */
  padding: 3px 15px; 
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 8px 8px 0 0;
  /* 可选：适当减小标题字体大小，进一步压缩高度 */
  font-size: 14px; 
  border-bottom: 1px solid #f0e8d0;
}

/* 同步调整头部按钮大小，保持比例协调 */
.header-buttons button {
  padding: 2px 8px; /* 减小按钮内边距 */
  font-size: 12px; /* 减小按钮文字大小 */
}

/* 最小化按钮（暖色调替换原蓝色） */
.minimize-btn {
  background: #fff0d0; /* 暖色调按钮背景 */
  border: 1px solid #f0e0b0; /* 暖色调按钮边框 */
  color: #8a7d5e; /* 暖色调文字 */
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}
.minimize-btn:hover {
  background: #f5dca8;
  border-color: #f0c880;
}

/* 修改.chat-messages样式，确保固定高度和滚动生效 */
.chat-messages {
  height: 500px; /* 固定高度（可按需调整） */
  overflow-y: auto; /* 内容超出时显示垂直滚动条 */
  padding: 15px;
  background-color: #fffdf8; /* 暖色调背景 */
  overflow-x: hidden; /* 隐藏横向滚动条 */
}
/* 确保消息容器不会被图片撑得过大 */
.message-item {
  overflow: hidden;
}

.message-bubble {
  display: flex;
  max-width: 90%;
}

.message-bubble.user {
  margin-left: auto;
  flex-direction: row-reverse;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  margin-right: 10px;
  border: 2px solid #f5dca8; /* 暖色调头像边框 */
}

.message-bubble.user .avatar {
  margin-right: 0;
  margin-left: 10px;
  border-color: #e0f0d8;
}

/* 消息气泡（暖色调区分用户/AI） */
.content {
  padding: 10px 15px;
  border-radius: 18px;
  margin-top: 5px;
  word-break: break-word;
}
/* AI消息气泡 */
.message-bubble.ai .content {
  background-color: #fff8e8; /* 暖色调AI气泡 */
  border: 1px solid #f0e8d0;
  padding: 12px 15px; /* 稍微增大内边距 */
  overflow: hidden; /* 防止图片超出气泡范围 */
}
/* 用户消息气泡 */
.message-bubble.user .content {
  background-color: #f8fff0; /* 清新暖色调用户气泡 */
  border: 1px solid #e0f0d8;
  padding: 12px 15px; /* 稍微增大内边距 */
  overflow: hidden; /* 防止图片超出气泡范围 */
}

/* 聊天图片样式优化 */
.chat-image {
  /* 限制最大宽度为对话框的80%，避免超出容器 */
  max-width: 80%;
  /* 限制最大高度，避免图片过长 */
  max-height: 200px;
  /* 保持图片比例，避免变形 */
  object-fit: contain;
  /* 增加边框圆角和间距，提升美观度 */
  border-radius: 8px;
  margin: 5px 0;
  /* 避免图片被挤压时出现滚动条 */
  display: block;
  border: 1px solid #f0e8d0; /* 暖色调图片边框 */
}
.sender {
  font-size: 12px;
  margin: 0 0 5px 0;
  color: #8a7d5e; /* 暖色调文字 */
}

.message-bubble.user .sender {
  color: #7d8a5e; /* 用户消息暖色调文字 */
  text-align: right;
}

.text {
  margin: 0;
  line-height: 1.5;
  color: #5a523e; /* 暖色调正文 */
}

.loading {
  text-align: center;
  padding: 10px;
  color: #8a7d5e; /* 暖色调加载文字 */
  font-size: 14px;
  background-color: #fff8e8;
  border-radius: 8px;
  border: 1px dashed #f0e8d0;
}

/* 聊天输入区域（暖色调替换原蓝色） */
.chat-input {
  display: flex;
  padding: 10px;
  border-top: 1px solid #f0e8d0; /* 暖色调分割线 */
  background-color: #fffdf8; /* 暖色调背景 */
}

.chat-input input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #f0e8d0; /* 暖色调输入框边框 */
  border-radius: 20px;
  outline: none;
  font-size: 14px;
  background-color: #fff;
}
.chat-input input:focus {
  border-color: #f0c880; /* 聚焦暖色调边框 */
}

/* 发送按钮（暖色调替换原蓝色） */
.chat-input button {
  margin-left: 10px;
  padding: 0 18px;
  background-color: #f5dca8; /* 暖色调按钮 */
  color: #8a7d5e; /* 暖色调文字 */
  border: 1px solid #f0c880; /* 暖色调边框 */
  border-radius: 20px;
  cursor: pointer;
}
.chat-input button:hover {
  background-color: #f0c880; /* hover暖色调 */
}
.chat-input button:disabled {
  background-color: #e8e4d8;
  border-color: #e0d8c0;
  color: #a09880;
  cursor: not-allowed;
}

/* 文件上传按钮样式（暖色调） */
.file-upload-input {
  display: none; /* 隐藏原生文件选择框 */
}

.file-upload-btn {
  margin-right: 10px;
  padding: 8px;
  color: #8a7d5e; /* 暖色调图标 */
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #fff8e8;
  border: 1px solid #f0e8d0;
  border-radius: 50%;
}

.file-upload-btn:hover {
  color: #7a6d4e; /* hover暖色调 */
  background-color: #f5dca8;
  border-color: #f0c880;
}

::v-deep .chat-image {
  max-width: 80% !important; /* 限制最大宽度为对话框80% */
  max-height: 200px !important; /* 限制最大高度200px */
  object-fit: contain !important; /* 保持比例，不拉伸 */
  border-radius: 8px;
  margin: 5px auto !important; /* 居中显示 */
  display: block !important;
  border: 1px solid #f0e8d0 !important;
}

/* 确保消息气泡不会被图片撑大 */
::v-deep .message-bubble .content {
  max-width: 100%;
  overflow: hidden;
}

/* 限制消息容器宽度，避免图片溢出 */
::v-deep .message-item {
  max-width: 100%;
  overflow: hidden;
}

/* 响应式调整：屏幕宽度小于1240px时自动纵向排列 */
@media (max-width: 1240px) {
  .chat-layout {
    flex-direction: column;
  }
}
</style>