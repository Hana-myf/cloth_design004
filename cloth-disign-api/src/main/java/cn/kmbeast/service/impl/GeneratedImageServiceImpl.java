package cn.kmbeast.service.impl;

import cn.kmbeast.mapper.GeneratedImageMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.GeneratedImageQueryDto;
import cn.kmbeast.pojo.entity.GeneratedImage;
import cn.kmbeast.service.GeneratedImageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Service
public class GeneratedImageServiceImpl implements GeneratedImageService {

    @Resource
    private GeneratedImageMapper generatedImageMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<GeneratedImage> save(GeneratedImage image) {
        System.out.println("接收到保存请求，参数：" + image);
        try {
            if (image.getGenerateTime() == null) {
                image.setGenerateTime(new Date());
            }
            int rows = generatedImageMapper.insert(image);
            System.out.println("插入数据库影响行数：" + rows);
            if (rows > 0) {
                return ApiResult.success(image);
            }
            return ApiResult.error("图片保存失败：数据库插入失败");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("图片保存失败：" + e.getMessage());
        }
    }

    @Override
    // 临时注释缓存注解：避免缓存旧数据（关键修改）
    // @Cacheable(value = "generatedImageCache",
    //         key = "'generatedImage_' + #queryDto.userId + '_' + #queryDto.imageName + '_' + (#queryDto.pageNum?:1) + '_' + (#queryDto.pageSize?:10)")
    public Result<List<GeneratedImage>> query(GeneratedImageQueryDto queryDto) {
        // 1. 完善参数默认值，确保分页有效
        if (queryDto.getPageNum() == null) {
            queryDto.setPageNum(1);
        }
        if (queryDto.getPageSize() == null) {
            queryDto.setPageSize(4); // 前端只显示4张，默认查4条
        }

        // 2. 打印查询参数（关键：确认userId是否正确）
        System.out.println("查询参数：userId=" + queryDto.getUserId()
                + ", pageNum=" + queryDto.getPageNum()
                + ", pageSize=" + queryDto.getPageSize());

        // 3. 执行查询并打印结果
        List<GeneratedImage> imageList = generatedImageMapper.query(queryDto);
        System.out.println("查询到的图片数量：" + imageList.size());
        // 打印每条数据的关键信息，确认是否有值
        imageList.forEach(img -> {
            System.out.println("图片ID：" + img.getId()
                    + ", URL：" + img.getImageUrl()
                    + ", 用户ID：" + img.getUserId());
        });

        Integer totalCount = generatedImageMapper.queryCount(queryDto);
        System.out.println("总记录数：" + totalCount);

        // 4. 确保返回格式正确
        return ApiResult.success(imageList, totalCount);
    }
}
