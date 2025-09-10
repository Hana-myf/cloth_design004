package cn.kmbeast.controller;

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
 * 完全独立的免认证图片生成控制器
 * 不依赖任何认证注解，确保绝对不会被拦截
 */
@RestController
// 使用全新路径，与现有所有路径无冲突
@RequestMapping("/no-auth/image")
public class NoAuthImageController {

    @Value("${ark.api-key}")
    private String apiKey;

    private static final String SUPPORTED_MODEL = "doubao-seededit-3-0-i2i-250628";

    private ArkService getArkService() {
        ConnectionPool connectionPool = new ConnectionPool(5, 30, TimeUnit.SECONDS);
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequestsPerHost(5);

        return ArkService.builder()
                .apiKey(apiKey)
                .connectionPool(connectionPool)
                .dispatcher(dispatcher)
                .build();
    }

    // 不添加任何认证注解，确保不会被任何切面拦截
    @PostMapping("/generate")
    public Map<String, Object> generateImage(@RequestBody Map<String, Object> requestParams) {
        Map<String, Object> result = new HashMap<>(3);
        ArkService arkService = null;

        try {
            // 参数校验
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
            if (!SUPPORTED_MODEL.equals(model)) {
                result.put("code", 400);
                result.put("msg", "不支持的模型，仅支持：" + SUPPORTED_MODEL);
                return result;
            }

            // 解析参数
            Integer seed = requestParams.get("seed") instanceof Number
                    ? ((Number) requestParams.get("seed")).intValue()
                    : (int) (Math.random() * 10000);
            Double guidanceScale = requestParams.get("guidance_scale") instanceof Number
                    ? ((Number) requestParams.get("guidance_scale")).doubleValue()
                    : 5.5;

            // 调用API
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

            result.put("code", 200);
            result.put("msg", "生成成功");
            result.put("data", imagesResponse.getData());

        } catch (Exception e) {
            result.put("code", 500);
            result.put("msg", "生成失败：" + e.getMessage());
            e.printStackTrace();
        } finally {
            if (arkService != null) {
                try { arkService.shutdownExecutor(); }
                catch (Exception e) { e.printStackTrace(); }
            }
        }

        return result;
    }
}

