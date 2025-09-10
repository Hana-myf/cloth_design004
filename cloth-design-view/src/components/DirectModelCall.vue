<template>
  <!-- 该组件无需UI，仅作为逻辑封装，通过props接收参数，事件返回结果 -->
</template>

<script>
import axios from 'axios';

export default {
  name: 'DirectModelCall',
  props: {
    // 接收生成图片所需的参数
    generateParams: {
      type: Object,
      required: true,
      default: () => ({
        prompt: '',
        image: '',
        model: 'doubao-seededit-3-0-i2i-250628',
        seed: 123,
        guidanceScale: 5.5
      })
    }
  },
  data() {
    return {
      // 模型接口配置（注意：生产环境不建议在此硬编码密钥）
      modelConfig: {
        apiUrl: 'https://ark.cn-beijing.volces.com/api/v3/images/generations', // 火山引擎图片生成接口
        apiKey: 'f2390958-8c5a-424a-9021-651e920c2bc4', 
        model: 'doubao-seededit-3-0-i2i-250628'
      },
      isLoading: false
    };
  },
  methods: {
    // 核心方法：直接调用模型接口生成图片
    async callModel() {
      this.isLoading = true;
      try {
        const { prompt, image, model, seed, guidanceScale } = this.generateParams;
        
        // 1. 构建请求参数（按第三方API要求格式）
        const requestData = {
          model: model,
          prompt: prompt,
          image: image, // 图片URL（需为公网可访问地址）
          response_format: 'url',
          seed: seed,
          guidance_scale: guidanceScale,
          size: 'adaptive',
          watermark: true
        };

        // 2. 调用第三方API（直接前端请求）
        const response = await axios.post(
          this.modelConfig.apiUrl,
          requestData,
          {
            headers: {
              'Content-Type': 'application/json',
              'Authorization': `Bearer ${this.modelConfig.apiKey}`, // 按第三方要求传递密钥
              'X-Timestamp': new Date().getTime().toString() // 部分API需要时间戳
            }
          }
        );

        // 3. 处理成功响应（根据第三方返回格式调整）
        if (response.data && response.data.data && response.data.data.length > 0) {
          const imageUrl = response.data.data[0].url;
          this.$emit('generateSuccess', {
            code: 200,
            data: { imageUrl: imageUrl },
            msg: '生成成功'
          });
        } else {
          throw new Error('模型返回数据格式异常');
        }

      } catch (error) {
        // 4. 处理错误（网络错误、API错误等）
        let errorMsg = '生成失败，请重试';
        if (error.response) {
          // 第三方API返回的错误（如密钥无效、参数错误）
          errorMsg = `API错误：${error.response.data?.msg || error.response.statusText}`;
        } else if (error.message) {
          errorMsg = error.message;
        }
        this.$emit('generateError', {
          code: 500,
          msg: errorMsg
        });
      } finally {
        this.isLoading = false;
      }
    }
  }
};
</script>