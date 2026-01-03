package cn.kmbeast.controller;

import cn.kmbeast.service.DressupService;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.TryOnRequestDTO;
import cn.kmbeast.utils.IdFactoryUtil;
import cn.kmbeast.utils.PathUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.io.IOException;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets; // 引入用于字符串转字节数组的编码
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/dressup")
public class DressupController {

    private final DressupService dressupService;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${my-server.api-context-path}")
    private String API;

    private final RestTemplate restTemplate;

    @Value("${openai.api.base:https://api.openai.com}")
    private String openaiBase;

    public DressupController(DressupService dressupService, RestTemplate restTemplateWithTimeoutAndProxy) {
        this.dressupService = dressupService;
        this.restTemplate = restTemplateWithTimeoutAndProxy;
    }

    /**
     * 新增接口：进行虚拟试衣。
     * 接收用户图片和服装图片，调用服务层处理，并返回合成后的图片。
     *
     * @param userImage 用户图片文件
     * @param clothImage 服装图片文件
     * @return 合成后的图片（作为字节流返回）
     */
    @PostMapping("/try-on")
    public ResponseEntity<byte[]> performTryOn(
            @RequestParam("userImage") MultipartFile userImage,
            @RequestParam("clothImage") MultipartFile clothImage) {
        try {
            byte[] resultImageBytes = dressupService.performTryOn(userImage, clothImage);

            // 返回合成后的图片，设置合适的 Content-Type
            HttpHeaders headers = new HttpHeaders();
            // 假设返回的图片是JPEG格式，你也可以根据实际情况设置为 PNG 或从外部服务获取
            headers.setContentType(MediaType.IMAGE_JPEG);

            return new ResponseEntity<>(resultImageBytes, headers, HttpStatus.OK);
        } catch (IllegalArgumentException e) {
            // 处理请求参数错误
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN); // 设置为纯文本类型
            // 将错误信息字符串转换为 UTF-8 字节数组
            return new ResponseEntity<>(e.getMessage().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.BAD_REQUEST);
        } catch (IOException e) {
            // 处理文件读写错误
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN); // 设置为纯文本类型
            String errorMessage = "文件处理失败: " + e.getMessage();
            // 将错误信息字符串转换为 UTF-8 字节数组
            return new ResponseEntity<>(errorMessage.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        } catch (RuntimeException e) {
            // 处理服务层抛出的其他运行时错误 (如外部API调用失败)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN); // 设置为纯文本类型
            // 将错误信息字符串转换为 UTF-8 字节数组
            return new ResponseEntity<>(e.getMessage().getBytes(StandardCharsets.UTF_8), headers, HttpStatus.BAD_GATEWAY); // BAD_GATEWAY 适合表示外部服务问题
        } catch (Exception e) {
            // 捕获其他未预期错误
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_PLAIN); // 设置为纯文本类型
            String errorMessage = "服务器内部错误: " + e.getMessage();
            // 将错误信息字符串转换为 UTF-8 字节数组
            return new ResponseEntity<>(errorMessage.getBytes(StandardCharsets.UTF_8), headers, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * 与前端当前实现匹配的 JSON 接口：/dressup/process
     * 接收人物与服装图片的 URL，当前先返回占位效果（直接返回人物图 URL）。
     * 如需调用真实 AI 服务，请在此处集成并返回生成后的图片 URL。
     */
    @PostMapping("/process")
    public Result<String> processTryOn(@RequestBody TryOnRequestDTO request) {
        if (request == null || request.getPersonImage() == null || request.getClothingImage() == null) {
            return Result.error("参数错误：personImage/clothingImage 不能为空");
        }
        try {
            // 1) 调用 OpenAI 生成图片（以描述性 prompt 合成人物与服装）
            restTemplate.setUriTemplateHandler(new DefaultUriBuilderFactory(openaiBase));

            String prompt = String.format(
                    "Compose a realistic try-on image: put the clothing from %s on the person in %s, keep body proportion, natural lighting, seamless blending, photorealistic result.",
                    request.getClothingImage(), request.getPersonImage());

            Map<String, Object> body = new java.util.HashMap<>();
            body.put("model", "gpt-image-1");
            body.put("prompt", prompt);
            body.put("size", "1024x1024");
            body.put("n", 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            org.springframework.http.HttpEntity<Map<String, Object>> entity = new org.springframework.http.HttpEntity<>(body, headers);
            ResponseEntity<String> aiResp = restTemplate.postForEntity("/v1/images/generations", entity, String.class);
            if (!aiResp.getStatusCode().is2xxSuccessful() || aiResp.getBody() == null) {
                return Result.error("调用AI服务失败");
            }

            // 2) 解析返回的 base64
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(aiResp.getBody());
            JsonNode dataArr = root.path("data");
            if (!dataArr.isArray() || dataArr.size() == 0) {
                return Result.error("AI未返回图片数据");
            }
            String base64 = dataArr.get(0).path("b64_json").asText();
            if (base64 == null || base64.isEmpty()) {
                return Result.error("AI图片数据为空");
            }

            // 3) 保存到本地 pic 目录
            byte[] imageBytes = java.util.Base64.getDecoder().decode(base64);
            String uuid = IdFactoryUtil.getFileId();
            String fileName = uuid + "-tryon.png";
            File fileDir = new File(PathUtils.getClassLoadRootPath() + "/pic");
            if (!fileDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                fileDir.mkdirs();
            }
            File out = new File(fileDir, fileName);
            try (FileOutputStream fos = new FileOutputStream(out)) {
                fos.write(imageBytes);
            }

            // 4) 返回可访问 URL（与 FileController 规则保持一致）
            String url = "http://localhost:21090" + API + "/file/getFile?fileName=" + fileName;
            return Result.success(url);
        } catch (Exception ex) {
            return Result.error("处理失败: " + ex.getMessage());
        }
    }

    /**
     * 保存到相册，占位实现
     */
    @PostMapping("/save-to-gallery")
    public Result<Boolean> saveToGallery(@RequestBody Map<String, Object> body) {
        // 在此持久化保存结果（占位：直接返回成功）
        return Result.success(true);
    }

    /**
     * 历史记录，占位实现
     */
    @GetMapping("/history")
    public Result<List<Map<String, Object>>> history() {
        // 返回空列表占位
        return Result.success(new ArrayList<>());
    }
}
