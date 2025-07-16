package com.yby6.mcp.server.parser;

import com.yby6.mcp.server.model.VideoInfo;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抽象顶层解析器
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
public abstract class BaseParser {
    
    // URL正则表达式模式 - 改进版本，支持更复杂的URL格式
    protected static final Pattern URL_REGEX_PATTERN = Pattern.compile("https?://[\\w.-]+(?:\\.[\\w.-]+)*(?:/[^\\s]*)?");
    
    // 默认请求头
    protected static final Map<String, String> DEFAULT_HEADERS = new HashMap<String, String>() {{
        put("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) EdgiOS/121.0.2277.107 Version/17.0 Mobile/15E148 Safari/604.1");
    }};
    
    /**
     * 解析分享链接
     *
     * @param shareUrl 分享链接
     * @return {@link VideoInfo }
     * @throws Exception 例外
     */
    public abstract VideoInfo parseShareUrl(String shareUrl) throws Exception;
    
    /**
     * 解析视频id
     *
     * @param videoId 视频ID
     * @return {@link VideoInfo }
     * @throws Exception 例外
     */
    public abstract VideoInfo parseVideoId(String videoId) throws Exception;
    
    /**
     * 从文本中提取URL
     *
     * @param text 包含URL的文本
     * @return 提取出的URL，如果没有找到则返回null
     */
    protected String extractUrlFromText(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }

        Matcher matcher = URL_REGEX_PATTERN.matcher(text);
        return matcher.find() ? matcher.group() : null;
    }
    
    /**
     * 获取平台名称
     *
     * @return 平台名称
     */
    public String getPlatformName() {
        return getClass().getSimpleName().replace("Parser", "");
    }
}
