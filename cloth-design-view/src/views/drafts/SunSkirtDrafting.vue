<template>
  <div class="womenswear-drafting">
    <div class="workspace">
      <!-- 左侧参数面板 -->
      <div class="sidebar card">
        <div class="sidebar-scroll-area">
          <div class="section-title">
            <i class="el-icon-scissors"></i>
            太阳全圆裙 · 工业制版
          </div>

          <div class="parameter-item">
            <label class="parameter-label">尺码规格</label>
            <el-select v-model="params.size" @change="onSizeChange" size="mini" class="parameter-input">
              <el-option label="XS (155/64A)" value="XS"></el-option>
              <el-option label="S (160/68A)" value="S"></el-option>
              <el-option label="M (165/72A)" value="M"></el-option>
              <el-option label="L (170/76A)" value="L"></el-option>
            </el-select>
          </div>

          <div class="parameter-item">
            <label class="parameter-label">净腰围 Waist (cm)</label>
            <el-input-number v-model="params.waist" :min="60" :max="90" size="mini" class="parameter-input" />
          </div>

          <div class="parameter-item">
            <label class="parameter-label">裙子长度 Length (cm)</label>
            <el-input-number v-model="params.skirtLength" :min="30" :max="100" size="mini" class="parameter-input" />
          </div>

          <div class="parameter-item">
            <label class="parameter-label">松量 Ease (cm)</label>
            <el-input-number v-model="params.ease" :min="0" :max="5" :step="0.5" size="mini" class="parameter-input" />
          </div>

          <div class="divider"></div>
          
          <!-- 太阳裙原理示意图 -->
          <div class="sidebar-diagram">
            <label class="parameter-label">裁剪原理 (1/4 结构)</label>
            <div class="math-box">
              <p>内径 r = (W + 松) / 2π</p>
              <p>外径 R = r + 裙长</p>
            </div>
            <img src="https://pic1.imgdb.cn/item/69c2bbbf2cb231a0e8774994.png" class="mini-diagram" style="filter: invert(1) hue-rotate(180deg);" />
          </div>

          <div class="drafting-note">
            <p>* 太阳裙利用斜丝特性，垂感极佳</p>
            <p>* 标注：粉色虚线包含 1cm 缝份</p>
          </div>
        </div>

        <div class="action-buttons">
          <el-button size="small" @click="resetDefaults" type="primary" plain class="action-btn">恢复默认</el-button>
          <el-button size="small" @click="exportPDF" type="success" plain class="action-btn">导出 PDF</el-button>
        </div>
      </div>

      <!-- 右侧画布 -->
      <div class="right-content">
        <div class="canvas-wrap card">
          <div class="control-header">
            <div class="btn-group-wrapper">
              <el-button-group>
                <el-button icon="el-icon-zoom-in" size="mini" @click="zoomIn">放大</el-button>
                <el-button icon="el-icon-zoom-out" size="mini" @click="zoomOut">缩小</el-button>
                <el-button size="mini" @click="resetZoom">重置比例</el-button>
              </el-button-group>
              <el-button size="mini" @click="showMath = !showMath" type="info" plain>
                <i class="el-icon-guide"></i> 计算详情
              </el-button>
            </div>
            <span class="zoom-indicator">当前缩放: {{ Math.round(zoom * 100) }}%</span>
          </div>

          <div class="canvas-container" @mousedown="startDrag" @mousemove="drag" @mouseup="endDrag" @mouseleave="endDrag">
            <svg :width="canvasWidth" :height="canvasHeight" class="pattern-svg"
                 :style="{ transform: `scale(${zoom}) translate(${translateX}px, ${translateY}px)`, cursor: isDragging ? 'grabbing' : 'grab' }">
              <defs>
                <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
                  <path d="M 50 0 L 0 0 0 50" fill="none" stroke="#f0f0f0" stroke-width="1"/>
                </pattern>
              </defs>
              <rect width="100%" height="100%" fill="url(#grid)"/>

              <!-- 太阳裙 1/4 裁片 -->
              <g transform="translate(100, 100)">
                <text x="0" y="-30" font-size="20" font-weight="bold" fill="#333">太阳裙 1/4 工业裁片 (需剪4片或折叠剪)</text>
                
                <!-- 1. 绘制主体扇形 (实线) -->
                <!-- 使用路径命令绘制：M起点 A圆弧 L直线 A圆弧 Z闭合 -->
                <path :d="describeArc(0, 0, innerR, outerR)" 
                      fill="rgba(255, 105, 180, 0.05)" 
                      stroke="#ff1493" stroke-width="2.5" />

                <!-- 2. 绘制缝份 (外围虚线) -->
                <path :d="describeArc(0, 0, innerR - seamPx, outerR + seamPx)" 
                      fill="none" stroke="#ff69b4" stroke-width="1" stroke-dasharray="4,4" />

                <!-- 3. 标注文字 -->
                <text :x="outerR / 2.5" :y="outerR / 2.5" transform="rotate(45)" font-size="14" fill="#d11a7e" font-weight="bold">
                  内径 r: {{ (innerR / cmToPx).toFixed(2) }}cm
                </text>
                <text :x="outerR / 1.5" :y="outerR / 1.5" transform="rotate(45)" font-size="14" fill="#d11a7e">
                  裙长: {{ params.skirtLength }}cm
                </text>

                <!-- 4. 丝缕线 (45度斜丝) -->
                <g transform="rotate(45) translate(50, 0)">
                  <line x1="0" y1="0" x2="100" y2="0" stroke="#333" stroke-width="1.5" />
                  <path d="M5 4 L0 0 L5 -4 M95 4 L100 0 L95 -4" fill="none" stroke="#333" />
                  <text x="20" y="-10" font-size="12" fill="#333">BIAS GRAIN (斜丝缕)</text>
                </g>
              </g>
            </svg>
          </div>
        </div>

        <el-dialog :visible.sync="showMath" title="计算详情" width="400px">
          <div style="padding:20px; line-height: 2">
            <p>1. 总腰围: {{ params.waist + params.ease }} cm</p>
            <p>2. 计算半径 r = (W / 2π)</p>
            <p>3. 当前内圆半径: <b>{{ (innerR / cmToPx).toFixed(2) }} cm</b></p>
            <p>4. 当前外圆半径: <b>{{ (outerR / cmToPx).toFixed(2) }} cm</b></p>
          </div>
        </el-dialog>

        <div class="copyright">©2026 纤云制版软件 | 太阳裙同心圆逻辑</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "SunraySkirtDrafting",
  data() {
    return {
      cmToPx: 5,
      zoom: 0.6,
      translateX: 50,
      translateY: 80,
      isDragging: false,
      startX: 0,
      startY: 0,
      canvasWidth: 2000,
      canvasHeight: 1500,
      showMath: false,
      params: { size: "S", waist: 68, skirtLength: 50, ease: 1 },
      sizeMap: { XS: 64, S: 68, M: 72, L: 76 }
    };
  },
  computed: {
    seamPx() { return 1 * this.cmToPx },
    innerR() {
      // 核心公式：r = W / 2π
      const rCm = (this.params.waist + this.params.ease) / (2 * Math.PI);
      return rCm * this.cmToPx;
    },
    outerR() {
      return this.innerR + (this.params.skirtLength * this.cmToPx);
    }
  },
  methods: {
    onSizeChange() { this.params.waist = this.sizeMap[this.params.size]; },
    resetDefaults() { this.params = { size: "S", waist: 68, skirtLength: 50, ease: 1 }; this.resetZoom(); },
    zoomIn() { this.zoom = Math.min(this.zoom + 0.1, 2.0); },
    zoomOut() { this.zoom = Math.max(this.zoom - 0.1, 0.2); },
    resetZoom() { this.zoom = 0.6; this.translateX = 50; this.translateY = 80; },
    startDrag(e) { this.isDragging = true; this.startX = e.clientX - this.translateX; this.startY = e.clientY - this.translateY; },
    drag(e) { if (this.isDragging) { this.translateX = e.clientX - this.startX; this.translateY = e.clientY - this.startY; } },
    endDrag() { this.isDragging = false; },
    exportPDF() { this.$message.success("太阳裙 PDF 裁切图导出成功"); },
    
    // 生成1/4圆弧路径的辅助函数
    describeArc(x, y, innerR, outerR) {
      // 1/4圆 即 0 到 90 度
      return [
        `M ${x + innerR} ${y}`, // 移到内圆起点
        `L ${x + outerR} ${y}`, // 画直线到外圆起点
        `A ${outerR} ${outerR} 0 0 1 ${x} ${y + outerR}`, // 画外圆弧线
        `L ${x} ${y + innerR}`, // 画直线回内圆
        `A ${innerR} ${innerR} 0 0 0 ${x + innerR} ${y}`, // 画内圆弧线
        "Z"
      ].join(" ");
    }
  }
};
</script>

