<template>
  <div class="color-extractor">
    <!-- 标题：手绘装饰点 -->
    <h4 class="extractor-title">
      <span class="title-dot"></span>色彩提取器
    </h4>

    <!-- 上传区域：暖色调按钮 -->
    <div class="upload-area">
      <el-input
        v-model="imageUrl"
        placeholder="粘贴公共图片URL"
        clearable
        class="url-input"
        @clear="clearImageUrl"
      ></el-input>

      <!-- 暖色调上传按钮（无蓝色） -->
      <el-upload
        class="image-uploader"
        action=""
        :auto-upload="false"
        :on-change="handleImageUpload"
        :show-file-list="false"
        accept="image/jpeg,image/png,image/webp"
        :disabled="isExtracting"
      >
        <el-button class="upload-btn">
          <i class="el-icon-cloud-upload"></i> 上传本地图片
        </el-button>
      </el-upload>

      <!-- 暖色调提取按钮（无蓝色） -->
      <el-button
        class="extract-btn"
        @click="extractColors"
        :disabled="!imageUrl && !uploadedFile || isExtracting"
      >
        <i class="el-icon-color"></i> {{ isExtracting ? '提取中...' : '提取主色' }}
      </el-button>
    </div>

    <!-- 手绘调色板：整齐排列 -->
    <div class="palette-area">
      <h5 class="palette-title">当前调色板</h5>
      <div class="color-palette" v-if="extractedColors.length > 0">
        <div
          v-for="(color, index) in extractedColors"
          :key="index"
          class="color-swatch"
          :class="{ selected: selectedColorIndexes.includes(index) }"
          :style="{ backgroundColor: color.hex }"
          @click="toggleColorSelect(index)"
          title="点击选中/取消"
        >
          <span class="color-hex">{{ color.hex }}</span>
        </div>
      </div>
      <div class="empty-palette" v-else>
        <i class="el-icon-paintbrush"></i>
        <p>上传图片后自动生成调色板</p>
      </div>
    </div>

    <!-- 图片预览：完整显示 -->
    <div class="preview-area" v-if="previewImageUrl">
      <p class="preview-label">提取参考图</p>
      <img :src="previewImageUrl" alt="提取参考图" class="preview-img" />
    </div>

    <!-- 空状态提示 -->
    <div class="global-empty-state" v-if="!previewImageUrl && extractedColors.length === 0">
      <i class="el-icon-picture-outline"></i>
      <p>粘贴图片URL或上传本地图片，提取主色调</p>
    </div>

    <!-- 错误提示 -->
    <div class="error-state" v-if="extractError">
      <i class="el-icon-warning"></i>
      <p>{{ extractError }}</p>
    </div>

    <!-- 暖色调应用按钮（无蓝色） -->
    <el-button
      class="apply-btn"
      @click="applySelectedColors"
      :disabled="extractedColors.length === 0 || selectedColorIndexes.length === 0"
    >
      应用选中的调色板
    </el-button>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'ColorExtractor',
  data() {
    return {
      imageUrl: '',
      uploadedFile: null,
      previewImageUrl: '',
      extractedColors: [],
      selectedColorIndexes: [],
      isExtracting: false,
      extractError: '',
      imgbbApiKey: 'ddfd3ce61ba74d50a1d1167d498e4c0f',
      maxColors: 8,
    };
  },
  methods: {
    clearImageUrl() {
      this.imageUrl = '';
      this.previewImageUrl = '';
      this.extractedColors = [];
      this.selectedColorIndexes = [];
      this.extractError = '';
    },

    async handleImageUpload(file) {
      const imageFile = file.raw;
      if (!imageFile || this.isExtracting) return;

      this.uploadedFile = imageFile;
      this.isExtracting = true;
      this.extractError = '';

      try {
        const formData = new FormData();
        formData.append('image', imageFile);
        const response = await fetch(`https://api.imgbb.com/1/upload?key=${this.imgbbApiKey}`, {
          method: 'POST',
          body: formData,
        });
        const result = await response.json();

        if (result.success) {
          this.previewImageUrl = result.data.url;
          this.imageUrl = '';
          this.$message.success('图片上传成功！');
        } else {
          throw new Error(result.error?.message || '图片上传失败');
        }
      } catch (error) {
        this.extractError = `上传失败：${error.message}`;
        this.$message.error(this.extractError);
      } finally {
        this.isExtracting = false;
      }
    },

    async extractColors() {
      if (this.isExtracting) return;
      this.isExtracting = true;
      this.extractError = '';
      this.extractedColors = [];
      this.selectedColorIndexes = [];

      try {
        const finalImageUrl = this.imageUrl.trim() || this.previewImageUrl;
        if (!finalImageUrl) {
          throw new Error('请先上传图片或输入图片URL');
        }

        const img = new Image();
        img.crossOrigin = 'Anonymous';
        img.onload = async () => {
          try {
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            const scale = 0.2;
            canvas.width = img.width * scale;
            canvas.height = img.height * scale;
            ctx.drawImage(img, 0, 0, canvas.width, canvas.height);

            const imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
            const pixels = imageData.data;
            const colorMap = new Map();

            for (let i = 0; i < pixels.length; i += 4) {
              const r = pixels[i];
              const g = pixels[i + 1];
              const b = pixels[i + 2];
              const a = pixels[i + 3];
              if (a < 128) continue;

              const hex = this.rgbToHex(r, g, b);
              const count = colorMap.get(hex) || 0;
              colorMap.set(hex, count + 1);
            }

            const sortedColors = Array.from(colorMap.entries())
              .sort((a, b) => b[1] - a[1])
              .slice(0, this.maxColors);

            this.extractedColors = sortedColors.map(([hex]) => ({
              hex,
              name: this.getColorName(hex),
            }));
            this.selectedColorIndexes = [0, 1, 2, 3].filter(i => i < this.extractedColors.length);
            this.$message.success(`成功生成${this.extractedColors.length}色调色板！`);
          } catch (error) {
            this.extractError = `提取失败：${error.message}`;
            this.$message.error(this.extractError);
          } finally {
            this.isExtracting = false;
          }
        };

        img.onerror = () => {
          this.extractError = '图片加载失败，请检查URL是否有效';
          this.$message.error(this.extractError);
          this.isExtracting = false;
        };

        img.src = finalImageUrl;
      } catch (error) {
        this.extractError = error.message;
        this.$message.error(this.extractError);
        this.isExtracting = false;
      }
    },

    rgbToHex(r, g, b) {
      return `#${((1 << 24) + (r << 16) + (g << 8) + b).toString(16).slice(1)}`;
    },

    getColorName(hex) {
      const colorNames = {
        '#ffffff': '白色', '#000000': '黑色', '#ff0000': '红色', '#00ff00': '绿色', '#0000ff': '蓝色',
        '#ffff00': '黄色', '#ff00ff': '洋红', '#00ffff': '青色', '#ffa500': '橙色', '#800080': '紫色',
      };
      return colorNames[hex.toLowerCase()] || `自定义色`;
    },

    toggleColorSelect(index) {
      if (this.selectedColorIndexes.includes(index)) {
        this.selectedColorIndexes = this.selectedColorIndexes.filter(i => i !== index);
      } else {
        this.selectedColorIndexes.push(index);
      }
    },

    applySelectedColors() {
      const selectedColors = this.selectedColorIndexes.map(i => this.extractedColors[i]);
      this.$emit('apply-colors', selectedColors);
      this.$message.success('调色板已应用！');
    },
  },
};
</script>

