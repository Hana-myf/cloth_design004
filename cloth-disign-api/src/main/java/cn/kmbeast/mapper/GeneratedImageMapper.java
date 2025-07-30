package cn.kmbeast.mapper;

import cn.kmbeast.pojo.dto.query.extend.GeneratedImageQueryDto;
import cn.kmbeast.pojo.entity.GeneratedImage;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface GeneratedImageMapper {
    List<GeneratedImage> query(GeneratedImageQueryDto queryDto);
    Integer queryCount(GeneratedImageQueryDto queryDto);
    int insert(GeneratedImage image);
}
