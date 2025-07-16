package com.yby6.mcp.server.parser.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.yby6.mcp.server.model.ImgInfo;
import com.yby6.mcp.server.model.VideoAuthor;
import com.yby6.mcp.server.model.VideoInfo;
import com.yby6.mcp.server.parser.BaseParser;
import com.yby6.mcp.server.utils.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抖音解析器
 *
 * @author Yangbuyi
 * @date 2025/07/16
 */
@Slf4j
public class DouyinParser extends BaseParser {
    
    private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) EdgiOS/121.0.2277.107 Version/17.0 Mobile/15E148 Safari/604.1";
    // 视频信息提取正则表达式
    private static final Pattern ROUTER_DATA_PATTERN = Pattern.compile("window\\._ROUTER_DATA\\s*=\\s*(.*?)</script>", Pattern.DOTALL);
    // 非法文件名字符正则表达式
    private static final Pattern ILLEGAL_FILENAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");
    
    
    @Override
    public VideoInfo parseShareUrl(String shareUrl) throws Exception {
        try {
            log.info("开始解析抖音分享链接: {}", shareUrl);
            
            // 1. 首先尝试从文本中提取URL
            String extractedUrl = extractUrlFromText(shareUrl);
            if (StringUtils.isNotBlank(extractedUrl)) {
                shareUrl = extractedUrl;
                log.info("从文本中提取到URL: {}", shareUrl);
            }
            
            // 2. 发送请求获取重定向后的URL，提取视频ID
            String videoId = extractVideoIdFromRedirect(extractedUrl);
            log.info("提取到视频ID: {}", videoId);
            
            // 3. 构建标准分享链接
            String standardShareUrl = "https://www.iesdouyin.com/share/video/" + videoId;
            
            // 4. 获取视频页面内容
            String pageContent = getPageContent(standardShareUrl);
            
            // 5. 解析视频信息
            return parseVideoInfo(pageContent, videoId);
            
        } catch (Exception e) {
            log.error("解析抖音分享链接失败", e);
            throw new Exception("解析抖音分享链接失败: " + e.getMessage(), e);
        }
    }
    
    @Override
    public VideoInfo parseVideoId(String videoId) throws Exception {
        // 根据视频ID构建分享链接
        String shareUrl = "https://www.iesdouyin.com/share/video/" + videoId;
        return parseShareUrl(shareUrl);
    }
    
    /**
     * 从分享URL中提取视频ID
     */
    private String extractVideoIdFromRedirect(String shareUrl) {
        try {
            log.info("开始从分享URL提取视频ID: {}", shareUrl);
            
            // 发送HTTP请求跟踪重定向，然后从最终URL提取
            try (HttpResponse response = HttpRequest.get(shareUrl)
                    .header("User-Agent", USER_AGENT)
                    .timeout(10000)
                    .setFollowRedirects(true)
                    .execute()) {
                
                if (response.isOk()) {
                    // 尝试从响应中获取当前URL信息
                    String responseBody = response.body();
                    
                    // 尝试从响应内容中找到视频ID
                    if (StringUtils.isNotBlank(responseBody)) {
                        String contentId = extractVideoIdFromContent(responseBody);
                        if (StringUtils.isNotBlank(contentId)) {
                            log.info("从响应内容提取到视频ID: {}", contentId);
                            return contentId;
                        }
                    }
                }
            } catch (Exception httpEx) {
                log.warn("HTTP请求失败，使用备用方法: {}", httpEx.getMessage());
            }
            
            // 如果以上都失败，使用正则表达式从原始URL中强制提取
            String regexId = extractIdWithRegex(shareUrl);
            if (StringUtils.isNotBlank(regexId)) {
                log.info("使用正则表达式提取到视频ID: {}", regexId);
                return regexId;
            }
            
            // 最后的降级处理
            String fallbackId = "unknown_" + System.currentTimeMillis();
            log.warn("所有方法都失败，使用降级ID: {}", fallbackId);
            return fallbackId;
            
        } catch (Exception e) {
            log.error("提取视频ID失败: {}", e.getMessage(), e);
            return "error_" + System.currentTimeMillis();
        }
    }
    
    /**
     * 从响应内容中提取视频ID
     */
    private String extractVideoIdFromContent(String content) {
        if (StringUtils.isBlank(content)) {
            return null;
        }
        
        // 尝试从HTML内容中提取视频ID
        Pattern[] patterns = {
                Pattern.compile("\"aweme_id\"\\s*:\\s*\"([^\"]+)\""),
                Pattern.compile("\"item_id\"\\s*:\\s*\"([^\"]+)\""),
                Pattern.compile("\"video_id\"\\s*:\\s*\"([^\"]+)\""),
                Pattern.compile("/video/([a-zA-Z0-9]{7,})"),
                Pattern.compile("/share/video/([a-zA-Z0-9]{7,})"),
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(content);
            if (matcher.find()) {
                String id = matcher.group(1);
                log.debug("从内容中提取到ID: {}", id);
                return id;
            }
        }
        
        return null;
    }
    
    /**
     * 使用正则表达式强制从URL中提取ID
     */
    private String extractIdWithRegex(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        
        // 强制提取模式
        Pattern[] patterns = {
                Pattern.compile("v\\.douyin\\.com/([a-zA-Z0-9]+)"),
                Pattern.compile("douyin\\.com/video/([a-zA-Z0-9]+)"),
                Pattern.compile("douyin\\.com/share/video/([a-zA-Z0-9]+)"),
                Pattern.compile("/([a-zA-Z0-9]{7,})/\\?"),
                Pattern.compile("/([a-zA-Z0-9]{7,})/?$"),
        };
        
        for (Pattern pattern : patterns) {
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                String id = matcher.group(1);
                log.debug("使用正则提取到ID: {}", id);
                return id;
            }
        }
        
        return null;
    }
    
    /**
     * 从URL路径中提取视频ID
     */
    private String extractVideoIdFromPath(String url) {
        if (StringUtils.isBlank(url)) {
            return null;
        }
        
        try {
            // 移除查询参数并按照Python逻辑处理
            String path = url.split("\\?")[0].replaceAll("/$", "");
            String[] parts = path.split("/");
            
            if (parts.length > 0) {
                String lastPart = parts[parts.length - 1];
                
                // 检查是否是有效的视频ID格式
                if (lastPart.matches("[a-zA-Z0-9]{7,}")) {
                    log.debug("从URL路径提取到ID: {}", lastPart);
                    return lastPart;
                }
            }
            
            // 尝试用正则表达式提取常见格式
            Pattern[] patterns = {
                    Pattern.compile("(?:video|note|share)/([a-zA-Z0-9]{7,})"),
                    Pattern.compile("/([a-zA-Z0-9]{11})"),
                    Pattern.compile("aweme_id=([a-zA-Z0-9]+)"),
                    Pattern.compile("item_id=([a-zA-Z0-9]+)")
            };
            
            for (Pattern pattern : patterns) {
                Matcher matcher = pattern.matcher(url);
                if (matcher.find()) {
                    String id = matcher.group(1);
                    log.debug("使用正则表达式从URL提取到ID: {}", id);
                    return id;
                }
            }
            
        } catch (Exception e) {
            log.warn("从URL路径提取ID时出错: {}", e.getMessage());
        }
        
        return null;
    }
    
    /**
     * 获取视频页面内容
     * 使用hutool发送HTTP请求
     */
    private String getPageContent(String url) throws Exception {
        try {
            log.info("正在获取页面内容: {}", url);
            
            String content = HttpRequest.get(url)
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.8,en;q=0.6")
                    .header("Accept-Encoding", "gzip, deflate")
                    .header("Connection", "keep-alive")
                    .header("Upgrade-Insecure-Requests", "1")
                    .timeout(15000)
                    .execute()
                    .body();
            
            if (StringUtils.isBlank(content)) {
                throw new Exception("获取到的页面内容为空");
            }
            
            log.info("成功获取页面内容，长度: {}", content.length());
            return content;
            
        } catch (Exception e) {
            log.error("获取页面内容失败: {}", url, e);
            throw new Exception("获取页面内容失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 解析视频信息
     */
    private VideoInfo parseVideoInfo(String pageContent, String videoId) throws Exception {
        try {
            VideoInfo videoInfo = new VideoInfo();
            
            // 1. 提取ROUTER_DATA
            Matcher matcher = ROUTER_DATA_PATTERN.matcher(pageContent);
            if (!matcher.find()) {
                throw new Exception("从HTML中解析视频信息失败");
            }
            
            String jsonData = matcher.group(1).trim();
            JsonNode rootNode = JsonUtil.parseJson(jsonData);
            
            // 2. 查找视频信息
            JsonNode loaderData = rootNode.get("loaderData");
            if (loaderData == null) {
                throw new Exception("未找到loaderData");
            }
            
            JsonNode videoInfoRes = null;
            
            // 尝试多种可能的页面key格式
            String[] possibleKeys = {
                    "video_" + videoId + "/page",
                    "note_" + videoId + "/page",
                    "video_(id)/page",
                    "note_(id)/page"
            };
            
            for (String key : possibleKeys) {
                if (loaderData.has(key)) {
                    videoInfoRes = loaderData.get(key).get("videoInfoRes");
                    if (videoInfoRes != null) {
                        log.info("找到视频信息，使用key: {}", key);
                        break;
                    }
                }
            }
            
            if (videoInfoRes == null) {
                // 记录可用的keys，帮助调试
                log.warn("可用的loaderData keys: {}", loaderData.fieldNames().toString());
                throw new Exception("无法从JSON中解析视频或图集信息，尝试的keys: " + String.join(", ", possibleKeys));
            }
            
            // 3. 提取视频详细信息
            JsonNode itemList = videoInfoRes.get("item_list");
            if (itemList == null || !itemList.isArray() || itemList.isEmpty()) {
                throw new Exception("未找到视频项目列表");
            }
            
            JsonNode videoItem = itemList.get(0);
            
            // 4. 获取图集图片地址
            List<ImgInfo> images = new ArrayList<>();
            JsonNode imagesNode = videoItem.get("images");
            if (imagesNode != null && imagesNode.isArray()) {
                for (JsonNode img : imagesNode) {
                    JsonNode urlListNode = img.get("url_list");
                    if (urlListNode != null && urlListNode.isArray() && !urlListNode.isEmpty()) {
                        String imageUrl = urlListNode.get(0).asText();
                        if (StringUtils.isNotBlank(imageUrl)) {
                            images.add(ImgInfo.builder().url(imageUrl).build());
                        }
                    }
                }
            }
            
            // 5. 获取视频播放地址
            String videoUrl = "";
            JsonNode video = videoItem.get("video");
            if (video != null) {
                JsonNode playAddr = video.get("play_addr");
                if (playAddr != null) {
                    JsonNode urlList = playAddr.get("url_list");
                    if (urlList != null && urlList.isArray() && !urlList.isEmpty()) {
                        // 获取无水印链接（将playwm替换为play）
                        videoUrl = urlList.get(0).asText().replace("playwm", "play");
                        
                        // 如果图集地址不为空时，因为没有视频，抖音返回的是一个音频
                        if (!images.isEmpty()) {
                            // 可以拿到音频
                            if (videoUrl.contains("douyinstatic.com/obj/ies-music")) {
                                // videoUrl 输出的：https://aweme.snssdk.com/aweme/v1/playwm/?video_id=https://sf5-hl-cdn-tos.douyinstatic.com/obj/ies-music/7057939755715070732.mp3&ratio=720p&line=0
                                // 截取 video_id=
                                videoUrl = videoUrl.split("video_id=")[1];
                                videoUrl = videoUrl.split("&")[0];
                                videoInfo.setMusicUrl(videoUrl);
                            }
                            // 清空视频播放地址
                            videoUrl = "";
                        }
                    }
                }
            }
            
            // 6. 获取视频标题
            String title = videoItem.has("desc") ? videoItem.get("desc").asText() : "";
            if (StringUtils.isBlank(title)) {
                title = "douyin_" + videoId;
            }
            
            // 7. 清理文件名中的非法字符
            title = ILLEGAL_FILENAME_PATTERN.matcher(title).replaceAll("_");
            
            // 8. 获取作者信息
            JsonNode author = videoItem.get("author");
            VideoAuthor videoAuthor = new VideoAuthor();
            if (author != null) {
                videoAuthor.setUid(author.has("uid") ? author.get("uid").asText() : "");
                videoAuthor.setName(author.has("nickname") ? author.get("nickname").asText() : "");
                videoAuthor.setAvatar(author.has("avatar_larger") ?
                        author.get("avatar_larger").get("url_list").get(0).asText() : "");
            }
            
            // 9. 获取封面信息
            String coverUrl = "";
            if (video != null && video.has("origin_cover") && video.get("origin_cover").has("url_list")) {
                JsonNode coverUrlList = video.get("origin_cover").get("url_list");
                if (coverUrlList.isArray() && !coverUrlList.isEmpty()) {
                    coverUrl = coverUrlList.get(0).asText();
                }
            } else if (video != null && video.has("cover") && video.get("cover").has("url_list")) {
                // 备用封面获取方式
                JsonNode coverUrlList = video.get("cover").get("url_list");
                if (coverUrlList.isArray() && !coverUrlList.isEmpty()) {
                    coverUrl = coverUrlList.get(0).asText();
                }
            }
            
            // 10. 构建返回对象
            videoInfo.setTitle(title);
            videoInfo.setVideoUrl(videoUrl);
            videoInfo.setCoverUrl(coverUrl);
            videoInfo.setImages(images);
            videoInfo.setAuthor(videoAuthor);
            
            return videoInfo;
            
        } catch (Exception e) {
            log.error("解析视频信息失败", e);
            throw new Exception("解析视频信息失败: " + e.getMessage(), e);
        }
    }
}
