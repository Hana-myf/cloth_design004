<template>
  <div class="line-main">
    <div>
      <span class="tag">{{ tag }}</span>
      <span class="time-show">
        <span class="top-bar" style="font-size: 12px;">时间选择</span>
        <el-select size="mini" style="width: 90px;" v-model="selectedValue" @change="handleTimeChange">
          <el-option v-for="item in timeOptions" :key="item.value" :label="item.label" :value="item.value" />
        </el-select>
      </span>
    </div>
    <div ref="chart" :style="{ width: '100%', height: height }"></div>
  </div>
</template>

<script>
import * as echarts from 'echarts';

export default {
  name: 'TagLine',
  props: {
    tag: { type: String, required: true },
    height: { type: String, default: '220px' },
    chartType: {
      type: String,
      required: true
    }
  },
  data() {
    return {
      chart: null,
      timeOptions: [
        { value: 7, label: '7天' },
        { value: 30, label: '30天' },
        { value: 90, label: '90天' }
      ],
      selectedValue: 30,
      dates: [],
      dataValues: []
    }
  },
  watch: {
    selectedValue() {
      this.generateChartData();
    }
  },
  mounted() {
    this.generateChartData();
  },
  methods: {
    generateChartData() {
      // 生成日期范围
      const endDate = new Date();
      const startDate = new Date();
      startDate.setDate(endDate.getDate() - this.selectedValue);
      
      this.dates = this.generateDateRange(startDate, endDate);
      
      // 根据图表类型生成模拟数据
      if (this.chartType === 'news') {
        this.generateNewsData();
      } else if (this.chartType === 'conference') {
        this.generateConferenceData();
      }
      
      this.initChart();
    },
    
    generateNewsData() {
      // 行业新闻模拟数据：16号3个，17号2个，19、21、23号各1个，27号2个
      const newsDataMap = {
        '16': 3,
        '17': 2,
        '19': 1,
        '21': 1,
        '23': 1,
        '27': 2
      };
      
      this.dataValues = this.dates.map(date => {
        const day = date.split('-')[2]; // 提取日期中的日
        return newsDataMap[day] || 0;
      });
    },
    
    generateConferenceData() {
      // 会议模拟数据：16号1个，17号2个，18、20、24号各1个，28号2个
      const conferenceDataMap = {
        '16': 1,
        '17': 2,
        '18': 1,
        '20': 1,
        '24': 1,
        '28': 2
      };
      
      this.dataValues = this.dates.map(date => {
        const day = date.split('-')[2]; // 提取日期中的日
        return conferenceDataMap[day] || 0;
      });
    },
    
    generateDateRange(startDate, endDate) {
      const dates = [];
      const currentDate = new Date(startDate);
      
      while (currentDate <= endDate) {
        const year = currentDate.getFullYear();
        const month = (currentDate.getMonth() + 1).toString().padStart(2, '0');
        const day = currentDate.getDate().toString().padStart(2, '0');
        dates.push(`${year}-${month}-${day}`);
        currentDate.setDate(currentDate.getDate() + 1);
      }
      
      return dates;
    },
    
    initChart() {
      if (!this.$refs.chart) return;
      
      // 销毁旧图表实例
      if (this.chart) {
        this.chart.dispose();
      }
      
      this.chart = echarts.init(this.$refs.chart);
      const option = {
        grid: {
          left: 40,
          right: 20,
          top: 50,
          bottom: 30
        },
        title: {
          text: this.tag,
          left: 'center',
          textStyle: {
            color: '#333',
            fontSize: 14
          }
        },
        tooltip: {
          trigger: 'axis',
          formatter: params => {
            return `${params[0].axisValue}<br/>数量: ${params[0].value}`;
          }
        },
        xAxis: {
          type: 'category',
          data: this.dates,
          axisLabel: {
            rotate: 45,
            fontSize: 10,
            formatter: value => {
              return value.split('-')[1] + '-' + value.split('-')[2]; // 显示月-日
            }
          }
        },
        yAxis: {
          type: 'value',
          minInterval: 1
        },
        series: [{
          name: this.tag,
          type: 'line',
          smooth: true,
          data: this.dataValues,
          symbol: 'circle',
          symbolSize: 6,
          itemStyle: {
            color: this.chartType === 'news' ? '#5B8FF9' : '#61DDAA'
          },
          lineStyle: {
            width: 2,
            color: this.chartType === 'news' ? '#5B8FF9' : '#61DDAA'
          },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: this.chartType === 'news' ? 'rgba(91, 143, 249, 0.3)' : 'rgba(97, 221, 170, 0.3)' },
              { offset: 1, color: this.chartType === 'news' ? 'rgba(91, 143, 249, 0)' : 'rgba(97, 221, 170, 0)' }
            ])
          },
          label: {
            show: true,
            position: 'top',
            fontSize: 10
          }
        }]
      };
      
      this.chart.setOption(option);
      
      // 窗口大小变化时重新调整图表大小
      window.addEventListener('resize', this.handleResize);
    },
    
    handleResize() {
      this.chart?.resize();
    },
    
    handleTimeChange() {
      this.$emit('timeChange', this.selectedValue);
    }
  },
  beforeDestroy() {
    window.removeEventListener('resize', this.handleResize);
    this.chart?.dispose();
  }
}
</script>

<style scoped lang="scss">
.line-main {
  margin-bottom: 5px;
  border-radius: 3px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
  background-color: #fff;
  padding: 10px;

  .tag {
    font-size: 16px;
    padding: 15px 6px;
    display: inline-block;
    color: #333;
    font-weight: bold;
  }

  .time-show {
    padding: 15px 6px;
    float: right;
  }
}
</style>