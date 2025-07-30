package cn.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

@Data
public class GeneratedImage implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;
    private Integer userId; // 用户ID
    private String imageName; // 图片名称
    private String imageUrl; // 图片URL
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'", timezone = "GMT+8")
    private Date generateTime; // 生成时间
}