<style scoped>
/* 容器：手绘暖风格基底 */
.color-extractor {
  padding: 15px;
  background-color: rgba(255, 255, 250, 0.98);
  height: 100%;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  gap: 16px;
  overflow: hidden;
}

/* 标题：手绘装饰点 */
.extractor-title {
  margin: 0;
  font-size: 16px;
  color: #8a7d5e;
  display: flex;
  align-items: center;
  gap: 8px;
}
.title-dot {
  display: inline-block;
  width: 8px;
  height: 16px;
  background-color: #f0c880;
  border-radius: 4px;
}

/* 上传区域：暖色调按钮+整齐布局 */
.upload-area {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}
.url-input {
  flex: 1;
  min-width: 120px;
  --el-input-border-color: #f0e8d0 !important;
  --el-input-hover-border-color: #f0c880 !important;
  --el-input-text-color: #8a7d5e !important;
  --el-input-placeholder-color: #a09880 !important;
  border-radius: 22px !important;
}

/* 所有按钮：强制暖色调（无蓝色） */
.upload-btn, .extract-btn, .apply-btn {
  --el-button-bg-color: #f5dca8 !important;
  --el-button-border-color: #f0c880 !important;
  --el-button-text-color: #8a7d5e !important;
  --el-button-hover-bg-color: #f0c880 !important;
  --el-button-hover-text-color: #7a6d4e !important;
  --el-button-disabled-bg-color: #f9eeda !important;
  --el-button-disabled-border-color: #f5dca8 !important;
  --el-button-disabled-text-color: #a09880 !important;
  border-radius: 22px !important;
  padding: 8px 16px !important;
}
.apply-btn {
  width: 100%;
  margin-top: auto;
}

