// vue.config.js
const bodyParser = require('body-parser');
const axios = require('axios'); // 你可能不需要 axios 来转发流了，但如果其他地方用，可以保留
const https = require('https'); // Node.js 内置的 https 模块

module.exports = {
  lintOnSave: false,
  devServer: {
    host: "localhost",
    port: 21091,
    https: false,
    overlay: { warning: false, errors: false },
    before(app) {
      app.use(bodyParser.json());

      app.post('/__coze_proxy_submit__', async (req, res) => {
        // console.log("[DEBUG 本地收到的 body]", req.body); // 打印 body 确认无误

        // 核心修改：使用 Node.js 的 http/https.request 来创建请求，并直接 pipe 响应
        const cozeReq = https.request( // 使用 https 模块，因为 Coze API 是 HTTPS
          'https://api.coze.cn/v3/chat',
          {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'Authorization': req.headers['authorization'],
              // 其他需要的头部，例如 User-Agent，如果 Coze API 要求的话
            }
          },
          cozeRes => { // cozeRes 是 Coze API 的响应流
            // 将 Coze API 的响应头转发给前端
            res.writeHead(cozeRes.statusCode, cozeRes.headers);
            // 将 Coze API 的响应流直接管道化 (pipe) 到前端的响应流
            cozeRes.pipe(res);
          }
        );

        // 处理请求体：将前端传来的 req.body 写入到 Coze API 的请求中
        cozeReq.write(JSON.stringify(req.body)); // 将 req.body 序列化为 JSON 字符串写入请求体
        cozeReq.end(); // 结束请求发送

        // 处理错误
        cozeReq.on('error', e => {
          console.error('Proxy request to Coze API failed:', e);
          res.status(500).json({ error: 'Proxy request failed', details: e.message });
        });
      });
    },

    proxy: {
      '/__coze_proxy_retrieve__': { // 这个 proxy 保持不变，因为它可能处理非流式请求
        target: 'https://api.coze.cn',
        changeOrigin: true,
        pathRewrite: {
          '^/__coze_proxy_retrieve__': '/v3/chat/retrieve'
        }
      },
      // 保持你的其他代理配置
      '/': {
        target: 'http://localhost:21090',
        changeOrigin: true,
      }
    }
  }
}