<template>
  <div class="color-extractor">
    <h4 class="tool-title">色彩提取器</h4>
    
    <!-- 图片上传区域 -->
    <div class="upload-area">
      <el-upload
        class="image-uploader"
        action=""
        :show-file-list="false"
        :http-request="handleImageUpload"
        :before-upload="beforeUpload"
        accept="image/*"
      >
        <div v-if="!imageUrl" class="upload-placeholder">
          <i class="el-icon-picture-outline"></i>
          <p>点击上传图片提取色彩</p>
        </div>
        <img 
          v-if="imageUrl" 
          :src="imageUrl" 
          class="uploaded-image"
          alt="待提取色彩的图片"
        >
      </el-upload>
      
      <el-button 
        v-if="imageUrl"
        type="text" 
        class="reset-btn"
        @click="resetUpload"
      >
        重新上传
      </el-button>
    </div>
    
    <!-- 提取结果区域 -->
    <div v-if="extractedColors.length > 0" class="result-area">
      <h5 class="result-title">提取的主要色彩</h5>
      <div class="color-palette">
        <div 
          v-for="(color, index) in extractedColors" 
          :key="index"
          class="color-item"
          :style="{ backgroundColor: color.hex }"
          @click="copyColorValue(color)"
        >
          <div class="color-value">
            <span>{{ color.hex }}</span>
            <span class="rgb-value">{{ color.rgb }}</span>
          </div>
          <div class="copy-tooltip" v-if="color.copied">
            已复制!
          </div>
        </div>
      </div>
      
      <el-button 
        type="primary" 
        size="small"
        class="apply-btn"
        @click="applyColors"
      >
        应用这些色彩
      </el-button>
    </div>
    
    <!-- 加载状态 -->
    <div v-if="loading" class="loading-state">
      <div class="custom-spinner"></div>
      <p>正在提取色彩...</p>
    </div>
    
    <!-- 提示信息 -->
    <div v-if="message" class="message提示">
      <p>{{ message }}</p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ColorExtractor',
  data() {
    return {
      // 上传的图片URL
      imageUrl: '',
      // 提取的色彩列表
      extractedColors: [],
      // 加载状态
      loading: false,
      // 提示信息
      message: ''
    };
  },
  methods: {
    // 上传前校验
    beforeUpload(file) {
      const isImage = file.type.startsWith('image/');
      const isLt5M = file.size / 1024 / 1024 < 5;
      
      if (!isImage) {
        this.message = '请上传图片文件';
        return false;
      }
      if (!isLt5M) {
        this.message = '图片大小不能超过5MB';
        return false;
      }
      
      this.message = '';
      return true;
    },
    
    // 处理图片上传（前端处理，不上传服务器）
    handleImageUpload(params) {
      const file = params.file;
      const reader = new FileReader();
      
      reader.onload = (e) => {
        this.imageUrl = e.target.result;
        this.extractColorsFromImage(this.imageUrl);
      };
      
      reader.readAsDataURL(file);
    },
    
    // 从图片中提取色彩
    extractColorsFromImage(imageUrl) {
      this.loading = true;
      this.extractedColors = [];
      
      const img = new Image();
      img.crossOrigin = 'anonymous'; // 处理跨域
      
      img.onload = () => {
        // 创建小尺寸画布用于分析（提高性能）
        const canvas = document.createElement('canvas');
        const ctx = canvas.getContext('2d');
        
        // 缩小图片尺寸，加快处理速度
        const maxDimension = 100;
        let width = img.width;
        let height = img.height;
        
        if (width > height) {
          if (width > maxDimension) {
            height = (height * maxDimension) / width;
            width = maxDimension;
          }
        } else {
          if (height > maxDimension) {
            width = (width * maxDimension) / height;
            height = maxDimension;
          }
        }
        
        canvas.width = width;
        canvas.height = height;
        ctx.drawImage(img, 0, 0, width, height);
        
        // 获取像素数据
        const imageData = ctx.getImageData(0, 0, width, height).data;
        const pixelCount = width * height;
        const colorFrequency = {};
        
        // 统计色彩频率（每5个像素取一个，提高性能）
        for (let i = 0; i < pixelCount; i += 5) {
          const index = i * 4;
          const r = imageData[index];
          const g = imageData[index + 1];
          const b = imageData[index + 2];
          const a = imageData[index + 3];
          
          // 忽略透明像素
          if (a < 255) continue;
          
          // 简化色彩（减少色值数量）
          const key = `${Math.round(r / 32) * 32},${Math.round(g / 32) * 32},${Math.round(b / 32) * 32}`;
          colorFrequency[key] = (colorFrequency[key] || 0) + 1;
        }
        
        // 转换为数组并按频率排序
        const sortedColors = Object.entries(colorFrequency)
          .sort((a, b) => b[1] - a[1])
          .map(([rgb]) => {
            const [r, g, b] = rgb.split(',').map(Number);
            return {
              hex: this.rgbToHex(r, g, b),
              rgb: `rgb(${r}, ${g}, ${b})`,
              copied: false
            };
          });
        
        // 取前8种主要色彩
        this.extractedColors = sortedColors.slice(0, 8);
        this.loading = false;
        
        if (this.extractedColors.length === 0) {
          this.message = '未能提取有效的色彩，请尝试其他图片';
        }
      };
      
      img.onerror = () => {
        this.loading = false;
        this.message = '图片加载失败，请重试';
      };
      
      img.src = imageUrl;
    },
    
    // RGB转十六进制
    rgbToHex(r, g, b) {
      return `#${this.componentToHex(r)}${this.componentToHex(g)}${this.componentToHex(b)}`;
    },
    
    componentToHex(c) {
      const hex = c.toString(16);
      return hex.length === 1 ? '0' + hex : hex;
    },
    
    // 复制色值到剪贴板
    copyColorValue(color) {
      // 重置所有复制状态
      this.extractedColors.forEach(c => c.copied = false);
      
      // 复制色值
      navigator.clipboard.writeText(color.hex).then(() => {
        color.copied = true;
        
        // 2秒后隐藏复制提示
        setTimeout(() => {
          color.copied = false;
        }, 2000);
      }).catch(() => {
        this.message = '复制失败，请手动复制';
        setTimeout(() => this.message = '', 2000);
      });
    },
    
    // 应用色彩（向父组件传递）
    applyColors() {
      this.$emit('apply-colors', this.extractedColors);
      this.$message.success('已应用所选色彩');
    },
    
    // 重置上传
    resetUpload() {
      this.imageUrl = '';
      this.extractedColors = [];
      this.message = '';
    }
  }
};
</script>

