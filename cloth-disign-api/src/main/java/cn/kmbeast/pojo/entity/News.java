package cn.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 健康资讯实体
 */
@Data
public class News {
    /**
     * 主键ID
     */
    private Integer id;
    /**
     * 资讯标题
     */
    private String name;
    private String content;
    private Integer tagId;
    private String cover;
    /**
     * 阅读者的ID列表，以“,”进行分割
     */
    private String readerIds;
    private Boolean isTop;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
