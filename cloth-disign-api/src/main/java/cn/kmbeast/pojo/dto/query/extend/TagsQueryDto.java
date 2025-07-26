package cn.kmbeast.pojo.dto.query.extend;

import cn.kmbeast.pojo.dto.query.base.QueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@Data
@EqualsAndHashCode(callSuper = true)
public class TagsQueryDto extends QueryDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private String name;
    private Integer pageNum;  // 新增分页参数
    private Integer pageSize; // 新增分页参数
}
