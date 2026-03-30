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
        <label class="upload-btn" :disabled="isBusy">
          <input type="file" accept="image/jpeg,image/png,image/webp" @change="handleImageUpload" hidden />
          上传图片（支持JPG/PNG/WEBP，最大5MB）
        </label>
        <p class="tip" v-if="currentImageUrl">已上传图片，可输入编辑需求</p>
      </div>

      <!-- 2. 图片预览区 -->
      <div class="preview-area" v-if="currentImageUrl">
        <img :src="currentImageUrl" alt="预览图" class="preview-img" />
      </div>

      <!-- 3. 编辑需求输入区 -->
      <div class="input-area">
        <input
          type="text"
          v-model="editInput"
          @keyup.enter="startEdit"
          placeholder="输入编辑需求（如：改成浅粉色、换成牛仔面料）"
          :disabled="isBusy || !currentImageUrl"
        />
        <button @click="startEdit" :disabled="isBusy || !currentImageUrl || !editInput.trim()">
          {{ isBusy ? '编辑中...' : '开始编辑' }}
        </button>
      </div>

      <!-- 4. 编辑结果区 -->
      <div class="result-area" v-if="editedImageUrl">
        <h4>编辑结果：</h4>
        <img :src="editedImageUrl" alt="编辑后图片" class="result-img" />
        <button @click="saveEditedImage" class="save-btn">保存到成果库</button>
      </div>

      <!-- 加载状态 -->
      <div v-if="isBusy" class="loading">正在处理中...</div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ImageEditor',
  props: {
    onSaveImage: { type: Function, required: true }
  },
  data() {
    return {
      isMinimized: false,
      currentImageUrl: null,   // 上传后的图片 URL
      editInput: '',           // 用户编辑需求
      editedImageUrl: null,    // 最终生成的图片 URL
      isBusy: false,           // 统一的忙碌状态（上传/编辑均使用）
      imgbbApiKey: 'ddfd3ce61ba74d50a1d1167d498e4c0f',

      // 图像编辑智能体配置
      editorConfig: {
        // 走代理，无需直连 apiUrl
        apiKey: 'pat_oNhoddgk898CKMqK8avnbPzZt7UlcXoiWxHW3QXanyIdTX3YExf0JHVKfHdXDgYH',
        botId: '7533205966545272882',
        // 建议固定，便于后台筛选追踪
        userId: 'image_editor_user_001',
        defaultImageUrl: 'https://ark-project.tos-cn-beijing.volces.com/doc_image/seededit_i2i.jpeg'
      },

      // 代理路径（与 vue.config.js 中保持一致）
      submitUrl: '/__coze_proxy_submit__',
      retrieveUrl: '/__coze_proxy_retrieve__'
    };
  },
  methods: {
    /* 图片上传（ImgBB） */
    async handleImageUpload(e) {
      const file = e.target.files?.[0];
      if (!file || this.isBusy) return;

      // 前端校验
      const maxSize = 5 * 1024 * 1024;
      if (file.size > maxSize) {
        this.$message.error('图片过大，最大支持5MB');
        return;
      }
      const validTypes = ['image/jpeg', 'image/png', 'image/webp'];
      if (!validTypes.includes(file.type)) {
        this.$message.error('格式不支持，仅允许：JPG、PNG、WEBP');
        return;
      }

      this.isBusy = true;
      this.editedImageUrl = null;

      try {
        const formData = new FormData();
        formData.append('image', file);

        const resp = await fetch(`https://api.imgbb.com/1/upload?key=${this.imgbbApiKey}`, {
          method: 'POST',
          body: formData
        });
        const json = await resp.json();

        if (json?.success) {
          this.currentImageUrl = json.data.url;
          this.$message.success('图片上传成功，可输入编辑需求');
        } else {
          throw new Error(json?.error?.message || 'ImgBB 上传失败');
        }
      } catch (err) {
        console.error('图片上传失败：', err);
        // 回退到默认图，便于继续调试
        this.currentImageUrl = this.editorConfig.defaultImageUrl;
        this.$message.error(`图片上传失败，已使用默认示例图：${err.message || '未知错误'}`);
      } finally {
        this.isBusy = false;
      }
    },

    /* 发起编辑（非流式 + 轮询） */
    async startEdit() {
      if (!this.currentImageUrl || !this.editInput.trim() || this.isBusy) return;

      this.isBusy = true;
      this.editedImageUrl = null;

      // 组合 prompt
      const prompt = `编辑需求：${this.editInput.trim()}，参考图片URL：${this.currentImageUrl}`;

      try {
        // 1) 提交任务（标准 messages）
        console.log('[ImageEditor][submit] messages:', prompt);
        const submitRes = await axios.post(
          this.submitUrl,
          {
            bot_id: this.editorConfig.botId,
            user_id: this.editorConfig.userId,
            stream: false,
            messages: [
              { role: 'user', content: prompt, content_type: 'text' }
            ]
          },
          { headers: { Authorization: `Bearer ${this.editorConfig.apiKey}` } }
        );

        const conversationId = submitRes?.data?.data?.conversation_id;
        if (!conversationId) {
          console.error('提交响应异常：', submitRes?.data);
          throw new Error('未获取到 conversation_id');
        }

        // 2) 轮询取结果
        const finalData = await this.pollForResult(conversationId);

        // 3) 解析图片 URL
        const content = finalData?.messages?.find(m => m.type === 'answer')?.content || '';
        const m = content.match(/https?:\/\/\S+\.(jpg|png|jpeg|webp)/);
        if (!m) throw new Error('智能体返回内容中未包含有效图片URL');

        this.editedImageUrl = m[0];
        this.$message.success('图片编辑完成！');
      } catch (err) {
        console.error('[ImageEditor] 编辑失败：', err);
        this.$message.error(`编辑失败：${err.response?.data?.msg || err.message || '未知错误'}`);
      } finally {
        this.isBusy = false;
      }
    },

    /* 轮询取结果（等待 120s） */
    async pollForResult(conversationId) {
      const maxAttempts = 40; // 40 * 3s = 120s
      const delay = 3000;

      for (let i = 0; i < maxAttempts; i++) {
        console.log(`[ImageEditor][retrieve] 第 ${i + 1} 次尝试...`);
        try {
          const resp = await axios.get(this.retrieveUrl, {
            params: { conversation_id: conversationId, bot_id: this.editorConfig.botId },
            headers: { Authorization: `Bearer ${this.editorConfig.apiKey}` }
          });

          const data = resp?.data?.data;

          if (data?.status === 'completed') {
            console.log('[ImageEditor] 任务完成:', data);
            return data;
          } else if (data?.status === 'failed') {
            throw new Error(data?.last_error?.msg || '任务失败（未知原因）');
          }
        } catch (e) {
          throw new Error(`取件失败：${e.message}`);
        }

        await new Promise(r => setTimeout(r, delay));
      }
      throw new Error('图片生成超时，AI模型可能正忙，请稍后再试');
    },

    /* 保存结果 */
    saveEditedImage() {
      if (!this.editedImageUrl) return;
      this.onSaveImage(this.editedImageUrl, `编辑结果_${Date.now()}`);
      this.$message.success('已保存到成果库');
    },

    /* 清空 */
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
.image-editor { padding: 12px; border: 1px solid #eee; border-radius: 8px; }
.editor-header { display: flex; align-items: center; justify-content: space-between; }
.header-buttons button { margin-left: 8px; }
.upload-area { margin: 12px 0; }
.preview-img, .result-img { max-width: 100%; border-radius: 6px; }
.input-area { display: flex; gap: 8px; margin-top: 12px; }
.loading { margin-top: 8px; color: #888; }
.save-btn { margin-top: 8px; }
</style>