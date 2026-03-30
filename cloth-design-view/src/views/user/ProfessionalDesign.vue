<template>
    <div class="pro-design">
        <!-- 顶部工具栏 -->
        <div class="toolbar card">
            <div class="left-tools">
                <el-radio-group v-model="activeTool" size="small">
                    <el-radio-button label="pen">画笔</el-radio-button>
                    <el-radio-button label="eraser">橡皮擦</el-radio-button>
                </el-radio-group>
                
                <el-color-picker v-model="strokeColor" show-alpha size="small" class="ml8" @change="updateColorAlpha"></el-color-picker>
                
                <!-- 新增：笔触类型选择 -->
                <el-select v-model="brushType" size="small" class="ml8" style="width: 100px;" placeholder="笔触">
                    <el-option label="硬铅笔" value="pencil" />
                    <el-option label="柔和毛笔" value="brush" />
                    <el-option label="马克笔" value="marker" />
                    <el-option label="水彩叠色" value="watercolor" />
                </el-select>

                <el-select v-model="lineWidth" size="small" class="ml8" style="width: 80px;">
                    <el-option v-for="w in [1,2,3,5,8,12,20]" :key="w" :label="w + ' px'" :value="w" />
                </el-select>

                <!-- 新增：透明度滑块 -->
                <div class="opacity-slider ml8">
                    <span class="label">不透明度</span>
                    <el-slider v-model="opacity" :min="1" :max="100" size="small" style="width: 80px;"></el-slider>
                </div>
            </div>
            <div class="right-tools">
                <el-button size="small" icon="el-icon-refresh-left" @click="undo">撤销</el-button>
                <el-button size="small" class="ml8" @click="clearDesign" icon="el-icon-delete">清空</el-button>
                <el-button size="small" type="success" class="ml8" @click="startConversion" icon="el-icon-magic-stick">转换为成衣</el-button>
            </div>
        </div>

        <div class="workspace">
            <!-- 左侧边栏 -->
            <div class="sidebar card">
                <div class="section-title">实时状态</div>
                <div class="status">
                    <span>当前工具：{{ activeTool === 'pen' ? '画笔' : '橡皮擦' }}</span>
                    <span>笔触类型：{{ getBrushTypeName() }}</span>
                    <span>不透明度：{{ opacity }}%</span>
                </div>

                <div class="text-input-section">
                    <el-input 
                        v-model="textInput" 
                        size="small" 
                        placeholder="输入设计描述" 
                        style="width: 100%;"
                    ></el-input>
                </div>

                <div class="divider"></div>
                <div class="section-title">成品预览</div>
                <div class="preview-box">
                    <transition name="el-fade-in">
                        <div v-if="clothingResult" class="preview-content">
                            <img :src="clothingResult" alt="成衣预览" class="preview-image">
                        </div>
                        <div v-else class="preview-placeholder">
                            <i class="el-icon-picture-outline"></i>
                            <p>点击上方按钮<br>开始 AI 转换</p>
                        </div>
                    </transition>
                </div>
            </div>

            <!-- 主画布区域 -->
            <div class="canvas-wrap card">
                <canvas ref="canvas" :width="canvasWidth" :height="canvasHeight" class="design-canvas"
                        @mousedown="onPointerDown" @mousemove="onPointerMove" @mouseup="onPointerUp" @mouseleave="onPointerUp"></canvas>
            </div>
        </div>

        <!-- 15秒加载遮罩 -->
        <div v-if="isConverting" class="loading-overlay">
            <div class="loading-content">
                <div class="loading-spinner"></div>
                <h3>AI 正在根据设计渲染成衣...</h3>
                <div class="progress-container">
                    <el-progress 
                        :percentage="progressPercentage" 
                        :stroke-width="15" 
                        color="#d4af37"
                        :format="progressFormat"
                    ></el-progress>
                </div>
                <p class="loading-tip">正在模拟面料质感，预计需要 15 秒</p>
            </div>
        </div>

        <!-- 结果大图预览 -->
        <el-dialog title="成衣渲染完成" v-model="previewDialogVisible" width="600px">
            <div class="result-display">
                <img :src="clothingResult" alt="渲染结果" class="full-result-image">
            </div>
            <template #footer>
                <el-button type="primary" @click="downloadClothingResult">下载图片</el-button>
                <el-button @click="previewDialogVisible = false">关闭</el-button>
            </template>
        </el-dialog>
    </div>
