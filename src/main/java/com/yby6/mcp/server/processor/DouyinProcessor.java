package com.yby6.mcp.server.processor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yby6.mcp.server.model.DouyinVideoInfo;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.apache.commons.lang3.StringUtils;
import org.noear.solon.annotation.Component;
import ws.schild.jave.Encoder;
import ws.schild.jave.EncoderException;
import ws.schild.jave.MultimediaObject;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 抖音视频处理器
 * 负责解析抖音分享链接，获取无水印视频下载链接
 */
@Component
@Slf4j
public class DouyinProcessor {

    private static final String USER_AGENT = "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) EdgiOS/121.0.2277.107 Version/17.0 Mobile/15E148 Safari/604.1";

    // 抖音分享链接提取正则表达式（更精确的匹配）
    private static final Pattern DOUYIN_URL_PATTERN = Pattern.compile(
            "https?://(?:v\\.douyin\\.com/[A-Za-z0-9]+/?|" +
            "www\\.douyin\\.com/(?:video|note)/[0-9]+|" +
            "www\\.iesdouyin\\.com/share/(?:video|note)/[0-9]+|" +
            "[a-zA-Z0-9.-]+\\.douyin\\.com/[^\\s]*)"
    );

    // 通用URL正则表达式（作为备用）
    private static final Pattern GENERAL_URL_PATTERN = Pattern.compile("https?://[^\\s]+");

    // 视频信息提取正则表达式
    private static final Pattern ROUTER_DATA_PATTERN = Pattern.compile("window\\._ROUTER_DATA\\s*=\\s*(.*?)</script>", Pattern.DOTALL);

    // 非法文件名字符正则表达式
    private static final Pattern ILLEGAL_FILENAME_PATTERN = Pattern.compile("[\\\\/:*?\"<>|]");

    // 默认API配置
    private static final String DEFAULT_API_BASE_URL = "https://api.siliconflow.cn/v1/audio/transcriptions";
    private static final String DEFAULT_MODEL = "FunAudioLLM/SenseVoiceSmall";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OkHttpClient httpClient;
    private final Path tempDir;

