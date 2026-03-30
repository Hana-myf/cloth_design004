<template>
  <div class="womenswear-drafting">
    <div class="workspace">
      <!-- 左侧参数面板 -->
      <div class="sidebar card">
        <div class="sidebar-scroll-area">
          <div class="section-title">
            <i class="el-icon-scissors"></i>
            多层蛋糕裙 · 工业制版
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
            <label class="parameter-label">腰围 Waist (cm)</label>
            <el-input-number v-model="params.waist" :min="60" :max="90" size="mini" class="parameter-input" />
          </div>

          <div class="parameter-item">
            <label class="parameter-label">裙长总长 Length (cm)</label>
            <el-input-number v-model="params.skirtLength" :min="40" :max="100" size="mini" class="parameter-input" />
          </div>

          <div class="parameter-item">
            <label class="parameter-label">层数选择 Layer</label>
            <el-radio-group v-model="params.layer" size="mini">
              <el-radio-button :label="2">2层</el-radio-button>
              <el-radio-button :label="3">3层</el-radio-button>
            </el-radio-group>
          </div>

          <div class="divider"></div>
          
          <!-- 蛋糕裙原理示意图 -->
          <div class="sidebar-diagram">
            <label class="parameter-label">裁剪原理</label>
            <div class="math-box">
              <p>每层高度 = 总裙长 / 层数</p>
              <p>第一层宽度 = 腰围 × 1.5</p>
              <p>第n层宽度 = 第n-1层宽度 × 1.6</p>
            </div>
            <img src="https://pic1.imgdb.cn/item/69c2a1642cb231a0e876d987.png" class="mini-diagram" />
          </div>

          <div class="drafting-note">
            <p>* 比例：层间宽度递增约 1.6 倍</p>
            <p>* 标注：粉色虚线为 1cm 缝份</p>
          </div>
        </div>

        <!-- 底部操作按钮：左右对齐平分 -->
        <div class="action-buttons">
          <el-button size="small" @click="resetDefaults" type="primary" plain class="action-btn">
            <i class="el-icon-refresh"></i> 恢复默认
          </el-button>
          <el-button size="small" @click="exportPDF" type="success" plain class="action-btn">
            <i class="el-icon-download"></i> 导出 PDF
          </el-button>
        </div>
      </div>

      <!-- 右侧画布 -->
      <div class="right-content">
        <div class="canvas-wrap card">
          <!-- 缩放与测量控制栏 -->
          <div class="control-header">
            <div class="btn-group-wrapper">
              <el-button-group>
                <el-button icon="el-icon-zoom-in" size="mini" @click="zoomIn">放大</el-button>
                <el-button icon="el-icon-zoom-out" size="mini" @click="zoomOut">缩小</el-button>
                <el-button size="mini" @click="resetZoom">重置比例</el-button>
              </el-button-group>
              
              <el-button size="mini" @click="showMeasurementGuide = !showMeasurementGuide" type="info" plain>
                <i class="el-icon-info"></i> 测量指南
              </el-button>
              <el-button size="mini" @click="showFlowChart = !showFlowChart" type="success" plain>
                <i class="el-icon-s-order"></i> 流程图
              </el-button>
              <el-button size="mini" @click="showMath = !showMath" type="primary" plain>
                <i class="el-icon-guide"></i> 计算详情
              </el-button>
            </div>
            <span class="zoom-indicator">当前缩放: {{ Math.round(zoom * 100) }}%</span>
          </div>

          <div class="canvas-container" 
               @mousedown="startDrag" @mousemove="drag" @mouseup="endDrag" @mouseleave="endDrag">
            <!-- translateY 设为 80 以确保第一层不被控制栏遮挡 -->
            <svg :width="canvasWidth" :height="canvasHeight" class="pattern-svg"
                 :style="{ transform: `scale(${zoom}) translate(${translateX}px, ${translateY}px)`, cursor: isDragging ? 'grabbing' : 'grab' }">
              <defs>
                <pattern id="grid" width="50" height="50" patternUnits="userSpaceOnUse">
                  <path d="M 50 0 L 0 0 0 50" fill="none" stroke="#f0f0f0" stroke-width="1"/>
                </pattern>
              </defs>
              <rect width="100%" height="100%" fill="url(#grid)"/>

              <!-- 垂直裁片展示 -->
              <g v-for="(tier, index) in tiersData" :key="index" :transform="`translate(60, ${100 + index * 240})`">
                <text x="0" y="-30" font-size="18" font-weight="bold" fill="#333">
                  Tier {{index + 1}} - 蛋糕裙裁片 (第 {{index + 1}} 层)
                </text>

                <rect :width="tier.wPx" :height="tier.hPx" fill="rgba(255, 105, 180, 0.05)" stroke="#ff1493" stroke-width="2.5" />
                <rect :x="-seamPx" :y="-seamPx" :width="tier.wPx + 2*seamPx" :height="tier.hPx + 2*seamPx" 
                      fill="none" stroke="#ff69b4" stroke-width="1" stroke-dasharray="4,4" />

                <g opacity="0.4">
                   <line v-for="n in 30" :key="n" :x1="(tier.wPx/30)*n" y1="0" :x2="(tier.wPx/30)*n" y2="10" stroke="#ff1493" stroke-width="1.2" />
                </g>

                <text :x="tier.wPx / 2" :y="tier.hPx / 2 + 6" text-anchor="middle" font-size="16" fill="#d11a7e" font-weight="bold">
                   {{tier.wCm}}cm × {{tier.hCm}}cm
                </text>
              </g>
            </svg>
          </div>
        </div>

        <el-dialog :visible.sync="showMeasurementGuide" title="测量指南" width="500px"><div style="padding:20px">根据腰围计算各层宽度...</div></el-dialog>
        <el-dialog :visible.sync="showFlowChart" title="流程图" width="700px" center>
            <img src="https://pic1.imgdb.cn/item/69ca89fa628aa9d58f8658dd.png" style="width:100%" />
        </el-dialog>

        <el-dialog :visible.sync="showMath" title="计算详情" width="400px">
          <div style="padding:20px; line-height: 2">
            <p>1. 总裙长: {{ params.skirtLength }} cm</p>
            <p>2. 层数: {{ params.layer }}</p>
            <p>3. 每层高度: {{ (params.skirtLength / params.layer).toFixed(2) }} cm</p>
            <p>4. 第一层宽度: {{ Math.round(params.waist * 1.5) }} cm</p>
            <p v-if="params.layer >= 2">5. 第二层宽度: {{ Math.round(params.waist * 1.5 * 1.6) }} cm</p>
            <p v-if="params.layer === 3">6. 第三层宽度: {{ Math.round(params.waist * 1.5 * 1.6 * 1.6) }} cm</p>
          </div>
        </el-dialog>

        <div class="copyright">©2026 纤云制版软件 | 工业标准渲染</div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "CakeSkirtDrafting",
  data() {
    return {
      cmToPx: 4,
      zoom: 0.6,
      translateX: 20,
      translateY: 80, // 初始位置向下偏移，避开工具栏
      isDragging: false,
      startX: 0,
      startY: 0,
      canvasWidth: 2000,
      canvasHeight: 1200,
      showMeasurementGuide: false,
      showFlowChart: false,
      showMath: false,
      params: { size: "S", waist: 68, skirtLength: 85, layer: 3 },
      sizeMap: { XS: 64, S: 68, M: 72, L: 76 }
    };
  },
  computed: {
    seamPx() { return 1 * this.cmToPx },
    tiersData() {
      const data = [];
      const hCm = (this.params.skirtLength / this.params.layer).toFixed(1);
      const w1 = Math.round(this.params.waist * 1.5);
      data.push({ wCm: w1, hCm: hCm, wPx: w1 * this.cmToPx, hPx: hCm * this.cmToPx });
      const w2 = Math.round(w1 * 1.6);
      data.push({ wCm: w2, hCm: hCm, wPx: w2 * this.cmToPx, hPx: hCm * this.cmToPx });
      if (this.params.layer === 3) {
        const w3 = Math.round(w2 * 1.6);
        data.push({ wCm: w3, hCm: hCm, wPx: w3 * this.cmToPx, hPx: hCm * this.cmToPx });
      }
      return data;
    }
  },
  methods: {
    onSizeChange() { this.params.waist = this.sizeMap[this.params.size]; },
    resetDefaults() { this.params = { size: "S", waist: 68, skirtLength: 85, layer: 3 }; this.resetZoom(); },
    zoomIn() { this.zoom = Math.min(this.zoom + 0.1, 2.0); },
    zoomOut() { this.zoom = Math.max(this.zoom - 0.1, 0.2); },
    resetZoom() { this.zoom = 0.6; this.translateX = 20; this.translateY = 80; },
    startDrag(e) { this.isDragging = true; this.startX = e.clientX - this.translateX; this.startY = e.clientY - this.translateY; },
    drag(e) { if (this.isDragging) { this.translateX = e.clientX - this.startX; this.translateY = e.clientY - this.startY; } },
    endDrag() { this.isDragging = false; },
    exportPDF() { this.$message.success("PDF 导出成功"); }
  }
};
</script>

