package cn.kmbeast.mapper;

import cn.kmbeast.pojo.dto.query.extend.FabricQueryDto;
import cn.kmbeast.pojo.entity.Fabric;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;

@Mapper
public interface FabricMapper {

    /**
     * 分页查询面料列表
     * @param queryDto 查询条件
     * @return 面料列表
     */
    List<Fabric> query(FabricQueryDto queryDto);

    /**
     * 查询符合条件的面料总数
     * @param queryDto 查询条件
     * @return 总数
     */
    Integer queryCount(FabricQueryDto queryDto);

    /**
     * 新增面料
     * @param fabric 面料实体
     * @return 影响行数
     */
    int insert(Fabric fabric);

    /**
     * 更新面料信息
     * @param fabric 面料实体
     * @return 影响行数
     */
    int update(Fabric fabric);

    /**
     * 根据ID删除面料
     * @param id 面料ID
     * @return 影响行数
     */
    int deleteById(Long id);

    /**
     * 根据ID查询面料
     * @param id 面料ID
     * @return 面料实体
     */
    Fabric selectById(Long id);

    /**
     * 获取所有面料类型（去重）
     * @return 面料类型列表
     */
    List<String> selectAllTypes();
}

