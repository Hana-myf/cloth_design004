package cn.kmbeast.service;
// cn.kmbeast.service.DressupService.java


import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * 试衣服务接口。
 * 定义了进行虚拟试衣的核心业务操作。
 */
public interface DressupService {

    /**
     * 处理试衣请求，调用外部AI服务进行图片合成。
     *
     * @param userImage 用户图片文件
     * @param clothImage 服装图片文件
     * @return 合成后的图片数据（字节数组）
     * @throws IOException 如果文件读取失败
     * @throws IllegalArgumentException 如果输入参数不合法（例如文件为空）
     * @throws RuntimeException 如果外部API调用失败或发生其他业务异常
     */
    byte[] performTryOn(MultipartFile userImage, MultipartFile clothImage) throws IOException;

    // 你可能还有其他 Dressup 相关的服务方法，例如：
    // void saveTryOnResult(String userId, byte[] imageBytes, String originalUserImageId, String originalClothImageId);
    // List<TryOnHistory> getTryOnHistory(String userId);
}
