<template>
  <div class="login-container" @mousemove="handleMouseMove">
    <!-- 1. 全屏背景图：浅黄色调处理 -->
    <div class="fashion-bg-wrapper">
      <img src="https://pic1.imgdb.cn/item/69c347ce2cb231a0e879475c.png" class="bg-img">
      <div class="bg-light-overlay"></div>
    </div>

    <!-- 2. 动态毛线团光标 -->
    <div class="yarn-cursor" :style="{ left: mouseX + 'px', top: mouseY + 'px' }">
      <div class="yarn-ball">🧶</div>
      <svg class="thread-tail">
        <path :d="threadPath" stroke="#e2b86e" stroke-width="1.5" fill="none" opacity="0.6" stroke-dasharray="4,3"/>
      </svg>
    </div>

    <div class="login-main">
      <!-- 左侧：时尚视觉区 -->
      <div class="brand-section">
        <div class="bubble-container">
          <!-- 泡泡组 -->
          <div class="bubble main-bubble">
            <img src="https://pic1.imgdb.cn/item/69c2c5062cb231a0e8776ad8.png" alt="Craft">
          </div>
          <div class="bubble sub-bubble-1">
            <img src="https://pic1.imgdb.cn/item/69c2c5892cb231a0e8776afa.png" alt="Show">
          </div>
          <div class="bubble sub-bubble-2">
            <img src="https://pic1.imgdb.cn/item/69c2c52c2cb231a0e8776ae3.png" alt="Detail">
          </div>
          <div class="bubble sub-bubble-3">
            <img src="https://pic1.imgdb.cn/item/69c3475c2cb231a0e87946c7.png" alt="Editorial">
          </div>
          
          <!-- 旋转文字环 -->
          <svg class="text-ring" viewBox="0 0 100 100">
            <path id="ringPath" d="M 50, 50 m -40, 0 a 40,40 0 1,1 80,0 a 40,40 0 1,1 -80,0" fill="none"/>
            <text font-size="5" fill="#000" font-weight="bold">
              <textPath xlink:href="#ringPath">INTELLIGENT FASHION • CLOUD WEAVE SYSTEM • </textPath>
            </text>
          </svg>
        </div>

        <div class="brand-content">
          <h1 class="slogan-text">让创意丝滑落地<br/>赋能每一寸织物</h1>
          <div class="process-line">
            <span>灵感生成</span> <i class="el-icon-right"></i>
            <span>精准制版</span> <i class="el-icon-right"></i>
            <span>虚拟试衣</span>
          </div>
        </div>
      </div>

      <!-- 右侧：登录操作区 -->
      <div class="login-section">
        <div class="login-panel">
          <!-- 移除Logo，保留品牌标题居中对齐 -->
          <div class="logo-wrapper">
            <span class="brand-name">纤云速织</span>
          </div>
          
          <h2 class="login-title">LOGIN CENTER</h2>
          
          <div class="form-container">
            <div class="v-input-group">
              <label>ACCOUNT</label>
              <div class="input-inner">
                <i class="el-icon-user"></i>
                <input v-model="act" placeholder="输入账号" />
              </div>
            </div>
            
            <div class="v-input-group">
              <label>PASSWORD</label>
              <div class="input-inner">
                <i class="el-icon-lock"></i>
                <input v-model="pwd" type="password" placeholder="输入密码" @keyup.enter="login" />
              </div>
            </div>
            
            <div class="btn-area">
              <div class="login-btn-v2" @click="login">
                <span>进入系统 / SIGN IN</span>
              </div>
            </div>
          </div>
          
          <div class="footer-tip">
            <p>没有账号？ <span class="no-act" @click="toDoRegister">建立新档案</span></p>
            <div class="divider"></div>
            <div class="identity-links">设计师 / 企业 / 师生入口</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
const ADMIN_ROLE = 1;
const USER_ROLE = 2;
const DELAY_TIME = 1300;
import request from "@/utils/request.js";
import { setToken } from "@/utils/storage.js";
import md5 from 'js-md5';
import { mapActions } from 'vuex';