</template>

<script>
export default {
    name: 'ProfessionalDesign',
    data() {
        return {
            activeTool: 'pen',
            brushType: 'pencil', // 默认笔触
            opacity: 100,        // 默认不透明度
            strokeColor: 'rgba(50,50,50,1)',
            lineWidth: 3,
            isDrawing: false,
            ctx: null,
            canvasWidth: 800,
            canvasHeight: 600,
            objects: [],
            isConverting: false,
            progressPercentage: 0,
            clothingResult: '',
            previewDialogVisible: false,
            textInput: ''
        }
    },
    computed: {
        toolLabel() { return this.activeTool === 'pen' ? '自由画笔' : '橡皮擦'; }
    },
    mounted() {
        this.ctx = this.$refs.canvas.getContext('2d');
        this.ctx.lineCap = 'round';
        this.ctx.lineJoin = 'round';
    },
    methods: {
        startConversion() {
            this.isConverting = true;
            this.progressPercentage = 0;
            this.clothingResult = ''; 
            const totalTime = 15000;
            const intervalTime = 100;
            const increment = 100 / (totalTime / intervalTime);

            const timer = setInterval(() => {
                this.progressPercentage = parseFloat((this.progressPercentage + increment).toFixed(1));
                if (this.progressPercentage >= 100) {
                    this.progressPercentage = 100;
                    clearInterval(timer);
                    this.finishConversion();
                }
            }, intervalTime);
        },
        finishConversion() {
            this.clothingResult = 'https://pic1.imgdb.cn/item/69c727320e9de3207f582855.png';
            this.isConverting = false;
            this.previewDialogVisible = true;
            this.$message.success('转换成功！');
        },
        progressFormat(p) { return p >= 100 ? '完成' : `${Math.floor(p)}%`; },
        downloadClothingResult() {
            const a = document.createElement('a');
            a.href = this.clothingResult;
            a.download = 'result.png';
            a.click();
        },
        // 获取笔触中文名称
        getBrushTypeName() {
            const names = { pencil: '硬铅笔', brush: '柔和毛笔', marker: '马克笔', watercolor: '水彩叠色' };
            return names[this.brushType];
        },
        updateColorAlpha() {
            // 此处由颜色选择器组件自动处理 rgba
        },
        onPointerDown(e) {
            this.isDrawing = true;
            const rect = this.$refs.canvas.getBoundingClientRect();
            const x = e.clientX - rect.left, y = e.clientY - rect.top;
            
            // 处理橡皮擦逻辑
            const finalColor = this.activeTool === 'eraser' ? 'rgba(255,253,248,1)' : this.strokeColor;
            const finalOpacity = this.activeTool === 'eraser' ? 1 : this.opacity / 100;

            this.objects.push({ 
                type: 'path', 
                points: [{x, y}], 
                color: finalColor, 
                width: this.lineWidth,
                alpha: finalOpacity,
                brush: this.brushType,
                mode: this.activeTool 
            });
        },
        onPointerMove(e) {
            if (!this.isDrawing) return;
            const rect = this.$refs.canvas.getBoundingClientRect();
            const x = e.clientX - rect.left, y = e.clientY - rect.top;
            this.objects[this.objects.length - 1].points.push({x, y});
            this.redraw();
        },
        onPointerUp() { this.isDrawing = false; },
        redraw() {
            this.ctx.clearRect(0, 0, this.canvasWidth, this.canvasHeight);
            this.objects.forEach(obj => {
                this.ctx.beginPath();
                this.ctx.strokeStyle = obj.color;
                this.ctx.lineWidth = obj.width;
                this.ctx.globalAlpha = obj.alpha;
                
                // 笔触样式分支
                if (obj.brush === 'marker') {
                    this.ctx.lineCap = 'square';
                    this.ctx.lineJoin = 'bevel';
                } else if (obj.brush === 'watercolor') {
                    this.ctx.shadowBlur = obj.width / 2;
                    this.ctx.shadowColor = obj.color;
                } else {
                    this.ctx.lineCap = 'round';
                    this.ctx.lineJoin = 'round';
                    this.ctx.shadowBlur = 0;
                }

                this.ctx.moveTo(obj.points[0].x, obj.points[0].y);
                obj.points.forEach(p => this.ctx.lineTo(p.x, p.y));
                this.ctx.stroke();
            });
            this.ctx.globalAlpha = 1.0; // 重置全局透明度
            this.ctx.shadowBlur = 0;    // 重置阴影
        },
        undo() { this.objects.pop(); this.redraw(); },
        clearDesign() { this.objects = []; this.clothingResult = ''; this.redraw(); }
    }
}
</script>

