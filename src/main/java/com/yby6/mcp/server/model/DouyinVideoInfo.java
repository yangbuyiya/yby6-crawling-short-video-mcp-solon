package com.yby6.mcp.server.model;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 抖音视频信息模型
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DouyinVideoInfo {
    
    /**
     * 视频ID
     */
    @JsonProperty("video_id")
    private String videoId;
    
    /**
     * 视频标题
     */
    private String title;
    
    /**
     * 无水印下载链接
     */
    @JsonProperty("download_url")
    private String downloadUrl;
    
    /**
     * 视频描述
     */
    private String description;
    
    /**
     * 处理状态
     */
    private String status;
    
    /**
     * 错误信息（如果有）
     */
    private String error;
    
    /**
     * 使用提示
     */
    @JsonProperty("usage_tip")
    private String usageTip;
} 