<style scoped lang="scss">
.womenswear-drafting {
  width: 100%; min-height: 100vh; background: #fdf6ec; padding: 15px; box-sizing: border-box;
}
.workspace { display: flex; gap: 15px; align-items: stretch; height: calc(100vh - 30px); }

.sidebar {
  width: 260px;
  background: #fff; border-radius: 12px; padding: 20px; box-shadow: 0 4px 15px rgba(0,0,0,0.06); flex-shrink: 0;
  display: flex; flex-direction: column; justify-content: space-between;
}
.sidebar-scroll-area { overflow-y: auto; flex: 1; padding-right: 5px; }

/* 侧边栏示意图样式 */
.sidebar-diagram {
  margin: 15px 0;
  .mini-diagram {
    width: 100%;
    border-radius: 8px;
    border: 1px solid #f0f0f0;
    margin-top: 5px;
  }
}

/* 数学公式盒子样式 */
.math-box {
  background: #f8f9fa;
  padding: 10px;
  border-radius: 6px;
  font-family: monospace;
  font-size: 12px;
  color: #666;
  margin-bottom: 10px;
  border: 1px dashed #ddd;
}

/* 按钮对齐样式 */
.action-buttons {
  display: flex;
  gap: 10px;
  padding-top: 15px;
  border-top: 1px solid #eee;
  .action-btn {
    flex: 1; /* 撑满并平分空间 */
    margin: 0 !important;
    padding: 10px 0;
  }
}

