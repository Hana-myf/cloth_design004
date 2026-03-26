package cn.kmbeast.controller;

import cn.kmbeast.aop.Protector;
import com.volcengine.ark.runtime.model.images.generation.GenerateImagesRequest;
import com.volcengine.ark.runtime.model.images.generation.ImagesResponse;
import com.volcengine.ark.runtime.model.images.generation.ResponseFormat;
import com.volcengine.ark.runtime.model.images.generation.Size;
import com.volcengine.ark.runtime.service.ArkService;
import okhttp3.ConnectionPool;
import okhttp3.Dispatcher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 图片生成接口代理控制器（完全绕开/api/前缀，避免认证）
 * 最终路径：http://localhost:21090/public/ai/generate-image
 */

@RestController
@RequestMapping("/public/ai")
public class ApiProxyController {

    @Value("${ark.api-key}")
    private String apiKey;

    private static final String SUPPORTED_MODEL = "doubao-seededit-3-0-i2i-250628";

    private ArkService getArkService() {
        ConnectionPool connectionPool = new ConnectionPool(
                5, 30, TimeUnit.SECONDS
        );
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(5);

        return ArkService.builder()
                .apiKey(apiKey)
                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .build();
    }
    @Protector
    @PostMapping("/generate-image")
    public Map<String, Object> generateImage(@RequestBody Map<String, Object> requestParams) {
        Map<String, Object> result = new HashMap<>(3);
        ArkService arkService = null;

        try {
            // 1. 参数校验
            String prompt = (String) requestParams.get("prompt");
            String imageUrl = (String) requestParams.get("image");
            String model = (String) requestParams.get("model");

            if (!StringUtils.hasText(prompt)) {
                result.put("code", 400);
                result.put("msg", "提示词（prompt）不能为空");
                return result;
            }
            if (!StringUtils.hasText(imageUrl)) {
                result.put("code", 400);
                result.put("msg", "参考图片URL（image）不能为空");
                return result;
            }
            if (!StringUtils.hasText(model) || !SUPPORTED_MODEL.equals(model)) {
                result.put("code", 400);
                result.put("msg", "不支持的模型，仅支持：" + SUPPORTED_MODEL);
                return result;
            }

            // 2. 解析可选参数
            Integer seed = (requestParams.get("seed") instanceof Number)
                    ? ((Number) requestParams.get("seed")).intValue()
                    : (int) (Math.random() * 10000);
            Double guidanceScale = (requestParams.get("guidance_scale") instanceof Number)
                    ? ((Number) requestParams.get("guidance_scale")).doubleValue()
                    : 5.5;

            // 3. 调用火山方舟API
            arkService = getArkService();
            GenerateImagesRequest generateRequest = GenerateImagesRequest.builder()
                    .model(model)
                    .prompt(prompt)
                    .image(imageUrl)
                    .responseFormat(ResponseFormat.Url)
                    .size(Size.Adaptive)
                    .guidanceScale(guidanceScale)
                    .seed(seed)
                    .watermark(true)
                    .build();

            ImagesResponse imagesResponse = arkService.generateImages(generateRequest);
            if (imagesResponse == null || imagesResponse.getData().isEmpty()) {
                throw new RuntimeException("API返回空结果");
            }

            // 4. 成功响应
            result.put("code", 200);
            result.put("msg", "生成成功");
            result.put("data", imagesResponse.getData());

        } catch (Exception e) {
            String errorMsg = "生成失败：";
            if (e.getMessage().contains("API Key")) errorMsg += "API Key无效";
            else if (e.getMessage().contains("404")) errorMsg += "模型未开通";
            else errorMsg += e.getMessage();

            result.put("code", 500);
            result.put("msg", errorMsg);
            e.printStackTrace();

        } finally {
            if (arkService != null) {
                try {
                    arkService.shutdownExecutor();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return result;
    }
}
