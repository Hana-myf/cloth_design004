package cn.kmbeast.controller;

import cn.kmbeast.aop.Pager;
import cn.kmbeast.pojo.api.Result;
import cn.kmbeast.pojo.dto.query.extend.FabricQueryDto;
import cn.kmbeast.pojo.entity.Fabric;
import cn.kmbeast.pojo.vo.FabricLibraryVO;
import cn.kmbeast.service.FabricService;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import java.util.List;

@RestController
@RequestMapping(value = "/fabric")
public class FabricController {

    @Resource
    private FabricService fabricService;

    /**
     * 分页查询面料列表
     * 使用@Pager注解处理分页，接收查询条件
     */
    @Pager
    @PostMapping(value = "/query")
    public Result<FabricLibraryVO> query(@RequestBody FabricQueryDto queryDto) {
        return fabricService.query(queryDto);
    }

    /**
     * 新增面料
     */
    @PostMapping(value = "/save")
    public Result<Fabric> save(@RequestBody Fabric fabric) {
        return fabricService.save(fabric);
    }

    /**
     * 更新面料信息
     */
    @PostMapping(value = "/update")
    public Result<Fabric> update(@RequestBody Fabric fabric) {
        return fabricService.update(fabric);
    }

    /**
     * 根据ID删除面料
     */
    @PostMapping(value = "/delete/{id}")
    public Result<Boolean> delete(@PathVariable Long id) {
        return fabricService.delete(id);
    }

    /**
     * 根据ID查询单个面料详情
     */
    @GetMapping(value = "/get/{id}")
    public Result<Fabric> getById(@PathVariable Long id) {
        return fabricService.getById(id);
    }

    /**
     * 获取所有面料类型（用于前端筛选下拉框）
     */
    @GetMapping(value = "/types")
    public Result<List<String>> getAllTypes() {
        return fabricService.getAllTypes();
    }
}

