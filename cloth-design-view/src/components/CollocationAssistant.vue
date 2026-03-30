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
      <div v-if="isLoading" class="loading">
        <div class="custom-spinner"></div>
        <span>正在思考搭配方案...</span>
      </div>
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
      apiKey: 'pat_oNhoddgk898CKMqK8avnbPzZt7UlcXoiWxHW3QXanyIdTX3YExf0JHVKfHdXDgYH',
      robotId: '7533187838620631066',
      userId: 'user_' + Math.random().toString(36).substr(2, 9),
      apiUrl: 'https://api.coze.cn/v3/chat',
      weatherData: null,
      weatherApiKey: 'YOUR_WEATHER_API_KEY', // 需要替换为真实的天气API密钥
      recommendList: [
        { style: '通勤风', desc: '羊毛面料 + 深灰/藏青 + 简约剪裁 + 单排扣设计' },
        { style: '休闲风', desc: '牛仔面料 + 水洗蓝 + 破洞元素 + 宽松版型' },
        { style: '轻奢风', desc: '真丝面料 + 香槟金/米白 + 蕾丝拼接 + 收腰设计' },
        { style: '运动风', desc: '速干面料 + 荧光色撞色 + 反光条 + 连帽设计' }
      ]
    };
  },
  mounted() {
    this.messages.push({
      content: '你好！我可以帮你搭配服装配饰，有什么需求吗？',
      isUser: false,
      avatar: 'https://pic1.imgdb.cn/item/69c54f5f53701b6d63d9b12c.png'
    });
    // 获取天气数据
    this.fetchWeather();
  },
  methods: {
    async sendMessage() {
      if (!this.userInput.trim() || this.isLoading) return;

      this.messages.push({
        content: this.userInput,
        isUser: true,
        avatar: 'https://pic1.imgdb.cn/item/69c347ce2cb231a0e879475c.png'
      });

      const userMessage = this.userInput;
      this.userInput = '';
      this.isLoading = true;

      const aiMsgIndex = this.messages.push({
        content: '',
        isUser: false,
        avatar: 'https://pic1.imgdb.cn/item/69c54f5f53701b6d63d9b12c.png'
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
              { role: "system", content: `当前天气信息：${this.getWeatherInfo()}\n请根据天气情况给出合适的服装搭配建议。`, content_type: "text" },
              { role: "user", content: userMessage, content_type: "text" }
            ]
          })
        });

        if (!response.ok) throw new Error(`请求失败: ${response.status}`);

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
                    // 去除{{output}}前缀
                    let content = data.content;
                    if (content.startsWith('{{output}}')) {
                      content = content.replace('{{output}}', '');
                    }
                    fullReply += content;
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
    handleRecommendSelect(item) {
      this.$emit('select-recommend', item);
    },
    scrollToBottom() {
      const container = this.$el.querySelector('.chat-messages');
      if (container) container.scrollTop = container.scrollHeight;
    },
    clearChatHistory() {
      if (confirm('确定要清空聊天记录吗？')) {
        this.messages = [];
        this.messages.push({
          content: '你好！我可以帮你搭配服装配饰，有什么需求吗？',
          isUser: false,
          avatar: 'https://pic1.imgdb.cn/item/69c54f5f53701b6d63d9b12c.png'
        });
      }
    },

    // 获取当前位置
    getCurrentLocation() {
      return new Promise((resolve, reject) => {
        if (navigator.geolocation) {
          navigator.geolocation.getCurrentPosition(
            (position) => {
              resolve({
                latitude: position.coords.latitude,
                longitude: position.coords.longitude
              });
            },
            (error) => {
              console.error('获取位置失败:', error);
              // 如果获取位置失败，使用默认位置（北京）
              resolve({ latitude: 39.9042, longitude: 116.4074 });
            }
          );
        } else {
          console.error('浏览器不支持地理定位');
          // 如果浏览器不支持地理定位，使用默认位置（北京）
          resolve({ latitude: 39.9042, longitude: 116.4074 });
        }
      });
    },

    // 获取天气数据
    async fetchWeather() {
      try {
        // 获取当前位置
        const location = await this.getCurrentLocation();
        
        // 使用OpenWeatherMap API获取天气数据
        // 注意：这里使用了免费的OpenWeatherMap API，需要替换为真实的API密钥
        const apiKey = this.weatherApiKey || 'YOUR_OPENWEATHERMAP_API_KEY';
        const response = await fetch(
          `https://api.openweathermap.org/data/2.5/weather?lat=${location.latitude}&lon=${location.longitude}&units=metric&lang=zh_cn&appid=${apiKey}`
        );
        
        if (!response.ok) {
          throw new Error('获取天气数据失败');
        }
        
        const data = await response.json();
        this.weatherData = {
          city: data.name,
          temperature: Math.round(data.main.temp),
          description: data.weather[0].description,
          icon: data.weather[0].icon
        };
        
        console.log('天气数据获取成功:', this.weatherData);
      } catch (error) {
        console.error('获取天气数据失败:', error);
        // 如果API调用失败，使用模拟数据
        this.weatherData = {
          city: '北京',
          temperature: 22,
          description: '晴',
          icon: '01d'
        };
      }
    },

    // 获取天气信息文本
    getWeatherInfo() {
      if (!this.weatherData) {
        return '当前天气信息获取中...';
      }
      return `${this.weatherData.city}，${this.weatherData.temperature}°C，${this.weatherData.description}`;
    }
  }
};
</script>

