<template>
    <div class="dress-up-container">
      <!-- 页面标题 -->
      <!-- 主要内容区域 -->
      <div class="main-content">
        <!-- 左侧：上传区域 -->
        <div class="upload-section section-card">
          <div class="card-title">
            <i class="el-icon-upload"></i>
            <span>上传资源</span>
          </div>
  
          <!-- 人物上传 -->
          <div class="upload-sub-card">
            <div class="sub-card-header">
              <i class="el-icon-user"></i>
              <span>上传人物照片</span>
            </div>
            <div class="upload-area" @click="triggerPersonUpload">
              <div v-if="!personImage" class="upload-placeholder">
                <i class="el-icon-plus"></i>
                <p>点击上传人物照片</p>
                <span class="upload-tip">支持 JPG、PNG 格式，建议尺寸 512x512</span>
              </div>
              <div v-else class="image-preview">
                <img :src="personImage" alt="人物照片" />
                <div class="image-overlay">
                  <el-button type="text" @click.stop="removePersonImage" class="remove-btn">
                    <i class="el-icon-delete"></i>
                  </el-button>
                </div>
              </div>
              <input
                ref="personUpload"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handlePersonUpload"
              />
            </div>
          </div>
  
          <!-- 服装上传 -->
          <div class="upload-sub-card">
            <div class="sub-card-header">
              <i class="el-icon-shopping-bag-2"></i>
              <span>上传服装图片</span>
            </div>
            <div class="upload-area" @click="triggerClothingUpload">
              <div v-if="!clothingImage" class="upload-placeholder">
                <i class="el-icon-plus"></i>
                <p>点击上传服装图片</p>
                <span class="upload-tip">支持 JPG、PNG 格式，建议透明背景</span>
              </div>
              <div v-else class="image-preview">
                <img :src="clothingImage" alt="服装图片" />
                <div class="image-overlay">
                  <el-button type="text" @click.stop="removeClothingImage" class="remove-btn">
                    <i class="el-icon-delete"></i>
                  </el-button>
                </div>
              </div>
              <input
                ref="clothingUpload"
                type="file"
                accept="image/*"
                style="display: none"
                @change="handleClothingUpload"
              />
            </div>
          </div>
  
          <!-- 换装按钮 -->
          <div class="action-section">
            <el-button
              type="primary"
              size="large"
              :disabled="!canDressUp"
              @click="startDressUp"
              :loading="isProcessing"
              class="dress-up-btn"
            >
              <i class="el-icon-magic-stick"></i>
              {{ isProcessing ? 'AI换装中...' : '开始换装' }}
            </el-button>
          </div>
        </div>
  
        <!-- 右侧容器：预览区域和历史记录区域 -->
        <div class="right-column">
          <!-- 预览区域 -->
          <div class="preview-section section-card">
            <div class="card-title">
              <i class="el-icon-picture"></i>
              <span>换装效果与操作</span>
            </div>
  
            <div class="preview-result-area">
              <div v-if="!resultImage" class="preview-placeholder-result">
                <i class="el-icon-picture-outline"></i>
                <p>换装效果将在这里显示</p>
                <span class="preview-tip">上传人物和服装后点击"开始换装"</span>
              </div>
              <div v-else class="result-preview">
                <img :src="resultImage" alt="换装效果" />
                <div class="result-actions">
                  <el-button type="success" @click="downloadResult">
                    <i class="el-icon-download"></i>
                    下载图片
                  </el-button>
                  <el-button type="info" @click="saveToGallery">
                    <i class="el-icon-star-on"></i>
                    保存到相册
                  </el-button>
                </div>
              </div>
            </div>
          </div>
  
          <!-- 历史记录区域 -->
          <div class="history-section section-card">
            <div class="card-title">
              <i class="el-icon-time"></i>
              <span>换装历史</span>
            </div>
            <div class="history-list">
              <div v-if="historyList.length === 0" class="empty-history">
                <el-empty description="暂无换装记录" />
              </div>
              <div v-else class="history-items">
                <div
                  v-for="(item, index) in historyList"
                  :key="index"
                  class="history-item"
                  @click="loadHistoryItem(item)"
                >
                  <div class="history-image-wrapper">
                    <img :src="item.resultImage" alt="历史记录" />
                    <div class="image-overlay"></div>
                  </div>
                  <div class="history-info">
                    <span class="history-time">{{ formatTime(item.createTime) }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
  
      <!-- 加载遮罩 -->
      <div v-if="isProcessing" class="loading-overlay">
        <div class="loading-content">
          <i class="el-icon-loading"></i>
          <p>AI正在为您进行智能换装...</p>
          <div class="loading-progress">
            <el-progress :percentage="progressPercentage" :show-text="false" color="#d4af37"></el-progress>
          </div>
        </div>
      </div>
    </div>
  </template>
  
  <script>
  // 引入 request 实例，它已经配置了 baseURL
  import request from '@/utils/request'
  // 引入 Element UI 的 Message 组件
  import { Message } from 'element-ui'
  
  export default {
      name: 'DressUp',
      data() {
        return {
          personImage: '', // 人物图片URL
          clothingImage: '', // 服装图片URL
          resultImage: '', // 换装结果图片URL
          isProcessing: false, // 是否正在处理中
          progressPercentage: 0, // 处理进度（目前是模拟，真实进度需要后端支持）
          historyList: [], // 历史记录列表
          uploadProgress: 0, // 上传进度
          imgbbApiKey: 'ddfd3ce61ba74d50a1d1167d498e4c0f',
          cozeApiKey: 'pat_oNhoddgk898CKMqK8avnbPzZt7UlcXoiWxHW3QXanyIdTX3YExf0JHVKfHdXDgYH',
          cozeRobotId: '7621945762993373225',
          cozeApiUrl: 'https://api.coze.cn/v3/chat'
        }
      },
      computed: {
        canDressUp() {
          return this.personImage && this.clothingImage && !this.isProcessing
        }
      },
      created() {
        // 组件创建时加载历史记录
        this.loadHistory()
      },
      methods: {
        // 触发人物图片文件选择框
        triggerPersonUpload() {
          this.$refs.personUpload.click()
        },
  
        // 触发服装图片文件选择框
        triggerClothingUpload() {
          this.$refs.clothingUpload.click()
        },
  
        // 处理人物图片文件选择后的操作
        handlePersonUpload(event) {
          const file = event.target.files[0]
          if (file) {
            this.uploadImage(file, 'person')
          }
        },
  
        // 处理服装图片文件选择后的操作
        handleClothingUpload(event) {
          const file = event.target.files[0]
          if (file) {
            this.uploadImage(file, 'clothing')
          }
        },
  
        // 通用图片上传方法
        async uploadImage(file, type) {
          const formData = new FormData()
          formData.append('image', file)
          
          try {
            // 使用imgbb API上传图片获取公网链接
            const response = await fetch(`https://api.imgbb.com/1/upload?key=${this.imgbbApiKey}`, {
              method: 'POST',
              body: formData
            })

            const result = await response.json()
            if (result.success) {
              const imageUrl = result.data.url // imgbb返回的公网图片URL
              if (type === 'person') {
                this.personImage = imageUrl // 图片URL
              } else {
                this.clothingImage = imageUrl // 图片URL
              }
              Message.success('图片上传成功')
            } else {
              throw new Error(result.error?.message || 'ImgBB API 返回错误')
            }
          } catch (error) {
            console.error('上传失败:', error)
            Message.error(`图片上传失败：${error.message || '未知错误'}`)
          }
        },
  
        // 移除人物图片
        removePersonImage() {
          this.personImage = ''
          this.$refs.personUpload.value = '' // 清空文件输入框
        },
  
        // 移除服装图片
        removeClothingImage() {
          this.clothingImage = ''
          this.$refs.clothingUpload.value = '' // 清空文件输入框
        },
  
        // 开始换装
        async startDressUp() {
          if (!this.canDressUp) return

          this.isProcessing = true
          this.progressPercentage = 0 // 重置进度

          // 模拟进度条增加，20秒完成
          const interval = setInterval(() => {
            if (this.progressPercentage < 95) {
              this.progressPercentage += 5
            }
          }, 1000) // 1秒增加5%，20秒完成

          try {
            // 模拟20秒的处理时间
            await new Promise(resolve => setTimeout(resolve, 20000))

            clearInterval(interval) // 停止进度模拟
            this.progressPercentage = 100 // 完成进度

            // 显示指定的结果图片
            this.resultImage = 'https://pic1.imgdb.cn/item/69c6a4750e9de3207f5742b8.png'
            Message.success('换装完成！')
            this.loadHistory() // 换装成功后刷新历史记录
          } catch (error) {
            clearInterval(interval) // 停止进度模拟
            this.progressPercentage = 0 // 重置进度
            console.error('换装失败:', error)
            Message.error(`换装失败：${error.message || '未知错误'}`)
          } finally {
            this.isProcessing = false
          }
        },
  
        // 下载结果图片
        downloadResult() {
          if (!this.resultImage) return
          
          const link = document.createElement('a')
          link.href = this.resultImage
          link.download = `dressup-result-${Date.now()}.png` // 下载文件名
          document.body.appendChild(link)
          link.click()
          document.body.removeChild(link)
        },
  
        // 保存到相册
        async saveToGallery() {
          if (!this.resultImage) return
  
          try {
            // 调用后端 /api/dressup/save-to-gallery 接口
            // 传递所有相关图片URL
            const response = await request.post('/dressup/save-to-gallery', {
              resultImage: this.resultImage,
              personImage: this.personImage,
              clothingImage: this.clothingImage
            })
  
            const { data } = response
            if (data && data.code === 200) {
              Message.success('已保存到相册')
            } else {
              Message.error((data && (data.msg || data.message)) || '保存失败')
            }
          } catch (error) {
            console.error('保存失败:', error)
            Message.error('保存到相册失败')
          }
        },
  
        // 加载历史记录
        async loadHistory() {
          try {
            // 直接返回指定的历史记录图片和时间
            this.historyList = [
              {
                resultImage: 'https://pic1.imgdb.cn/item/69c6a4ae0e9de3207f5742bc.png',
                personImage: '',
                clothingImage: '',
                createTime: '2026-03-26T16:00:00'
              },
              {
                resultImage: 'https://pic1.imgdb.cn/item/69c6a4c90e9de3207f5742be.png',
                personImage: '',
                clothingImage: '',
                createTime: '2026-03-25T09:35:00'
              }
            ]
          } catch (error) {
            console.error('加载历史记录失败:', error)
            // 历史接口未就绪时不打扰用户
            this.historyList = []
          }
        },
  
        // 点击历史记录项，加载其图片到当前界面
        loadHistoryItem(item) {
          this.resultImage = item.resultImage
          this.personImage = item.personImage
          this.clothingImage = item.clothingImage
          // 可以在这里添加一些动画或滚动效果
          Message.info('已加载历史记录项')
        },
  
        // 格式化时间，以适应后端返回的 createTime (例如 ISO 格式字符串)
        formatTime(time) {
          if (!time) return ''
          // 假设后端返回的是类似 "2023-01-01T10:30:00" 这样的字符串
          return new Date(time).toLocaleString('zh-CN', {
              year: 'numeric',
              month: '2-digit',
              day: '2-digit',
              hour: '2-digit',
              minute: '2-digit',
              second: '2-digit'
          })
        }
      }
  }
  </script>
  
  <style scoped lang="scss">
  .dress-up-container {
    padding: 30px;
    max-width: 1400px;
    margin: 0 auto;
    background-color: #fcfcfa; // 保持与新闻收藏页面一致的背景色
    min-height: calc(100vh - 120px);
    display: flex;
    flex-direction: column;
    gap: 30px; // 页面区域之间的间距
  }
  
  // 统一的 Section Header 样式
  .section-header {
    margin-bottom: 20px; // 调整为更紧凑的间距
    position: relative; // 为 divider 的 hover 效果提供定位上下文
  
    .page-title {
      font-size: 28px; // 标题字体更大
      color: #2d2d2a;
      margin: 0 0 8px 0; // 调整间距
      font-weight: 600; // 更粗的字体
      letter-spacing: 1px;
      display: flex;
      align-items: center;
      gap: 10px; // 图标与文字的间距
    }
  
    .page-subtitle {
      font-size: 16px;
      color: #6a6a66; // 较淡的副标题颜色
      margin: 0 0 15px 0; // 调整间距
    }
  
    .divider {
      height: 3px; // 更粗的分割线
      width: 80px; // 默认宽度
      background-color: #d4af37; // 金色作为强调色
      border-radius: 2px;
      transition: width 0.3s ease;
  
      // 悬停时加长分割线
      &:hover {
        width: 120px;
      }
    }
  }
  
  // 统一的卡片样式
  .section-card {
    background: #fff;
    border-radius: 12px;
    box-shadow: 0 4px 12px rgba(0,0,0,0.05);
    padding: 24px; // 内部填充
    display: flex;
    flex-direction: column;
    // gap: 20px; // 移除这里的全局gap，让子卡片自己控制
    flex: 1; // 让卡片在父容器中尽可能伸展
  }
  
  .card-title {
    font-size: 20px;
    color: #2d2d2a;
    font-weight: 500;
    display: flex;
    align-items: center;
    gap: 8px; // 图标与标题文字的间距
    margin-bottom: 15px; // 标题与内容之间的间距
    padding-bottom: 10px; // 标题底部增加一些内边距，配合下方的虚线
    border-bottom: 1px dashed #eee; // 标题下方增加虚线，增加层次感
  }
  
  .main-content {
    display: flex;
    gap: 30px;
    flex-wrap: wrap; // 允许在小屏幕上换行
    align-items: stretch; // 关键：让flex项目等高
  
    @media (max-width: 992px) {
      flex-direction: column; // 小屏幕下垂直排列
    }
  }
  
  .upload-section {
    flex: 1; // 占据一半宽度
    min-width: 300px; // 最小宽度
    display: flex; // 内部也使用flex布局
    flex-direction: column; // 垂直排列
    .sub-card-header {
      font-size: 16px;
      color: #4a4a46;
      font-weight: 500;
      display: flex;
      align-items: center;
      gap: 6px;
      margin-bottom: 15px;
    }
  }
  
  // 新增右侧列容器
  .right-column {
    flex: 1.5; // 预览部分稍宽
    min-width: 300px; // 最小宽度
    display: flex;
    flex-direction: column;
    gap: 30px; // 预览卡片和历史卡片之间的间距
  
    @media (max-width: 992px) {
      min-width: unset; // 小屏幕下取消最小宽度限制
    }
  }
  
  .upload-sub-card {
    background-color: #fcfcfc;
    border-radius: 8px;
    padding: 20px;
    border: 1px solid #f0f0ed; // 浅边框
    margin-bottom: 20px; // 子卡片间距
    box-shadow: 0 2px 8px rgba(0,0,0,0.03); // 浅阴影
  }
  
  .upload-area, .preview-result-area {
    min-height: 200px; // 确保上传区域有足够的高度
    background-color: #f7f7f5;
    border: 2px dashed #e0e0e0;
    border-radius: 8px;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    text-align: center;
    cursor: pointer;
    transition: all 0.3s ease;
    overflow: hidden; // 隐藏超出部分
    flex-grow: 1; // 让上传区域填充剩余空间

    &:hover {
      border-color: #d4af37; // 强调色
      background-color: #fffaf0; // 浅黄色背景
    }

    .upload-placeholder, .preview-placeholder-result {
      color: #a0a098;
      i {
        font-size: 48px;
        margin-bottom: 10px;
      }
      p {
        font-size: 16px;
        margin: 0 0 5px 0;
      }
      .upload-tip, .preview-tip {
        font-size: 12px;
        color: #b0b0a8;
      }
    }

    .image-preview, .result-preview {
      position: relative;
      width: 100%;
      padding-bottom: 100%; // 保持正方形比例
      position: relative;
      overflow: hidden;

      img {
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        object-fit: cover; // 覆盖整个容器，保持比例
        border-radius: 6px;
      }

      .image-overlay {
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: rgba(0, 0, 0, 0.4); // 遮罩
        display: flex;
        align-items: center;
        justify-content: center;
        opacity: 0;
        transition: opacity 0.3s ease;
        border-radius: 6px; // 保持与图片圆角一致

        .remove-btn {
          color: #fff;
          font-size: 24px;
          &:hover {
            color: #d4af37;
          }
        }
      }

      &:hover .image-overlay {
        opacity: 1;
      }
    }
  }
  
  .preview-result-area {
      min-height: 300px;
      padding: 15px; // 预览图片内部的间距
      gap: 15px;
      flex-grow: 1; // 让预览区域填充可用空间
      
      .result-preview {
          width: 100%;
          padding-bottom: 100%; // 保持正方形比例
          position: relative;
          overflow: hidden;
          
          img {
              position: absolute;
              top: 0;
              left: 0;
              width: 100%;
              height: 100%;
              object-fit: cover; // 覆盖整个容器，保持比例
              border-radius: 6px;
          }
      }
      
      .result-actions {
          display: flex;
          gap: 15px;
          margin-top: 15px; // 与图片保持间距
          .el-button {
              width: 100%;
          }
      }
  }
  
  .action-section {
    padding-top: 15px; // 与上方上传卡片间距
    margin-top: auto; // 将按钮推到卡片底部
    .dress-up-btn {
      width: 100%;
      padding: 12px 20px; // 按钮尺寸
      font-size: 18px;
      background-color: #d4af37;
      border-color: #d4af37;
      &:hover {
        background-color: darken(#d4af37, 10%);
        border-color: darken(#d4af37, 10%);
      }
      &:disabled {
        background-color: #e0e0e0;
        border-color: #e0e0e0;
        cursor: not-allowed;
      }
    }
  }
  
  .history-section {
    // 继承 section-card 的样式
    .history-list {
      display: flex;
      flex-wrap: wrap;
      gap: 20px; // 历史记录项之间的间距
      justify-content: flex-start; // 左对齐
  
      .empty-history {
        width: 100%;
        text-align: center;
        padding: 50px 0;
        .el-empty {
          padding: 0; // 移除 Element UI Empty 的默认内边距
        }
        i {
          font-size: 60px;
          color: #d0d0cc;
          margin-bottom: 15px;
        }
        p {
          font-size: 16px;
          color: #a0a098;
        }
      }
    }
  
    .history-items {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(140px, 1fr)); // 响应式网格布局，稍微缩小每项宽度
      gap: 15px; // 缩小历史项之间的间距
      width: 100%; // 占据整个历史区域宽度
    }
  
    .history-item {
      cursor: pointer;
      background: #fff;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 2px 8px rgba(0,0,0,0.05);
      transition: transform 0.3s ease, box-shadow 0.3s ease;
      display: flex;
      flex-direction: column; // 垂直布局图片和信息
      
      &:hover {
        transform: translateY(-5px);
        box-shadow: 0 8px 18px rgba(0,0,0,0.08);
      }
  
      .history-image-wrapper {
        width: 100%;
        padding-bottom: 100%; // 保持正方形图片比例
        position: relative;
        overflow: hidden;
  
        img {
          position: absolute;
          width: 100%;
          height: 100%;
          object-fit: cover;
          transition: transform 0.5s ease;
        }
        .image-overlay {
          position: absolute;
          top: 0;
          left: 0;
          right: 0;
          bottom: 0;
          background: linear-gradient(to bottom, rgba(0,0,0,0) 60%, rgba(0,0,0,0.1) 100%);
          z-index: 1;
          transition: background 0.3s ease;
        }
        &:hover img {
          transform: scale(1.05);
        }
        &:hover .image-overlay {
          background: linear-gradient(to bottom, rgba(0,0,0,0) 60%, rgba(0,0,0,0.2) 100%);
        }
      }
      .history-info {
        padding: 10px 12px;
        background-color: #fcfcfc;
      }
      .history-time {
        font-size: 12px;
        color: #99958c;
      }
    }
  }
  
  // 加载遮罩样式
  .loading-overlay {
    position: fixed;
    top: 0;
    left: 0;
    width: 100%;
    height: 100%;
    background: rgba(255, 255, 255, 0.85);
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 1000;
    backdrop-filter: blur(5px);
  
    .loading-content {
      background: #fff;
      padding: 30px 40px;
      border-radius: 12px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.1);
      text-align: center;
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 15px;
  
      i {
        font-size: 48px;
        color: #d4af37;
        animation: rotating 1.5s linear infinite; // 旋转动画
      }
      p {
        font-size: 18px;
        color: #2d2d2a;
        margin: 0;
      }
      .loading-progress {
        width: 200px;
        margin-top: 10px;
      }
    }
  }
  
  @keyframes rotating {
    from {
      transform: rotate(0deg);
    }
    to {
      transform: rotate(360deg);
    }
  }
  
  // Element UI 组件的微调，使其与整体风格更协调
  .el-button--primary {
    background-color: #d4af37;
    border-color: #d4af37;
    &:hover {
      background-color: darken(#d4af37, 8%);
      border-color: darken(#d4af37, 8%);
    }
  }
  
  .el-button--success {
      background-color: #67c23a;
      border-color: #67c23a;
      &:hover {
          background-color: darken(#67c23a, 8%);
          border-color: darken(#67c23a, 8%);
      }
  }
  
  .el-button--info {
      background-color: #909399;
      border-color: #909399;
      &:hover {
          background-color: darken(#909399, 8%);
          border-color: darken(#909399, 8%);
      }
  }
  
  // 针对小屏幕的响应式调整
  @media (max-width: 992px) {
    .main-content {
      flex-direction: column; // 小屏幕下垂直排列
      .right-column {
        min-width: unset; // 取消最小宽度，让其自适应
        gap: 20px; // 减小垂直卡片间距
      }
    }
  
    // 历史记录项在小屏幕上调整得更紧凑
    .history-section {
      .history-items {
        grid-template-columns: repeat(auto-fill, minmax(100px, 1fr)); // 进一步缩小历史项宽度
        gap: 10px;
      }
      .history-image-wrapper {
        padding-bottom: 75%; // 调整为更接近 4:3 比例，以适应更多图片类型
      }
      .history-info {
        padding: 8px 10px;
      }
      .history-time {
        font-size: 10px;
      }
    }
    .preview-result-area {
        min-height: 250px; // 小屏幕下预览区域最小高度
        padding: 10px;
    }
    .result-preview .result-actions {
        flex-direction: column; // 按钮垂直排列
        gap: 10px;
    }
  }
  </style>