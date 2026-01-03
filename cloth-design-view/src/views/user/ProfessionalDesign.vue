<template>
    <div class="pro-design">
        <div class="toolbar card">
            <div class="left-tools">
                <el-radio-group v-model="activeTool" size="small">
                    <el-radio-button label="select">选择</el-radio-button>
                    <el-radio-button label="pen">画笔</el-radio-button>
                    <el-radio-button label="line">直线</el-radio-button>
                    <el-radio-button label="rect">矩形</el-radio-button>
                    <el-radio-button label="circle">圆形</el-radio-button>
                    <el-radio-button label="text">文字</el-radio-button>
                </el-radio-group>
                <el-color-picker v-model="strokeColor" show-alpha size="small" class="ml8"></el-color-picker>
                <el-select v-model="lineWidth" size="small" class="ml8" style="width: 90px;">
                    <el-option v-for="w in [1,2,3,4,6,8,10,12]" :key="w" :label="w + ' px'" :value="w" />
                </el-select>
                <el-switch class="ml8" v-model="showGrid" active-text="网格"></el-switch>
                <el-switch class="ml8" v-model="snapToGrid" active-text="吸附"></el-switch>
                <el-select v-model="gridSize" size="small" class="ml8" style="width: 100px;">
                    <el-option v-for="g in [8,10,16,20,24,32]" :key="g" :label="g + ' px'" :value="g" />
                </el-select>
                <el-select v-model="brush.type" size="small" class="ml8" style="width: 110px;">
                    <el-option label="基础画笔" value="basic" />
                    <el-option label="纹理画笔" value="texture" />
                    <el-option label="缝线画笔" value="stitch" />
                </el-select>
                <el-select v-if="brush.type==='texture'" v-model="brush.textureKey" size="small" class="ml8" style="width: 140px;">
                    <el-option label="棉麻纹理" value="linen" />
                    <el-option label="牛仔纹理" value="denim" />
                    <el-option label="针织纹理" value="knit" />
                </el-select>
                <el-input-number v-if="brush.type!=='basic'" class="ml8" v-model="brush.spacing" :min="2" :max="60" :step="2" size="small"></el-input-number>
                <el-input-number v-if="brush.type==='stitch'" class="ml8" v-model="brush.stitchSize" :min="2" :max="24" :step="1" size="small"></el-input-number>
            </div>
            <div class="right-tools">
                <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onImportImage" />
                <el-button size="small" icon="el-icon-picture" @click="$refs.fileInput.click()">导入图片</el-button>
                <el-button size="small" icon="el-icon-refresh-left" @click="undo">撤销</el-button>
                <el-button size="small" icon="el-icon-refresh-right" @click="redo">重做</el-button>
                <el-button size="small" class="ml8" @click="clearDesign" icon="el-icon-delete">清空</el-button>
                <el-button size="small" class="ml8" @click="exportJSON" icon="el-icon-upload2">导出JSON</el-button>
                <el-button size="small" type="primary" class="ml8" @click="saveImage" icon="el-icon-download">导出PNG</el-button>
            </div>
        </div>
        <div class="workspace">
            <div class="sidebar card">
                <div class="section-title">画布</div>
                <div class="row"><span>宽度</span><el-input-number size="mini" v-model="canvasWidth" :min="320" :max="3000" :step="10"></el-input-number></div>
                <div class="row"><span>高度</span><el-input-number size="mini" v-model="canvasHeight" :min="320" :max="3000" :step="10"></el-input-number></div>
                <div class="divider"></div>
                <div class="section-title">状态</div>
                <div class="status">
                    <span>工具：{{ toolLabel }}</span>
                    <span>颜色：<i class="color-dot" :style="{background: strokeColor}"></i></span>
                    <span>线宽：{{ lineWidth }}px</span>
                    <span v-if="brush.type!=='basic'">间距：{{ brush.spacing }}</span>
                </div>
            </div>
            <div class="canvas-wrap card">
                <canvas ref="canvas" :width="canvasWidth" :height="canvasHeight" class="design-canvas"
                        @mousedown="onPointerDown" @mousemove="onPointerMove" @mouseup="onPointerUp" @mouseleave="onPointerUp"
                        @touchstart.prevent="onTouchStart" @touchmove.prevent="onTouchMove" @touchend.prevent="onPointerUp"></canvas>
                <div class="floating-palette" v-show="showPalette">
                    <div class="swatch" v-for="c in recentColors" :key="c" :style="{background:c}"
                         @click="strokeColor=c"></div>
                </div>
            </div>
        </div>
        <div class="footer-status card">
            <div>坐标：{{ cursorPos.x }}, {{ cursorPos.y }}</div>
            <div>网格：{{ showGrid ? '开' : '关' }} / 吸附：{{ snapToGrid ? '开' : '关' }}</div>
            <div>画笔：{{ brush.type }} / 纹理：{{ brush.textureKey }}</div>
        </div>
    </div>
