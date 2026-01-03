package cn.kmbeast.service.impl;

// cn.kmbeast.service.impl.DressupServiceImpl.java


import cn.kmbeast.service.DressupService; // 导入接口
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * DressupService 接口的实现类。
 * 负责与外部AI试衣服务进行交互。
 */
@Service // 标记为一个Spring服务组件，会被Spring自动扫描并注册
public class DressupServiceImpl implements DressupService { // 实现 DressupService 接口

    // 假设这是你外部AI试衣服务的URL，从 application.yml 中读取
    @Value("${dressup.tryon.api.url}")
    private String tryOnApiUrl;

    private final RestTemplate restTemplate;

    // 构造函数注入 RestTemplate
    public DressupServiceImpl(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * @inheritDoc
     */
    @Override // 明确表示实现接口方法
    public byte[] performTryOn(MultipartFile userImage, MultipartFile clothImage) throws IOException {
        if (userImage == null || userImage.isEmpty() || clothImage == null || clothImage.isEmpty()) {
            throw new IllegalArgumentException("用户图片和服装图片不能为空。");
        }

        // 构建请求头
        HttpHeaders headers = new HttpHeaders();
        // 外部API通常接受 multipart/form-data，所以设置 Content-Type
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        // 构建请求体
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        // 假设外部API需要的文件参数名为 "user_image" 和 "cloth_image"
        // 注意：这里需要将MultipartFile转换为合适的Resource或ByteArrayResource
        body.add("user_image", new ByteArrayResource(userImage.getBytes()) {
            @Override
            public String getFilename() {
                return userImage.getOriginalFilename();
            }
        });
        body.add("cloth_image", new ByteArrayResource(clothImage.getBytes()) {
            @Override
            public String getFilename() {
                return clothImage.getOriginalFilename();
            }
        });

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

        try {
            // 调用外部AI服务
            // 假设外部服务直接返回图片二进制数据
            ResponseEntity<byte[]> response = restTemplate.postForEntity(tryOnApiUrl, requestEntity, byte[].class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return response.getBody();
            } else {
                // 处理非成功的HTTP状态码
                String errorMessage = "调用外部试衣API失败，状态码：" + response.getStatusCode() + ", 响应体：" +
                        (response.hasBody() && response.getBody() != null ? new String(response.getBody()) : "无");
                throw new RuntimeException(errorMessage);
            }
        } catch (Exception e) {
            // 捕获 RestTemplate 抛出的异常（如连接超时，目标服务不可用等）
            throw new RuntimeException("调用外部试衣API时发生错误: " + e.getMessage(), e);
        }
    }

    // 你可能还有其他 DressupService 接口方法的实现...
}
