package com.yby6.mcp.server.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 图集图片信息模型
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ImgInfo {
    
    /**
     * 图片URL
     */
    private String url;
    
    /**
     * Live Photo视频地址（小红书等平台支持）
     */
    private String livePhotoUrl;
}
