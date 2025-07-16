package com.yby6.mcp.server.tools;

import com.yby6.mcp.server.model.VideoInfo;
import com.yby6.mcp.server.service.VideoParseService;
import com.yby6.mcp.server.service.VideoTextExtractor;
import com.yby6.mcp.server.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.noear.solon.ai.annotation.ToolMapping;
import org.noear.solon.ai.mcp.server.annotation.McpServerEndpoint;
import org.noear.solon.annotation.Inject;
import org.noear.solon.annotation.Param;

/**
 * MCP工具类
 * 提供视频下载链接获取等功能，支持多平台
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@McpServerEndpoint(sseEndpoint = "/mcp/sse")
@Slf4j
public class VideoServer {
    
    @Inject
    private VideoParseService videoParseService;
    
    @Inject
    private VideoTextExtractor videoTextExtractor;
    
    
    /**
     * 解析视频分享链接，支持多平台
     *
     * @param shareUrl 分享链接
     * @return 包含视频信息的JSON字符串
     */
    @ToolMapping(name = "share_url_parse_tool", description = "解析视频分享链接，获取视频信息，支持抖音、快手、小红书等多平台")
    public String parseShareUrl(@Param(name = "shareUrl", description = "分享链接") String shareUrl) {
        try {
            log.info("开始处理分享链接: {}", shareUrl);
            
            VideoInfo videoInfo = videoParseService.parseShareUrl(shareUrl);
            
            log.info("成功获取视频信息: title={}", videoInfo.getTitle());
            return JsonUtil.toJsonString(new Response(videoInfo));
        } catch (Exception e) {
            log.error("解析分享链接失败", e);
            return createErrorResponse("解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 根据平台和视频ID解析视频信息
     *
     * @param source  视频来源平台
     * @param videoId 视频ID
     * @return 包含视频信息的JSON字符串
     */
    @ToolMapping(name = "video_id_parse_tool", description = "根据视频来源和ID解析视频信息")
    public String parseVideoId(String source, String videoId) {
        try {
            log.info("开始解析视频: source={}, videoId={}", source, videoId);
            
            VideoInfo videoInfo = videoParseService.parseVideoId(source, videoId);
            
            log.info("成功解析视频信息: title={}", videoInfo.getTitle());
            
            // 构建响应
            Response response = new Response();
            response.code = 200;
            response.msg = "解析成功";
            response.data = videoInfo;
            
            return JsonUtil.toJsonString(response);
            
        } catch (Exception e) {
            log.error("解析视频ID失败", e);
            return createErrorResponse("解析失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取支持的平台列表
     *
     * @return 支持的平台信息
     */
    @ToolMapping(name = "get_supported_platforms", description = "获取支持的视频平台列表")
    public String getSupportedPlatforms() {
        try {
            String platforms = """
                    {
                      "code": 200,
                      "msg": "获取成功",
                      "data": {
                        "supported_platforms": [
                          {
                            "name": "抖音",
                            "source": "douyin",
                            "domains": ["v.douyin.com", "www.iesdouyin.com", "www.douyin.com"]
                          },
                          {
                            "name": "快手",
                            "source": "kuaishou",
                            "domains": ["v.kuaishou.com"]
                          },
                          {
                            "name": "小红书",
                            "source": "redbook",
                            "domains": ["www.xiaohongshu.com", "xhslink.com"]
                          },
                          {
                            "name": "微博",
                            "source": "weibo",
                            "domains": ["weibo.com"]
                          },
                          {
                            "name": "皮皮虾",
                            "source": "pipixia",
                            "domains": ["h5.pipix.com"]
                          }
                        ]
                      }
                    }
                    """;
            
            return platforms;
        } catch (Exception e) {
            log.error("获取平台列表失败", e);
            return createErrorResponse("获取平台列表失败: " + e.getMessage());
        }
    }
    
    /**
     * 从分享链接提取视频中的文本内容（适用于所有平台）
     *
     * @param shareText  分享链接或包含链接的文本
     * @param apiKey     语音识别API密钥（可选，如果不提供则从环境变量YBY6_API_KEY获取）
     * @param apiBaseUrl API基础URL（可选，默认使用SiliconFlow）
     * @param model      语音识别模型（可选，默认使用SenseVoiceSmall）
     * @return 提取的文本内容
     */
    @ToolMapping(name = "share_text_parse_tool", description = "从分享链接提取视频中的文本内容，需要传递apikey，否则无法使用视频内容提取功能！")
    public String extractTextFromShareUrl(
            @Param(name = "shareText", description = "分享链接或包含链接的文本") String shareText,
            @Param(name = "apiKey", description = "语音识别API密钥", required = false) String apiKey,
            @Param(name = "apiBaseUrl", description = "API基础URL", required = false) String apiBaseUrl,
            @Param(name = "model", description = "语音识别模型", required = false) String model
    ) {
        try {
            log.info("开始提取视频文本内容: {}", shareText);
            
            // 获取API密钥
            String finalApiKey = apiKey;
            if (StringUtils.isBlank(finalApiKey)) {
                finalApiKey = System.getenv("YBY6_API_KEY");
                if (StringUtils.isBlank(finalApiKey)) {
                    return createErrorResponse("未设置环境变量 YBY6_API_KEY，且未提供 apiKey 参数，请在配置中添加语音识别API密钥");
                }
            }
            
            // 1. 解析视频信息
            VideoInfo videoInfo = videoParseService.parseShareUrl(shareText);
            
            // 2. 提取文本内容
            String textContent = videoTextExtractor.extractTextFromVideo(videoInfo, finalApiKey, apiBaseUrl, model);
            
            // 3. 构建成功响应
            Response response = new Response();
            response.code = 200;
            response.msg = "文本提取完成";
            response.data = new TextExtractionResult(textContent, videoInfo.getTitle(), "已成功提取视频中的文本内容");
            
            return JsonUtil.toJsonString(response);
            
        } catch (Exception e) {
            log.error("提取视频文本失败", e);
            return createErrorResponse("提取视频文本失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取使用指南
     *
     * @return 使用指南文档
     */
    @ToolMapping(name = "get_usage_guide", description = "获取多平台视频解析功能的使用指南")
    public String getUsageGuide() {
        return """
                {
                  "code": 200,
                  "msg": "获取成功",
                  "data": {
                    "guide": "# 多平台视频解析使用指南\\n\\n## 功能说明\\n支持解析抖音、快手、小红书、微博、皮皮虾等平台的视频信息。\\n\\n## 工具说明\\n- `share_url_parse_tool`: 解析分享链接，自动识别平台\\n- `video_id_parse_tool`: 根据平台和视频ID解析\\n- `share_text_parse_tool`: 从分享链接提取视频中的文本内容（需要API密钥）\\n- `get_supported_platforms`: 获取支持的平台列表\\n\\n## 使用方法\\n1. 直接使用分享链接解析（推荐）\\n2. 指定平台和视频ID解析\\n3. 提取视频文本内容（需要语音识别API密钥）\\n\\n## 返回信息\\n- 视频标题、封面、下载链接\\n- 作者信息（昵称、头像等）\\n- 图集信息（如果是图片内容）\\n- 音乐信息（如果有背景音乐）\\n- 文本内容（仅限文本提取功能）"
                  }
                }
                """;
    }
    
    /**
     * 创建错误响应
     */
    private String createErrorResponse(String errorMessage) {
        try {
            Response response = new Response();
            response.code = 500;
            response.msg = errorMessage;
            response.data = null;
            
            return JsonUtil.toJsonString(response);
        } catch (Exception e) {
            return String.format("""
                    {
                      "code": 500,
                      "msg": "%s",
                      "data": null
                    }
                    """, escapeJsonString(errorMessage));
        }
    }
    
    /**
     * 转义JSON字符串中的特殊字符
     */
    private String escapeJsonString(String input) {
        if (input == null) {
            return "";
        }
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
    
    /**
     * 统一响应格式
     */
    public static class Response {
        public int code;
        public String msg;
        public Object data;
        
        public Response() {
        }
        
        public Response(int code, String msg, Object data) {
            this.code = code;
            this.msg = msg;
            this.data = data;
        }
        
        public Response(int code, String msg) {
            this.code = code;
            this.msg = msg;
            this.data = null;
        }
        
        public Response(String msg) {
            this.code = 500;
            this.msg = msg;
            this.data = null;
        }
        
        public Response(Object data) {
            this.code = 200;
            this.msg = "success";
            this.data = data;
        }
    }
    
    /**
     * 文本提取结果
     */
    public static class TextExtractionResult {
        public String textContent;
        public String videoTitle;
        public String message;
        
        public TextExtractionResult(String textContent, String videoTitle, String message) {
            this.textContent = textContent;
            this.videoTitle = videoTitle;
            this.message = message;
        }
    }
}
