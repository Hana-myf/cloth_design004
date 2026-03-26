<template>
  <div class="womenswear-main">
    <div class="content">
      <div class="header-section">
        <h1 class="title">女装智能制版中心</h1>
        <p class="subtitle">Intelligent Pattern Making System</p>
      </div>
      
      <!-- 功能卡片区：一排展示 -->
      <div class="card-container">
        <!-- 蛋糕裙 -->
        <div class="custom-card pink-theme" @click="$router.push('/womenswear/cake-skirt')">
          <div class="icon-box">
            <img src="https://pic1.imgdb.cn/item/69c2a0732cb231a0e876d983.png" alt="蛋糕裙" class="card-icon">
          </div>
          <div class="text-info">
            <span class="en-tag">Tiered Skirt</span>
            <h3 class="card-title">蛋糕裙制版</h3>
          </div>
        </div>

        <!-- 太阳裙 -->
        <div class="custom-card orange-theme" @click="$router.push('/womenswear/sun-skirt')">
          <div class="icon-box">
            <img src="https://pic1.imgdb.cn/item/69c2be142cb231a0e87755c7.png" alt="太阳裙" class="card-icon">
          </div>
          <div class="text-info">
            <span class="en-tag">Sun Skirt</span>
            <h3 class="card-title">太阳裙制版</h3>
          </div>
        </div>

        <!-- 敬请期待 -->
        <div class="custom-card disabled-theme" @click="showComingSoon">
          <div class="icon-box">
            <!-- 这里可以放一个默认的等待图标或之前的SVG -->
            <svg width="80" height="100" viewBox="0 0 150 200">
              <rect x="35" y="60" width="80" height="120" rx="10" fill="none" stroke="#ccc" stroke-width="2" stroke-dasharray="5"/>
              <circle cx="75" cy="35" r="15" fill="none" stroke="#ccc" stroke-width="2"/>
            </svg>
          </div>
          <div class="text-info">
            <span class="en-tag">More Coming</span>
            <h3 class="card-title">敬请期待</h3>
          </div>
        </div>
      </div>

      <!-- 底部流程图 -->
      <div class="workflow-section">
        <h2 class="workflow-title">制版工艺流程</h2>
        <div class="workflow-steps">
          <div class="step">
            <div class="step-num">01</div>
            <div class="step-text">测量人体数据</div>
          </div>
          <div class="arrow">➔</div>
          <div class="step">
            <div class="step-num">02</div>
            <div class="step-text">输入关键尺寸</div>
          </div>
          <div class="arrow">➔</div>
          <div class="step">
            <div class="step-num">03</div>
            <div class="step-text">实时计算裁片</div>
          </div>
          <div class="arrow">➔</div>
          <div class="step">
            <div class="step-num">04</div>
            <div class="step-text">导出PDF图纸</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "WomenswearMain",
  methods: {
    showComingSoon() {
      this.$message.info('更多高级裙装版型正在开发中...');
    }
  }
};
</script>

<style scoped lang="scss">
.womenswear-main {
  min-height: 100vh;
  background: linear-gradient(135deg, #fffaf0 0%, #fff5f5 100%);
  padding: 40px 20px;
  font-family: "PingFang SC", "Microsoft YaHei", sans-serif;

  .header-section {
    text-align: center;
    margin-bottom: 60px;
    .title {
      font-size: 36px;
      color: #2c3e50;
      letter-spacing: 2px;
      margin-bottom: 8px;
    }
    .subtitle {
      color: #95a5a6;
      font-size: 14px;
      text-transform: uppercase;
      letter-spacing: 4px;
    }
  }

  .card-container {
    display: flex;
    justify-content: center;
    align-items: center;
    gap: 30px;
    margin-bottom: 80px;
    flex-wrap: nowrap; 

    .custom-card {
      width: 280px;
      height: 380px;
      background: rgba(255, 255, 255, 0.85);
      backdrop-filter: blur(10px);
      border-radius: 24px;
      border: 1px solid rgba(255, 255, 255, 0.8);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      transition: all 0.4s cubic-bezier(0.165, 0.84, 0.44, 1);
      box-shadow: 0 10px 25px rgba(0,0,0,0.05);

      &:hover {
        transform: translateY(-12px);
        box-shadow: 0 20px 40px rgba(0,0,0,0.1);
      }

      .icon-box {
        margin-bottom: 25px;
        transition: transform 0.4s;
        height: 160px;
        display: flex;
        align-items: center;
        justify-content: center;
        
        .card-icon {
          max-width: 150px;
          max-height: 150px;
          object-fit: contain;
        }
      }

      .text-info {
        text-align: center;
        .en-tag {
          font-size: 12px;
          color: #bdc3c7;
          text-transform: uppercase;
          letter-spacing: 1px;
        }
        .card-title {
          font-size: 20px;
          color: #34495e;
          margin-top: 5px;
          font-weight: 600;
        }
      }

      &.pink-theme:hover {
        border-color: #ffb6c1;
        background: #fffafa;
        .card-title { color: #ff69b4; }
      }

      &.orange-theme:hover {
        border-color: #ffd700;
        background: #fffef0;
        .card-title { color: #ff8c00; }
      }

      &.disabled-theme {
        opacity: 0.6;
        cursor: not-allowed;
        background: rgba(245, 245, 245, 0.5);
      }
    }
  }

  .workflow-section {
    max-width: 900px;
    margin: 0 auto;
    background: rgba(255, 255, 255, 0.6);
    border-radius: 30px;
    padding: 35px;
    border: 1px dashed #e4e7ed;
    box-shadow: inset 0 2px 10px rgba(0,0,0,0.02);

    .workflow-title {
      text-align: center;
      font-size: 18px;
      color: #909399;
      margin-bottom: 35px;
      font-weight: normal;
    }

    .workflow-steps {
      display: flex;
      justify-content: space-around;
      align-items: center;

      .step {
        text-align: center;
        flex: 1;
        .step-num {
          font-size: 28px;
          font-weight: bold;
          color: #f2f2f2;
          margin-bottom: 5px;
          font-family: Georgia, serif;
          text-shadow: 1px 1px 0 #dcdfe6;
        }
        .step-text {
          font-size: 15px;
          color: #5e6d82;
        }
      }

      .arrow {
        color: #dcdfe6;
        font-size: 20px;
        padding: 0 10px;
      }
    }
  }
}

@media (max-width: 1000px) {
  .card-container {
    flex-wrap: wrap;
    gap: 20px;
  }
}
</style>