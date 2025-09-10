package cn.kmbeast.service;

import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.FabricQueryDto;
import cn.kmbeast.pojo.entity.Fabric;
import cn.kmbeast.pojo.vo.FabricLibraryVO;
import java.util.List;

public interface FabricService {
    /**
     * 分页查询面料列表
     */
    Result<FabricLibraryVO> query(FabricQueryDto queryDto);

    /**
     * 新增面料
     */
    Result<Fabric> save(Fabric fabric);

    /**
     * 更新面料信息
     */
    Result<Fabric> update(Fabric fabric);

    /**
     * 根据ID删除面料
     */
    Result<Boolean> delete(Long id);

    /**
     * 根据ID查询面料详情
     */
    Result<Fabric> getById(Long id);

    /**
     * 获取所有面料类型
     */
    Result<List<String>> getAllTypes();
}