    public DouyinProcessor() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        // 创建临时目录
        try {
            this.tempDir = Files.createTempDirectory("douyin_processor_" + IdUtil.fastSimpleUUID());
            log.info("创建临时目录: {}", tempDir);
        } catch (IOException e) {
            throw new RuntimeException("无法创建临时目录", e);
        }
    }

    /**
     * 从分享文本中解析抖音视频信息
     *
     * @param shareUrl 包含抖音分享链接的文本
     * @return 视频信息
     * @throws Exception 解析失败时抛出异常
     */
    public DouyinVideoInfo parseShareUrl(String shareUrl) throws Exception {
        try {
            log.info("提取到分享链接: {}", shareUrl);
            // 2. 发送请求获取重定向后的URL，提取视频ID
            String videoId = extractVideoIdFromRedirect(shareUrl);
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

    /**
     * 从文本中提取抖音分享URL
     * 优先匹配抖音特定格式，如果没有找到则使用通用URL匹配
     */
    private String extractShareUrl(String shareText) {
        if (StringUtils.isBlank(shareText)) {
            return null;
        }

        // 首先尝试匹配抖音特定的URL格式
        Matcher douyinMatcher = DOUYIN_URL_PATTERN.matcher(shareText);
        if (douyinMatcher.find()) {
            return douyinMatcher.group();
        }

        // 如果没有找到抖音特定格式，则使用通用URL匹配作为备用
        Matcher generalMatcher = GENERAL_URL_PATTERN.matcher(shareText);
        if (generalMatcher.find()) {
            String url = generalMatcher.group();
            // 检查是否包含抖音相关域名
            if (url.contains("douyin.com") || url.contains("dy")) {
                return url;
            }
        }

        return null;
    }


    /**
     * 从分享URL中提取视频ID
     */
    private String extractVideoIdFromRedirect(String shareUrl) {
        try {
            log.info("开始从分享URL提取视频ID: {}", shareUrl);
            // 方法2: 发送HTTP请求跟踪重定向，然后从最终URL提取
            try {
                try (HttpResponse response = HttpRequest.get(shareUrl)
                        .header("User-Agent", USER_AGENT)
                        .timeout(10000)
                        .setFollowRedirects(true)
                        .execute()) {

                    if (response.isOk()) {
                        // 尝试从响应中获取当前URL信息
                        // 由于hutool没有直接获取最终URL的方法，我们从响应头或内容中分析
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
                }
            } catch (Exception httpEx) {
                log.warn("HTTP请求失败，使用备用方法: {}", httpEx.getMessage());
            }

            // 方法3: 如果以上都失败，使用正则表达式从原始URL中强制提取
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
    private DouyinVideoInfo parseVideoInfo(String pageContent, String videoId) throws Exception {
        try {
            // 1. 提取ROUTER_DATA
            Matcher matcher = ROUTER_DATA_PATTERN.matcher(pageContent);
            if (!matcher.find()) {
                throw new Exception("从HTML中解析视频信息失败");
            }

            String jsonData = matcher.group(1).trim();
            JsonNode rootNode = objectMapper.readTree(jsonData);

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
            JsonNode video = videoItem.get("video");
            JsonNode playAddr = video.get("play_addr");
            JsonNode urlList = playAddr.get("url_list");

            if (urlList == null || !urlList.isArray() || urlList.isEmpty()) {
                throw new Exception("未找到视频播放地址");
            }

            // 4. 获取无水印链接（将playwm替换为play）
            String videoUrl = urlList.get(0).asText().replace("playwm", "play");

            // 5. 获取视频标题
            String title = videoItem.get("desc").asText();
            if (StringUtils.isBlank(title)) {
                title = "douyin_" + videoId;
            }

            // 6. 清理文件名中的非法字符
            title = ILLEGAL_FILENAME_PATTERN.matcher(title).replaceAll("_");

            // 7. 构建返回对象
            return DouyinVideoInfo.builder()
                    .videoId(videoId)
                    .title(title)
                    .downloadUrl(videoUrl)
                    .description("视频标题: " + title)
                    .status("success")
                    .usageTip("可以直接使用此链接下载无水印视频")
                    .build();

        } catch (Exception e) {
            log.error("解析视频信息失败", e);
            throw new Exception("解析视频信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从抖音分享链接提取视频中的文本内容
     *
     * @param shareText  抖音分享链接或包含链接的文本
     * @param apiKey     语音识别API密钥
     * @param apiBaseUrl API基础URL（可选，默认使用SiliconFlow）
     * @param model      语音识别模型（可选，默认使用SenseVoiceSmall）
     * @return 提取的文本内容
     * @throws Exception 处理失败时抛出异常
     */
    public String extractDouyinText(String shareText, String apiKey, String apiBaseUrl, String model) throws Exception {
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("API密钥不能为空");
        }

        try {
            log.info("开始提取抖音视频文本内容");

            // 1. 提取分享链接
            String shareUrl = extractShareUrl(shareText);
            if (StringUtils.isBlank(shareUrl)) {
                throw new IllegalArgumentException("未找到有效的分享链接");
            }

            // 2. 解析视频信息
            log.info("正在解析抖音分享链接...");
            DouyinVideoInfo videoInfo = parseShareUrl(shareUrl);

            // 3. 下载视频
            log.info("正在下载视频...");
            Path videoPath = downloadVideo(videoInfo);

            try {
                // 4. 提取音频
                log.info("正在提取音频...");
                Path audioPath = extractAudio(videoPath);

                try {
                    // 5. 提取文本
                    log.info("正在从音频中提取文本...");
                    String textContent = extractTextFromAudio(audioPath, apiKey,
                            StringUtils.isNotBlank(apiBaseUrl) ? apiBaseUrl : DEFAULT_API_BASE_URL,
                            StringUtils.isNotBlank(model) ? model : DEFAULT_MODEL);

                    log.info("文本提取完成!");
                    return textContent;

                } finally {
                    // 清理音频文件
                    cleanupFiles(audioPath);
                }

            } finally {
                // 清理视频文件
                cleanupFiles(videoPath);
            }

        } catch (Exception e) {
            log.error("提取抖音视频文本失败", e);
            throw new Exception("提取抖音视频文本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 下载视频到临时目录
     *
     * @param videoInfo 视频信息
     * @return 下载的视频文件路径
     * @throws Exception 下载失败时抛出异常
     */
    private Path downloadVideo(DouyinVideoInfo videoInfo) throws Exception {
        String filename = videoInfo.getVideoId() + ".mp4";
        Path videoPath = tempDir.resolve(filename);

        log.info("正在下载视频: {} -> {}", videoInfo.getTitle(), videoPath);

        Request request = new Request.Builder()
                .url(videoInfo.getDownloadUrl())
                .header("User-Agent", USER_AGENT)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("下载视频失败: " + response);
            }

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("响应体为空");
            }

            long totalSize = body.contentLength();
            long downloadedSize = 0;

            try (InputStream inputStream = body.byteStream();
                 FileOutputStream outputStream = new FileOutputStream(videoPath.toFile())) {

                byte[] buffer = new byte[8192];
                int bytesRead;

                while ( (bytesRead = inputStream.read(buffer)) != -1 ) {
                    outputStream.write(buffer, 0, bytesRead);
                    downloadedSize += bytesRead;

                    if (totalSize > 0) {
                        double progress = (double) downloadedSize / totalSize * 100;
                        if (downloadedSize % (1024 * 1024) == 0) { // 每MB打印一次进度
                            log.info("下载进度: {:.1f}%", progress);
                        }
                    }
                }
            }

            log.info("视频下载完成: {}", videoPath);
            return videoPath;

        } catch (IOException e) {
            log.error("下载视频时发生错误", e);
            throw new Exception("下载视频失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从视频文件中提取音频
     *
     * @param videoPath 视频文件路径
     * @return 音频文件路径
     * @throws Exception 提取失败时抛出异常
     */
    private Path extractAudio(Path videoPath) throws Exception {
        Path audioPath = videoPath.resolveSibling(
                FileUtil.getPrefix(videoPath.getFileName().toString()) + ".mp3");

        log.info("正在从视频提取音频: {} -> {}", videoPath, audioPath);

        try {
            // 使用JAVE2进行音频提取
            File source = videoPath.toFile();
            File target = audioPath.toFile();

            // 设置音频属性
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("libmp3lame");
            audio.setBitRate(128000);
            audio.setChannels(1);
            audio.setSamplingRate(16000);

            // 设置编码属性
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("mp3");
            attrs.setAudioAttributes(audio);

            // 执行转换
            Encoder encoder = new Encoder();
            encoder.encode(new MultimediaObject(source), target, attrs);

            log.info("音频提取完成: {}", audioPath);
            return audioPath;

        } catch (EncoderException e) {
            log.error("提取音频时发生错误", e);
            throw new Exception("提取音频失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从音频文件中提取文字
     *
     * @param audioPath  音频文件路径
     * @param apiKey     API密钥
     * @param apiBaseUrl API基础URL
     * @param model      语音识别模型
     * @return 提取的文本内容
     * @throws Exception 提取失败时抛出异常
     */
    private String extractTextFromAudio(Path audioPath, String apiKey, String apiBaseUrl, String model) throws Exception {
        log.info("正在调用语音识别API提取文本...");

        try {
            File audioFile = audioPath.toFile();

            // 构建multipart请求
            RequestBody fileBody = RequestBody.create(audioFile, MediaType.parse("audio/mpeg"));
            RequestBody modelBody = RequestBody.create(model, MediaType.parse("text/plain"));

            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", audioFile.getName(), fileBody)
                    .addFormDataPart("model", model)
                    .build();

            Request request = new Request.Builder()
                    .url(apiBaseUrl)
                    .header("Authorization", "Bearer " + apiKey)
                    .post(requestBody)
                    .build();

            try (Response response = httpClient.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    throw new IOException("API调用失败: " + response);
                }

                ResponseBody body = response.body();
                if (body == null) {
                    throw new IOException("API响应体为空");
                }

                String responseText = body.string();
                log.info("API响应: {}", responseText);

                // 解析响应
                try {
                    JsonNode responseJson = objectMapper.readTree(responseText);
                    if (responseJson.has("text")) {
                        String extractedText = responseJson.get("text").asText();
                        log.info("成功提取文本内容: {}", extractedText);
                        return extractedText;
                    } else {
                        log.warn("API响应中没有找到text字段，返回原始响应");
                        return responseText;
                    }
                } catch (Exception e) {
                    log.warn("解析API响应JSON失败，返回原始响应: {}", e.getMessage());
                    return responseText;
                }
            }

        } catch (IOException e) {
            log.error("调用语音识别API时发生错误", e);
            throw new Exception("提取文字失败: " + e.getMessage(), e);
        }
    }

    /**
     * 清理指定的文件
     *
     * @param filePaths 要清理的文件路径
     */
    private void cleanupFiles(Path... filePaths) {
        for (Path filePath : filePaths) {
            if (filePath != null && Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    log.info("已清理文件: {}", filePath);
                } catch (IOException e) {
                    log.warn("清理文件失败: {}, 错误: {}", filePath, e.getMessage());
                }
            }
        }
    }

    /**
     * 清理临时目录
     */
    public void cleanup() {
        if (tempDir != null && Files.exists(tempDir)) {
            try {
                FileUtil.del(tempDir.toFile());
                log.info("已清理临时目录: {}", tempDir);
            } catch (Exception e) {
                log.warn("清理临时目录失败: {}, 错误: {}", tempDir, e.getMessage());
            }
        }
    }
}