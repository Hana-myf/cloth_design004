package cn.kmbeast.pojo.dto.query.extend;

import cn.kmbeast.pojo.dto.query.base.QueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class GeneratedImageQueryDto extends QueryDto implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer userId;
    private String imageName;
    private Date startGenerateTime;
    private Date endGenerateTime;
    private Integer pageNum;
    private Integer pageSize;
}
