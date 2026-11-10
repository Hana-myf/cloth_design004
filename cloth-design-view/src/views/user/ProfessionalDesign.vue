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
                    <span>透明度：{{ opacity }}%</span>
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
                            <!-- 新增：查看图片按钮 -->
                            <el-button 
                                size="small" 
                                @click="previewDialogVisible = true"
                                style="margin-top: 10px; width: 100%; background-color: #f9eeda; border-color: #f0e8d0; color: #8a7d5e;"
                            >
                                查看图片
                            </el-button>
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

            <!-- 右侧工具栏 -->
            <div class="right-sidebar card">
                <!-- 工具 -->
                <div class="section-title">工具</div>
                <div class="tool-section">
                    <el-button size="small" @click="addMask" class="tool-button">
                        <i class="el-icon-view"></i>
                        <span>蒙版</span>
                    </el-button>
                    <el-button size="small" @click="addClip" class="tool-button">
                        <i class="el-icon-scissors"></i>
                        <span>剪裁</span>
                    </el-button>
                </div>

                <!-- 图层管理 -->
                <div class="section-title">图层</div>
                <div class="layers-panel">
                    <div class="layer-item" v-for="(layer, index) in layers" :key="index" @click="selectLayer(index)">
                        <div class="layer-info">
                            <i class="el-icon-caret-right"></i>
                            <span>{{ layer.name }}</span>
                        </div>
                    </div>
                    <el-button size="small" type="warning" @click="addLayer" style="width: 100%; margin-top: 8px; background-color: #f0c880; border-color: #f0c880; color: #8a7d5e; box-sizing: border-box;">
                        <i class="el-icon-plus"></i> 添加
                    </el-button>
                </div>

                <!-- 图层调整 -->
                <div class="section-title">调整</div>
                <div class="adjust-panel">
                    <div class="adjust-item">
                        <span class="label">透明度</span>
                        <el-slider v-model="opacity" :min="0" :max="100" size="small"></el-slider>
                    </div>
                    <div class="adjust-item">
                        <span class="label">亮度</span>
                        <el-slider v-model="brightness" :min="0" :max="200" size="small"></el-slider>
                    </div>
                    <div class="adjust-item">
                        <span class="label">对比度</span>
                        <el-slider v-model="contrast" :min="0" :max="200" size="small"></el-slider>
                    </div>
                </div>

                <!-- 色彩情况 -->
                <div class="section-title">颜色</div>
                <div class="color-panel">
                    <div class="color-info">
                        <div class="color-preview" :style="{ backgroundColor: strokeColor }"></div>
                        <span class="color-value">{{ strokeColor }}</span>
                    </div>
                </div>

                <!-- 导出功能 -->
                <div class="section-title">导出</div>
                <div class="export-panel">
                    <el-dropdown>
                        <el-button size="small" type="warning" style="width: 100%; background-color: #f0c880; border-color: #f0c880; color: #8a7d5e;">
                            <i class="el-icon-download"></i> 导出 <i class="el-icon-arrow-down"></i>
                        </el-button>
                        <el-dropdown-menu slot="dropdown">
                            <el-dropdown-item>导出线稿</el-dropdown-item>
                            <el-dropdown-item>导出效果图</el-dropdown-item>
                            <el-dropdown-item>导出制版 DXF</el-dropdown-item>
                            <el-dropdown-item>导出生产单 PDF</el-dropdown-item>
                            <el-dropdown-item>导出分层 PSD</el-dropdown-item>
                        </el-dropdown-menu>
                    </el-dropdown>
                </div>
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
        <el-dialog 
            title="" 
            :visible.sync="previewDialogVisible" 
            width="800px"
            center
            custom-class="clothing-dialog"
        >
            <div class="result-display">
                <img :src="clothingResult" alt="渲染结果" class="full-result-image">
            </div>
            <template #footer>
                <el-button 
                    @click="downloadClothingResult" 
                    style="background-color: #f0c880; border-color: #f0c880; color: #8a7d5e;"
                >
                    下载图片
                </el-button>
                <el-button 
                    @click="previewDialogVisible = false" 
                    style="background-color: #f9eeda; border-color: #f0e8d0; color: #8a7d5e;"
                >
                    关闭
                </el-button>
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
            opacity: 80,        // 默认不透明度
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
            textInput: '' // 用于输入设计描述
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
            this.clothingResult = 'https://pic1.imgdb.cn/item/69dd977df26639441e6e1864.png';
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
    overflow-x: auto;
    width: 100%;
    min-width: fit-content;
}

.card {
    background-color: #fff8e8;
    border: 1px solid #f0e8d0;
    border-radius: 16px;
    box-shadow: 0 6px 18px rgba(240, 224, 180, 0.15);
    box-sizing: border-box;
    flex-shrink: 0;
}

.toolbar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 12px 20px;
    flex-wrap: nowrap;
    gap: 12px;
    width: 100%;
    box-sizing: border-box;
    min-width: calc(200px + 16px + 1fr + 16px + 200px);
}

