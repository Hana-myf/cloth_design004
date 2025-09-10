package cn.kmbeast.pojo.dto.query.extend;

import cn.kmbeast.pojo.dto.query.base.QueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

/**
 * 面料库查询DTO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class FabricQueryDto extends QueryDto implements Serializable {
    private static final long serialVersionUID = 1L;

    // 面料名称（支持模糊查询）
    private String name;

    // 面料类型（精确查询，如：棉、羊毛）
    private String type;

    // 创建时间范围查询 - 开始时间
    private Date startCreateTime;

    // 创建时间范围查询 - 结束时间
    private Date endCreateTime;

}