<style scoped>
.color-extractor {
  padding: 16px;
  border-top: 1px solid #eee;
}

.tool-title {
  margin: 0 0 16px;
  font-size: 16px;
  color: #333;
}

/* 上传区域样式 */
.upload-area {
  position: relative;
  margin-bottom: 20px;
}

.upload-placeholder {
  width: 100%;
  height: 140px;
  border: 1px dashed #ddd;
  border-radius: 8px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #999;
  background-color: #fafafa;
  cursor: pointer;
  transition: border-color 0.2s;
}

.upload-placeholder:hover {
  border-color: #1890ff;
}

.upload-placeholder .el-icon-picture-outline {
  font-size: 32px;
  margin-bottom: 8px;
}

.uploaded-image {
  width: 100%;
  height: 180px;
  object-fit: contain;
  border-radius: 8px;
  border: 1px solid #eee;
  background-color: #f9f9f9;
}

.reset-btn {
  position: absolute;
  top: 8px;
  right: 8px;
  color: #666;
  background-color: rgba(255, 255, 255, 0.8);
  padding: 2px 8px;
}

/* 结果区域样式 */
.result-area {
  margin-top: 20px;
}

.result-title {
  font-size: 14px;
  color: #666;
  margin: 0 0 12px;
}

.color-palette {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.color-item {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
}

.color-item:hover {
  transform: scale(1.05);
}

.color-value {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 4px;
  background: rgba(0, 0, 0, 0.5);
  color: white;
  font-size: 12px;
}

.rgb-value {
  display: block;
  font-size: 10px;
  opacity: 0.8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.copy-tooltip {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(0, 0, 0, 0.7);
  color: white;
  padding: 4px 8px;
  border-radius: 4px;
  font-size: 12px;
}

.apply-btn {
  margin-top: 8px;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
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

/* 提示信息 */
.message提示 {
  text-align: center;
  padding: 10px;
  color: #f56c6c;
  font-size: 14px;
}
</style>
