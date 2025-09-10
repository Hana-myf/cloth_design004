<template>
  <div class="collocation-assistant">
    <div class="chat-header">
      <h3>搭配助手</h3>
      <div class="header-buttons">
        <button @click="clearChatHistory" class="clear-btn">清空记录</button>
        <button @click="isMinimized = !isMinimized" class="minimize-btn">
          {{ isMinimized ? '展开' : '收起' }}
        </button>
      </div>
    </div>

    <div v-if="!isMinimized" class="chat-messages">
      <div v-for="(msg, index) in messages" :key="index" class="message-item">
        <div :class="['message-bubble', msg.isUser ? 'user' : 'ai']">
          <img :src="msg.avatar" class="avatar" alt="头像">
          <div class="content">
            <p class="sender">{{ msg.isUser ? '我' : '搭配助手' }}</p>
            <div class="text">{{ msg.content }}</div>
          </div>
        </div>
      </div>
      <div v-if="isLoading" class="loading">正在思考搭配方案...</div>
    </div>

    <div v-if="!isMinimized" class="chat-input">
      <input 
        type="text" 
        v-model="userInput" 
        @keyup.enter="sendMessage" 
        placeholder="输入搭配需求...按回车发送"
        :disabled="isLoading"
      >
      <button @click="sendMessage" :disabled="isLoading || !userInput.trim()">发送</button>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CollocationAssistant',
  data() {
    return {
      messages: [],
      userInput: '',
      isLoading: false,
      isMinimized: false,
      // 搭配助手专属配置（使用指定robotid）
      apiKey: 'pat_mY0OSWqTlyfxYW4cV8fu7vGY6jXupqFhQJ2FvyZfasve8SgKMGmFoFoS5Rshonq5',
      robotId: '7533187838620631066', // 搭配助手robotid
      userId: 'user_' + Math.random().toString(36).substr(2, 9),
      apiUrl: 'https://api.coze.cn/v3/chat'
    };
  },
  mounted() {
    // 初始化欢迎消息
    this.messages.push({
      content: '你好！我可以帮你搭配服装配饰，有什么需求吗？',
      isUser: false,
      avatar: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/chatsdk-logo.png'
    });
  },
  methods: {
    async sendMessage() {
      if (!this.userInput.trim() || this.isLoading) return;

      // 添加用户消息
      this.messages.push({
        content: this.userInput,
        isUser: true,
        avatar: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/coze-logo.png'
      });

      const userMessage = this.userInput;
      this.userInput = '';
      this.isLoading = true;

      // 添加AI消息占位
      const aiMsgIndex = this.messages.push({
        content: '',
        isUser: false,
        avatar: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/chatsdk-logo.png'
      }) - 1;

      try {
        // 调用Coze API（与CozeChat逻辑一致）
        const response = await fetch(this.apiUrl, {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.apiKey}`
          },
          body: JSON.stringify({
            bot_id: this.robotId,
            user_id: this.userId,
            stream: true, // 保持流式响应
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

        if (!response.ok) throw new Error(`请求失败: ${response.status}`);

        const reader = response.body.getReader();
        const decoder = new TextDecoder();
        let fullReply = '';
        let currentEvent = null;

        // 处理流式响应（与CozeChat完全一致）
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

        this.isLoading = false;

      } catch (error) {
        console.error('搭配助手API错误:', error);
        this.messages[aiMsgIndex].content = `获取搭配建议失败: ${error.message}`;
        this.isLoading = false;
      }
    },

    scrollToBottom() {
      const container = this.$el.querySelector('.chat-messages');
      if (container) container.scrollTop = container.scrollHeight;
    },

    clearChatHistory() {
      if (confirm('确定要清空聊天记录吗？')) {
        this.messages = [];
        // 重新添加欢迎消息
        this.messages.push({
          content: '你好！我可以帮你搭配服装配饰，有什么需求吗？',
          isUser: false,
          avatar: 'https://lf-coze-web-cdn.coze.cn/obj/eden-cn/lm-lgvj/ljhwZthlaukjlkulzlp/coze/chatsdk-logo.png'
        });
      }
    }
  }
};
</script>

<style scoped>
.collocation-assistant {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 4px;
  overflow: hidden;
}

.chat-header {
  padding: 10px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f5f5f5;
  border-bottom: 1px solid #eee;
}

.chat-header h3 {
  margin: 0;
  font-size: 14px;
  color: #333;
}

.header-buttons {
  display: flex;
  gap: 8px;
}

.clear-btn, .minimize-btn {
  padding: 4px 8px;
  font-size: 12px;
  border: none;
  border-radius: 3px;
  background: #e0e0e0;
  cursor: pointer;
}

.chat-messages {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  background: #fff;
}

.message-item {
  margin-bottom: 12px;
}

.message-bubble {
  display: flex;
  max-width: 80%;
}

.message-bubble.user {
  margin-left: auto;
}

.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  margin-right: 8px;
}

.message-bubble.user .avatar {
  margin-right: 0;
  margin-left: 8px;
}

.content {
  padding: 8px 12px;
  border-radius: 16px;
  background: #f0f0f0;
}

.message-bubble.user .content {
  background: #1890ff;
  color: white;
}

.sender {
  margin: 0 0 4px;
  font-size: 12px;
  opacity: 0.8;
}

.text {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
}

.loading {
  text-align: center;
  padding: 10px;
  color: #666;
  font-size: 12px;
}

.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #eee;
}

.chat-input input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  outline: none;
}

.chat-input button {
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.chat-input button:disabled {
  background: #ccc;
  cursor: not-allowed;
}
</style>
