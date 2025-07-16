package com.yby6.mcp.server.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 视频作者信息模型
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VideoAuthor {
    
    /**
     * 作者ID
     */
    private String uid;
    
    /**
     * 作者昵称
     */
    private String name;
    
    /**
     * 作者头像
     */
    private String avatar;
}