.left-tools, .right-tools {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-shrink: 0;
}



.workspace {
    display: flex;
    flex: 1;
    gap: 16px;
    width: 100%;
    box-sizing: border-box;
    min-width: calc(200px + 16px + 600px + 16px + 200px);
}

.sidebar {
    width: 200px;
    padding: 15px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    flex-shrink: 0;
}

.right-sidebar {
    width: 200px;
    padding: 15px;
    display: flex;
    flex-direction: column;
    gap: 12px;
    flex-shrink: 0;
    max-height: calc(100vh - 120px);
    overflow-y: auto;
}

/* 🔴 核心修复：工具按钮组，强制100%宽度+统一样式 */
.tool-section {
    display: flex;
    flex-direction: column;
    gap: 8px;
    width: 100%;
    box-sizing: border-box;
    .tool-button {
        width: 80% !important;
        min-width: unset !important;
        height: 36px !important;
        display: flex !important;
        align-items: center !important;
        justify-content: center !important;
        gap: 6px !important;
        font-size: 11px !important;
        border-radius: 8px !important;
        box-sizing: border-box !important;
        margin: 0 auto !important;
        padding: 0 10px !important;
    }
}

/* 🔴 强制覆盖Element Plus按钮默认样式，彻底统一 */
:deep(.el-button.tool-button) {
    width: 80% !important;
    height: 36px !important;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    font-size: 11px !important;
    border-radius: 8px !important;
    box-sizing: border-box !important;
    margin: 0 auto !important;
    padding: 0 10px !important;
}

.layers-panel {
    max-height: 220px;
    overflow-y: auto;
    .layer-item {
        padding: 6px;
        border-radius: 4px;
        cursor: pointer;
        font-size: 11px;
        &:hover {
            background-color: #f9eeda;
        }
        .layer-info {
            display: flex;
            align-items: center;
            gap: 6px;
        }
    }
    :deep(.el-button) {
        width: 80% !important;
        margin: 8px auto 0 auto !important;
        display: block !important;
    }
}

.adjust-panel {
    .adjust-item {
        margin-bottom: 8px;
        .label {
            display: block;
            margin-bottom: 4px;
            font-size: 10px;
            color: #8a7d5e;
        }
    }
}

.color-panel {
    .color-info {
        display: flex;
        align-items: center;
        gap: 8px;
        .color-preview {
            width: 20px;
            height: 20px;
            border-radius: 4px;
            border: 1px solid #f0e8d0;
        }
        .color-value {
            font-size: 9px;
            color: #8a7d5e;
            flex: 1;
            word-break: break-all;
        }
    }
}

.export-panel {
    margin-top: 4px;
}

/* 弹窗样式美化 */
.clothing-dialog {
    .el-dialog__header {
        background-color: #fff8e8;
        border-bottom: 1px solid #f0e8d0;
        .el-dialog__title {
            color: #8a7d5e;
            font-weight: 500;
            font-size: 16px;
        }
    }
    .el-dialog__body {
        background-color: #fffdf8;
        padding: 20px;
    }
    .el-dialog__footer {
        background-color: #fff8e8;
        border-top: 1px solid #f0e8d0;
        padding: 15px 20px;
    }
}

.result-display {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 400px;
    background-color: #fcf9f2;
    border-radius: 8px;
    padding: 20px;
    box-sizing: border-box;
}

.full-result-image {
    max-width: 100%;
    max-height: 500px;
    object-fit: contain;
    border-radius: 4px;
    box-shadow: 0 4px 12px rgba(240, 224, 180, 0.2);
}

.section-title {
    font-weight: 500;
    color: #8a7d5e;
    font-size: 14px;
    margin-bottom: 8px;
    display: flex;
    align-items: center;
    gap: 6px;
    &::before {
        content: "";
        width: 6px;
        height: 12px;
        background-color: #f0c880;
        border-radius: 3px;
    }
}

.divider { height: 1px; background-color: #f0e8d0; margin: 6px 0; }

.status {
    display: flex;
    flex-direction: column;
    gap: 6px;
    color: #8a7d5e;
    font-size: 13px;
}

.text-input-section {
    margin: 8px 0;
}

.canvas-wrap {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
    background-color: #fcf9f2;
    min-width: 600px;
}

.design-canvas {
    background-color: #fffdf8;
    box-shadow: 0 2px 8px rgba(240, 224, 180, 0.1);
    border-radius: 4px;
    border: 1px dashed #f0e8d0;
    cursor: crosshair;
    width: 100%;
    height: 100%;
}

.preview-box {
    margin-top: 10px;
    padding: 15px;
    background-color: #f9eeda;
    border-radius: 8px;
    height: 220px;
    display: flex;
    flex-direction: column;
    justify-content: center;
    align-items: center;
    border: 1px dashed #f0c880;
    .preview-image { max-width: 100%; max-height: 80%; object-fit: contain; }
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