export default {
  name: "Login",
  data() {
    return {
      act: '',
      pwd: '',
      mouseX: 0,
      mouseY: 0,
      history: []
    }
  },
  computed: {
    threadPath() {
      if (this.history.length < 2) return '';
      return `M ${this.history.map(p => `${p.x - this.mouseX},${p.y - this.mouseY}`).join(' L ')}`;
    }
  },
  methods: {
    handleMouseMove(e) {
      this.mouseX = e.clientX;
      this.mouseY = e.clientY;
      this.history.push({ x: e.clientX, y: e.clientY });
      if (this.history.length > 12) this.history.shift();
    },
    toDoRegister(){ this.$router.push('/register'); },
    ...mapActions('user', { loginToStore: 'login' }),
    async login() {
      if (!this.act || !this.pwd) {
        this.$swal.fire({ title: '校验', text: '请输入账号密码', icon: 'warning', showConfirmButton: false, timer: DELAY_TIME });
        return;
      }
      const hashedPwd = md5(md5(this.pwd));
      const paramDTO = { userAccount: this.act, userPwd: hashedPwd };
      try {
        const { data } = await request.post(`user/login`, paramDTO);
        if (data.code !== 200) {
          this.$swal.fire({ title: '失败', text: data.msg, icon: 'error', showConfirmButton: false, timer: DELAY_TIME });
          return;
        }
        this.loginToStore({ id: data.data.id, role: data.data.role, token: data.data.token });
        setToken(data.data.token);
        this.$swal.fire({
          title: '登录成功',
          text: '正在跳转...',
          icon: false,
          showConfirmButton: false,
          timer: DELAY_TIME,
          customClass: {
            popup: 'login-success-popup',
            title: 'login-success-title',
            content: 'login-success-content'
          },
          background: 'rgba(255, 255, 255, 0.9)',
          backdrop: 'rgba(61, 47, 36, 0.3)'
        });
        setTimeout(() => { this.navigateToRole(data.data.role); }, DELAY_TIME);
      } catch (error) {
        this.$message.error('请求出错');
      }
    },
    navigateToRole(role) {
      if (role === ADMIN_ROLE) this.$router.push('/admin');
      else if (role === USER_ROLE) this.$router.push('/user');
    }
  }
};
</script>

<style lang="scss" scoped>
.login-container {
  width: 100%; min-height: 100vh;
  background: #fdfaf5; display: flex; justify-content: center; align-items: center;
  overflow: hidden; position: relative; cursor: none;
  font-family: "PingFang SC", "Optima", sans-serif;
}

.fashion-bg-wrapper {
  position: absolute; inset: 0; z-index: 0;
  .bg-img { width: 100%; height: 100%; object-fit: cover; filter: sepia(0.2) brightness(1.1) contrast(0.9); }
  .bg-light-overlay {
    position: absolute; inset: 0;
    background: linear-gradient(135deg, rgba(253, 250, 245, 0.9) 0%, rgba(253, 250, 245, 0.6) 50%, rgba(253, 250, 245, 0.8) 100%);
  }
}

.yarn-cursor {
  position: fixed; pointer-events: none; z-index: 9999; transform: translate(-50%, -50%);
  .yarn-ball { font-size: 32px; filter: drop-shadow(0 5px 10px rgba(0,0,0,0.1)); }
  .thread-tail { position: absolute; top: 0; left: 0; overflow: visible; width: 1px; height: 1px; }
}