<style scoped lang="scss">
.pro-design {
    display: flex;
    flex-direction: column;
    min-height: 100vh;
    background: url("https://pic1.imgdb.cn/item/6958e1eeda3df73ea1bc5a2c.png") center/cover no-repeat;
    background-color: rgba(255, 255, 250, 0.95);
    background-blend-mode: overlay;
    padding: 16px;
    gap: 16px;
    font-family: "Microsoft YaHei", Arial, sans-serif;
    box-sizing: border-box;
}

.card {
    background-color: #fff8e8;
    border: 1px solid #f0e8d0;
    border-radius: 16px;
    box-shadow: 0 6px 18px rgba(240, 224, 180, 0.15);
}

.toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    flex-wrap: wrap;
    gap: 10px;
}

.left-tools, .right-tools {
    display: flex;
    align-items: center;
    gap: 12px;
}

.opacity-slider {
    display: flex;
    align-items: center;
    gap: 8px;
    .label {
        font-size: 12px;
        color: #8a7d5e;
        white-space: nowrap;
    }
}

.workspace {
    display: flex;
    flex: 1;
    gap: 16px;
}

.sidebar {
    width: 260px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
}

.section-title {
    font-weight: 500;
    color: #8a7d5e;
    font-size: 16px;
    margin-bottom: 12px;
    display: flex;
    align-items: center;
    gap: 8px;
    &::before {
        content: "";
        width: 8px;
        height: 16px;
        background-color: #f0c880;
        border-radius: 4px;
    }
}

.divider { height: 1px; background-color: #f0e8d0; margin: 8px 0; }

.status {
    display: flex;
    flex-direction: column;
    gap: 8px;
    color: #8a7d5e;
    font-size: 13px;
}

.text-input-section {
    margin: 12px 0;
}

.canvas-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #fcf9f2; /* 轻微区别于卡片色 */
}

.design-canvas {
    background-color: #fffdf8;
    box-shadow: 0 2px 8px rgba(240, 224, 180, 0.1);
    border-radius: 4px;
    border: 1px dashed #f0e8d0;
    cursor: crosshair;
}

.preview-box {
    margin-top: 10px;
    padding: 15px;
    background-color: #f9eeda;
    border-radius: 8px;
    height: 220px;
    display: flex;
    justify-content: center;
    align-items: center;
    border: 1px dashed #f0c880;
    .preview-image { max-width: 100%; max-height: 100%; object-fit: contain; }
    .preview-placeholder { text-align: center; color: #b0a080; i { font-size: 36px; margin-bottom: 12px; } }
}

.loading-overlay {
    position: fixed; top: 0; left: 0; right: 0; bottom: 0;
    background-color: rgba(255, 253, 248, 0.9);
    display: flex; justify-content: center; align-items: center; z-index: 9999;
    .loading-content {
        background-color: #fff8e8; padding: 40px; border-radius: 12px; text-align: center; width: 420px;
        h3 { color: #8a7d5e; margin: 20px 0; }
    }
}

.loading-spinner {
    width: 60px; height: 60px; border: 4px solid #f3e9d0;
    border-top: 4px solid #d4af37; border-radius: 50%;
    margin: 0 auto; animation: spin 1s linear infinite;
}

@keyframes spin { 100% { transform: rotate(360deg); } }

.full-result-image { width: 100%; border-radius: 8px; }

.ml8 { margin-left: 8px; }

:deep(.el-radio-group .el-radio-button__inner) {
    background-color: #fff8e8; border-color: #f0e8d0; color: #8a7d5e;
    &.is-active { background-color: #f0c880; border-color: #f0c880; color: #8a7d5e; }
}
:deep(.el-slider__runway) { background-color: #eee4cc; }
:deep(.el-slider__bar) { background-color: #f0c880; }
:deep(.el-slider__button) { border-color: #f0c880; }
</style>