<style scoped>
.collocation-assistant {
  height: 100%;
  display: flex;
  flex-direction: column;
  border-radius: 8px;
  overflow: hidden;
  background-color: rgba(255, 255, 250, 0.98);
  border: 1px solid #f0e8d0;
}

/* 聊天头部 */
.chat-header {
  padding: 10px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #fff8e8;
  border-bottom: 1px solid #f0e8d0;
}
.chat-header h3 {
  margin: 0;
  font-size: 14px;
  color: #8a7d5e;
  display: flex;
  align-items: center;
  gap: 8px;
}
.chat-header h3::before {
  content: "";
  width: 6px;
  height: 14px;
  background-color: #f0c880;
  border-radius: 3px;
}
.header-buttons {
  display: flex;
  gap: 8px;
}
.clear-btn, .minimize-btn {
  padding: 4px 8px;
  font-size: 12px;
  border: 1px solid #f0e8d0;
  border-radius: 20px;
  background: #fffdf8;
  color: #8a7d5e;
  cursor: pointer;
  transition: all 0.2s ease;
}
.clear-btn:hover, .minimize-btn:hover {
  background: #f5dca8;
  color: #7a6d4e;
  border-color: #f0c880;
}

/* 聊天消息区 */
.chat-messages {
  flex: 1;
  padding: 12px;
  overflow-y: auto;
  background: #fffdf8;
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
  flex-direction: row-reverse;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  margin-right: 8px;
  border: 1px solid #f0e8d0;
}
.message-bubble.user .avatar {
  margin-right: 0;
  margin-left: 8px;
}
.content {
  padding: 8px 12px;
  border-radius: 16px;
  background: #fff8e8;
  border: 1px solid #f0e8d0;
  box-shadow: 0 1px 3px rgba(240, 200, 128, 0.1);
}
.message-bubble.user .content {
  background: #f5dca8;
  color: #8a7d5e;
  border-color: #f0c880;
}
.sender {
  margin: 0 0 4px;
  font-size: 12px;
  opacity: 0.8;
  color: #8a7d5e;
}
.message-bubble.user .sender {
  color: #7a6d4e;
}
.text {
  margin: 0;
  font-size: 14px;
  line-height: 1.5;
  color: #8a7d5e;
}

/* 加载状态 */
.loading {
  text-align: center;
  padding: 10px;
  color: #8a7d5e;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}
.custom-spinner {
  width: 16px;
  height: 16px;
  border: 2px solid #f0e8d0;
  border-top-color: #f0c880;
  border-radius: 50%;
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 输入区 */
.chat-input {
  display: flex;
  gap: 8px;
  padding: 12px;
  border-top: 1px solid #f0e8d0;
  background: #fff8e8;
}
.chat-input input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #f0e8d0;
  border-radius: 20px;
  outline: none;
  background: #fffdf8;
  color: #8a7d5e;
}
.chat-input input:focus {
  border-color: #f0c880;
  box-shadow: 0 0 0 2px rgba(240, 200, 128, 0.1);
}
.chat-input input:disabled {
  background: #f5f5f5;
  color: #999;
}
.chat-input button {
  padding: 8px 16px;
  background: #f5dca8;
  color: #8a7d5e;
  border: 1px solid #f0c880;
  border-radius: 20px;
  cursor: pointer;
  transition: all 0.2s ease;
}
.chat-input button:hover:not(:disabled) {
  background: #f0c880;
  color: #7a6d4e;
  box-shadow: 0 2px 6px rgba(240, 200, 128, 0.2);
}
.chat-input button:disabled {
  background: #eee;
  color: #999;
  border-color: #ddd;
  cursor: not-allowed;
}
</style>