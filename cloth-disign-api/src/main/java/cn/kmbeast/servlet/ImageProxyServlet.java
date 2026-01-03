package cn.kmbeast.servlet;

import javax.net.ssl.*;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@WebServlet("/imageProxy")
public class ImageProxyServlet extends HttpServlet {

    // 初始化时禁用SSL证书校验（全局生效）
    static {
        try {
            // 1. 创建信任所有证书的TrustManager
            TrustManager[] trustAllCerts = new TrustManager[]{
                    new X509TrustManager() {
                        @Override
                        public X509Certificate[] getAcceptedIssuers() { return null; }
                        @Override
                        public void checkClientTrusted(X509Certificate[] certs, String authType) {}
                        @Override
                        public void checkServerTrusted(X509Certificate[] certs, String authType) {}
                    }
            };

            // 2. 初始化SSL上下文，信任所有证书
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());

            // 3. 设置全局SSL工厂，忽略证书校验
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            System.out.println("===== SSL证书校验已禁用 =====");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void init() {
        System.out.println("===== ImageProxyServlet 初始化成功 =====");
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*"); // 允许所有前端域名
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "User-Agent, Referer, Accept, Content-Type");
        resp.setHeader("Access-Control-Max-Age", "3600"); // 预检缓存1小时

        // 如果是OPTIONS预检请求，直接返回200
        if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
            resp.setStatus(200);
            try {
                resp.getWriter().write("OK");
            } catch (Exception e) {}
            return;
        }
        HttpURLConnection conn = null;
        try {
            // 1. 获取前端传的URL参数
            String targetUrl = req.getParameter("url");
            if (targetUrl == null || targetUrl.isEmpty()) {
                resp.setStatus(400);
                resp.getWriter().write("参数url不能为空");
                return;
            }
            System.out.println("请求目标URL：" + targetUrl);

            // 2. 发起HTTPS请求（此时已禁用证书校验）
            URL url = new URL(targetUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(10000); // 10秒超时
            conn.setReadTimeout(10000);
            conn.setDoInput(true);

            // 3. 模拟浏览器请求头
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.coze.cn");

            // 4. 打印响应码（排查关键）
            int responseCode = conn.getResponseCode();
            System.out.println("目标URL响应码：" + responseCode);
            if (responseCode != 200) {
                resp.setStatus(responseCode);
                resp.getWriter().write("目标URL返回错误码：" + responseCode);
                return;
            }

            // 5. 透传图片给前端
            resp.setContentType(conn.getContentType());
            InputStream in = conn.getInputStream();
            OutputStream out = resp.getOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            out.flush();
            out.close();
            in.close();

        } catch (Exception e) {
            System.err.println("Servlet执行异常：");
            e.printStackTrace();
            try {
                resp.setStatus(500);
                resp.getWriter().write("服务器内部错误：" + e.getMessage());
            } catch (Exception ex) {}
        } finally {
            if (conn != null) conn.disconnect();
        }
    }
    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        resp.setHeader("Access-Control-Allow-Headers", "User-Agent, Referer, Accept, Content-Type");
        resp.setStatus(200);
    }
}
