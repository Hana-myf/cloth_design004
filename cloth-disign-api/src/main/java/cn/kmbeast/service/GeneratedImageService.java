package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.GeneratedImageQueryDto;
import cn.kmbeast.pojo.entity.GeneratedImage;
import java.util.List;

public interface GeneratedImageService {
    Result<List<GeneratedImage>> query(GeneratedImageQueryDto queryDto);
    Result<GeneratedImage> save(GeneratedImage image);
}
