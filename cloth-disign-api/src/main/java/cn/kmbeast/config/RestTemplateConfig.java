package cn.kmbeast.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import java.net.InetSocketAddress;
import java.net.Proxy;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        // 你可以在这里进行RestTemplate的进一步配置，如超时设置等
        return new RestTemplate();
    }

    @Bean(name = "restTemplateWithTimeoutAndProxy")
    public RestTemplate restTemplateWithTimeoutAndProxy(
            @Value("${http.proxy.host:}") String proxyHost,
            @Value("${http.proxy.port:0}") int proxyPort
    ) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        if (proxyHost != null && !proxyHost.isEmpty() && proxyPort > 0) {
            factory.setProxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        }
        return new RestTemplate(factory);
    }
}
