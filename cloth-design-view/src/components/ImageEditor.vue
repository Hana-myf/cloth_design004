<template>
  <div class="image-editor">
    <!-- 组件头部：展开/收起+标题 -->
    <div class="editor-header">
      <h3>图像编辑助手</h3>
      <div class="header-buttons">
        <button @click="clearEditor" class="clear-btn">清空编辑</button>
        <button @click="isMinimized = !isMinimized" class="minimize-btn">
          {{ isMinimized ? '展开' : '收起' }}
        </button>
      </div>
    </div>

    <!-- 编辑内容区（收起时隐藏） -->
    <div v-if="!isMinimized" class="editor-content">
      <!-- 1. 图片上传区 -->
      <div class="upload-area">
        <label class="upload-btn" :disabled="isEditing">
          <input type="file" accept="image/*" @change="handleImageUpload" hidden>
          上传图片（支持JPG/PNG）
        </label>
        <p class="tip" v-if="currentImageUrl">已上传图片，可输入编辑需求</p>
      </div>

      <!-- 2. 图片预览区 -->
      <div class="preview-area" v-if="currentImageUrl">
        <img :src="currentImageUrl" alt="预览图" class="preview-img">
      </div>

      <!-- 3. 编辑需求输入区 -->
      <div class="input-area">
        <input 
          type="text" 
          v-model="editInput" 
          @keyup.enter="startEdit" 
          placeholder="输入编辑需求（如：改成浅粉色、换成牛仔面料）"
          :disabled="isEditing || !currentImageUrl"
        >
        <button @click="startEdit" :disabled="isEditing || !currentImageUrl || !editInput.trim()">
          {{ isEditing ? '编辑中...' : '开始编辑' }}
        </button>
      </div>

      <!-- 4. 编辑结果区 -->
      <div class="result-area" v-if="editedImageUrl">
        <h4>编辑结果：</h4>
        <img :src="editedImageUrl" alt="编辑后图片" class="result-img">
        <button @click="saveEditedImage" class="save-btn">保存到成果库</button>
      </div>

      <!-- 加载状态 -->
      <div v-if="isEditing" class="loading">正在处理图片...</div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ImageEditor',
  props: {
    // 接收主页面的"保存成果库"回调，实现数据同步
    onSaveImage: {
      type: Function,
      required: true
    }
  },
  data() {
    return {
      isMinimized: false,       // 是否收起组件
      currentImageUrl: null,    // 上传的原始图片URL
      editInput: '',            // 编辑需求输入
      editedImageUrl: null,     // 编辑后的图片URL
      isEditing: false,         // 编辑中状态
      // 图像编辑智能体配置（使用你的API Key）
      editorConfig: {
        apiUrl: 'https://api.coze.cn/v3/chat',  // Coze API地址（支持图像编辑交互）
        apiKey: 'pat_mY0OSWqTlyfxYW4cV8fu7vGY6jXupqFhQJ2FvyZfasve8SgKMGmFoFoS5Rshonq5',
        botId: '7533205966545272882',  // 图像编辑智能体ID（需替换为你的实际botId）
        userId: 'user_' + Math.random().toString(36).substr(2, 9),
        defaultImageUrl: 'https://ark-project.tos-cn-beijing.volces.com/doc_image/seededit_i2i.jpeg'
      }
    };
  },
  methods: {
    // 1. 处理图片上传（适配智能体可访问格式）
    async handleImageUpload(e) {
      const file = e.target.files[0];
      if (!file) return;

      // 开发调试：直接使用默认图（生产环境可替换为OSS上传）
      this.currentImageUrl = this.editorConfig.defaultImageUrl;
      this.editedImageUrl = null; // 重置之前的编辑结果
      this.$message.success('图片已上传，可输入编辑需求');
    },

    // 2. 调用智能体开始图像编辑
    async startEdit() {
      if (!this.currentImageUrl || !this.editInput.trim() || this.isEditing) return;

      this.isEditing = true;
      this.editedImageUrl = null; // 清空之前的结果

      try {
        // 构建Coze API请求参数（支持图像+文本交互）
        const response = await axios.post(
          this.editorConfig.apiUrl,
          {
            bot_id: this.editorConfig.botId,
            user_id: this.editorConfig.userId,
            stream: false, // 图像编辑无需流式响应，一次性返回结果
            auto_save_history: true,
            additional_messages: [
              {
                role: "user",
                content: `编辑需求：${this.editInput}，参考图片URL：${this.currentImageUrl}`,
                content_type: "text" // 若Coze支持image类型，可改为"image"并传URL
              }
            ]
          },
          {
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${this.editorConfig.apiKey}`
            }
          }
        );

        const resData = response.data;
        // 解析智能体返回的编辑后图片URL（需根据Coze实际返回格式调整）
        // 假设智能体返回的content中包含图片URL，如："编辑完成：<img src='xxx'>"
        const imageUrlMatch = resData.data.content.match(/https?:\/\/\S+\.(jpg|png|jpeg)/);
        if (!imageUrlMatch) throw new Error('智能体未返回有效图片URL');

        this.editedImageUrl = imageUrlMatch[0];
        this.$message.success('图片编辑完成！');

      } catch (error) {
        console.error('图像编辑失败：', error);
        this.$message.error(`编辑失败：${error.message || '未知错误'}`);
      } finally {
        this.isEditing = false;
      }
    },

    // 3. 保存编辑结果到主页面的成果库
    saveEditedImage() {
      if (!this.editedImageUrl) return;
      // 通过props回调将结果传递给主页面
      this.onSaveImage(this.editedImageUrl, `编辑结果_${new Date().getTime()}`);
      this.$message.success('已保存到成果库');
    },

    // 4. 清空编辑状态
    clearEditor() {
      this.currentImageUrl = null;
      this.editInput = '';
      this.editedImageUrl = null;
      this.$message.info('已清空编辑内容');
    }
  }
};
</script>

<style scoped>
.image-editor {
  border-radius: 4px;
  border: 1px solid #eee;
  overflow: hidden;
  margin-bottom: 16px;
}

.editor-header {
  padding: 10px 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #f5f5f5;
  border-bottom: 1px solid #eee;
}

.editor-header h3 {
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

.editor-content {
  padding: 16px;
  background: #fff;
}

.upload-area {
  margin-bottom: 16px;
}

.upload-btn {
  display: inline-block;
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border-radius: 4px;
  cursor: pointer;
}

.upload-btn:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.tip {
  margin: 8px 0 0;
  font-size: 12px;
  color: #666;
}

.preview-area {
  margin-bottom: 16px;
  text-align: center;
}

.preview-img {
  max-width: 100%;
  max-height: 300px;
  border-radius: 4px;
  border: 1px solid #eee;
}

.input-area {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.input-area input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  outline: none;
}

.input-area button {
  padding: 8px 16px;
  background: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.input-area button:disabled {
  background: #ccc;
  cursor: not-allowed;
}

.result-area {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px dashed #eee;
  text-align: center;
}

.result-img {
  max-width: 100%;
  max-height: 400px;
  border-radius: 4px;
  border: 1px solid #eee;
  margin: 8px 0;
}

.save-btn {
  margin-top: 8px;
  padding: 6px 12px;
  background: #52c41a;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.loading {
  text-align: center;
  padding: 10px;
  color: #666;
  font-size: 12px;
}
</style>