</template>

<script>
// 逻辑部分保持不变，仅修改样式
export default {
    name: 'ProfessionalDesign',
    data() {
        return {
            activeTool: 'select',
            strokeColor: 'rgba(0,0,0,1)',
            lineWidth: 3,
            isDrawing: false,
            startX: 0,
            startY: 0,
            ctx: null,
            canvasWidth: 1200,
            canvasHeight: 700,
            objects: [],
            preview: null,
            selectedId: null,
            history: [],
            future: [],
            showGrid: true,
            snapToGrid: true,
            gridSize: 16,
            brush: {
                type: 'basic',
                spacing: 8,
                stitchSize: 6,
                textureKey: 'linen',
                patterns: {}
            },
            cursorPos: { x: 0, y: 0 },
            recentColors: ['#111','#e91e63','#ff9800','#4caf50','#2196f3','rgba(0,0,0,0.6)'],
            showPalette: true,
        }
    },
    computed: {
        toolLabel() {
            const map = { select: '选择', pen: '画笔', line: '直线', rect: '矩形', circle: '圆形', text: '文字' };
            return map[this.activeTool] || this.activeTool;
        }
    },
    mounted() {
        const canvas = this.$refs.canvas;
        this.ctx = canvas.getContext('2d');
        this.ctx.lineCap = 'round';
        this.ctx.lineJoin = 'round';
        this.resizeToContainer();
        window.addEventListener('resize', this.resizeToContainer);
        window.addEventListener('keydown', this.onKeyDown);
        this.pushHistory();
        this.redraw();
    },
    beforeDestroy() {
        window.removeEventListener('resize', this.resizeToContainer);
        window.removeEventListener('keydown', this.onKeyDown);
    },
    methods: {
        resizeToContainer() {
            const wrap = this.$el.querySelector('.canvas-wrap');
            if (!wrap) return;
            const padding = 20;
            const width = wrap.clientWidth - padding * 2;
            const height = Math.max(480, (wrap.clientHeight || 720) - padding * 2);
            this.canvasWidth = Math.max(320, width);
            this.canvasHeight = Math.max(480, height);
            this.$nextTick(this.redraw);
        },
        canvasPos(evt) {
            const rect = this.$refs.canvas.getBoundingClientRect();
            return { x: evt.clientX - rect.left, y: evt.clientY - rect.top };
        },
        snap(point) {
            if (!this.snapToGrid) return point;
            const gx = Math.round(point.x / this.gridSize) * this.gridSize;
            const gy = Math.round(point.y / this.gridSize) * this.gridSize;
            return { x: gx, y: gy };
        },
        onKeyDown(e) {
            if (e.ctrlKey && (e.key === 'z' || e.key === 'Z')) {
                e.preventDefault();
                this.undo();
            } else if (e.ctrlKey && (e.key === 'y' || (e.shiftKey && (e.key === 'Z' || e.key === 'z')))) {
                e.preventDefault();
                this.redo();
            } else if (e.key === 'Delete' || e.key === 'Backspace') {
                this.deleteSelected();
            }
        },
        onPointerDown(evt) {
            let { x, y } = this.canvasPos(evt);
            ({ x, y } = this.snap({ x, y }));
            this.isDrawing = true;
            this.startX = x; this.startY = y;
            if (this.activeTool === 'pen') {
                this.preview = { type: 'path', points: [{ x, y }], color: this.strokeColor, width: this.lineWidth };
            } else if (['line','rect','circle'].includes(this.activeTool)) {
                this.preview = { type: this.activeTool, x1: x, y1: y, x2: x, y2: y, color: this.strokeColor, width: this.lineWidth };
            } else if (this.activeTool === 'text') {
                const text = prompt('输入文字内容');
                if (text) {
                    const obj = { id: this.uid(), type: 'text', x, y, text, color: this.strokeColor, size: Math.max(14, this.lineWidth * 6) };
                    this.objects.push(obj);
                    this.pushHistory();
                    this.redraw();
                }
                this.isDrawing = false;
            } else if (this.activeTool === 'select') {
                const hit = this.hitTest(x, y);
                this.selectedId = hit ? hit.id : null;
                this.redraw();
                this.isDrawing = false;
            }
        },
        onPointerMove(evt) {
            if (!this.isDrawing) return;
            let { x, y } = this.canvasPos(evt);
            ({ x, y } = this.snap({ x, y }));
            this.cursorPos = { x: Math.round(x), y: Math.round(y) };
            if (!this.preview) return;
            if (this.preview.type === 'path') {
                this.preview.points.push({ x, y });
            } else {
                this.preview.x2 = x; this.preview.y2 = y;
            }
            this.redraw();
            this.drawPreview();
        },
        onPointerUp() {
            if (!this.isDrawing) return;
            this.isDrawing = false;
            if (this.preview) {
                this.commitPreview();
            }
            this.pushHistory();
            this.redraw();
        },
        onTouchStart(e) {
            const touch = e.touches[0];
            this.onPointerDown(touch);
        },
        onTouchMove(e) {
            const touch = e.touches[0];
            this.onPointerMove(touch);
        },
        applyStroke(color, width) {
            this.ctx.strokeStyle = (typeof color !== 'undefined' && color !== null) ? color : this.strokeColor;
            this.ctx.fillStyle = (typeof color !== 'undefined' && color !== null) ? color : this.strokeColor;
            this.ctx.lineWidth = (typeof width !== 'undefined' && width !== null) ? width : this.lineWidth;
        },
        drawObject(obj) {
            this.applyStroke(obj.color, obj.width);
            if (obj.type === 'line') {
                this.ctx.beginPath();
                this.ctx.moveTo(obj.x1, obj.y1);
                this.ctx.lineTo(obj.x2, obj.y2);
                this.ctx.stroke();
            } else if (obj.type === 'rect') {
                const x = Math.min(obj.x1, obj.x2);
                const y = Math.min(obj.y1, obj.y2);
                const w = Math.abs(obj.x2 - obj.x1);
                const h = Math.abs(obj.y2 - obj.y1);
                if (obj.fillPatternKey) {
                    const p = this.ensurePattern(obj.fillPatternKey);
                    this.ctx.save(); this.ctx.fillStyle = p; this.ctx.fillRect(x, y, w, h); this.ctx.restore();
                }
                this.ctx.strokeRect(x, y, w, h);
            } else if (obj.type === 'circle') {
                const r = Math.hypot(obj.x2 - obj.x1, obj.y2 - obj.y1);
                this.ctx.beginPath();
                this.ctx.arc(obj.x1, obj.y1, r, 0, Math.PI * 2);
                if (obj.fillPatternKey) {
                    const p = this.ensurePattern(obj.fillPatternKey);
                    this.ctx.save(); this.ctx.fillStyle = p; this.ctx.fill(); this.ctx.restore();
                }
                this.ctx.stroke();
            } else if (obj.type === 'path') {
                if (obj.points.length < 2) return;
                if (!obj.brush || obj.brush.type === 'basic') {
                    this.ctx.beginPath();
                    this.ctx.moveTo(obj.points[0].x, obj.points[0].y);
                    for (let i = 1; i < obj.points.length; i++) this.ctx.lineTo(obj.points[i].x, obj.points[i].y);
                    this.ctx.stroke();
                } else if (obj.brush.type === 'texture') {
                    this.drawTexturePath(obj);
                } else if (obj.brush.type === 'stitch') {
                    this.drawStitchPath(obj);
                }
            } else if (obj.type === 'text') {
                this.ctx.save();
                this.ctx.fillStyle = obj.color;
                this.ctx.font = `${obj.size || 18}px sans-serif`;
                this.ctx.textAlign = 'left';
                this.ctx.textBaseline = 'alphabetic';
                this.ctx.fillText(obj.text, obj.x, obj.y);
                this.ctx.restore();
            } else if (obj.type === 'image' && obj.imageEl) {
                this.ctx.drawImage(obj.imageEl, obj.x, obj.y, obj.w, obj.h);
            }
        },
        drawPreview() {
            if (!this.preview) return;
            const p = this.preview;
            const color = 'rgba(0, 0, 0, 0.4)';
            const width = Math.max(1, (p.width || this.lineWidth));
            this.applyStroke(color, width);
            if (p.type === 'path') {
                if (p.points.length < 2) return;
                this.ctx.setLineDash([6, 6]);
                this.ctx.beginPath();
                this.ctx.moveTo(p.points[0].x, p.points[0].y);
                for (let i = 1; i < p.points.length; i++) this.ctx.lineTo(p.points[i].x, p.points[i].y);
                this.ctx.stroke();
                this.ctx.setLineDash([]);
            } else if (['line','rect','circle'].includes(p.type)) {
                this.ctx.setLineDash([6, 6]);
                this.drawObject(p);
                this.ctx.setLineDash([]);
            }
        },
        commitPreview() {
            const p = this.preview;
            if (!p) return;
            const obj = { id: this.uid(), ...p };
            if (obj.type === 'path') {
                obj.brush = JSON.parse(JSON.stringify(this.brush));
            }
            this.objects.push(obj);
            this.preview = null;
        },
        ensurePattern(key) {
            if (this.brush.patterns[key]) return this.brush.patterns[key];
            const canvas = document.createElement('canvas');
            const ctx = canvas.getContext('2d');
            canvas.width = canvas.height = 32;
            ctx.clearRect(0,0,32,32);
            if (key === 'linen') {
                ctx.fillStyle = '#ddd'; ctx.fillRect(0,0,32,32);
                ctx.strokeStyle = 'rgba(0,0,0,0.08)';
                for (let i=0;i<32;i+=4){ctx.beginPath();ctx.moveTo(i,0);ctx.lineTo(i,32);ctx.stroke();}
                for (let j=0;j<32;j+=4){ctx.beginPath();ctx.moveTo(0,j);ctx.lineTo(32,j);ctx.stroke();}
            } else if (key === 'denim') {
                ctx.fillStyle = '#2c5aa0'; ctx.fillRect(0,0,32,32);
                ctx.strokeStyle = 'rgba(255,255,255,0.12)';
                for (let i=0;i<32;i+=2){ctx.beginPath();ctx.moveTo(i,0);ctx.lineTo(0,i);ctx.stroke();}
            } else if (key === 'knit') {
                ctx.fillStyle = '#e6e6e6'; ctx.fillRect(0,0,32,32);
                ctx.strokeStyle = 'rgba(0,0,0,0.12)';
                for (let i=0;i<32;i+=8){ctx.beginPath();ctx.arc(16,i,6,0,Math.PI);ctx.stroke();}
            }
            const pattern = this.ctx.createPattern(canvas, 'repeat');
            this.brush.patterns[key] = pattern;
            return pattern;
        },
        drawTexturePath(pathObj) {
            const pts = pathObj.points;
            const spacing = Math.max(2, (pathObj.brush && pathObj.brush.spacing) || 8);
            const pattern = this.ensurePattern((pathObj.brush && pathObj.brush.textureKey) || 'linen');
            for (let i = 1, acc = 0; i < pts.length; i++) {
                const a = pts[i-1], b = pts[i];
                const segLen = Math.hypot(b.x-a.x, b.y-a.y);
                if (segLen === 0) continue;
                const ux = (b.x-a.x)/segLen, uy = (b.y-a.y)/segLen;
                for (acc; acc <= segLen; acc += spacing) {
                    const cx = a.x + ux*acc, cy = a.y + uy*acc;
                    const size = (pathObj.width || 6) * 2;
                    this.ctx.save();
                    this.ctx.translate(cx, cy);
                    this.ctx.fillStyle = pattern;
                    this.ctx.beginPath();
                    this.ctx.arc(0, 0, size/2, 0, Math.PI*2);
                    this.ctx.fill();
                    this.ctx.restore();
                }
                acc = acc - segLen;
            }
        },
        drawStitchPath(pathObj) {
            const pts = pathObj.points;
            const spacing = Math.max(2, (pathObj.brush && pathObj.brush.spacing) || 8);
            const stitchSize = Math.max(2, (pathObj.brush && pathObj.brush.stitchSize) || 6);
            this.ctx.strokeStyle = pathObj.color;
            this.ctx.lineWidth = Math.max(1, pathObj.width || 2);
            for (let i = 1, acc = 0; i < pts.length; i++) {
                const a = pts[i-1], b = pts[i];
                const segLen = Math.hypot(b.x-a.x, b.y-a.y);
                if (segLen === 0) continue;
                const ux = (b.x-a.x)/segLen, uy = (b.y-a.y)/segLen;
                for (acc; acc <= segLen; acc += spacing) {
                    const cx = a.x + ux*acc, cy = a.y + uy*acc;
                    const nx = -uy, ny = ux;
                    this.ctx.beginPath();
                    this.ctx.moveTo(cx - nx*stitchSize*0.5, cy - ny*stitchSize*0.5);
                    this.ctx.lineTo(cx + nx*stitchSize*0.5, cy + ny*stitchSize*0.5);
                    this.ctx.stroke();
                }
                acc = acc - segLen;
            }
        },
        redraw() {
            this.ctx.clearRect(0, 0, this.canvasWidth, this.canvasHeight);
            if (this.showGrid) this.drawGrid();
            for (const obj of this.objects) this.drawObject(obj);
            if (this.selectedId) {
                const obj = this.objects.find(o => o.id === this.selectedId);
                if (obj) this.drawSelectionBox(obj);
            }
        },
        drawGrid() {
            const g = this.gridSize;
            this.ctx.save();
            this.ctx.strokeStyle = 'rgba(0,0,0,0.06)';
            this.ctx.lineWidth = 1;
            for (let x = 0; x <= this.canvasWidth; x += g) {
                this.ctx.beginPath();
                this.ctx.moveTo(x, 0);
                this.ctx.lineTo(x, this.canvasHeight);
                this.ctx.stroke();
            }
            for (let y = 0; y <= this.canvasHeight; y += g) {
                this.ctx.beginPath();
                this.ctx.moveTo(0, y);
                this.ctx.lineTo(this.canvasWidth, y);
                this.ctx.stroke();
            }
            this.ctx.restore();
        },
        drawSelectionBox(obj) {
            let x, y, w, h;
            if (obj.type === 'line' || obj.type === 'rect' || obj.type === 'circle') {
                x = Math.min(obj.x1, obj.x2); y = Math.min(obj.y1, obj.y2);
                w = Math.abs(obj.x2 - obj.x1); h = Math.abs(obj.y2 - obj.y1);
                if (obj.type === 'circle') { x = obj.x1 - Math.hypot(obj.x2-obj.x1, obj.y2-obj.y1); y = obj.y1 - Math.hypot(obj.x2-obj.x1, obj.y2-obj.y1); w = h = 2*Math.hypot(obj.x2-obj.x1,obj.y2-obj.y1); }
            } else if (obj.type === 'path') {
                const xs = obj.points.map(p => p.x); const ys = obj.points.map(p => p.y);
                x = Math.min(...xs); y = Math.min(...ys); w = Math.max(...xs) - x; h = Math.max(...ys) - y;
            } else if (obj.type === 'text') {
                const textLen = (obj.text && obj.text.length) || 1;
                x = obj.x; y = obj.y - (obj.size || 18); w = textLen * (obj.size || 18) * 0.6; h = (obj.size || 18) * 1.2;
            } else if (obj.type === 'image') {
                x = obj.x; y = obj.y; w = obj.w; h = obj.h;
            }
            this.ctx.save();
            this.ctx.strokeStyle = '#f0c880';
            this.ctx.setLineDash([4, 4]);
            this.ctx.strokeRect(x, y, w, h);
            this.ctx.restore();
        },
        hitTest(x, y) {
            for (let i = this.objects.length - 1; i >= 0; i--) {
                const o = this.objects[i];
                if (o.type === 'line') {
                    const dist = this.pointToSegmentDistance({ x, y }, { x: o.x1, y: o.y1 }, { x: o.x2, y: o.y2 });
                    if (dist <= Math.max(6, o.width)) return o;
                } else if (o.type === 'rect') {
                    const rx = Math.min(o.x1, o.x2), ry = Math.min(o.y1, o.y2), rw = Math.abs(o.x2 - o.x1), rh = Math.abs(o.y2 - o.y1);
                    if (x >= rx && x <= rx + rw && y >= ry && y <= ry + rh) return o;
                } else if (o.type === 'circle') {
                    const r = Math.hypot(o.x2 - o.x1, o.y2 - o.y1);
                    if (Math.hypot(x - o.x1, y - o.y1) <= r) return o;
                } else if (o.type === 'path') {
                    for (let j = 1; j < o.points.length; j++) {
                        const a = o.points[j - 1], b = o.points[j];
                        const dist = this.pointToSegmentDistance({ x, y }, a, b);
                        if (dist <= Math.max(6, o.width)) return o;
                    }
                } else if (o.type === 'text') {
                    const size = o.size || 18;
                    const textLen = (o.text && o.text.length) || 1;
                    const w = textLen * size * 0.6, h = size * 1.2;
                    if (x >= o.x && x <= o.x + w && y >= o.y - size && y <= o.y - size + h) return o;
                } else if (o.type === 'image') {
                    if (x >= o.x && x <= o.x + o.w && y >= o.y && y <= o.y + o.h) return o;
                }
            }
            return null;
        },
        pointToSegmentDistance(p, a, b) {
            const dx = b.x - a.x, dy = b.y - a.y;
            if (dx === 0 && dy === 0) return Math.hypot(p.x - a.x, p.y - a.y);
            const t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy);
            const tt = Math.max(0, Math.min(1, t));
            const proj = { x: a.x + tt * dx, y: a.y + tt * dy };
            return Math.hypot(p.x - proj.x, p.y - proj.y);
        },
        pushHistory() {
            const snapshot = JSON.stringify({ objects: this.serializeObjects() });
            this.history.push(snapshot);
            if (this.history.length > 80) this.history.shift();
            this.future = [];
        },
        serializeObjects() {
            return this.objects.map(o => {
                const { imageEl, ...pure } = o; return pure;
            });
        },
        async restoreFrom(snapshot) {
            const state = JSON.parse(snapshot);
            this.objects = await Promise.all((state.objects || []).map(async o => {
                if (o.type === 'image' && o.src) {
                    const img = await this.loadImage(o.src);
                    return { ...o, imageEl: img };
                }
                return o;
            }));
            this.redraw();
        },
        async undo() {
            if (this.history.length <= 1) return;
            const current = this.history.pop();
            this.future.push(current);
            const prev = this.history[this.history.length - 1];
            await this.restoreFrom(prev);
        },
        async redo() {
            if (this.future.length === 0) return;
            const next = this.future.pop();
            this.history.push(next);
            await this.restoreFrom(next);
        },
        clearDesign() {
            this.objects = [];
            this.preview = null; this.selectedId = null;
            this.pushHistory();
            this.redraw();
        },
        saveImage() {
            const dataUrl = this.$refs.canvas.toDataURL('image/png');
            const a = document.createElement('a');
            a.href = dataUrl;
            a.download = `design-${Date.now()}.png`;
            a.click();
        },
        exportJSON() {
            const json = JSON.stringify({ objects: this.serializeObjects() }, null, 2);
            const blob = new Blob([json], { type: 'application/json' });
            const url = URL.createObjectURL(blob);
            const a = document.createElement('a');
            a.href = url; a.download = `design-${Date.now()}.json`; a.click();
            URL.revokeObjectURL(url);
        },
        onImportImage(e) {
            const file = e.target.files && e.target.files[0];
            if (!file) return;
            const url = URL.createObjectURL(file);
            this.loadImage(url).then(img => {
                const w = Math.min(300, img.width), h = Math.min(300, img.height);
                const obj = { id: this.uid(), type: 'image', x: 40, y: 40, w, h, src: url, imageEl: img };
                this.objects.push(obj);
                this.pushHistory();
                this.redraw();
            });
            e.target.value = '';
        },
        sampleColorAt(x, y) {
            try {
                const data = this.ctx.getImageData(x, y, 1, 1).data;
                const rgba = `rgba(${data[0]},${data[1]},${data[2]},${(data[3]/255).toFixed(2)})`;
                this.strokeColor = rgba;
                if (!this.recentColors.includes(rgba)) this.recentColors.unshift(rgba);
            } catch (e) { /* ignore */ }
        },
        loadImage(src) {
            return new Promise(resolve => { const img = new Image(); img.onload = () => resolve(img); img.src = src; });
        },
        deleteSelected() {
            if (!this.selectedId) return;
            this.objects = this.objects.filter(o => o.id !== this.selectedId);
            this.selectedId = null;
            this.pushHistory();
            this.redraw();
        },
        uid() { return 'id-' + Math.random().toString(36).slice(2, 9); },
    },
    watch: {
        showGrid() { this.redraw(); },
        gridSize() { this.redraw(); }
    }
}
</script>

