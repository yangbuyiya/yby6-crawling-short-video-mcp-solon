package com.yby6.mcp.server.service;

import com.yby6.mcp.server.model.VideoInfo;
import com.yby6.mcp.server.model.VideoSource;
import com.yby6.mcp.server.parser.ParserFactory;
import org.noear.solon.annotation.Component;

/**
 * 视频解析服务
 * 统一的视频解析服务入口
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@Component
public class VideoParseService {
    
    public VideoInfo parseShareUrl(String shareUrl) throws Exception {
        return ParserFactory.parseShareUrl(shareUrl);
    }
    
    public VideoInfo parseVideoId(String source, String videoId) throws Exception {
        VideoSource videoSource = VideoSource.valueOf(source.toUpperCase());
        return ParserFactory.parseVideoId(videoSource, videoId);
    }
} 