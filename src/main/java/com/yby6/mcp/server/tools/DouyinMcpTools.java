package com.yby6.mcp.server.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yby6.mcp.server.model.DouyinVideoInfo;
import com.yby6.mcp.server.processor.DouyinProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.noear.solon.ai.annotation.ToolMapping;
import org.noear.solon.annotation.Component;

/**
 * 抖音MCP工具类
 * 提供抖音视频下载链接获取等功能
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class DouyinMcpTools {

    private final DouyinProcessor douyinProcessor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取抖音视频的无水印下载链接
     *
     * @param shareLink 抖音分享链接或包含链接的文本
     * @return 包含下载链接和视频信息的JSON字符串
     */
    @ToolMapping(name = "get_douyin_download_link", description = "获取抖音视频的无水印下载链接")
    public String getDouyinDownloadLink(String shareLink) {
        try {
            log.info("开始处理抖音分享链接: {}", shareLink);

            // 使用处理器解析视频信息
            DouyinVideoInfo videoInfo = douyinProcessor.parseShareUrl(shareLink);

            log.info("成功获取视频信息: videoId={}, title={}", videoInfo.getVideoId(), videoInfo.getTitle());

            // 转换为JSON字符串返回
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(videoInfo);

        } catch (Exception e) {
            log.error("获取抖音下载链接失败", e);

            // 构建错误响应
            DouyinVideoInfo errorResponse = DouyinVideoInfo.builder().status("error").error("获取下载链接失败: " + e.getMessage()).build();

            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorResponse);
            } catch (Exception jsonEx) {
                log.error("构建错误响应失败", jsonEx);
                return "{\"status\":\"error\",\"error\":\"获取下载链接失败: " + e.getMessage() + "\"}";
            }
        }
    }

    /**
     * 解析抖音分享链接，获取视频基本信息
     *
     * @param shareLink 抖音分享链接或包含链接的文本
     * @return 视频信息（JSON格式字符串）
     */
    @ToolMapping(name = "parse_douyin_video_info", description = "解析抖音分享链接，获取视频基本信息")
    public String parseDouyinVideoInfo(String shareLink) {
        try {
            log.info("开始解析抖音视频信息: {}", shareLink);

            // 使用处理器解析视频信息
            DouyinVideoInfo videoInfo = douyinProcessor.parseShareUrl(shareLink);

            // 构建简化的响应（只包含基本信息）
            DouyinVideoInfo simpleInfo = DouyinVideoInfo.builder().videoId(videoInfo.getVideoId()).title(videoInfo.getTitle()).downloadUrl(videoInfo.getDownloadUrl()).status("success").build();

            log.info("成功解析视频信息: {}", simpleInfo.getVideoId());

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(simpleInfo);

        } catch (Exception e) {
            log.error("解析抖音视频信息失败", e);

            DouyinVideoInfo errorResponse = DouyinVideoInfo.builder().status("error").error(e.getMessage()).build();

            try {
                return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(errorResponse);
            } catch (Exception jsonEx) {
                log.error("构建错误响应失败", jsonEx);
                return "{\"status\":\"error\",\"error\":\"" + e.getMessage() + "\"}";
            }
        }
    }
    
    /**
     * 从抖音分享链接提取视频中的文本内容
     * 
     * @param shareLink 抖音分享链接或包含链接的文本
     * @param apiKey 语音识别API密钥（可选，如果不提供则从环境变量DOUYIN_API_KEY获取）
     * @param apiBaseUrl API基础URL（可选，默认使用SiliconFlow）
     * @param model 语音识别模型（可选，默认使用SenseVoiceSmall）
     * @return 提取的文本内容
     */
    @ToolMapping(name = "extract_douyin_text", 
                description = "从抖音分享链接提取视频中的文本内容。需要设置环境变量 DOUYIN_API_KEY 或提供 apiKey 参数")
    public String extractDouyinText(String shareLink, String apiKey, String apiBaseUrl, String model) {
        try {
            log.info("开始提取抖音视频文本内容: {}", shareLink);
            
            // 获取API密钥
            String finalApiKey = apiKey;
            if (StringUtils.isBlank(finalApiKey)) {
                finalApiKey = System.getenv("DOUYIN_API_KEY");
                if (StringUtils.isBlank(finalApiKey)) {
                    return createErrorResponse("未设置环境变量 DOUYIN_API_KEY，且未提供 apiKey 参数，请在配置中添加语音识别API密钥");
                }
            }
            
            // 调用处理器提取文本
            String textContent = douyinProcessor.extractDouyinText(shareLink, finalApiKey, apiBaseUrl, model);
            
            // 构建成功响应
            String successResponse = String.format("""
                {
                  "status": "success",
                  "text_content": "%s",
                  "message": "文本提取完成"
                }
                """, escapeJsonString(textContent));
            
            log.info("成功提取文本内容");
            return successResponse;
            
        } catch (Exception e) {
            log.error("提取抖音视频文本失败", e);
            return createErrorResponse("提取抖音视频文本失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取抖音视频文本提取使用指南
     * 
     * @return 使用指南文档
     */
    @ToolMapping(name = "douyin_text_extraction_guide", 
                description = "获取抖音视频文本提取功能的使用指南")
    public String getDouyinTextExtractionGuide() {
        return """
        # 抖音视频文本提取使用指南
        
        ## 功能说明
        这个工具可以从抖音分享链接中提取视频的文本内容，以及获取无水印下载链接。
        
        ## 环境变量配置
        请确保设置了以下环境变量：
        - `DOUYIN_API_KEY`: 语音识别API密钥（如SiliconFlow API密钥）
        
        ## 使用步骤
        1. 复制抖音视频的分享链接
        2. 设置环境变量 DOUYIN_API_KEY 或在调用时提供 apiKey 参数
        3. 使用相应的工具进行操作
        
        ## 工具说明
        - `extract_douyin_text`: 完整的文本提取流程（需要API密钥）
        - `get_douyin_download_link`: 获取无水印视频下载链接（无需API密钥）
        - `parse_douyin_video_info`: 仅解析视频基本信息
        
        ## 参数说明
        - shareLink: 抖音分享链接或包含链接的文本（必需）
        - apiKey: 语音识别API密钥（可选，如不提供则从环境变量获取）
        - apiBaseUrl: API基础URL（可选，默认: https://api.siliconflow.cn/v1/audio/transcriptions）
        - model: 语音识别模型（可选，默认: FunAudioLLM/SenseVoiceSmall）
        
        ## 注意事项
        - 需要提供有效的API密钥（通过环境变量或参数）
        - 中间文件会自动清理
        - 支持大部分抖音视频格式
        - 获取下载链接无需API密钥
        
        ## 示例用法
        ```
        extract_douyin_text("抖音分享链接", "your-api-key", null, null)
        ```
        """;
    }
    
    /**
     * 创建错误响应
     */
    private String createErrorResponse(String errorMessage) {
        return String.format("""
            {
              "status": "error",
              "error": "%s"
            }
            """, escapeJsonString(errorMessage));
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
}