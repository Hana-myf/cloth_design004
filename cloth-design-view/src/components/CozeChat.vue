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
        class="image-viewer"
      />
    </div>
  </div>
</template>

<script>
import ImageViewer from './ImageViewer.vue';
export default {
  name: 'CozeChat',
  components: {
    ImageViewer
  },
  data() {
    return {
      messages: [],
      userInput: '',
      isLoading: false,
      isMinimized: false,
      apiKey: 'pat_sk8iOnz5qzT9cU18pxYZSWboH3f6oynlyQctR1ObYP9vV9lDTUYsw4lPtQjxjzfe',
      robotId: '7475343940674371584',
      userId: 'user_' + Math.random().toString(36).substr(2, 9),
      apiUrl: 'https://api.coze.cn/v3/chat',
      lastAiReply: ''
    };
  },
  methods: {
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
        avatar: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/chatsdk-logo.png'
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
        avatar: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/chatsdk-logo.png'
      });
      this.scrollToBottom(); // 滚动到底部
    }
  },
  mounted() {
    this.messages.push({
      content: '你好！我是AI助手，有什么可以帮你的吗？',
      isUser: false,
      avatar: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/chatsdk-logo.png'
    });
  }
};
</script>

<style scoped>
/* 横向布局容器 */
.chat-layout {
  display: flex;
  gap: 20px; /* 两框之间的间距 */
  padding: 20px;
  max-width: 1400px; /* 限制最大宽度，避免过宽 */
  margin: 0 auto; /* 居中显示 */
}

/* 左侧聊天框样式 */
.chat-container {
  width: 600px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.1);
  font-family: Arial, sans-serif;
  display: flex;
  flex-direction: column; /* 纵向布局，内容区自适应高度 */
}

/* 右侧图片预览框样式 */
.image-container {
  width: 600px; /* 与聊天框等宽 */
  border: 1px solid #e0e0e0; /* 相同边框 */
  border-radius: 8px; /* 相同圆角 */
  box-shadow: 0 2px 10px rgba(0,0,0,0.1); /* 相同阴影 */
  font-family: Arial, sans-serif;
  display: flex;
  flex-direction: column;
}

/* 图片框头部（与聊天框头部风格一致） */
.image-header {
  background-color: #165DFF;
  color: white;
  padding: 12px 15px;
  border-radius: 8px 8px 0 0;
}

.image-header h3 {
  margin: 0;
  font-size: 16px;
}

/* 图片预览工具的容器 */
.image-viewer {
  flex: 1; /* 自适应填充剩余高度 */
  padding: 15px;
}

/* 聊天框头部 */
.chat-header {
  background-color: #165DFF;
  color: white;
  padding: 12px 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-radius: 8px 8px 0 0;
}
/* 标题栏按钮容器 */
.header-buttons {
  display: flex;
  gap: 8px; /* 按钮间距 */
}

/* 清空按钮样式 */
.clear-btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.clear-btn:hover {
  background: rgba(255,255,255,0.3); /*  hover效果 */
}

.minimize-btn {
  background: rgba(255,255,255,0.2);
  border: none;
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

/* 修改.chat-messages样式，确保固定高度和滚动生效 */
.chat-messages {
  height: 500px; /* 固定高度（可按需调整） */
  overflow-y: auto; /* 内容超出时显示垂直滚动条 */
  padding: 15px;
  background-color: #f9f9f9;
  /* 新增：隐藏横向滚动条（避免内容过宽） */
  overflow-x: hidden;
}

.message-item {
  margin-bottom: 15px;
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
}

.message-bubble.user.avatar {
  margin-right: 0;
  margin-left: 10px;
}

.content {
  padding: 10px 15px;
  border-radius: 18px;
  margin-top: 5px;
  word-break: break-word;
}

.message-bubble.user.content {
  background-color: #165DFF;
  color: white;
}

.message-bubble.ai.content {
  background-color: white;
  border: 1px solid #e0e0e0;
}

.sender {
  font-size: 12px;
  margin: 0 0 5px 0;
  color: #666;
}

.message-bubble.user.sender {
  color: #d0d0d0;
  text-align: right;
}

.text {
  margin: 0;
  line-height: 1.5;
}

.loading {
  text-align: center;
  padding: 10px;
  color: #666;
  font-size: 14px;
}

/* 聊天输入区域 */
.chat-input {
  display: flex;
  padding: 10px;
  border-top: 1px solid #e0e0e0;
}

.chat-input input {
  flex: 1;
  padding: 10px 15px;
  border: 1px solid #e0e0e0;
  border-radius: 20px;
  outline: none;
  font-size: 14px;
}

.chat-input button {
  margin-left: 10px;
  padding: 0 18px;
  background-color: #165DFF;
  color: white;
  border: none;
  border-radius: 20px;
  cursor: pointer;
}

.chat-input button:hover {
  background-color: #0e47d7;
}

/* 响应式调整：屏幕宽度小于1240px时自动纵向排列 */
@media (max-width: 1240px) {
  .chat-layout {
    flex-direction: column;
  }
}
</style>