<style scoped lang="scss">
/* 全局手绘暖风格基底 - 替换为指定背景图 */
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
    overflow: hidden;
}

/* 统一卡片样式 - 手绘暖风格 */
.card {
    background-color: #fff8e8;
    border: 1px solid #f0e8d0;
    border-radius: 16px;
    box-shadow: 0 6px 18px rgba(240, 224, 180, 0.15);
}

/* 工具栏样式 */
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
    flex-wrap: wrap;
    gap: 8px;
}

.ml8 {
    margin-left: 8px;
}

/* Element UI组件样式覆盖 - 统一暖色调 */
// 单选按钮
:deep(.el-radio-group .el-radio-button__inner) {
    background-color: #fff8e8;
    border-color: #f0e8d0;
    color: #8a7d5e;
    padding: 8px 12px;
    &:hover {
        background-color: #f9eeda;
        border-color: #f0c880;
    }
    &.is-active {
        background-color: #f0c880;
        border-color: #f0c880;
        color: #8a7d5e;
    }
}

// 颜色选择器
:deep(.el-color-picker--small) {
    height: 32px;
    .el-color-picker__trigger {
        width: 32px;
        height: 32px;
        border-radius: 4px;
        border: 1px solid #f0e8d0;
        background-color: #fff8e8;
    }
}

