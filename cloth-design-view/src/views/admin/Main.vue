<template>
    <div class="dashboard-container">
        <!-- 顶部标题 -->
        <div class="dashboard-header">
            <h3 class="dashboard-title">数据仪表盘</h3>
            <div class="dashboard-actions">
                <el-button size="small" type="primary" @click="refreshData">
                    <i class="el-icon-refresh"></i> 刷新数据
                </el-button>
            </div>
        </div>
        <div class="chart-row pie-charts">
            <div class="chart-card">
                <PieChart 
                    tag="作品统计" 
                    :types="worksTypes" 
                    :values="worksValues"
                    bag="#f5f7fa" 
                    fontColor="#333"
                    height="320px"
                />
            </div>
            <div class="chart-card">
                <PieChart 
                    tag="用户统计" 
                    :types="userTypes" 
                    :values="userValues"
                    bag="#f5f7fa" 
                    fontColor="#333"
                    height="320px"
                />
            </div>
        </div>
        <div class="chart-row line-charts">
            <div class="chart-card">
                <LineChart 
                    tag="数量趋势" 
                    chartType="conference" 
                    height="320px"
                />
            </div>
            <div class="chart-card">
                <LineChart 
                    tag="新闻趋势" 
                    chartType="news" 
                    height="320px"
                />
            </div>
        </div>
    </div>
</template>

<script>
// 引入图表组件
import PieChart from '@/components/PieChart.vue';
import LineChart from '@/components/LineChart.vue';

export default {
    name: "Main",
    components: {
        PieChart,
        LineChart
    },
    data() {
        return {
            // 页面状态
            loading: false,
            // 作品统计数据
            worksTypes: ['虚拟试衣作品', '用户设计'],
            worksValues: [7, 28],
            // 用户统计数据
            userTypes: ['管理员', '普通用户', '被禁言用户'],
            userValues: [2, 10, 1]
        }
    },
    created() {
        // 页面初始化逻辑
    },
    methods: {
        // 刷新数据方法
        refreshData() {
            this.loading = true;
            // 模拟数据刷新延迟
            setTimeout(() => {
                this.loading = false;
                this.$message.success('数据刷新成功');
            }, 800);
        }
    }
};
</script>

<style scoped lang="scss">
.dashboard-container {
    padding: 20px;
    display: flex;
    flex-direction: column;
    gap: 20px;
}

.dashboard-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    
    .dashboard-title {
        font-size: 20px;
        font-weight: bold;
        color: #333;
    }
    
    .dashboard-actions {
        display: flex;
        gap: 10px;
    }
}

.chart-row {
    display: flex;
    gap: 20px;
    margin-bottom: 20px;
    
    /* 响应式布局 - 小屏幕下堆叠显示 */
    @media (max-width: 1024px) {
        flex-direction: column;
    }
    
    /* 饼图行 - 调整列宽 */
    &.pie-charts {
        .chart-card {
            flex: 1;
        }
    }
    
    /* 折线图行 - 调整列宽 */
    &.line-charts {
        .chart-card {
            flex: 1;
        }
    }
}

.chart-card {
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    overflow: hidden;
    background-color: #fff;
    transition: transform 0.3s ease;
    
    &:hover {
        transform: translateY(-5px);
    }
}

/* 优化图表容器样式 */
.pie-main {
    padding: 10px;
}

.line-main {
    padding: 10px;
}
</style>