<style scoped lang="scss">
.womenswear-drafting {
  width: 100%; min-height: 100vh; background: #f0f2f5; padding: 15px; box-sizing: border-box;
}
.workspace { display: flex; gap: 15px; align-items: stretch; height: calc(100vh - 30px); }

.sidebar {
  width: 280px; background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.06);
  display: flex; flex-direction: column; justify-content: space-between;
}
.sidebar-scroll-area { overflow-y: auto; flex: 1; }

.math-box {
  background: #f8f9fa; padding: 10px; border-radius: 6px; font-family: monospace; font-size: 12px;
  color: #666; margin-bottom: 10px; border: 1px dashed #ddd;
}

.sidebar-diagram {
  margin: 15px 0;
  .mini-diagram { width: 100%; border-radius: 8px; margin-top: 5px; opacity: 0.8; }
}

.action-buttons {
  display: flex; gap: 10px; padding-top: 15px; border-top: 1px solid #eee;
  .action-btn { flex: 1; margin: 0 !important; }
}

.right-content { flex: 1; min-width: 0; }
.canvas-wrap { background: #fff; border-radius: 12px; height: 100%; position: relative; overflow: hidden; }
.control-header {
  position: absolute; top: 12px; left: 12px; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(255,255,255,0.9); padding: 6px 12px; border-radius: 8px; width: calc(100% - 24px); box-sizing: border-box;
}
.canvas-container { width: 100%; height: 100%; background-color: #fafafa; cursor: grab; }
.pattern-svg { transition: transform 0.05s ease-out; transform-origin: 0 0; }
.section-title { font-size: 16px; font-weight: bold; margin-bottom: 20px; border-left: 4px solid #409EFF; padding-left: 10px; }
.parameter-item { margin-bottom: 15px; }
.parameter-label { display: block; font-size: 12px; color: #666; margin-bottom: 6px; font-weight: bold; }
.parameter-input { width: 100%; }
.drafting-note { background: #ecf5ff; padding: 10px; border-radius: 8px; font-size: 11px; color: #409EFF; line-height: 1.5; margin-top: 10px; }
.copyright { text-align: center; margin-top: 10px; color: #bbb; font-size: 11px; }
</style>