// 选择器
:deep(.el-select--small) {
    --el-select-border-color: #f0e8d0;
    --el-select-hover-border-color: #f0c880;
    --el-select-text-color: #8a7d5e;
    --el-select-placeholder-color: #a09880;
    .el-input__inner {
        height: 32px;
        line-height: 32px;
        background-color: #fff8e8;
    }
    .el-input__icon {
        line-height: 32px;
        color: #8a7d5e;
    }
}

// 输入数字
:deep(.el-input-number--small) {
    --el-input-border-color: #f0e8d0;
    --el-input-hover-border-color: #f0c880;
    .el-input__inner {
        height: 32px;
        line-height: 32px;
        background-color: #fff8e8;
        color: #8a7d5e;
    }
    .el-input-number__decrease, .el-input-number__increase {
        border-color: #f0e8d0;
        background-color: #fff8e8;
        color: #8a7d5e;
        &:hover {
            background-color: #f9eeda;
            border-color: #f0c880;
        }
    }
}

// 开关
:deep(.el-switch) {
    --el-switch-on-color: #f0c880;
    --el-switch-off-color: #f0e8d0;
    --el-switch-on-text-color: #8a7d5e;
    --el-switch-off-text-color: #a09880;
    &.ml8 {
        margin-left: 12px;
    }
}

