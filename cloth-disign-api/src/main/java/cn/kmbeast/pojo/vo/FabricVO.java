package cn.kmbeast.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 单个面料信息VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FabricVO {
    /**
     * 面料ID
     */
    private Long id;

    /**
     * 面料名称（如：纯棉平纹、羊毛混纺）
     */
    private String name;

    /**
     * 面料类型（如：棉、羊毛、丝绸）
     */
    private String type;

    /**
     * 面料图片URL
     */
    private String imageUrl;

    /**
     * 面料描述
     */
    private String description;

    /**
     * 创建时间（可选，用于排序或展示）
     */
    private Date createTime;
}
