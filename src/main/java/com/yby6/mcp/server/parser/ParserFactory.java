package com.yby6.mcp.server.parser;

import com.yby6.mcp.server.model.VideoInfo;
import com.yby6.mcp.server.model.VideoSource;
import com.yby6.mcp.server.parser.impl.DouyinParser;
import com.yby6.mcp.server.parser.impl.RedBookParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParserFactory {
    
    private static final Map<VideoSource, ParserConfig> PARSER_MAPPING = new HashMap<>();
    
    static {
        // 抖音
        PARSER_MAPPING.put(VideoSource.DOUYIN, new ParserConfig(
                List.of("v.douyin.com", "www.iesdouyin.com", "www.douyin.com"),
                DouyinParser.class
        ));
        // 小红书
        PARSER_MAPPING.put(VideoSource.REDBOOK, new ParserConfig(
                List.of("www.xiaohongshu.com", "xhslink.com"),
                RedBookParser.class
        ));
    }
    
    /**
     * 解析共享url
     *
     * @param shareUrl 共享网址
     * @return {@link VideoInfo }
     * @throws Exception 例外
     */
    public static VideoInfo parseShareUrl(String shareUrl) throws Exception {
        VideoSource source = detectVideoSource(shareUrl);
        if (source == null) {
            throw new Exception("不支持的视频平台: " + shareUrl);
        }
        
        BaseParser parser = createParser(source);
        return parser.parseShareUrl(shareUrl);
    }
    
    /**
     * 解析视频id
     *
     * @param source  来源
     * @param videoId 视频ID
     * @return {@link VideoInfo }
     * @throws Exception 例外
     */
    public static VideoInfo parseVideoId(VideoSource source, String videoId) throws Exception {
        BaseParser parser = createParser(source);
        return parser.parseVideoId(videoId);
    }
    
    /**
     * 检测视频源
     *
     * @param shareUrl 共享网址
     * @return {@link VideoSource }
     */
    public static VideoSource detectVideoSource(String shareUrl) {
        for (Map.Entry<VideoSource, ParserConfig> entry : PARSER_MAPPING.entrySet()) {
            ParserConfig config = entry.getValue();
            for (String domain : config.domains()) {
                if (shareUrl.contains(domain)) {
                    return entry.getKey();
                }
            }
        }
        return null;
    }
    
    private static BaseParser createParser(VideoSource source) throws Exception {
        ParserConfig config = PARSER_MAPPING.get(source);
        if (config == null) {
            throw new Exception("不支持的视频来源: " + source);
        }
        
        try {
            return config.parserClass().getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new Exception("创建解析器失败: " + source, e);
        }
    }
    
    /**
     * 获取支持来源
     *
     * @return {@link Set }<{@link VideoSource }>
     */
    public static Set<VideoSource> getSupportedSources() {
        return PARSER_MAPPING.keySet();
    }
    
    /**
     * 解析器配置
     *
     * @author Yangbuyi
     * @date 2025/07/16
     */
    private record ParserConfig(List<String> domains, Class<? extends BaseParser> parserClass) {
    }
}