// 按钮
:deep(.el-button) {
    --el-button-bg-color: #f5dca8;
    --el-button-border-color: #f0c880;
    --el-button-text-color: #8a7d5e;
    --el-button-hover-bg-color: #f0c880;
    --el-button-hover-text-color: #7a6d4e;
    --el-button-disabled-bg-color: #f9eeda;
    --el-button-disabled-border-color: #f5dca8;
    --el-button-disabled-text-color: #a09880;
    border-radius: 22px !important;
    &.el-button--primary {
        --el-button-bg-color: #f5dca8;
        --el-button-border-color: #f0c880;
        --el-button-text-color: #8a7d5e;
    }
}

/* 工作区样式 */
.workspace {
    display: flex;
    flex: 1;
    gap: 16px;
    @media (max-width: 992px) {
        flex-direction: column;
    }
}

/* 侧边栏样式 */
.sidebar {
    width: 260px;
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 16px;
    @media (max-width: 992px) {
        width: 100%;
    }
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

.row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;
    span {
        color: #8a7d5e;
        font-size: 14px;
    }
    :deep(.el-input-number) {
        width: 100px;
    }
}

.divider {
    height: 1px;
    background-color: #f0e8d0;
    margin: 15px 0;
}

.status {
    display: flex;
    flex-direction: column;
    gap: 8px;
    color: #8a7d5e;
    font-size: 13px;
    span {
        display: flex;
        align-items: center;
        gap: 6px;
    }
}