/* 手绘调色板：整齐排列 */
.palette-area {
  background-color: #fff8e8;
  border: 1px solid #f0e8d0;
  border-radius: 12px;
  padding: 12px;
  box-shadow: 0 3px 8px rgba(240, 224, 180, 0.12);
}
.palette-title {
  margin: 0 0 12px;
  font-size: 14px;
  color: #8a7d5e;
  display: flex;
  align-items: center;
  gap: 6px;
}
.palette-title::before {
  content: "";
  width: 6px;
  height: 12px;
  background-color: #f0c880;
  border-radius: 3px;
}
.color-palette {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
}
.color-swatch {
  width: 100%;
  height: 40px;
  border-radius: 8px;
  border: 2px solid #fff;
  box-shadow: 0 2px 4px rgba(240, 200, 128, 0.2);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s ease;
}
.color-swatch:hover {
  transform: scale(1.05);
}
.color-swatch.selected {
  box-shadow: 0 0 0 2px #f0c880;
}
.color-hex {
  font-size: 10px;
  color: #fff;
  text-shadow: 0 0 2px rgba(0,0,0,0.5);
  font-weight: bold;
}
.empty-palette {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px 0;
  color: #8a7d5e;
}
.empty-palette i {
  font-size: 36px;
  color: #f0c880;
  margin-bottom: 8px;
}
.empty-palette p {
  margin: 0;
  font-size: 13px;
}

/* 图片预览：完整显示 */
.preview-area {
  border: 1px solid #f0e8d0;
  border-radius: 8px;
  padding: 12px;
  background-color: #fff8e8;
}
.preview-label {
  margin: 0 0 8px;
  font-size: 13px;
  color: #8a7d5e;
}
.preview-img {
  width: 100%;
  max-height: 200px;
  object-fit: contain;
  border-radius: 4px;
  border: 1px solid #f0e8d0;
}

/* 空状态/错误状态：手绘风格 */
.global-empty-state, .error-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 20px;
  border-radius: 10px;
  background-color: #fff8e8;
  border: 1px dashed #f0e8d0;
}
.global-empty-state i {
  font-size: 48px;
  color: #f0c880;
  margin-bottom: 10px;
}
.error-state i {
  font-size: 48px;
  color: #e8a07e;
  margin-bottom: 10px;
}
.global-empty-state p, .error-state p {
  margin: 0;
  font-size: 14px;
  color: #8a7d5e;
}
.error-state p {
  color: #e8a07e;
}
</style>