package cn.kmbeast.service.impl;

import cn.kmbeast.mapper.FabricMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.FabricQueryDto;
import cn.kmbeast.pojo.entity.Fabric;
import cn.kmbeast.pojo.vo.FabricLibraryVO;
import cn.kmbeast.pojo.vo.FabricVO;
import cn.kmbeast.service.FabricService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class FabricServiceImpl implements FabricService {

    @Resource
    private FabricMapper fabricMapper;

    @Override
    public Result<FabricLibraryVO> query(FabricQueryDto queryDto) {
        // 1. 设置分页默认值
//        if (queryDto.getCurrent() == null) {
//            queryDto.setCurrent(1);
//        }
//        if (queryDto.getSize() == null) {
//            queryDto.setSize(10); // 默认每页10条
//        }
        if (queryDto.getCurrent() == null || queryDto.getCurrent() < 1) {
            queryDto.setCurrent(1);
        }
        // 确保 size 合理（如 1-100 之间）
        if (queryDto.getSize() == null || queryDto.getSize() < 1 || queryDto.getSize() > 100) {
            queryDto.setSize(10);
        }
        // 2. 执行查询
        List<Fabric> fabricList = fabricMapper.query(queryDto);
        Integer totalCount = fabricMapper.queryCount(queryDto);

        // 3. 转换为VO（实体转VO，隐藏不必要字段）
        List<FabricVO> fabricVOList = fabricList.stream()
                .map(fabric -> FabricVO.builder()
                        .id(fabric.getId())
                        .name(fabric.getName())
                        .type(fabric.getType())
                        .imageUrl(fabric.getImageUrl())
                        .description(fabric.getDescription())
                        .createTime(fabric.getCreateTime())
                        .build())
                .collect(Collectors.toList());

        // 4. 封装返回结果（包含总数和列表）
        FabricLibraryVO resultVO = FabricLibraryVO.builder()
                .totalCount(totalCount)
                .fabricList(fabricVOList)
                .build();

        return ApiResult.success(resultVO);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Fabric> save(Fabric fabric) {
        try {
            // 1. 设置时间默认值
            Date now = new Date();
            if (fabric.getCreateTime() == null) {
                fabric.setCreateTime(now);
            }
            fabric.setUpdateTime(now);

            // 2. 执行插入
            int rows = fabricMapper.insert(fabric);
            if (rows > 0) {
                return ApiResult.success(fabric);
            }
            return ApiResult.error("面料保存失败：数据库插入失败");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("面料保存失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Fabric> update(Fabric fabric) {
        try {
            // 1. 校验ID是否存在
            if (fabric.getId() == null) {
                return ApiResult.error("面料ID不能为空");
            }
            Fabric existing = fabricMapper.selectById(fabric.getId());
            if (existing == null) {
                return ApiResult.error("面料不存在");
            }

            // 2. 更新时间
            fabric.setUpdateTime(new Date());

            // 3. 执行更新
            int rows = fabricMapper.update(fabric);
            if (rows > 0) {
                return ApiResult.success(fabricMapper.selectById(fabric.getId())); // 返回更新后的数据
            }
            return ApiResult.error("面料更新失败：数据库更新失败");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("面料更新失败：" + e.getMessage());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(Long id) {
        try {
            // 1. 校验ID是否存在
            if (id == null) {
                return ApiResult.error("面料ID不能为空");
            }
            Fabric existing = fabricMapper.selectById(id);
            if (existing == null) {
                return ApiResult.error("面料不存在");
            }

            // 2. 执行删除
            int rows = fabricMapper.deleteById(id);
            if (rows > 0) {
                return ApiResult.success(true);
            }
            return ApiResult.error("面料删除失败：数据库删除失败");
        } catch (Exception e) {
            e.printStackTrace();
            return ApiResult.error("面料删除失败：" + e.getMessage());
        }
    }

    @Override
    public Result<Fabric> getById(Long id) {
        if (id == null) {
            return ApiResult.error("面料ID不能为空");
        }
        Fabric fabric = fabricMapper.selectById(id);
        if (fabric == null) {
            return ApiResult.error("面料不存在");
        }
        return ApiResult.success(fabric);
    }

    @Override
    public Result<List<String>> getAllTypes() {
        List<String> types = fabricMapper.selectAllTypes();
        return ApiResult.success(types);
    }
}
