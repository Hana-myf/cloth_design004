package cn.kmbeast.pojo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 消息实体
 */
@Data
public class Message {
    /**
     * 主键ID
     */
    private Integer id;
    private String content;
    private Integer messageType;
    private Integer receiverId;
    private Integer senderId;
    private Boolean isRead;
    private Integer contentId;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
