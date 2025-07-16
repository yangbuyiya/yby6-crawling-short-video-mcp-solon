package com.yby6.mcp.server.parser.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.yby6.mcp.server.model.VideoInfo;
import com.yby6.mcp.server.model.VideoAuthor;
import com.yby6.mcp.server.model.ImgInfo;
import com.yby6.mcp.server.parser.BaseParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

/**
 * 小红书解析器
 * ，支持视频和图集解析
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@Slf4j
public class RedBookParser extends BaseParser {
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private static final String[] WINDOWS_USER_AGENTS = {
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Safari/537.36",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:119.0) Gecko/20100101 Firefox/119.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0",
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36 Edg/119.0.0.0"
    };
    
    /**
     * 获取随机Windows User-Agent
     */
    private String getRandomWindowsUserAgent() {
        Random random = new Random();
        return WINDOWS_USER_AGENTS[random.nextInt(WINDOWS_USER_AGENTS.length)];
    }
    
    @Override
    public VideoInfo parseShareUrl(String shareUrl) throws Exception {
        log.info("开始解析小红书分享链接: {}", shareUrl);
        
        // 从输入文本中提取真正的URL
        String extractedUrl = extractUrlFromText(shareUrl);
        if (StringUtils.isBlank(extractedUrl)) {
            throw new Exception("无法从输入文本中提取有效的URL: " + shareUrl);
        }
        
        log.info("提取到的URL: {}", extractedUrl);
        
        // 构建请求头，使用随机Windows User-Agent
        Map<String, String> headers = new HashMap<>();
        String randomUserAgent = getRandomWindowsUserAgent();
        headers.put("User-Agent", randomUserAgent);
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8");
        headers.put("Accept-Encoding", "gzip, deflate, br");
        headers.put("Connection", "keep-alive");
        headers.put("Upgrade-Insecure-Requests", "1");
        headers.put("Sec-Fetch-Dest", "document");
        headers.put("Sec-Fetch-Mode", "navigate");
        headers.put("Sec-Fetch-Site", "none");
        
        log.info("使用User-Agent: {}", randomUserAgent);
        
        // 发送GET请求，跟随重定向
        HttpResponse response = HttpRequest.get(extractedUrl)
                .headerMap(headers, true)
                .timeout(15000)
                .setFollowRedirects(true)
                .execute();
        
        if (!response.isOk()) {
            throw new Exception("获取小红书页面失败: " + response.getStatus());
        }
        
        String responseBody = response.body();
        if (StringUtils.isBlank(responseBody)) {
            throw new Exception("页面内容为空");
        }
        
        log.debug("页面内容长度: {} 字符", responseBody.length());
        
        // 使用正则表达式提取页面中的JSON数据
        Pattern pattern = Pattern.compile("window\\.__INITIAL_STATE__\\s*=\\s*(.*?)</script>", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(responseBody);
        
        if (!matcher.find()) {
            // 如果找不到 __INITIAL_STATE__，尝试查找其他可能的数据结构
            log.warn("未找到 window.__INITIAL_STATE__，尝试查找其他数据结构");
            
            // 尝试查找其他可能的初始化数据
            Pattern[] alternativePatterns = {
                Pattern.compile("window\\._INITIAL_STATE_\\s*=\\s*(.*?)</script>", Pattern.DOTALL),
                Pattern.compile("window\\.__NUXT__\\s*=\\s*(.*?)</script>", Pattern.DOTALL),
                Pattern.compile("window\\.__APOLLO_STATE__\\s*=\\s*(.*?)</script>", Pattern.DOTALL)
            };
            
            for (Pattern altPattern : alternativePatterns) {
                Matcher altMatcher = altPattern.matcher(responseBody);
                if (altMatcher.find()) {
                    log.info("找到替代数据结构: {}", altPattern.pattern());
                    matcher = altMatcher;
                    break;
                }
            }
            
            if (!matcher.find()) {
                // 记录页面内容的关键部分用于调试
                log.error("解析失败，页面可能已改版。页面开头内容: {}",
                    responseBody.length() > 500 ? responseBody.substring(0, 500) : responseBody);
                throw new Exception("parse video json info from html fail - 页面结构可能已变更");
            }
        }
        
        String jsonText = matcher.group(1).trim();
        log.debug("提取到的JSON长度: {} 字符", jsonText.length());
        
        // 移除结尾的分号
        if (jsonText.endsWith(";")) {
            jsonText = jsonText.substring(0, jsonText.length() - 1);
        }
        
        // 使用YAML解析器解析（因为Python版本使用yaml.safe_load）
        JsonNode jsonData;
        try {
            jsonData = yamlMapper.readTree(jsonText);
            log.debug("YAML解析成功");
        } catch (Exception e) {
            // 如果YAML解析失败，尝试使用JSON解析
            log.warn("YAML解析失败，尝试JSON解析: {}", e.getMessage());
            try {
                jsonData = objectMapper.readTree(jsonText);
                log.debug("JSON解析成功");
            } catch (Exception jsonEx) {
                log.error("JSON解析也失败: {}", jsonEx.getMessage());
                log.error("无法解析的JSON片段: {}",
                    jsonText.length() > 200 ? jsonText.substring(0, 200) + "..." : jsonText);
                throw new Exception("JSON/YAML解析均失败: " + jsonEx.getMessage());
            }
        }
        
        // 获取note信息
        JsonNode noteInfo = jsonData.path("note");
        if (noteInfo.isMissingNode()) {
            log.error("未找到note字段，可用字段: {}", getAvailableFields(jsonData));
            throw new Exception("未找到note字段，页面结构可能已变更");
        }
        
        String noteId = noteInfo.path("currentNoteId").asText("");
        log.debug("提取到noteId: {}", noteId);
        
        // 验证返回：小红书的分享链接有有效期，过期后会返回 undefined
        if ("undefined".equals(noteId) || StringUtils.isBlank(noteId)) {
            log.error("noteId无效: {}", noteId);
            throw new Exception("parse fail: note id in response is undefined - 链接可能已过期");
        }
        
        JsonNode noteDetailMap = noteInfo.path("noteDetailMap");
        if (noteDetailMap.isMissingNode()) {
            log.error("未找到noteDetailMap字段");
            throw new Exception("未找到noteDetailMap字段");
        }
        
        JsonNode noteDetail = noteDetailMap.path(noteId);
        if (noteDetail.isMissingNode()) {
            log.error("未找到noteId对应的详情: {}", noteId);
            throw new Exception("未找到noteId对应的详情: " + noteId);
        }
        
        JsonNode note = noteDetail.path("note");
        if (note.isMissingNode()) {
            log.error("未找到笔记详情数据，noteDetail可用字段: {}", getAvailableFields(noteDetail));
            throw new Exception("未找到笔记详情数据");
        }
        
        log.info("成功解析note数据");
        
        // 构建视频信息
        VideoInfo videoInfo = VideoInfo.builder().build();
        
        // 获取视频地址
        String videoUrl = "";
        JsonNode video = note.path("video");
        if (!video.isMissingNode()) {
            JsonNode media = video.path("media");
            JsonNode stream = media.path("stream");
            JsonNode h264Array = stream.path("h264");
            
            if (h264Array.isArray() && !h264Array.isEmpty()) {
                videoUrl = h264Array.get(0).path("masterUrl").asText("");
            }
        }
        videoInfo.setVideoUrl(videoUrl);
        
        // 获取图集图片地址
        List<ImgInfo> images = new ArrayList<>();
        if (StringUtils.isBlank(videoUrl)) {
            // 如果没有视频，说明是图集
            JsonNode imageList = note.path("imageList");
            if (imageList.isArray()) {
                for (JsonNode imgItem : imageList) {
                    String urlDefault = imgItem.path("urlDefault").asText("");
                    if (StringUtils.isNotBlank(urlDefault)) {
                        // 个别图片有水印，替换图片域名
                        String imageId = urlDefault.split("/")[urlDefault.split("/").length - 1].split("!")[0];
                        
                        // 如果链接中带有 spectrum/，替换域名时需要带上
                        String spectrumStr = urlDefault.contains("spectrum/") ? "spectrum/" : "";
                        
                        String newUrl = String.format(
                                "https://ci.xiaohongshu.com/notes_pre_post/%s%s?imageView2/format/jpg",
                                spectrumStr, imageId
                        );
                        
                        ImgInfo imgInfo = ImgInfo.builder()
                                .url(newUrl)
                                .build();
                        
                        // 检查是否有 livephoto 视频地址
                        boolean livePhoto = imgItem.path("livePhoto").asBoolean(false);
                        if (livePhoto) {
                            JsonNode imgStream = imgItem.path("stream");
                            JsonNode imgH264 = imgStream.path("h264");
                            if (imgH264.isArray() && !imgH264.isEmpty()) {
                                String livePhotoUrl = imgH264.get(0).path("masterUrl").asText("");
                                imgInfo.setLivePhotoUrl(livePhotoUrl);
                            }
                        }
                        
                        images.add(imgInfo);
                    }
                }
            }
        }
        videoInfo.setImages(images);
        
        // 设置封面
        JsonNode imageList = note.path("imageList");
        if (imageList.isArray() && !imageList.isEmpty()) {
            String coverUrl = imageList.get(0).path("urlDefault").asText("");
            videoInfo.setCoverUrl(coverUrl);
        }
        
        // 设置标题
        String title = note.path("title").asText("");
        if (StringUtils.isBlank(title)) {
            title = "redbook_" + noteId;
        }
        videoInfo.setTitle(title);
        
        // 设置作者信息
        JsonNode user = note.path("user");
        VideoAuthor author = VideoAuthor.builder()
                .uid(user.path("userId").asText(""))
                .name(user.path("nickname").asText(""))
                .avatar(user.path("avatar").asText(""))
                .build();
        videoInfo.setAuthor(author);
        
        // 设置状态信息
        videoInfo.setStatus("success");
        if (StringUtils.isNotBlank(videoUrl)) {
            videoInfo.setDescription("小红书视频: " + title);
            videoInfo.setUsageTip("已成功解析小红书视频信息");
        } else {
            videoInfo.setDescription("小红书图集: " + title + " (共" + images.size() + "张图片)");
            videoInfo.setUsageTip("已成功解析小红书图集信息，包含" + images.size() + "张图片");
        }
        
        log.info("小红书内容解析完成: {} ({})", title, StringUtils.isNotBlank(videoUrl) ? "视频" : "图集");
        return videoInfo;
    }
    
    /**
     * 获取JsonNode中可用的字段名，用于调试
     */
    private String getAvailableFields(JsonNode node) {
        if (node == null || node.isMissingNode()) {
            return "null";
        }
        
        List<String> fieldNames = new ArrayList<>();
        node.fieldNames().forEachRemaining(fieldNames::add);
        return String.join(", ", fieldNames);
    }
    
    @Override
    public VideoInfo parseVideoId(String videoId) throws Exception {
        throw new UnsupportedOperationException("小红书暂不支持直接解析视频ID");
    }
    
    @Override
    public String getPlatformName() {
        return "小红书";
    }
}