.color-dot {
    display: inline-block;
    width: 16px;
    height: 16px;
    border-radius: 50%;
    border: 1px solid #f0e8d0;
    vertical-align: middle;
}

/* 画布区域样式 */
.canvas-wrap {
    flex: 1;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    min-height: 60vh;
    padding: 20px;
    position: relative;
}

.design-canvas {
    max-width: 100%;
    height: auto;
    background-color: #fffdf8;
    box-shadow: 0 2px 8px rgba(240, 224, 180, 0.1);
    border-radius: 4px;
    border: 1px dashed #f0e8d0;
}

/* 浮动调色板样式 */
.floating-palette {
    position: absolute;
    right: 20px;
    bottom: 20px;
    display: flex;
    gap: 10px;
    padding: 10px 15px;
    border-radius: 20px;
    box-shadow: 0 6px 20px rgba(240, 224, 180, 0.15);
    background-color: #fff8e8;
    border: 1px solid #f0e8d0;
}

.swatch {
    width: 24px;
    height: 24px;
    border-radius: 50%;
    border: 1px solid #f0e8d0;
    cursor: pointer;
    transition: transform 0.2s ease-in-out, box-shadow 0.2s ease-in-out;
    &:hover {
        transform: scale(1.1);
        box-shadow: 0 0 0 3px rgba(240, 200, 128, 0.3);
    }
}

