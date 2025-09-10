package cn.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Date;
@Data
public class Fabric {
    private Long id;             // 主键ID
    private String name;        // 面料名称
    private String type;        // 面料类型
    private String imageUrl;    // 图片URL
    private String description; // 描述
    private Date createTime;    // 创建时间
    private Date updateTime;    // 更新时间
}