.right-content { flex: 1; min-width: 0; height: 100%; }
.canvas-wrap {
  background: #fff; border-radius: 12px; height: 100%; position: relative; overflow: hidden; border: 1px solid #eee;
}
.control-header {
  position: absolute; top: 12px; left: 12px; z-index: 10;
  display: flex; align-items: center; justify-content: space-between;
  background: rgba(255,255,255,0.96); padding: 6px 12px; border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.08); width: calc(100% - 24px); box-sizing: border-box;
}
.btn-group-wrapper { display: flex; gap: 8px; align-items: center; }
.zoom-indicator { font-size: 12px; color: #888; white-space: nowrap; margin-left: 10px; }
.canvas-container { width: 100%; height: 100%; background-color: #fafafa; cursor: grab; }
.pattern-svg { transition: transform 0.05s ease-out; transform-origin: 0 0; }
.section-title { font-size: 16px; font-weight: bold; margin-bottom: 20px; border-left: 4px solid #ff69b4; padding-left: 10px; }
.parameter-item { margin-bottom: 15px; }
.parameter-label { display: block; font-size: 12px; color: #666; margin-bottom: 6px; font-weight: bold; }
.parameter-input { width: 100%; }
.drafting-note { background: #fff0f6; padding: 10px; border-radius: 8px; font-size: 11px; color: #c41d7f; line-height: 1.5; }
.copyright { text-align: center; margin-top: 10px; color: #bbb; font-size: 11px; }
.divider { height: 1px; background: #eee; margin-bottom: 15px; }
</style>