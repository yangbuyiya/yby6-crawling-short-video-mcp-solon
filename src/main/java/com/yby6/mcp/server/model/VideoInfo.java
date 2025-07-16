package com.yby6.mcp.server.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 视频信息模型 - 支持多平台视频解析
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@Data
@Builder
@AllArgsConstructor
public class VideoInfo {
    
    private String videoUrl;
    private String coverUrl;
    private String title;
    private String musicUrl;
    private List<ImgInfo> images;
    private VideoAuthor author;
    private String description;
    private String status;
    private String usageTip;
    

     public VideoInfo() {
        this.images = new ArrayList<>();
        this.author = new VideoAuthor();
    }
}
