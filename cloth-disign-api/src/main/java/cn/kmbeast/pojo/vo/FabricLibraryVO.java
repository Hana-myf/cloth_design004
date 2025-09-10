package cn.kmbeast.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 面料库VO（用于封装面料库查询结果）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder  // 新增Builder注解，方便构建对象
public class FabricLibraryVO {
    /**
     * 面料总数
     */
    private Integer totalCount;

    /**
     * 面料数据列表（单个面料的详细信息）
     */
    private List<FabricVO> fabricList;
}
