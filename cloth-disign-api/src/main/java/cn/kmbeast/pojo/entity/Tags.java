package cn.kmbeast.pojo.entity;
//
//import lombok.Data;
//
///**
// * 标签表
// */
//@Data
//public class Tags {
//    /**
//     * ID
//     */
//    private Integer id;
//    /**
//     * 标签名
//     */
//    private String name;
//}

import lombok.Data;

import java.io.Serializable;

@Data
public class Tags implements Serializable {
    private static final long serialVersionUID = 1L;
    private Integer id;
    private String name;
}