.bubble-container {
  position: relative; width: 600px; height: 500px; margin-bottom: 30px;
  .bubble { position: absolute; overflow: hidden; box-shadow: 0 15px 35px rgba(0,0,0,0.1); background: #fff; img { width: 100%; height: 100%; object-fit: cover; } }
  .main-bubble { width: 336px; height: 336px; top: 10px; left: 60px; z-index: 2; border-radius: 50%; animation: simple-float 8s infinite ease-in-out; }
  .sub-bubble-1 { width: 156px; height: 156px; top: 0px; right: 60px; z-index: 1; border-radius: 50%; animation: float 6s infinite ease-in-out; }
  .sub-bubble-2 { width: 132px; height: 132px; bottom: 20px; right: 100px; z-index: 3; border-radius: 50%; animation: float 8s infinite ease-in-out reverse; }
  .sub-bubble-3 { width: 168px; height: 168px; bottom: 80px; left: -30px; z-index: 4; border-radius: 50%; animation: float 7s infinite ease-in-out 1s; }
  .text-ring { position: absolute; width: 520px; height: 520px; top: -50px; left: -30px; animation: rotate 30s linear infinite; opacity: 0.9; }
}

.login-main {
  width: 90%; max-width: 1150px; display: flex; z-index: 10;
  .brand-section {
    flex: 1.2; padding-right: 50px; display: flex; flex-direction: column; justify-content: center;
    .slogan-text { font-size: 38px; color: #3d2f24; font-weight: 600; line-height: 1.3; margin-bottom: 20px; }
    .process-line { color: #e2b86e; letter-spacing: 2px; font-weight: 500; font-size: 14px; }
  }

  .login-section {
    flex: 0.8;
    .login-panel {
      background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(15px);
      padding: 60px 50px; border-radius: 40px; border: 1px solid rgba(255,255,255,0.6);
      box-shadow: 0 30px 60px rgba(74, 58, 42, 0.1);
      .logo-wrapper { 
        text-align: center; margin-bottom: 20px;
        .brand-name { font-size: 28px; font-weight: 600; color: #3d2f24; letter-spacing: 4px; }
      }
      .login-title { text-align: center; font-size: 12px; letter-spacing: 6px; color: #aaa; margin-bottom: 40px; }
      .v-input-group {
        margin-bottom: 25px;
        label { font-size: 10px; color: #bca080; letter-spacing: 2px; margin-bottom: 8px; display: block; }
        .input-inner {
          position: relative; border-bottom: 1px solid #e2b86e;
          i { position: absolute; left: 0; top: 12px; color: #e2b86e; }
          input { width: 100%; border: none; background: transparent; padding: 12px 0 12px 25px; font-size: 15px; color: #4a3a2a; outline: none; }
        }
      }
      .login-btn-v2 { width: 100%; padding: 18px; text-align: center; background: #3d2f24; color: #fff; border-radius: 12px; font-size: 14px; font-weight: 600; cursor: pointer; transition: 0.3s; &:hover { transform: translateY(-3px); background: #1a1510; } }
      .footer-tip { margin-top: 30px; text-align: center; font-size: 13px; color: #8c7662; .no-act { color: #e2b86e; font-weight: bold; cursor: pointer; } .divider { height: 1px; background: rgba(0,0,0,0.05); margin: 20px auto; width: 60%; } .identity-links { font-size: 11px; opacity: 0.5; } }
    }
  }
}

@keyframes simple-float { 0%, 100% { transform: translateY(0) scale(1); } 50% { transform: translateY(-10px) scale(1.02); } }
@keyframes float { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-15px); } }
@keyframes rotate { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
@media (max-width: 950px) { .brand-section { display: none; } .login-main { justify-content: center; } }

/* 登录成功弹窗样式 */
.login-success-popup {
  border-radius: 20px !important;
  border: 1px solid rgba(226, 184, 110, 0.3) !important;
  box-shadow: 0 20px 40px rgba(74, 58, 42, 0.15) !important;
  backdrop-filter: blur(10px) !important;
  padding: 30px !important;
  animation: popup-slide-in 0.5s ease-out forwards !important;
  position: relative !important;
  overflow: hidden !important;
}

.login-success-popup::before {
  content: '' !important;
  position: absolute !important;
  top: -50% !important;
  left: -50% !important;
  width: 200% !important;
  height: 200% !important;
  background: linear-gradient(
    45deg,
    rgba(226, 184, 110, 0.1) 0%,
    rgba(226, 184, 110, 0) 25%,
    rgba(226, 184, 110, 0) 50%,
    rgba(226, 184, 110, 0) 75%,
    rgba(226, 184, 110, 0.1) 100%
  ) !important;
  animation: shimmer 3s linear infinite !important;
  z-index: 0 !important;
}

.login-success-title {
  color: #3d2f24 !important;
  font-size: 20px !important;
  font-weight: 600 !important;
  margin-bottom: 15px !important;
  font-family: "PingFang SC", "Optima", sans-serif !important;
  animation: title-fade-in 0.8s ease-out forwards !important;
  position: relative !important;
  z-index: 1 !important;
}

.login-success-content {
  color: #8c7662 !important;
  font-size: 14px !important;
  font-family: "PingFang SC", "Optima", sans-serif !important;
  animation: content-fade-in 1s ease-out forwards !important;
  position: relative !important;
  z-index: 1 !important;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.login-success-content::after {
  content: '' !important;
  display: inline-block !important;
  width: 20px !important;
  height: 20px !important;
  border: 2px solid #e2b86e !important;
  border-top: 2px solid transparent !important;
  border-radius: 50% !important;
  margin-left: 10px !important;
  animation: spin 1s linear infinite !important;
}

@keyframes popup-slide-in {
  0% {
    transform: scale(0.8) translateY(-20px) !important;
    opacity: 0 !important;
  }
  100% {
    transform: scale(1) translateY(0) !important;
    opacity: 1 !important;
  }
}

@keyframes title-fade-in {
  0% {
    opacity: 0 !important;
    transform: translateY(-10px) !important;
  }
  100% {
    opacity: 1 !important;
    transform: translateY(0) !important;
  }
}

@keyframes content-fade-in {
  0% {
    opacity: 0 !important;
    transform: translateY(10px) !important;
  }
  100% {
    opacity: 1 !important;
    transform: translateY(0) !important;
  }
}

@keyframes shimmer {
  0% {
    transform: rotate(0deg) !important;
  }
  100% {
    transform: rotate(360deg) !important;
  }
}

@keyframes spin {
  0% {
    transform: rotate(0deg) !important;
  }
  100% {
    transform: rotate(360deg) !important;
  }
}
</style>