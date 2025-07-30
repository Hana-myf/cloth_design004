package cn.kmbeast.controller;

import cn.kmbeast.aop.Pager;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.GeneratedImageQueryDto;
import cn.kmbeast.pojo.entity.GeneratedImage;
import cn.kmbeast.service.GeneratedImageService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(value = "/generatedImage")
public class GeneratedImageController {

    @Resource
    private GeneratedImageService generatedImageService;

    @Pager
    @PostMapping(value = "/query")
    public Result<List<GeneratedImage>> query(@RequestBody GeneratedImageQueryDto queryDto) {
        return generatedImageService.query(queryDto);
    }

    // 新增保存图片的接口
    @PostMapping(value = "/save")
    public Result<GeneratedImage> save(@RequestBody GeneratedImage image) {
        return generatedImageService.save(image);
    }
}
