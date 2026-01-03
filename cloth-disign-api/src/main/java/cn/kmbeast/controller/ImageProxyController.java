package cn.kmbeast.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

@RestController
@RequestMapping("/dressup") // 核心改：从/proxy改成/dressup（已放行）
@CrossOrigin("*")
public class ImageProxyController {

    // 读取配置文件中的HTTP代理配置
    @Value("${http.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${http.proxy.port:7890}")
    private int proxyPort;

    @Value("${http.proxy.enabled:true}")
    private boolean proxyEnabled;

    // 图片代理接口（路径改成/dressup/image）
    @GetMapping("/image")
    public void proxyImage(@RequestParam String url, HttpServletResponse response) {
        // 原有代理逻辑完全不变！只改了@RequestMapping和@GetMapping的路径
        HttpURLConnection conn = null;
        try {
            // 1. 禁用SSL校验
            SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                @Override
                public X509Certificate[] getAcceptedIssuers() { return null; }
            }}, new SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            // 2. 配置HTTP代理
            if (proxyEnabled) {
                System.setProperty("http.proxyHost", proxyHost);
                System.setProperty("http.proxyPort", String.valueOf(proxyPort));
                System.setProperty("https.proxyHost", proxyHost);
                System.setProperty("https.proxyPort", String.valueOf(proxyPort));
                System.out.println("启用HTTP代理：" + proxyHost + ":" + proxyPort);
            }

            // 3. 请求Coze图片
            URL cozeUrl = new URL(url);
            conn = (HttpURLConnection) cozeUrl.openConnection();
            if (conn instanceof HttpsURLConnection && proxyEnabled) {
                ((HttpsURLConnection) conn).setSSLSocketFactory(sslContext.getSocketFactory());
            }

            conn.setRequestMethod("GET");
            conn.setConnectTimeout(8000);
            conn.setReadTimeout(8000);
            conn.setDoInput(true);

            // 4. 模拟前端请求头
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
            conn.setRequestProperty("Referer", "https://www.coze.cn");
            conn.setRequestProperty("Accept", "image/webp,image/png,image/jpeg,image/gif,*/*");
            conn.setRequestProperty("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
            conn.setRequestProperty("Cache-Control", "no-cache");
            conn.setRequestProperty("Pragma", "no-cache");

            // 5. 打印日志
            int responseCode = conn.getResponseCode();
            System.out.println("Coze响应码：" + responseCode + "，URL：" + url);
            System.out.println("Coze响应类型：" + conn.getContentType());

            // 6. 透传响应头
            response.setContentType(conn.getContentType());
            if (conn.getContentLength() > 0) {
                response.setContentLength(conn.getContentLength());
            }

            // 7. 流拷贝
            InputStream in = conn.getInputStream();
            OutputStream out = response.getOutputStream();
            byte[] buffer = new byte[4096];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
                out.flush();
            }
            in.close();
            out.close();

        } catch (Exception e) {
            response.setStatus(500);
            System.out.println("代理失败，URL：" + url);
            e.printStackTrace();
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
            if (proxyEnabled) {
                System.clearProperty("http.proxyHost");
                System.clearProperty("http.proxyPort");
                System.clearProperty("https.proxyHost");
                System.clearProperty("https.proxyPort");
            }
        }
    }
}