/* 页脚状态样式 */
.footer-status {
    display: flex;
    align-items: center;
    justify-content: space-around;
    padding: 12px 20px;
    font-size: 13px;
    color: #8a7d5e;
    flex-wrap: wrap;
    gap: 8px 20px;
}

/* 全局滚动条样式 - 统一手绘风格 */
::-webkit-scrollbar {
    width: 8px;
    height: 8px;
}
::-webkit-scrollbar-track {
    background-color: #fff8e8;
    border-radius: 4px;
}
::-webkit-scrollbar-thumb {
    background-color: #f5dca8;
    border-radius: 4px;
    border: 1px solid #fff8e8;
}

/* 小屏幕适配 */
@media (max-width: 768px) {
    .toolbar {
        flex-direction: column;
        align-items: flex-start;
        .left-tools, .right-tools {
            width: 100%;
            justify-content: space-around;
            margin-bottom: 10px;
            &:last-child {
                margin-bottom: 0;
            }
        }
        .ml8 {
            margin-left: 0;
            margin-right: 8px;
        }
    }
    .pro-design {
        padding: 10px;
    }
    .card {
        margin-bottom: 10px;
    }
    .sidebar {
        padding: 15px;
        .row {
            flex-direction: column;
            align-items: flex-start;
            gap: 5px;
            span { margin-bottom: 5px; }
        }
    }
    .canvas-wrap {
        padding: 15px;
        min-height: 50vh;
    }
    .footer-status {
        flex-direction: column;
        align-items: flex-start;
        gap: 8px;
    }
}
</style>