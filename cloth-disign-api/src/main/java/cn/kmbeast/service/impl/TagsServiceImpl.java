package cn.kmbeast.service.impl;
//
//import cn.kmbeast.mapper.TagsMapper;
//import cn.kmbeast.pojo.api.ApiResult;
//import cn.kmbeast.pojo.api.PageResult;
//import cn.kmbeast.pojo.api.Result;
//import cn.kmbeast.pojo.dto.query.extend.TagsQueryDto;
//import cn.kmbeast.pojo.entity.Tags;
//import cn.kmbeast.service.TagsService;
//import org.springframework.stereotype.Service;
//
//import javax.annotation.Resource;
//import java.util.List;
//
///**
// * 标签业务逻辑实现
// */
//@Service
//public class TagsServiceImpl implements TagsService {
//
//    @Resource
//    private TagsMapper tagsMapper;
//
//    /**
//     * 标签新增
//     *
//     * @param tags 参数
//     * @return Result<Void>
//     */
//    @Override
//    public Result<Void> save(Tags tags) {
//        tagsMapper.save(tags);
//        return ApiResult.success();
//    }
//
//    /**
//     * 标签删除
//     *
//     * @param ids 参数
//     * @return Result<Void>
//     */
//    @Override
//    public Result<Void> batchDelete(List<Long> ids) {
//        tagsMapper.batchDelete(ids);
//        return ApiResult.success();
//    }
//
//    /**
//     * 标签修改
//     *
//     * @param tags 参数
//     * @return Result<Void>
//     */
//    @Override
//    public Result<Void> update(Tags tags) {
//        tagsMapper.update(tags);
//        return ApiResult.success();
//    }
//
//    /**
//     * 标签查询
//     *
//     * @param tagsQueryDto 查询参数
//     * @return Result<List < Tags>>
//     */
//    @Override
//    public Result<List<Tags>> query(TagsQueryDto tagsQueryDto) {
//        List<Tags> tagsList = tagsMapper.query(tagsQueryDto);
//        Integer totalCount = tagsMapper.queryCount(tagsQueryDto);
//        return PageResult.success(tagsList, totalCount);
//    }
//
//
//}

import cn.kmbeast.mapper.TagsMapper;
import cn.kmbeast.pojo.api.ApiResult;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.TagsQueryDto;
import cn.kmbeast.pojo.entity.Tags;
import cn.kmbeast.service.TagsService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 标签业务逻辑实现（含缓存功能）
 */
@Service
public class TagsServiceImpl implements TagsService {

    @Resource
    private TagsMapper tagsMapper;

    /**
     * 标签新增 - 新增后清除相关缓存
     */
    @Override
    @CacheEvict(value = "tagsCache", allEntries = true)
    public Result<Void> save(Tags tags) {
        tagsMapper.save(tags);
        return ApiResult.success();
    }

    /**
     * 标签删除 - 删除后清除相关缓存
     */
    @Override
    @CacheEvict(value = "tagsCache", allEntries = true)
    public Result<Void> batchDelete(List<Long> ids) {
        tagsMapper.batchDelete(ids);
        return ApiResult.success();
    }

    /**
     * 标签修改 - 修改后清除相关缓存
     */
    @Override
    @CacheEvict(value = "tagsCache", allEntries = true)
    public Result<Void> update(Tags tags) {
        tagsMapper.update(tags);
        return ApiResult.success();
    }

    /**
     * 标签查询 - 结果放入缓存
     * 使用查询条件生成缓存键，相同条件的查询直接取缓存
     */
    @Override
    @Cacheable(value = "tagsCache",
            key = "'tags_' + #tagsQueryDto.name + '_' + (#tagsQueryDto.pageNum?:1) + '_' + (#tagsQueryDto.pageSize?:10)")
    public Result<List<Tags>> query(TagsQueryDto tagsQueryDto) {
        // 确保分页参数有默认值
        if (tagsQueryDto.getPageNum() == null) {
            tagsQueryDto.setPageNum(1);
        }
        if (tagsQueryDto.getPageSize() == null) {
            tagsQueryDto.setPageSize(10);
        }

        List<Tags> tagsList = tagsMapper.query(tagsQueryDto);
        Integer totalCount = tagsMapper.queryCount(tagsQueryDto);
        return Result.success(tagsList, totalCount);
    }
}
//import cn.kmbeast.mapper.TagsMapper;
//import cn.kmbeast.pojo.api.Result;
//import cn.kmbeast.pojo.dto.query.extend.TagsQueryDto;
//import cn.kmbeast.pojo.entity.Tags;
//import cn.kmbeast.service.TagsService;
//import org.springframework.stereotype.Service;
//import javax.annotation.Resource;
//import java.util.List;
//import java.util.concurrent.TimeUnit;
//import cn.kmbeast.utils.RedisUtils;
//
//@Service
//public class TagsServiceImpl implements TagsService {
//    @Resource
//    private RedisUtils redisUtils;
//    @Resource
//    private TagsMapper tagsMapper;
//
//    @Override
//    public Result<Void> save(Tags tags) {
//        return null;
//    }
//
//    @Override
//    public Result<Void> batchDelete(List<Long> ids) {
//        return null;
//    }
//
//    @Override
//    public Result<Void> update(Tags tags) {
//        return null;
//    }
//
//    @Override
//    public Result<List<Tags>> query(TagsQueryDto tagsQueryDto) {
//        // 构建缓存key
//        String cacheKey = "tags_query_" + tagsQueryDto.hashCode();
//
//        // 先查缓存
//        List<Tags> cacheResult = (List<Tags>) redisUtils.get(cacheKey);
//        if (cacheResult != null) {
//            log.info("从Redis获取标签数据: key={}", cacheKey);
//            return Result.success(cacheResult);
//        }
//
//        // 缓存未命中，查数据库
//        List<Tags> dbResult = tagsMapper.query(tagsQueryDto);
//        if (dbResult != null && !dbResult.isEmpty()) {
//            // 写入缓存（设置30分钟过期）
//            redisUtils.set(cacheKey, dbResult, 30, TimeUnit.MINUTES);
//            log.info("写入Redis缓存: key={}, size={}", cacheKey, dbResult.size());
//        }
//
//        return Result.success(dbResult);
//    }
//}
