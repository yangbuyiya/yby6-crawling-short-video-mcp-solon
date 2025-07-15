package com.yby6.mcp.server;

import com.yby6.mcp.server.tools.DouyinMcpTools;
import lombok.RequiredArgsConstructor;
import org.noear.solon.annotation.Controller;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Mapping;
import org.noear.solon.annotation.Param;

@Controller
@RequiredArgsConstructor
public class DemoController {
    
    @Inject
    private DouyinMcpTools douyinMcpTools;
    
    @Mapping("/hello")
    public String hello(@Param(defaultValue = "world") String name) {
        return String.format("Hello %s!", name);
    }
    
    /**
     * 测试抖音视频下载链接获取功能
     */
    @Mapping("/test/douyin")
    public String testDouyinDownload(@Param String shareLink) {
        if (shareLink == null || shareLink.trim().isEmpty()) {
            return "请提供抖音分享链接参数: ?shareLink=https://v.douyin.com/xxx";
        }
        
        try {
            return douyinMcpTools.getDouyinDownloadLink(shareLink);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }
    
    /**
     * 测试抖音视频信息解析功能
     */
    @Mapping("/test/douyin/info")
    public String testDouyinInfo(@Param String shareLink) {
        if (shareLink == null || shareLink.trim().isEmpty()) {
            return "请提供抖音分享链接参数: ?shareLink=https://v.douyin.com/xxx";
        }
        
        try {
            return douyinMcpTools.parseDouyinVideoInfo(shareLink);
        } catch (Exception e) {
            return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
        }
    }
}